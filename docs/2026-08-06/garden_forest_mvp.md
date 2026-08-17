# 历史林花园 MVP（2026-08-06）

更新日期：2026-08-17

## 产品契约

- **主模型：** 历史林；日/周回看；**角朝向自己的伪 3D 菱形花园 + 6×6 均匀格**。
- **地面：** 经典等距（`tileH = tileW * 0.5`）压扁菱形；**每格同形同大**，无近大远小翘曲；土色轻微远近明暗。
- **植物：** `scaleOf(depth)` 近大远小（只缩放精灵）。
- **填树：** 按 `createdAt` 落位；每株 `preferredSlot = hash(plantId) % 36`，冲突则线性探测空位；**已占格永不因新树重排**。
- **开题前选树：** `PlantSessionGate`；锁种用代币解锁。
- **判定（`GardenForestRules`）：**
  - `isCompleted && 题数 > 0` → `ALIVE`（不受容错影响）
  - `isCompleted && 题数 == 0` → 不记账
  - `!isCompleted` 且确认选树后墙钟 **< 60s** → 不记账（含已答题）
  - `!isCompleted` 且墙钟 **≥ 60s** → `WITHERED`
  - 同 `sessionId` 已有 `WITHERED`、本次应为 `ALIVE` → **升级为活树**
  - `sessionOpenedAtMillis == 0`（如写作）视为无容错
- **枯苗铲除：** 点选枯苗，花 **50** 代币铲除；`refId = garden_remove:{userId}:{plantId}`；活苗不可铲。
- **免费听/读：** 每次开练使用 `free_{module}:{itemRef}:{uuid}` 作为 attempt sessionId，避免同篇重练被静默跳过。
- **中途退出文案：**
  - 容错内（独立文案，不复用枯苗警告）：标题「确定要退出练习吗？」；正文说明短时容错内退出不留下花园记录；按钮「退出练习」/「继续练习」
  - 容错外：标题「中途退出会种下枯苗」；按钮「仍然退出（种下枯苗）」/「继续学习」
  - 选树页说明：完成练习会种进花园；约一分钟内退出不留记录，超时退出变枯苗
- **代币：** 背单词/复习发币；听/读/写/测验不发币；每日任务领取接口保留。

## 格子布局

- `ForestLayout.GRID_SIZE = 6`（36 格）；`preferredSlot` + linear probe。
- `ForestScene`：菱形外框 + 等大同形等距土格 + 树脚锚格心；树精灵近大远小（远≈0.48× → 近≈1.28×，相对格子宽度）。
- 空态文案：「花园土地已备好…」。

## 关键路径

| 层 | 路径 |
|----|------|
| 规则 | `GardenForestRules.kt` |
| 引擎 | `GardenEngine.kt`、`ForestLayout.kt` |
| UI | `ForestPanel` / `ForestScene` / `PlantSpeciesPicker` |
| 接线 | `MainViewModel.handleStudyResult` |

## 旧 16 格养成

`garden_plots` / `GardenPlotSection` 不再作为主路径。
