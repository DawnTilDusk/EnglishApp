"""Generate example sentences (EN + ZH) for vocabulary_words via LLM, then write back.

Two phases, run separately so the drafts can be reviewed before they hit the DB:

    python scripts/generate_word_examples.py draft   # LLM -> docs/words/word_examples.json
    python scripts/generate_word_examples.py apply   # JSON -> public.vocabulary_words

Style is graded by difficulty_value (grade level):
  <= 13  textbook context, plain campus/family/friend scenes
  >= 14  a small memorable hook (contrast, tiny twist) while staying on-syllabus

Reuses the Management API pattern from apply_fltrp_word_books.py.
"""
from __future__ import annotations

import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env.local"
OUT_PATH = ROOT / "docs" / "words" / "word_examples.json"
PROJECT_REF = "xojbnrxnkgkqacrkcmqc"

WRITE_BATCH = 60
LLM_BATCH = 20
LLM_MODEL = "deepseek-v4-pro"
LLM_URL_DEFAULT = "https://api.deepseek.com/v1/chat/completions"
MAX_RETRY = 3


def resolve_llm_config(env: dict[str, str]) -> tuple[str, str, str]:
    """根据 env 推断模型 / URL / API Key，并识别协议族。

    返回 (llm_url, api_key, protocol)；protocol ∈ {"openai", "anthropic"}。

    优先级：
    1. 显式 URL：LLM_URL > DEEPSEEK_API_URL > ANTHROPIC_BASE_URL > OPENAI_BASE_URL；
    2. 显式 KEY：LLM_API_KEY / ANTHROPIC_API_KEY / DEEPSEEK_API_KEY / OPENAI_API_KEY；
    3. URL 含 /anthropic 或 /v1/messages → Anthropic Messages API，否则 → OpenAI Chat。
    """
    url = (
        env.get("LLM_URL")
        or env.get("DEEPSEEK_API_URL")
        or env.get("ANTHROPIC_BASE_URL")
        or env.get("OPENAI_BASE_URL")
        or ""
    ).strip().rstrip("/")
    if not url:
        url = LLM_URL_DEFAULT.rstrip("/")
    key = (
        env.get("LLM_API_KEY")
        or env.get("ANTHROPIC_API_KEY")
        or env.get("DEEPSEEK_API_KEY")
        or env.get("OPENAI_API_KEY")
        or ""
    )
    lowered = url.lower()
    if "anthrop" in lowered or "/v1/messages" in lowered:
        protocol = "anthropic"
        if not lowered.endswith("/v1/messages"):
            url = url.rstrip("/") + "/v1/messages"
    else:
        protocol = "openai"
        if not lowered.endswith("/chat/completions"):
            url = url.rstrip("/") + "/chat/completions"
    return url, key, protocol

SYSTEM_PROMPT = """你为中国初中生英语背单词 App 生成例句。每个单词产出一个英文例句和它的中文翻译。

硬性要求：
1. 例句必须包含该单词本身（原形，或 -s/-es/-ed/-ing 这类常见变形；对于不规则动词用过去式/过去分词也算），因为程序会把它挖空做情境填空题。
2. 例句长度 8-14 个词，只用同级或更简单的词汇，不要生僻词。
3. 例句必须体现给定的词性和释义，一词多义时用第一个释义。
4. 中文翻译要自然通顺，不要逐词硬译，句末用句号。
5. grade_level <= 13：贴合校园、家庭、朋友的教材语境，平实好懂。
   grade_level >= 14：在合规用词内加一个小记忆点（对比、轻微反差、具体画面），但不许生造情境。
6. 不要用引号包裹例句，不要加音标、词性标注或任何解释。

只输出 JSON 数组，每项形如：
{"word_id": "...", "example_sentence": "...", "example_translation": "..."}
不要输出 markdown 代码块，不要输出任何其他文字。"""


def normalize_english(raw: str) -> str:
    """清洗 vocabulary_words.english 字段中的噪音，返回实际要在例句中出现的「核心词」。

    处理项（按 1999 词样本中实际出现的脏数据排列）：
    - 去掉前导 '*' 等符号；去掉尾部修饰符 ˌ 等；
    - 去掉中英文括号及其内所有内容（比如不规则变形提示 shine (shone)、send（sent））；
    - 去掉前后空白；
    - 对于短语（take sb. temperature / count down / join in 等），抽取真正会出现在例句里的核心词：
        * 含 sb. / sth. 的搭配：取短语最后一个名词/动词
        * 其它短语：取第一个实义动词
    """
    if not raw:
        return ""
    s = raw.strip()
    # 前导噪音
    s = s.lstrip("*•·- ")
    # 尾部噪音：ˌ 等音标修饰符、尾标点
    s = s.rstrip("ˌ.,;:!? ")
    # 去英文括号及内容
    s = re.sub(r"\s*\([^)]*\)\s*", " ", s)
    # 去中文括号及内容
    s = re.sub(r"\s*（[^）]*）\s*", " ", s)
    s = s.strip()
    # 处理短语
    if " " in s:
        # 含 sb./sth. 的短语：取最后一个不含标点的词作为核心
        lowered = s.lower()
        if "sb." in lowered or "sth." in lowered or "somebody" in lowered or "something" in lowered:
            tokens = [t for t in s.split() if t and t not in {"sb.", "sth.", "somebody", "something"}]
            # 过滤掉 of / to / a / the 这类虚词
            stop = {"of", "to", "a", "an", "the", "for", "with"}
            meaningful = [t for t in tokens if t.lower().rstrip(".") not in stop]
            if meaningful:
                core = meaningful[-1]
                return core.rstrip(".,;:!?").strip()
        # 普通短语（count down / join in）取第一个词
        first = s.split()[0] if s.split() else ""
        return first.rstrip(".,;:!?").strip()
    return s.rstrip(".,;:!?").strip()


# 常见不规则动词「原形 -> {过去式, 过去分词, 现在分词（若特殊）}」映射，
# 至少覆盖当前 28 条 rejected 中的所有不规则；遇到新的再补充。
IRREGULAR_VERBS: dict[str, set[str]] = {
    "be": {"was", "were", "been", "being"},
    "become": {"became", "become", "becoming"},
    "begin": {"began", "begun", "beginning"},
    "break": {"broke", "broken", "breaking"},
    "bring": {"brought", "bringing"},
    "build": {"built", "building"},
    "buy": {"bought", "buying"},
    "catch": {"caught", "catching"},
    "come": {"came", "come", "coming"},
    "cost": {"cost", "costing"},
    "cut": {"cut", "cutting"},
    "do": {"did", "done", "doing"},
    "draw": {"drew", "drawn", "drawing"},
    "drink": {"drank", "drunk", "drinking"},
    "drive": {"drove", "driven", "driving"},
    "eat": {"ate", "eaten", "eating"},
    "fall": {"fell", "fallen", "falling"},
    "feel": {"felt", "feeling"},
    "find": {"found", "finding"},
    "fly": {"flew", "flown", "flying"},
    "forget": {"forgot", "forgotten", "forgetting"},
    "get": {"got", "gotten", "getting"},
    "give": {"gave", "given", "giving"},
    "go": {"went", "gone", "going"},
    "grow": {"grew", "grown", "growing"},
    "have": {"had", "having"},
    "hear": {"heard", "hearing"},
    "keep": {"kept", "keeping"},
    "know": {"knew", "known", "knowing"},
    "lay": {"laid", "laying"},
    "leave": {"left", "leaving"},
    "lend": {"lent", "lending"},
    "let": {"let", "letting"},
    "lie": {"lay", "lain", "lying"},
    "lose": {"lost", "losing"},
    "make": {"made", "making"},
    "mean": {"meant", "meaning"},
    "meet": {"met", "meeting"},
    "pay": {"paid", "paying"},
    "put": {"put", "putting"},
    "read": {"read", "reading"},
    "ride": {"rode", "ridden", "riding"},
    "ring": {"rang", "rung", "ringing"},
    "run": {"ran", "running"},
    "say": {"said", "saying"},
    "see": {"saw", "seen", "seeing"},
    "sell": {"sold", "selling"},
    "send": {"sent", "sending"},
    "set": {"set", "setting"},
    "shine": {"shone", "shined", "shining"},
    "show": {"showed", "shown", "showing"},
    "shut": {"shut", "shutting"},
    "sing": {"sang", "sung", "singing"},
    "sit": {"sat", "sitting"},
    "sleep": {"slept", "sleeping"},
    "speak": {"spoke", "spoken", "speaking"},
    "spend": {"spent", "spending"},
    "stand": {"stood", "standing"},
    "steal": {"stole", "stolen", "stealing"},
    "swim": {"swam", "swum", "swimming"},
    "take": {"took", "taken", "taking"},
    "teach": {"taught", "teaching"},
    "tell": {"told", "telling"},
    "think": {"thought", "thinking"},
    "throw": {"threw", "thrown", "throwing"},
    "understand": {"understood", "understanding"},
    "wake": {"woke", "woken", "waking"},
    "wear": {"wore", "worn", "wearing"},
    "win": {"won", "winning"},
    "write": {"wrote", "written", "writing"},
}


def _regular_variants(word: str) -> list[str]:
    """根据拼写规则派生：-s/-es/-ed/-ing 等，处理 y→ied 与双写辅音结尾。"""
    w = word
    vs: list[str] = [w]
    lower = w.lower()
    is_vowel_end = lower[-1:] in {"a", "e", "i", "o", "u"}
    last_two = lower[-2:]
    last_one = lower[-1:]
    consonants_except_y = "bcdfghjklmnpqrstvwxz"

    # 第三人称单数
    if lower.endswith(("s", "x", "z", "ch", "sh")) or lower.endswith("o"):
        vs.append(w + "es")
    else:
        vs.append(w + "s")
    # 辅音 + y → y→ies
    if last_one == "y" and len(lower) >= 2 and lower[-2] not in "aeiou":
        vs.append(w[:-1] + "ies")

    # 过去式/过去分词 ed
    vs.append(w + "ed")
    # 不发音 e 结尾 → 只加 d
    if last_one == "e":
        vs.append(w + "d")
    # 辅音 + y → y→ied
    if last_one == "y" and len(lower) >= 2 and lower[-2] not in "aeiou":
        vs.append(w[:-1] + "ied")
    # 重读闭音节（粗略：短词、末尾是「辅音+元音+辅音」且最后辅音非 w/x/y）
    if (
        len(lower) >= 3
        and last_one in consonants_except_y
        and lower[-2] in "aeiou"
        and lower[-3] in consonants_except_y
    ):
        doubled = w + last_one + "ed"
        vs.append(doubled)
        vs.append(w + last_one + "ing")

    # 现在分词 ing
    vs.append(w + "ing")
    # 不发音 e → 去 e + ing
    if last_one == "e" and not lower.endswith("ee") and not lower.endswith("ie"):
        vs.append(w[:-1] + "ing")
    # ie → y + ing
    if lower.endswith("ie"):
        vs.append(w[:-2] + "ying")

    # 比较级/最高级（主要针对形容词，对动词无害）
    vs.append(w + "r")
    vs.append(w + "st")
    if last_one == "e":
        vs.append(w + "r")
        vs.append(w + "st")
    return vs


def contains_word(sentence: str, raw_word: str) -> bool:
    """判断 sentence 是否包含 raw_word（或其常见变形）。

    流程：
    1. normalize_english 清洗 english 字段噪音，得到「核心词」core；
    2. core 为空 → 放行（避免因为字段过脏而整批丢失）；
    3. 用规则派生词形 + 查不规则动词表，得到全部候选；
    4. 任一个候选以「整词」形式在 sentence 中出现即算命中。
    """
    core = normalize_english(raw_word)
    if not core:
        return True
    candidates: set[str] = set()
    for v in _regular_variants(core):
        candidates.add(v.lower())
    # 不规则动词（以 core 为 key）
    irr = IRREGULAR_VERBS.get(core.lower())
    if irr:
        for form in irr:
            candidates.add(form)
            candidates.add(form.capitalize())
    # 把 core 本身再放一次，避免大小写被漏掉
    candidates.add(core)
    candidates.add(core.capitalize())
    candidates.add(core.lower())
    lower_sent = sentence.lower()
    pattern = re.compile(
        r"\b(" + "|".join(re.escape(c) for c in sorted(candidates, key=len, reverse=True)) + r")\b"
    )
    return bool(pattern.search(lower_sent))


def load_env() -> dict[str, str]:
    values: dict[str, str] = {}
    if ENV_FILE.exists():
        for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            values[key.strip()] = value.strip().strip('"').strip("'")
    for key in (
        "SUPABASE_ACCESS_TOKEN",
        "SUPABASE_URL",
        "ANTHROPIC_API_KEY",
        "DEEPSEEK_API_KEY",
        "OPENAI_API_KEY",
        "DEEPSEEK_API_URL",
        "OPENAI_BASE_URL",
        "ANTHROPIC_BASE_URL",
        "LLM_URL",
        "LLM_API_KEY",
    ):
        values.setdefault(key, os.environ.get(key, ""))
    return values


def project_ref(env: dict[str, str]) -> str:
    ref = PROJECT_REF
    url = env.get("SUPABASE_URL", "")
    if "://" in url:
        host = url.split("://", 1)[1].split("/", 1)[0]
        if host.endswith(".supabase.co"):
            ref = host.split(".")[0]
    return ref


def run_sql(token: str, ref: str, sql: str) -> object:
    body = json.dumps({"query": sql}).encode("utf-8")
    req = urllib.request.Request(
        f"https://api.supabase.com/v1/projects/{ref}/database/query",
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "Mozilla/5.0 SeedieExampleGen/1.0",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {e.code}: {err}") from e


def sql_str(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def fetch_pending(token: str, ref: str) -> list[dict]:
    sql = """
SELECT w.word_id, w.english, w.part_of_speech, w.translation,
       COALESCE(w.difficulty_value, b.grade_level, 13) AS grade_level
FROM public.vocabulary_words w
LEFT JOIN public.word_books b ON b.book_id = w.book_id
WHERE w.example_sentence IS NULL OR w.example_sentence = ''
   OR w.example_translation IS NULL OR w.example_translation = ''
ORDER BY w.book_id, w.sort_order;
"""
    rows = run_sql(token, ref, sql)
    return rows if isinstance(rows, list) else []


def call_llm(
    api_key: str,
    words: list[dict],
    *,
    llm_url: str,
    protocol: str,
) -> list[dict]:
    payload = [
        {
            "word_id": w["word_id"],
            "english": w["english"],
            "part_of_speech": w.get("part_of_speech") or "",
            "translation": w.get("translation") or "",
            "grade_level": w.get("grade_level") or 13,
        }
        for w in words
    ]
    user_content = json.dumps(payload, ensure_ascii=False)
    common_headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        ),
    }
    if protocol == "anthropic":
        body_obj = {
            "model": LLM_MODEL,
            "max_tokens": 4096,
            "system": SYSTEM_PROMPT,
            "messages": [{"role": "user", "content": user_content}],
        }
        headers = {
            **common_headers,
            "x-api-key": api_key,
            "anthropic-version": "2023-06-01",
        }
    else:
        body_obj = {
            "model": LLM_MODEL,
            "max_tokens": 4096,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_content},
            ],
        }
        headers = {
            **common_headers,
            "Authorization": f"Bearer {api_key}",
        }
    body = json.dumps(body_obj, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(llm_url, data=body, method="POST", headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {e.code}: {detail}") from e
    if protocol == "anthropic":
        blocks = data.get("content") or []
        parts: list[str] = []
        for blk in blocks:
            if isinstance(blk, dict) and blk.get("type") == "text":
                parts.append(blk.get("text") or "")
        text = "".join(parts).strip()
    else:
        text = ((data.get("choices") or [{}])[0].get("message") or {}).get("content") or ""
        text = text.strip()
    if text.startswith("```"):
        text = re.sub(r"^```[a-zA-Z]*\n?", "", text)
        text = re.sub(r"\n?```$", "", text).strip()
    return json.loads(text)


def draft(env: dict[str, str]) -> None:
    llm_url, api_key, protocol = resolve_llm_config(env)
    if not api_key:
        sys.exit(
            "LLM API key missing; put ANTHROPIC_API_KEY / DEEPSEEK_API_KEY / "
            "OPENAI_API_KEY / LLM_API_KEY in .env.local or the environment"
        )
    print(f"using LLM: {LLM_MODEL}  url={llm_url}  protocol={protocol}")
    token = env.get("SUPABASE_ACCESS_TOKEN", "")
    if not token:
        sys.exit("SUPABASE_ACCESS_TOKEN missing")
    ref = project_ref(env)

    pending = fetch_pending(token, ref)
    print(f"pending words: {len(pending)}")
    if not pending:
        return

    done: dict[str, dict] = {}
    if OUT_PATH.exists():
        for item in json.loads(OUT_PATH.read_text(encoding="utf-8")):
            done[item["word_id"]] = item
        print(f"resuming, already drafted: {len(done)}")

    todo = [w for w in pending if w["word_id"] not in done]
    skipped: list[str] = []
    consecutive_failures = 0

    for start in range(0, len(todo), LLM_BATCH):
        chunk = todo[start : start + LLM_BATCH]
        by_id = {w["word_id"]: w for w in chunk}
        for attempt in range(1, MAX_RETRY + 1):
            try:
                results = call_llm(
                    api_key,
                    chunk,
                    llm_url=llm_url,
                    protocol=protocol,
                )
                break
            except Exception as exc:  # noqa: BLE001 - network/parse, retry then skip
                print(f"  batch {start} attempt {attempt} failed: {exc}")
                if attempt == MAX_RETRY:
                    results = []
                else:
                    time.sleep(3 * attempt)

        if results:
            consecutive_failures = 0
        else:
            consecutive_failures += 1
            if consecutive_failures >= 2:
                sys.exit(
                    f"\n连续 {consecutive_failures} 批全部失败，已停止，避免把剩余 "
                    f"{len(todo) - start} 个词全撞一遍。请先按上面的报错修好接口，"
                    f"已生成的 {len(done)} 条留在 {OUT_PATH}，重跑会自动续。"
                )

        for item in results:
            wid = item.get("word_id")
            src = by_id.get(wid)
            sentence = (item.get("example_sentence") or "").strip()
            translation = (item.get("example_translation") or "").strip()
            if not src or not sentence or not translation:
                continue
            if not contains_word(sentence, src["english"]):
                skipped.append(f"{wid} ({src['english']}): {sentence}")
                continue
            done[wid] = {
                "word_id": wid,
                "english": src["english"],
                "example_sentence": sentence,
                "example_translation": translation,
            }

        OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
        OUT_PATH.write_text(
            json.dumps(sorted(done.values(), key=lambda x: x["word_id"]), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        print(f"  {min(start + LLM_BATCH, len(todo))}/{len(todo)} drafted, saved {len(done)}")

    print(f"\ndrafted {len(done)} -> {OUT_PATH}")
    if skipped:
        print(f"rejected {len(skipped)} (target word absent from sentence):")
        for line in skipped[:20]:
            print(f"  {line}")
        print("re-run draft to retry the rejected ones")


def apply(env: dict[str, str]) -> None:
    token = env.get("SUPABASE_ACCESS_TOKEN", "")
    if not token:
        sys.exit("SUPABASE_ACCESS_TOKEN missing")
    if not OUT_PATH.exists():
        sys.exit(f"{OUT_PATH} not found; run draft first")
    ref = project_ref(env)

    items = json.loads(OUT_PATH.read_text(encoding="utf-8"))
    print(f"applying {len(items)} rows...")

    for start in range(0, len(items), WRITE_BATCH):
        chunk = items[start : start + WRITE_BATCH]
        values = ",\n".join(
            f"({sql_str(i['word_id'])}, {sql_str(i['example_sentence'])}, {sql_str(i['example_translation'])})"
            for i in chunk
        )
        sql = f"""
UPDATE public.vocabulary_words AS w
SET example_sentence = v.sentence,
    example_translation = v.translation
FROM (VALUES
{values}
) AS v(word_id, sentence, translation)
WHERE w.word_id = v.word_id;
"""
        run_sql(token, ref, sql)
        print(f"  {min(start + WRITE_BATCH, len(items))}/{len(items)}")

    left = run_sql(
        token,
        ref,
        "SELECT COUNT(*) AS n FROM public.vocabulary_words "
        "WHERE example_sentence IS NULL OR example_sentence = '';",
    )
    print(f"still missing example_sentence: {left}")


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if mode not in {"draft", "apply"}:
        sys.exit(__doc__)
    env = load_env()
    if mode == "draft":
        draft(env)
    else:
        apply(env)


if __name__ == "__main__":
    main()
