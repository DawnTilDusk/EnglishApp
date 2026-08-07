package com.example.seedie.ui.theme

import androidx.compose.ui.graphics.Color

// Core Palette - Seedie IP 的主色调，负责整体品牌情绪，其他补充色不能顶替它们的位置
val PrimaryGreen = Color(0xFF66BB6A) // 生机绿：小芽最鲜活的绿，用作按钮、选中、图标主色
val SecondaryBrown = Color(0xFF8D6E63) // 橡木棕：泥土/枝干的沉稳中性色，用作次级文字/装饰
val AccentOrange = Color(0xFFFFB74D) // 暖阳橙：阳光高亮色，用作奖励、代币、警示等强调点

// Surfaces
val BackgroundMint = Color(0xFFF1F8E9) // 淡薄荷绿：全局大面积背景，保留 IP 底色
val SurfaceCream = Color(0xFFFFFFFF) // 奶油白：卡片主体表面，保持清爽

// State Colors
val HeatmapLight = Color(0xFFE8F5E9) // 浅苔：热力图最浅一格，与主色调保持同调
val HeatmapDark = Color(0xFF2E7D32) // 深林：热力图最深一格，代表长期坚持后的沉淀

// ------------------------------------------------------------------
// Supplementary Palette - 补充色板
// 说明：以下颜色只用于「丰富层次」，包括描边、渐变、容器背景、列表分割、图标副色、
// 表格 / 图表切片等，不会替换 Core Palette 的三个主色，主色调仍由生机绿套装承担。
// ------------------------------------------------------------------

// Green Scale - 完整绿色色阶（从最浅到最深），用于统一的绿色系视觉层级
val Green50 = Color(0xFFEDF7EE)  // 晨露绿：最浅一档，适合超大面积浅色背景、留白区域
val Green100 = Color(0xFFCEE9CF) // 新芽白绿：浅色卡片背景、Chip 背景
val Green200 = Color(0xFFAEDBB0) // 嫩芽绿：次级容器、分组分隔背景
val Green300 = Color(0xFF8ECC91) // 春叶绿：禁用/次要按钮、浅色高亮
val Green400 = Color(0xFF6FBE73) // 生机嫩绿：辅助高亮、进度条中段
val Green500 = Color(0xFF4FB054) // 标准生长绿：图表次级切片、常规强调补位
val Green600 = Color(0xFF419045) // 深叶绿：描边、图标深化、图表主要辅色
val Green700 = Color(0xFF337136) // 苍林绿：正文强对比文字、深色描边
val Green800 = Color(0xFF245127) // 深林绿：深色主题主要文字、重要标题
val Green900 = Color(0xFF163117) // 墨林绿：最深一档，用于深色主题背景或极致对比

// Earthy & Natural Palette - 自然大地色系，与绿色主色搭配丰富层次
val ForestDeep = Color(0xFF2D5016) // 深森林：饱和度低的暗绿，适合自然主题深色背景/顶栏
val MossGreen = Color(0xFF4A7C2D)  // 苔藓绿：偏黄的中绿，与土壤色搭配呈现苔藓质感
val SageGreen = Color(0xFF7BA05B)  // 鼠尾草绿：柔和的灰绿过渡色，适合插画/图表辅助色
val OliveYellow = Color(0xFFA8B545) // 橄榄黄绿：暖调偏黄，用于成熟/收获相关状态
val KhakiGold = Color(0xFFD2C464)   // 卡其金：温暖麦穗色调，可作为奖励/成就点缀色
val DeepCocoa = Color(0xFF3B2F2F)   // 深可可棕：近黑棕，用于深色主题文字或土壤深处描绘
val WarmTaupe = Color(0xFF8B7355)   // 温暖驼色：中性木质棕，装饰花盆/枝干等自然元素
