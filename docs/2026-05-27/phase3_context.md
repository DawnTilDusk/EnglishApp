# Phase 3 上下文速查（2026-05-27）

> 供下次对话快速恢复 Phase 3 的关键设计决策与实现细节。

## 一、Supabase 云端结构

### 三张公开只读表

```
public.word_books
  book_id TEXT PK | title | description | language | difficulty | version
  word_count | cover_url | updated_at BIGINT

public.word_book_modules
  module_id TEXT PK | book_id FK→word_books CASCADE | title | sort_order | word_count

public.vocabulary_words
  word_id TEXT PK | book_id FK→word_books CASCADE | module_id FK→word_book_modules
  english | phonetic | part_of_speech | translation | example_sentence
  difficulty_level | reward_token | estimated_duration_sec | sort_order | audio_url
```

### Storage
- Bucket：`word-audio`（PUBLIC）
- 音频路径：`word-audio/{book_id}/{word_id}.mp3`

## 二、本地 Room v6 结构变更

### 新增表
- `word_book_modules`（对应 `WordBookModuleEntity`）

### 修改表
- `word_books` 新增列：`coverUrl TEXT`
- `vocabulary_words` 新增列：`moduleId TEXT`、`audioUrl TEXT`、新增 moduleId 索引

### 迁移路径
```
v1→v2→v3→v4→v5→v6（当前）
MIGRATION_5_6 已在 SeedieDatabaseMigrations.kt 实现并注册
```

## 三、关键设计决策

| 决策 | 结论 | 原因 |
|------|------|------|
| 词书数据组织 | 单张 vocabulary_words 表，bookId 区分 | 标准关系型设计；同词可在多书出现内容不同 |
| Module 游标 | 全书 sort_order 连续，不分 Module 重置 | 学习游标只有一个，穿越模块边界自然推进 |
| 音频加载策略 | ExoPlayer 流式播放 + 自动本地缓存 | 整体预下载体积过大（500词×50KB=25MB） |
| 版本管理 | 本地 version vs 云端 version，高则全量重下 | 词书内容更新时确保用户获取最新数据 |
| 静态词包 fallback | 有已下载词书时跳过静态词包；无词书时兜底 | 避免词书未下载时无词可学导致崩溃 |
| 词书选择入口 | Profile 页（暂定，后续可调整）| 用户提供，非永久决策 |

## 四、词书数据格式约定

### word_id 命名规则
```
{book_id}-{4位序号}
示例：oxford3000-0001, oxford3000-0002
```
**不用单词本身作 ID**（同词可在多本书出现，word_id 是主键不能重复）

### sort_order 规则
- 全书统一连续递增（1, 2, 3 ... N）
- **不按 Module 重置**，Module 1 的最后一词是 50，Module 2 从 51 开始

### CSV 模板位置
```
docs/templates/template_books.csv
docs/templates/template_modules.csv
docs/templates/template_words.csv
```

## 五、关键文件路径（Phase 3 新增/修改）

| 文件 | 状态 | 说明 |
|------|------|------|
| `data/local/entity/WordBookModuleEntity.kt` | ✅ 新建 | Module 实体 |
| `data/local/entity/WordBookEntity.kt` | ✅ 修改 | 新增 coverUrl |
| `data/local/entity/VocabularyWordEntity.kt` | ✅ 修改 | 新增 moduleId, audioUrl |
| `data/local/dao/WordBookModuleDao.kt` | ✅ 新建 | Module DAO |
| `data/local/dao/WordBookDao.kt` | ✅ 修改 | 新增多个查询方法 |
| `data/local/dao/VocabularyWordDao.kt` | ✅ 修改 | 新增 deleteByBook, getCount |
| `data/local/SeedieDatabaseMigrations.kt` | ✅ 修改 | 新增 MIGRATION_5_6 |
| `data/local/SeedieDatabase.kt` | ✅ 修改 | v5→v6，注册新 Entity/DAO |
| `di/DatabaseModule.kt` | ✅ 修改 | 注册 MIGRATION_5_6 和新 DAO |
| `data/remote/Models.kt` | ⬜ 待改 | 新增三个 Supabase 数据类 |
| `data/remote/WordBookRemoteDataSource.kt` | ⬜ 待建 | 网络拉取逻辑 |
| `data/sync/syncer/WordBookSyncer.kt` | ⬜ 待建 | 下载器 |
| `ui/screens/profile/wordbookselection/` | ⬜ 待建 | 词书选择页 |

## 六、下次对话的起点

**如果词书数据已导入 Supabase**：直接从 Step 4（Remote Models）开始动手

**如果词书数据尚未导入**：也可先做 Step 4-6 代码，数据就绪后联调

下次对话提示语建议：
> 读取 docs/2026-05-27/phase3_context.md，继续做 Phase 3 的 Step 4（Remote Models + 网络层）
