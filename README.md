# Seedie

英语学习 + 游戏化激励产品。学生端为横屏 Android 应用（背单词赚代币、虚拟花园）；机构管理员与教师使用 Web 控制台。后端统一走 Supabase。

## 仓库结构

```
EnglishApp/
├── app/                 # Android 学生端（Gradle 单模块 :app）
├── web/                 # 机构 / 教师 B 端控制台（Next.js）
├── supabase/migrations/ # 数据库迁移（Postgres + RLS）
├── scripts/             # 运维脚本（如远程应用迁移）
└── docs/                # 开发文档（按日期分目录）
```

## 技术栈

### Android（`app/`）

| 类别 | 技术 |
|------|------|
| 语言 / 构建 | Kotlin 2.2、AGP 9.1、Gradle、KSP |
| UI | Jetpack Compose + Material 3（横屏锁定） |
| 架构 | Clean Architecture 分层 + Hilt DI |
| 本地数据 | Room、DataStore |
| 异步 | Kotlin Coroutines |
| 导航 | Navigation Compose |
| 后端客户端 | Supabase Kotlin SDK（Auth + Postgrest）、Ktor Android |
| 媒体 | Media3（词书音频等） |

包名 / `applicationId`：`com.example.seedie`。版本目录见 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)。

### Web（`web/`）

| 类别 | 技术 |
|------|------|
| 框架 | Next.js 15（App Router） |
| UI | React 19、TypeScript |
| 后端客户端 | `@supabase/supabase-js`、`@supabase/ssr` |

角色路由：`agency_admin` → `/agency/*`，`teacher` → `/teacher/*`；学生账号请用 Android 端。细节见 [`web/README.md`](web/README.md)。

### 后端

| 类别 | 技术 |
|------|------|
| BaaS | Supabase（Auth、Postgres、RLS、Storage、RPC） |
| Schema | SQL 迁移位于 `supabase/migrations/` |

多租户角色大致为 `agency_admin` / `teacher` / `student`。表用途与权限见 [`docs/2026-07-24/supabase_table_map.md`](docs/2026-07-24/supabase_table_map.md)。

## 本地运行（简要）

**Android**

```bash
# Windows
.\gradlew.bat :app:assembleDebug
```

需 Android Studio / JDK 11+，以及 `local.properties` 中的 SDK 路径。

**Web**

```bash
cd web
cp .env.example .env.local   # 填入与 Android 相同的项目 URL / anon key
npm install
npm run dev
```

**文档**

完整功能与架构说明：[`docs/2026-06-28/project_overview.md`](docs/2026-06-28/project_overview.md)。按日变更记录在 `docs/YYYY-MM-DD/changelog.md`。
