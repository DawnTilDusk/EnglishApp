# Tab 3 词汇量趋势与森林布局改造 TODO

更新日期：2026-08-17

## 目标

- 恢复词汇量趋势的时间范围筛选：`近 7 天`、`近 30 天`、`近 1 年`、`本月`、`本年度`。
- 使用真实词汇检测历史生成连续的日趋势；真实测评点不可被修改。
- 对两个真实测评点之间的未测评日期进行确定性插值，并加入不超过 `±5 词` 的轻微、稳定波动。
- 将左侧统计栏顶部的“所选时段森林：存活树苗 / 枯萎树苗”HUD 移入右侧森林模块；释放出的高度优先分给左侧的词汇量趋势图。

## 非目标与数据口径

- 不使用全局线性回归：它会改写真实测评结果。采用相邻真实测评点之间的**线性插值**。
- 不每次打开页面重新生成随机数；补点必须由固定种子生成，同一历史数据多次查看得到完全相同的结果。
- 不把补点伪装成真实测评，也不把插值后的逐日差值称为“真实每日增长”。
- 测评前的日期不猜测；最后一次测评后的日期保持最后一次结果，不伪造新的增长。

## TODO

### 1. 恢复趋势范围与指标状态

- [ ] 在 `GardenViewModel` 或独立的趋势状态模型中定义 `TrendRangeOption`：`Last7Days`、`Last30Days`、`LastYear`、`ThisMonth`、`ThisYear`。
- [ ] 恢复 `StatsPanelSection` 中的范围下拉菜单；沿用原来 Tab 3 的交互样式，但删除原先的静态演示曲线。
- [ ] 将旧的“累计掌握”名称调整为“词汇量估算”或“检测估算趋势”，避免把检测值误解为精确掌握词数。
- [ ] “增长区间”改名为“检测区间变化”，只基于相邻两个真实测评锚点计算；不展示由插值产生的虚构日增长。

### 2. 扩展词汇检测历史读取

- [ ] 为 `ProfileRepository` 新增趋势专用查询：读取所选时间段内的全部 `user_vocabulary_estimates`，并额外读取范围开始日前最近的一条记录。
- [ ] 移除趋势读取固定为最近 30 次的限制；年度趋势不能因检测次数多而遗漏较早记录。
- [ ] 使用 Supabase 的 `created_at` 范围过滤与排序，保留现有 RLS，不新增数据库表或迁移。
- [ ] 将 `created_at` 统一按设备/产品时区转为 `LocalDate` 后再聚合，避免 UTC 零点附近的记录落入错误日期。

### 3. 生成每日真实锚点

- [ ] 按本地自然日分组检测历史。
- [ ] 同一天有多次测评时，取该日**最高** `vocabularySize` 作为当天的真实锚点。
- [ ] 为趋势点增加来源字段：`ActualMeasurement`、`Interpolated`、`CarriedForward`。
- [ ] 保留真实锚点的测评次数，以便 Tooltip 显示“当日 2 次测评，取最高值”等必要说明。

### 4. 生成稳定的插值与轻微波动

- [ ] 对相邻真实锚点 `(dateA, valueA)` 与 `(dateB, valueB)` 之间的每个空日期先计算线性插值：`base = valueA + (valueB - valueA) * progress`。
- [ ] 使用由 `startRecordId + endRecordId + date + trendAlgorithmVersion` 派生的固定种子，生成 `[-1, 1]` 的伪随机值；禁止使用无种子的 `Random`。
- [ ] 令扰动在真实锚点处为零，例如使用 `sin(π × progress)` 作为包络；中间日期才允许出现波动。
- [ ] 扰动幅度为 `min(5, 两端数值差的合理比例)`，并将最终值约束在两个真实锚点的最小值与最大值之间；真实锚点本身绝不加扰动。
- [ ] 最后一次真实测评到今天使用相同数值的 `CarriedForward` 点，不加随机波动；首次真实测评之前不生成点。
- [ ] 对同一输入编写确定性测试：重进页面、切换范围、旋转屏幕后补点值不变。

### 5. 按范围取样并绘制趋势

- [ ] `近 7 天`、`近 30 天`、`本月`：按日绘制；30 天和整月仅显示部分横轴标签，点击/拖动仍可查看每一天。
- [ ] `近 1 年`、`本年度`：先生成每日序列，再以每月最后一个可用日为展示点，避免横轴过密。
- [ ] 实测点使用实心点和实线；插值点使用空心/淡色点与虚线；延续点使用更弱的样式。
- [ ] Tooltip 必须显示日期、词汇量、点位来源：`实际测评`、`相邻测评估算` 或 `沿用最近测评`。
- [ ] 图例和说明文案明确：虚线及淡色点为未测评日期的趋势估算，不代表新增测评。
- [ ] 空状态、只有一次测评、范围内无测评但范围前有锚点、测评值下降、跨月/跨年等情况均要有稳定展示。

### 6. 调整 Tab 3 左右栏布局

- [ ] 从 `StatsPanelSection` 删除 `forestAliveCount` / `forestWitheredCount` 参数及顶部“所选时段森林”HUD。
- [ ] 从 `DataGardenScreen` 删除向左侧统计栏传递森林数量的代码。
- [ ] 在 `ForestPanel` 的日期范围控制区下方，将现有简短计数统一为森林内 HUD，例如：`所选时段森林 · 存活树苗 3 · 枯萎树苗 1`；不新增重复计数。
- [ ] 左侧栏删除 HUD 后，将“学习时间分布”与“词汇量趋势”卡片改为约 `0.9 : 1.1` 的高度比例，优先给词汇量趋势图增加可用高度。
- [ ] 验证窄屏或字体放大时，森林 HUD、日期导航和“园丁小屋”按钮不会发生遮挡或换行错位。

### 7. 测试与验收

- [ ] 为“同日取最大值”“两锚点线性插值”“固定种子扰动”“前后边界”“年视图月度采样”增加 JVM 单元测试。
- [ ] 为趋势范围切换、空状态、实测/补点 Tooltip 和左侧卡片高度增加 Compose/UI 验收检查。
- [ ] 执行相关单元测试与 Android 编译检查。
- [ ] 人工验收：同一账号多次打开 Tab 3，所有补点值和曲线形态保持一致；完成一次新测评后，仅受影响区间重算。

## 涉及文件

- `app/src/main/java/com/example/seedie/domain/repository/ProfileRepository.kt`
- `app/src/main/java/com/example/seedie/data/repository/ProfileRepositoryImpl.kt`
- `app/src/main/java/com/example/seedie/ui/screens/garden/GardenViewModel.kt`
- `app/src/main/java/com/example/seedie/ui/screens/garden/StatsPanelSection.kt`
- `app/src/main/java/com/example/seedie/ui/screens/garden/DataGardenScreen.kt`
- `app/src/main/java/com/example/seedie/ui/screens/garden/ForestPanel.kt`
- `app/src/test/java/com/example/seedie/ui/screens/garden/` 下的新增趋势测试

