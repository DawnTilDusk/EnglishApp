# 阅读题库六册扩容与年级过滤

| 项 | 内容 |
| --- | --- |
| 日期 | 2026-08-07 |
| 状态 | 已落地 |

## 结构

- 档位：`g7a`…`g9b`（七上–九下），每档 ≥5 套；现库合计 30 套 / 150 题 / 600 选项。
- 资料映射：初一→七上+七下；初二→八上+八下；初三→九上+九下；小学/高中/`其他`→自由目录空态。
- `content_origin=ai_generated`：库内溯源，客户端不展示。
- 教师作业仍可选全库；学生作业入口不按年级拦截。

## 关键路径

- 迁移：`supabase/migrations/030_reading_grade_bands.sql`
- 生成 / 应用：`scripts/generate_reading_030.py`、`scripts/apply_reading_030.py`
- App：`ReadingGradeBands`、`PracticeCatalogRepositoryImpl` 按资料过滤
