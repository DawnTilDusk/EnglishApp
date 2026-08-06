# Changelog 2026-08-06

## 花园土地平整化

- 地面恢复**伪 3D 等距菱形**（`tileH = tileW * 0.5`），角朝向自己；每格同形同大，无顶点缩放翘曲。
- 树精灵近大远小加强为约 **0.48×～1.28×**（相对格子宽度），便于辨认。

### 文档

- 修订 [garden_forest_mvp.md](./garden_forest_mvp.md)

## 花园文案 / 居中 / 定格 / 阅读种树

- 空态文案改为「花园土地…」；树脚锚在格心。
- 填树改为 per-plant preferred slot + 线性探测，新增植物不挪动旧树。
- 免费听/读每次开练唯一 attempt sessionId；`WITHERED`→`ALIVE` 可升级同 session。

### 文档

- 修订 [garden_forest_mvp.md](./garden_forest_mvp.md)

## 菱形均匀格子 + 稳定随机占格

- 花园改为角朝向自己的菱形盘；内部 6×6 逻辑均匀格；透视近大远小。
- 填树改为基于植物 id 的稳定随机占格（不再远→近顺序填）。

## 森林格子 + 中途退出必种枯树

- `!isCompleted` 始终写枯苗（含 0 题）；退出 Dialog 统一枯苗文案。

## 历史林花园 MVP

- 历史林、开题前选树、代币解锁树种；听/读/写/测验不发币；背单词/复习发币保留。
- Room DB → v10：`garden_plants`、`garden_unlocks`。

### 文档

- [garden_forest_mvp.md](./garden_forest_mvp.md)
- 修订 [economy_token_system.md](../2026-07-24/economy_token_system.md)
- 修订 [project_overview.md](../2026-06-28/project_overview.md) §5.6 / §5.8
