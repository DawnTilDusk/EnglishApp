import { ConsoleShell } from "@/components/ConsoleShell";
import { requireAssignedStudent } from "@/lib/teacher-student";
import Link from "next/link";

type Section = "overview" | "checkins" | "vocab" | "economy";

const SECTIONS: { id: Section; label: string }[] = [
  { id: "overview", label: "概览" },
  { id: "checkins", label: "签到" },
  { id: "vocab", label: "词书" },
  { id: "economy", label: "流水" },
];

function parseSection(raw: string | undefined): Section {
  if (raw === "checkins" || raw === "vocab" || raw === "economy") return raw;
  return "overview";
}

function formatEpochMs(value: number | null | undefined): string {
  if (value == null || Number.isNaN(Number(value))) return "—";
  const ms = Number(value);
  // App stores epoch millis; guard against accidental seconds.
  const date = new Date(ms < 1e12 ? ms * 1000 : ms);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("zh-CN");
}

export default async function TeacherStudentDetailPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ section?: string }>;
}) {
  const { id } = await params;
  const { section: sectionRaw } = await searchParams;
  const section = parseSection(sectionRaw);
  const { profile, student, supabase } = await requireAssignedStudent(id);

  const sectionHref = (s: Section) =>
    s === "overview"
      ? `/teacher/students/${id}`
      : `/teacher/students/${id}?section=${s}`;

  let stats: Record<string, unknown> | null = null;
  let statsError: string | null = null;
  let checkIns: Array<{
    date: string;
    is_checked_in: boolean;
    study_time_minutes: number;
  }> = [];
  let checkInsError: string | null = null;
  let bookProgress: Array<{
    book_id: string;
    learned_word_count: number;
    next_word_sort_order_cursor: number;
    active_round_id: string | null;
    updated_at: number;
  }> = [];
  let studyRounds: Array<{
    round_id: string;
    book_id: string;
    status: string;
    target_word_count: number;
    introduced_word_count: number;
    mastered_word_count: number;
    updated_at: number;
  }> = [];
  let vocabError: string | null = null;
  let transactions: Array<{
    id: string;
    amount: number;
    reason: string;
    ref_id: string | null;
    created_at: string | null;
  }> = [];
  let economyError: string | null = null;

  if (section === "overview" || section === "economy") {
    const { data, error } = await supabase.rpc("get_teacher_student_stats", {
      p_student_id: id,
    });
    if (error) statsError = error.message;
    else stats = (data ?? null) as Record<string, unknown> | null;
  }

  if (section === "checkins") {
    const { data, error } = await supabase
      .from("user_check_ins")
      .select("date, is_checked_in, study_time_minutes")
      .eq("user_id", id)
      .order("date", { ascending: false })
      .limit(90);
    if (error) checkInsError = error.message;
    else checkIns = data ?? [];
  }

  if (section === "vocab") {
    const userIdText = id;
    const [bookRes, roundsRes] = await Promise.all([
      supabase
        .from("user_vocabulary_book_progress")
        .select(
          "book_id, learned_word_count, next_word_sort_order_cursor, active_round_id, updated_at"
        )
        .eq("user_id", userIdText)
        .order("updated_at", { ascending: false })
        .limit(50),
      supabase
        .from("user_vocabulary_study_rounds")
        .select(
          "round_id, book_id, status, target_word_count, introduced_word_count, mastered_word_count, updated_at"
        )
        .eq("user_id", userIdText)
        .order("updated_at", { ascending: false })
        .limit(50),
    ]);
    if (bookRes.error || roundsRes.error) {
      vocabError = bookRes.error?.message ?? roundsRes.error?.message ?? null;
    } else {
      bookProgress = bookRes.data ?? [];
      studyRounds = roundsRes.data ?? [];
    }
  }

  if (section === "economy") {
    const { data, error } = await supabase
      .from("user_economy_transactions")
      .select("id, amount, reason, ref_id, created_at")
      .eq("user_id", id)
      .order("created_at", { ascending: false })
      .limit(100);
    if (error) economyError = error.message;
    else transactions = data ?? [];
  }

  return (
    <ConsoleShell
      profile={profile}
      title={`学生 · ${student.name}`}
      tabs={[
        { href: "/teacher", label: "学生", active: true },
        { href: "/teacher/shop", label: "商城（只读）" },
        { href: "/teacher/orders", label: "订单（只读）" },
      ]}
    >
      <div className="stack">
        <div className="tabs">
          {SECTIONS.map((s) => (
            <Link
              key={s.id}
              href={sectionHref(s.id)}
              className={section === s.id ? "active" : undefined}
            >
              {s.label}
            </Link>
          ))}
        </div>

        {section === "overview" && (
          <div className="card stack">
            {statsError && <p className="error">{statsError}</p>}
            <div>
              <div className="muted">学号</div>
              <div>{student.student_no || "—"}</div>
            </div>
            <div>
              <div className="muted">班级</div>
              <div>{student.class_id || "—"}</div>
            </div>
            {stats && (
              <div className="row">
                <div className="card" style={{ flex: 1 }}>
                  <div className="muted">签到天数</div>
                  <strong>{String(stats.total_check_ins ?? 0)}</strong>
                </div>
                <div className="card" style={{ flex: 1 }}>
                  <div className="muted">学习分钟</div>
                  <strong>{String(stats.total_study_minutes ?? 0)}</strong>
                </div>
                <div className="card" style={{ flex: 1 }}>
                  <div className="muted">已学词数</div>
                  <strong>{String(stats.learned_word_count ?? 0)}</strong>
                </div>
                <div className="card" style={{ flex: 1 }}>
                  <div className="muted">代币余额</div>
                  <strong>{String(stats.token_balance ?? 0)}</strong>
                </div>
              </div>
            )}
            <p className="muted" style={{ margin: 0 }}>
              仅展示 App 已同步到云端的数据。
            </p>
          </div>
        )}

        {section === "checkins" && (
          <div className="card stack">
            {checkInsError && <p className="error">{checkInsError}</p>}
            <table>
              <thead>
                <tr>
                  <th>日期</th>
                  <th>签到</th>
                  <th>学习分钟</th>
                </tr>
              </thead>
              <tbody>
                {checkIns.map((row) => (
                  <tr key={row.date}>
                    <td>{row.date}</td>
                    <td>{row.is_checked_in ? "是" : "否"}</td>
                    <td>{row.study_time_minutes}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!checkInsError && checkIns.length === 0 && (
              <p className="muted">暂无签到记录。</p>
            )}
          </div>
        )}

        {section === "vocab" && (
          <div className="stack">
            {vocabError && (
              <div className="card">
                <p className="error">{vocabError}</p>
              </div>
            )}
            <div className="card stack">
              <h2 style={{ marginTop: 0 }}>词书进度</h2>
              <table>
                <thead>
                  <tr>
                    <th>词书</th>
                    <th>已学词数</th>
                    <th>光标</th>
                    <th>活跃轮次</th>
                    <th>更新时间</th>
                  </tr>
                </thead>
                <tbody>
                  {bookProgress.map((row) => (
                    <tr key={row.book_id}>
                      <td>{row.book_id}</td>
                      <td>{row.learned_word_count}</td>
                      <td>{row.next_word_sort_order_cursor}</td>
                      <td>{row.active_round_id || "—"}</td>
                      <td>{formatEpochMs(row.updated_at)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!vocabError && bookProgress.length === 0 && (
                <p className="muted">暂无词书进度。</p>
              )}
            </div>
            <div className="card stack">
              <h2 style={{ marginTop: 0 }}>近期学习轮次</h2>
              <table>
                <thead>
                  <tr>
                    <th>轮次</th>
                    <th>词书</th>
                    <th>状态</th>
                    <th>目标</th>
                    <th>已引入</th>
                    <th>已掌握</th>
                    <th>更新时间</th>
                  </tr>
                </thead>
                <tbody>
                  {studyRounds.map((row) => (
                    <tr key={row.round_id}>
                      <td>{row.round_id}</td>
                      <td>{row.book_id}</td>
                      <td>{row.status}</td>
                      <td>{row.target_word_count}</td>
                      <td>{row.introduced_word_count}</td>
                      <td>{row.mastered_word_count}</td>
                      <td>{formatEpochMs(row.updated_at)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!vocabError && studyRounds.length === 0 && (
                <p className="muted">暂无学习轮次。</p>
              )}
            </div>
          </div>
        )}

        {section === "economy" && (
          <div className="card stack">
            {statsError && <p className="error">{statsError}</p>}
            {economyError && <p className="error">{economyError}</p>}
            <div>
              <div className="muted">代币余额</div>
              <strong>{String(stats?.token_balance ?? 0)}</strong>
            </div>
            <table>
              <thead>
                <tr>
                  <th>时间</th>
                  <th>变动</th>
                  <th>原因</th>
                  <th>ref_id</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((row) => (
                  <tr key={row.id}>
                    <td>
                      {row.created_at
                        ? new Date(row.created_at).toLocaleString("zh-CN")
                        : "—"}
                    </td>
                    <td>{row.amount}</td>
                    <td>{row.reason}</td>
                    <td>{row.ref_id || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!economyError && transactions.length === 0 && (
              <p className="muted">暂无代币流水。</p>
            )}
          </div>
        )}
      </div>
    </ConsoleShell>
  );
}
