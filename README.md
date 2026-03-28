# Seedie

## 开发文档与状态追踪（20260328）

- [Seedie App Changelog (当前开发状态)](docs/changelog.md)
- [Seedie App TODO List (待实现与打磨功能)](docs/todo_list.md)
- [Seedie App 技术实现方案 (Specs)](docs/spec/spec.md)
- [Seedie App 任务拆解 (Tasks)](docs/spec/tasks.md)

## Git 分支管理策略

我们采用轻量级的 **Feature Branch Workflow**。

### 核心分支说明

- **`main`**: 生产分支。仅存放最稳定的发布版本，禁止直接提交。
- **`develop`**: 汇总分支。两人的代码集散地，用于日常集成和联调。
- **`feature/功能名`**: 临时分支。每个新功能或 UI 改动都应在此分支进行。

### 命名规范

- **新功能**: `feature/tab1-heatmap`, `feature/nav-pager-logic`
- **修复 Bug**: `fix/nav-bar-offset`
- **重构**: `refactor/database-schema`

## 实际操作指令示例

### 1. 开始开发新功能

在开始编写新功能前，请确保基于最新的 `develop` 分支创建你的 `feature` 分支：

```bash
# 切换到 develop 分支
git checkout develop

# 拉取最新的 develop 代码
git pull origin develop

# 基于 develop 创建并切换到新功能分支 (例如开发 heatmap 功能)
git checkout -b feature/tab1-heatmap
```

### 2. 提交代码

在你的功能分支上完成代码编写后，将代码提交到本地：

```bash
# 查看更改状态
git status

# 将更改的文件添加到暂存区 (或指定具体文件 git add <file>)
git add .

# 提交更改，写明提交信息
git commit -m "feat: 添加 tab1 heatmap UI"
```

### 3. 推送并合并代码

功能开发完成并测试无误后，将分支推送到远程仓库，并准备合并到 `develop` 分支：

```bash
# 将当前功能分支推送到远程仓库
git push -u origin feature/tab1-heatmap
```

**推荐操作**：在 GitHub 页面上从 `feature/tab1-heatmap` 向 `develop` 分支发起 Pull Request (PR)，由队友进行代码审查后合并。

**本地直接合并（如果无需 Code Review）**：

```bash
# 切换回 develop 分支
git checkout develop

# 拉取最新 develop 避免冲突
git pull origin develop

# 将功能分支合并到 develop
git merge feature/tab1-heatmap

# 推送合并后的 develop 到远程
git push origin develop

# 删除本地的临时功能分支（可选）
git branch -d feature/tab1-heatmap
```

##
