# 词汇量检测（外研分档线性估测）

更新日期：2026-08-17

## 口径

| 项 | 说明 |
|----|------|
| 词库 | 外研版初中六册词书（`fltrp-g7-vol1` … `fltrp-g9-vol2`） |
| 估测上限 | **1800** |
| 每档题量 | **12** 词随机抽样 |
| 晋级 | **≥7/12** 进入下一档，否则停档 |
| 每档贡献 | 一律 `(答对 / 12) × 本档 quota`（含已晋级档；取整） |
| Profile「词汇量」 | = 最近一次检测估测（`profiles.vocabulary_size`）；未测显示「未检测」 |
| 花园「词汇量趋势」 | 可切换近 7 天、近 30 天、近 1 年、本月、本年度；同日取最高检测值。相邻实测点之间按日线性插值并加入稳定的轻微波动，实测/估算/延续点在图中明确区分。 |
| 背单词 | **不**改词汇量 |
| 选项 | 英→中四选一；约 **40%** 将第 4 项替换为「以上都不对」（保留该项原对错语义） |

## 配额（冻结）

按导入后各册词数归一化到 1800，写入 App `VocabularyQuizConstants.GRADE_BANDS`：

`[549, 354, 268, 230, 294, 105]`（七上→九下）

## 公式

- 每一档（通过或停档 / 中途退出）：`(答对 / 12) × 本档 quota`，再累加
- 结果 `coerceIn(0, 1800)`；六档全对时比例之和为 1800

## 持久化

- RPC `set_my_vocabulary_estimate`：更新 `profiles.vocabulary_size` / `vocabulary_estimated_at`，并 **append** 一行 `user_vocabulary_estimates`
- App：`MainViewModel` 在 quiz `estimatedVocabulary` 时调用；花园经 `ProfileRepository.listMyVocabularyTrendEstimates` 按所选范围读取历史记录及范围前最近一次检测

## 关键文件

- 估测：`GradeBandVocabularyEstimator.kt`
- 常量：`VocabularyQuizConstants.kt`
- 题池：`VocabularyQuizRepositoryImpl`（Room + Remote 一次拉齐六册）
- 历史：`038_user_vocabulary_estimates.sql`；花园 `StatsPanelSection` / `GardenViewModel`
