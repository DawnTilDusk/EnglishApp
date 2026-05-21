# Claude Project Context — EnglishApp (Seedie)

> 本文件供 Claude Code 在新对话中快速恢复上下文使用。最后更新：2026-05-19（第二次）

---

## 项目基本信息

- **应用名**: Seedie（英语学习 App）
- **包名**: `com.example.seedie`
- **Git 用户**: Carlos-L
- **分支策略**: `main`（生产） ← `develop`（集成） ← `feature/*`
- **当前活跃分支**: `develop`

---

## 架构概览

**单模块 Clean Architecture**，三层划分：

```
app/src/main/java/com/example/seedie/
├── data/
│   ├── local/          # Room 数据库（v3），entity/ + dao/
│   ├── remote/         # Supabase 客户端、AuthService、Models
│   └── repository/     # Repository 实现
├── domain/
│   ├── model/          # StudyResult, UserSessionState, RewardEvent
│   ├── repository/     # Repository 接口
│   └── usecase/        # GardenEngine, RewardEventBus
├── ui/
│   ├── navigation/     # Screen, SeedieNavHost, SeedieNavGraph
│   ├── screens/        # splash / login / main / dashboard / learning / garden / profile
│   ├── components/     # BottomNavigationBar, CustomIndicatorPanel
│   └── theme/          # Color, Type, Shape, Theme, ModifierExt
└── di/                 # AppModule, DatabaseModule, RepositoryModule, DispatcherModule
```

**核心技术栈**：
- UI: Jetpack Compose (BOM 2025.01.00)
- DI: Hilt 2.59
- 本地 DB: Room 2.8.3（`seedie_database`，当前版本 v3）
- 云端: Supabase 3.5.0（Auth + Postgrest），区域 `xojbnrxnkgkqacrkcmqc.supabase.co`
- 网络: Ktor Client 3.4.2
- 异步: Coroutines + StateFlow
- 本地存储: DataStore（JWT Token、设备 ID）

---

## 本地数据库表清单（Room v5）

| 表名 | Entity | 主键 | syncStatus | 说明 |
|------|--------|------|------------|------|
| `check_ins` | CheckInEntity | (userId, date) | ✅ | 签到热力图 |
| `daily_tasks` | DailyTaskEntity | — | ❌ 不同步 | 每日任务 |
| `economy_transactions` | EconomyTransactionEntity | id (UUID String) | ✅ | 代币流水 |
| `garden_plots` | GardenPlotEntity | (userId, plotIndex) | ✅ | 花园 4×4 地块 |
| `vocabulary_words` | VocabularyWordEntity | wordId | ❌ 只读下载 | 单词库 |
| `word_books` | WordBookEntity | bookId | ❌ 只读下载 | 词书元数据 |
| `vocabulary_book_progress` | VocabularyBookProgressEntity | (userId, bookId) | ✅ | 词书级进度 |
| `vocabulary_study_rounds` | VocabularyStudyRoundEntity | roundId | ✅ | 学习轮次 |
| `vocabulary_study_round_words` | VocabularyStudyRoundWordEntity | (roundId, wordId) | ❌ 不同步 | 轮次-单词关联（临时数据） |
| `vocabulary_word_learning_progress` | VocabularyWordLearningProgressEntity | (userId, bookId, wordId) | ✅ | 单词级学习进度 |
| `sync_operations` | SyncOperationEntity | operationId (UUID) | — | 离线操作队列 |

**迁移历史**：v1→v2（词书表）→ v3（进度/轮次表）→ v4（全表加 userId，Economy PK 改 UUID）→ v5（加 sync_operations 表）

---

## Supabase 云端现状

### 既有表（勿动）

| 表 | 用途 | 归属 |
|---|---|---|
| `profiles` | 用户角色（student/teacher/admin）、设备 ID | 认证系统 |
| `students` | 学生姓名、机构 ID | 旧系统 |
| `agencies` | 机构信息 | 旧系统 |
| `points_ledger` | 旧积分流水（agency/student 模型，与 App 不兼容）| 旧系统 |
| `study_events` | 旧学习事件（JSONB，与 App 不兼容）| 旧系统 |
| `content_items` / `content_assignments` / `rewards` / `redemptions` | 旧系统内容管理 | 旧系统 |

> ⚠️ 旧系统表使用 `agency_id + student_id` 模型，与 App 的 `auth.uid()` 模型不兼容，Phase 4 之前均不涉及。

### 新建同步表（Phase 1-2 已完成）

| Supabase 表 | 对应本地表 | RLS 策略 | 状态 |
|---|---|---|---|
| `user_check_ins` | `check_ins` | `auth.uid() = user_id` | ✅ 已建，已验证 |
| `user_vocabulary_word_learning_progress` | `vocabulary_word_learning_progress` | `auth.uid() = user_id` | ✅ 已建，已验证 |
| `user_vocabulary_study_rounds` | `vocabulary_study_rounds` | `auth.uid() = user_id` | ✅ 已建，已验证 |
| `user_vocabulary_book_progress` | `vocabulary_book_progress` | `auth.uid() = user_id` | ✅ 已建，已验证 |
| `word_books` | `word_books` | 公开只读 | ⬜ Phase 3 |
| `vocabulary_words` | `vocabulary_words` | 公开只读 | ⬜ Phase 3 |
| `user_economy_transactions` | `economy_transactions` | 只能 INSERT | ⬜ Phase 4 |
| `user_garden_plots` | `garden_plots` | `auth.uid() = user_id` | ⬜ Phase 4 |

---

## 2026-05-19 讨论：Room ↔ Supabase 同步方案

### 已确定的数据分类

| 类型 | 表 | 触发时机 | 冲突策略 |
|------|---|---------|---------|
| **A 只读下载** | `word_books`, `vocabulary_words` | 启动/按需 | Server wins，version 对比 |
| **B 变化时上传** | `check_ins`, `vocabulary_word_learning_progress`, `vocabulary_study_rounds`, `vocabulary_book_progress` | 学习单元结束/签到后 | Last-write-wins by updatedAt |
| **C 追加上传** | `economy_transactions` | 定时批量（30min） | Append-only，无冲突 |
| **D 双向同步** | `garden_plots` | 启动拉取 + 本地变化后上传 | Server wins，本地乐观更新 |
| **E 不同步** | `vocabulary_study_round_words`, `daily_tasks` | — | 本地临时数据 |
| **F 未来扩展** | 成就、听力进度、口语记录、通知 | — | 预留 SyncManager 接口 |

### 同步基础架构（已设计，待实现）

**1. 前置改造（Phase 0，必须先做）**
- 所有需要同步的 Entity 加 `userId: String` 字段
- `EconomyTransactionEntity` PK 改为 UUID String（当前 autoGenerate Int 多设备必碰撞）
- Room Migration v3 → v4
- Supabase 建立用户数据表 + RLS（`auth.uid() = user_id`）

**2. 新增 Room 表：`sync_operations`（离线队列）**
```kotlin
SyncOperationEntity(
    operationId: String,   // UUID
    userId: String,
    tableName: String,
    operationType: String, // UPSERT | DELETE
    payload: String,       // JSON
    createdAt: Long,
    retryCount: Int,
    status: String         // PENDING | FAILED
)
```

**3. 脏标记（Type B/C Entity 新增字段）**
```kotlin
val syncStatus: String = "PENDING"  // PENDING | SYNCED | FAILED
val syncedAt: Long? = null
```

**4. SyncManager 接口**
```kotlin
interface SyncManager {
    suspend fun syncNow(scope: SyncScope): SyncResult
    fun enqueue(operation: SyncOperation)
    fun observeSyncState(): Flow<SyncState>
}
enum class SyncScope { ALL, CHECK_IN, LEARNING_PROGRESS, GARDEN, ECONOMY, VOCAB_DOWNLOAD }
```

**5. WorkManager 任务**
- `ProgressSyncWorker`：PERIODIC 15min
- `EconomySyncWorker`：PERIODIC 30min
- `VocabDownloadWorker`：ONE_TIME 按需
- `GardenSyncWorker`：ONE_TIME App 前台时

**6. NetworkConnectivityObserver**
- 监听网络恢复事件，自动消费 `sync_operations` 队列

### Supabase 需新建的表

| Supabase 表 | 对应本地表 | RLS |
|---|---|---|
| `user_check_ins` | `check_ins` | uid = user_id |
| `user_learning_progress` | `vocabulary_word_learning_progress` | uid = user_id |
| `user_study_rounds` | `vocabulary_study_rounds` | uid = user_id |
| `user_book_progress` | `vocabulary_book_progress` | uid = user_id |
| `user_economy_transactions` | `economy_transactions` | 只能 INSERT |
| `user_garden_plots` | `garden_plots` | uid = user_id |

### 实施阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 0 | Entity 加 userId；economy PK 改 UUID；Migration v3→v4；全部 DAO 加 userId 过滤 | ✅ 完成并验证 |
| Phase 1 | sync_operations 表；SyncManager；NetworkObserver；CheckInSyncer；SplashViewModel；check_ins 全链路验证 | ✅ 完成并验证 |
| Phase 2 | 三张进度表 pending 查询 + updateSyncStatus；VocabularyProgressSyncer；Supabase 建3张表+RLS | ✅ 完成并验证 |
| Phase 3 | word_books 定时拉取；vocabulary_words 按需下载 | 🔄 进行中 |
| Phase 4 | economy_transactions 批量上传；garden_plots 双向同步 | ⬜ 待开始 |
| Phase 5 | 成就/听力/口语等未来表接入 SyncManager | ⬜ 待开始 |

---

## 词汇学习核心逻辑（备忘）

- 活跃队列 4 词，本轮目标 10 词
- 学习模式：首次见词→英文选中文；后续→随机关卡；连通 3 关 = 掌握
- 复习模式：拼写输入，5 秒倒计时提示首字母
- 断点续学：每次操作后自动保存 `VocabularyPracticeResumeSnapshot`
- 核心 ViewModel：`VocabularyPracticeViewModel`（1000+ 行）

---

## 同步基础架构文件（Phase 1-2 新增）

| 用途 | 路径 |
|------|------|
| 同步管理接口 + SyncScope + SyncResult | `data/sync/SyncManager.kt` |
| 同步管理实现（网络恢复自动触发） | `data/sync/SyncManagerImpl.kt` |
| 网络连通性观察者 | `data/sync/NetworkConnectivityObserver.kt` |
| 签到同步器 | `data/sync/syncer/CheckInSyncer.kt` |
| 词汇进度同步器（3张表） | `data/sync/syncer/VocabularyProgressSyncer.kt` |
| 离线队列 Entity | `data/local/entity/SyncOperationEntity.kt` |
| 离线队列 DAO | `data/local/dao/SyncOperationDao.kt` |
| 同步 DI 模块 | `di/SyncModule.kt` |
| 签到签到页 ViewModel | `ui/screens/splash/SplashViewModel.kt` |

## 关键文件路径

| 用途 | 路径 |
|------|------|
| Supabase 客户端 | `data/remote/SupabaseClient.kt` |
| 认证服务 | `data/remote/AuthService.kt` |
| 远端数据模型 | `data/remote/Models.kt` |
| Room 数据库 | `data/local/SeedieDatabase.kt` |
| 数据库迁移 | `data/local/SeedieDatabaseMigrations.kt` |
| DI 模块 | `di/AppModule.kt`, `DatabaseModule.kt`, `RepositoryModule.kt` |
| 词汇练习仓库 | `data/repository/VocabularyPracticeRepositoryImpl.kt` |
| 词汇练习 VM | `ui/screens/learning/practice/VocabularyPracticeViewModel.kt` |
| 导航定义 | `ui/navigation/Screen.kt`, `SeedieNavHost.kt` |
| 应用范围 CoroutineScope | `di/AppModule.kt`（`@ApplicationScope` qualifier）|

---

## 开发日志

### 2026-05-19（第一次 session）— Phase 0 完成

**修改模块**：
- 6 个 Entity：`CheckInEntity`、`EconomyTransactionEntity`、`GardenPlotEntity`、`VocabularyWordLearningProgressEntity`、`VocabularyStudyRoundEntity`、`VocabularyBookProgressEntity`
- 对应 DAO：全部加 `userId` 过滤参数
- `EconomyManagerImpl`、`GardenEngine`：注入 AuthService，用 `flatMapLatest` 响应 session 切换
- `VocabularyPracticeRepositoryImpl`：注入 AuthService，所有 Entity 构建加 `userId = currentUserId()`
- `SeedieDatabase`：版本 v3 → v4
- `SeedieDatabaseMigrations`：新增 MIGRATION_3_4（6 张表重建）
- `DatabaseModule`：注册迁移

**修复问题**：
- `GardenPlotSection.kt` 编译错误：构造参数顺序变更后使用具名参数修复

**验证**：真机编译+运行通过，功能正常

---

### 2026-05-19（第二次 session）— Phase 1 + Phase 2 完成

#### 新增功能

**Phase 1 — 签到同步链路**：
- `SyncOperationEntity` + `SyncOperationDao`：离线队列表
- `NetworkConnectivityObserver`：`callbackFlow` + `ConnectivityManager` 监听网络，`distinctUntilChanged()`
- `SyncManager` 接口 + `SyncManagerImpl`：`init` 块监听网络恢复自动触发全量同步
- `CheckInSyncer`：读取 PENDING 签到 → upsert `user_check_ins` → 更新 syncStatus
- `SplashViewModel`：等待 session 就绪 → 写本地 Room → 触发 `syncNow(CHECK_IN)`
- `SplashScreen`：接入 `hiltViewModel()`，按钮调用 `viewModel.checkIn(onNavigateToMain)`
- `AppModule`：新增 `@ApplicationScope` qualifier + CoroutineScope provider
- `AndroidManifest`：新增 `ACCESS_NETWORK_STATE` 权限
- `DatabaseModule`：注册 MIGRATION_4_5，新增 SyncOperationDao provider
- `SeedieDatabase`：版本 v4 → v5
- `SyncModule`：Hilt abstract 模块绑定接口实现

**Phase 2 — 词汇进度同步**：
- 3 个 DAO 新增 `getPendingXxx()` + `updateSyncStatus()`
- `VocabularyProgressSyncer`：同步 word_learning_progress / study_rounds / book_progress
- `SyncScope` 新增 `VOCABULARY_PROGRESS`
- `SyncManagerImpl` 更新 `when` 分支，ALL scope 同时执行两个 syncer
- `VocabularyPracticeRepositoryImpl`：注入 SyncManager，4 个写操作方法末尾加 `syncNow(VOCABULARY_PROGRESS)`
- Supabase：建 3 张表 + RLS（`user_vocabulary_word_learning_progress` / `user_vocabulary_study_rounds` / `user_vocabulary_book_progress`）

#### 修复的问题

| 问题 | 原因 | 修复 |
|------|------|------|
| 签到后 Supabase 无数据，本地也无写入 | **时序竞争**：`isLoggedIn=true` 触发 UI 切换时，`restoreSessionFromAuth()` 还未完成，`currentSession.value` 为 null | `SplashViewModel` 改用 `currentSession.filterNotNull().first()` 挂起等待 session 就绪 |
| `CheckInSyncer` 编译错误 | 缺少 `import javax.inject.Singleton` | 补充 import |

#### 架构决策

1. **userId 来源**：永远是 `auth.users.id`（Supabase Auth 自动生成 UUID），本地 Room 迁移时旧数据用 `""` 占位，不影响新写入
2. **旧 Supabase 表不兼容**：`points_ledger`、`study_events` 等使用 `agency_id/student_id` 模型，与 App 的 `auth.uid()` 模型不兼容，完全独立，不修改
3. **同步触发策略**：Repository 写操作末尾直接 `syncNow()`（在调用方协程内执行），不使用 WorkManager——适合即时性要求较高的学习进度数据
4. **Supabase 时间戳类型**：统一用 `BIGINT`（epoch 毫秒）与 Room 的 `Long` 对齐，避免转换
5. **SyncScope 设计**：枚举扩展式——每加一类同步只需新增枚举值 + Syncer 类 + `when` 分支，不改接口

#### 验证结果

- Phase 1：点击签到 → Logcat 确认 userId 有效 → Supabase `user_check_ins` 出现对应行 ✅
- Phase 2：完成词汇练习 → Supabase 三张表同步数据正确 ✅

---

## 当前项目状态（2026-05-19）

| 模块 | 状态 |
|------|------|
| 本地 Room 数据库 | v5，所有同步表含 userId + syncStatus |
| 签到同步 | ✅ 生产可用 |
| 词汇进度同步 | ✅ 生产可用 |
| 词书/单词下载 | ⬜ Phase 3 待实现 |
| 代币同步 | ⬜ Phase 4 待实现 |
| 花园同步 | ⬜ Phase 4 待实现 |
| 离线队列（sync_operations） | 表已建，Syncer 未使用（当前直连失败仅 log）|

---

## TODO

- [ ] **Phase 3**：`WordBookSyncer`（Supabase → 本地，按需下载），替换 `VocabularyStaticWordPack` 硬编码数据
- [ ] **Phase 4**：`EconomyTransactionSyncer`（追加上传）+ `GardenPlotSyncer`（双向同步）
- [ ] **离线队列消费**：当前 Syncer 失败只 log，后续应写入 `sync_operations` 并在网络恢复时重试
- [ ] **Phase 5**：成就/听力/口语等未来模块通过 SyncManager 扩展接入

---

## 下一阶段建议（Phase 3）

Phase 3 方向：**词书内容从 Supabase 下载到本地，替代当前硬编码的 `VocabularyStaticWordPack`**。

关键决策点：
1. **Supabase 建表**：`word_books`（公开只读） + `vocabulary_words`（公开只读，按 bookId 分批拉取）
2. **触发时机**：App 首次启动且本地无词书时下载；管理员更新词书版本时按 version 对比增量更新
3. **下载状态**：`WordBookEntity.downloadStatus` 已有字段（`PENDING` / `DOWNLOADING` / `COMPLETED`），直接使用
4. **向后兼容**：下载完成前继续使用 `VocabularyStaticWordPack` 兜底，下载成功后切换到云端数据
