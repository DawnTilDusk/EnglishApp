# 词汇量检测（外研分档线性估测）

更新日期：2026-08-04

## 口径

| 项 | 说明 |
|----|------|
| 词库 | 外研版初中六册词书（`fltrp-g7-vol1` … `fltrp-g9-vol2`） |
| 估测上限 | **1800** |
| 每档题量 | **12** 词随机抽样 |
| 晋级 | **≥11/12** 进入下一档，否则停档按比例估测 |
| Profile「词汇量」 | = 最近一次检测估测（`profiles.vocabulary_size`）；未测显示「未检测」 |
| 背单词 | **不**改词汇量 |
| 选项 | 英→中四选一；约 **40%** 将第 4 项替换为「以上都不对」（保留该项原对错语义） |

## 配额（冻结）

按导入后各册词数归一化到 1800，写入 App `VocabularyQuizConstants.GRADE_BANDS`：

`[549, 354, 268, 230, 294, 105]`（七上→九下）

## 公式

- 已通过档：累加该档 `quota`
- 停档 / 中途退出：`prior + (答对 / 12) × 本档 quota`
- 六档全通：`1800`

## 关键文件

- 估测：`GradeBandVocabularyEstimator.kt`
- 常量：`VocabularyQuizConstants.kt`
- 题池：`VocabularyQuizRepositoryImpl`（Room + Remote 一次拉齐六册）
- 持久化：`set_my_vocabulary_estimate` RPC；`MainViewModel` 在 quiz `estimatedVocabulary` 时写入
