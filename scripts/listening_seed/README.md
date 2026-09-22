# 原创听力扩充包：2026-09-22

本批次是为 Seedie 编写的原创练习，不是历年真题或网络转载。

| 年级 | 组数 | 题数 | 主题 |
| --- | --- | --- | --- |
| 七年级 | 4 | 12 | 阅读小组、失物招领、野餐安排、水果购物 |
| 八年级 | 4 | 12 | 博物馆出行、晚间习惯、校园菜园、图书交换 |
| 九年级 | 4 | 12 | 维修工坊、交通选择、调查样本、食物浪费 |

每组包含英文原文、中文翻译、3 道四选一题、唯一正确答案及中文解析。音频使用本机 F5-TTS 和项目已有参考素材合成，属于单人短文/通知，不伪装成双人对话。

## 文件

- `original_20260922.json`：内容源文件；`grade` 为 7/8/9。
- `original_20260922.sql`：事务式、只追加的导入脚本。ID 带批次，不覆盖旧题。包含仓库既有 042 更新中的可选中文字段定义。
- `../../EngResource/listening_original_20260922/`：12 段 WAV、12 段 MP3、音频哈希及转写核对结果。本目录被现有 `.gitignore` 排除，但文件已保存在本机。
- `../../EngResource/listening_original_20260922/review.md`：方便逐题审阅的文本版本。

数据库原有 `grade_level` 使用学期编码（13/14=七年级，15/16=八年级，17/18=九年级）。本批次分别写入 13/15/17，`book_id` 留空，作为自主听力练习。

## 验证与限制

- 内容结构：12 组、36 题、144 选项；题号唯一，每题答案均在选项中。
- 音频使用本机 Whisper base.en 自动转写并对照文本，另核对答题所需的信息；这不等同于人工逐句听审。
- 目录回归测试覆盖新增材料可见、教材与无题材料排除、排序、完成标记及标题回退。
- 已经用户授权，将 12 段音频发布到公开 `audio-pep/original/20260922/`，逐段下载并通过 SHA-256 校验；12 组、36 题、144 选项已通过 Data API 导入并逐字段回读确认，客户端公开读取也已验证。结果见 `../../EngResource/listening_original_20260922/import_verification.json`。
- 云端尚无 `transcript_zh` 字段，现有管理令牌返回 401，因此中文原文翻译目前只保存在本地内容包；英文原文、题目、答案、中文解析和音频均已入库。恢复管理权限后可应用既有 042 字段更新，再单独按本批次 ID 补写翻译。两个导入脚本均只追加，重复执行不会给已有记录补写翻译。
- 原文与题目是原创内容；语音仍沿用既有 F5-TTS 模型和参考音频，其使用条件不会因题目原创而改变。

## 复用命令

在仓库根目录，使用已有 `f5-tts` 环境的 Python：

```powershell
python scripts/prepare_listening_pack.py
python scripts/prepare_listening_pack.py --generate
python scripts/check_listening_audio.py
python scripts/prepare_listening_pack.py --sql
```

本批次已获公开发布授权。上传和 Data API 导入命令：

```powershell
python scripts/prepare_listening_pack.py --upload --sql
python scripts/import_listening_pack_rest.py
```

上传使用现有公开 `audio-pep` 桶内的独立 `original/20260922/` 目录，文件名带内容哈希，不覆盖教材音频。每个文件上传后会通过公开 URL 下载并核对 SHA-256。Data API 导入使用本地服务端凭据，先暂存材料，确认题目与选项完整后再加入自主练习目录，并回读确认各字段和客户端读取权限。`import_listening_pack.py --apply` 是另一个需要有效管理 API 凭据的 SQL 导入入口，本次未使用。

重新编译 App 后，听力自主练习目录会显示有题目的非教材材料；仅更新数据库不会改变旧安装包中只显示 `l08-01` 的筛选逻辑。
