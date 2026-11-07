# Android 提交检查兼容性修正（2026-11-07）

## 原因与修改

项目最低支持 API 24，但 java.time 系列 API 默认需要 API 26。为日期、任务日归属、活动统计及花园趋势启用 coreLibraryDesugaring，使用 com.android.tools:desugar_jdk_libs:2.1.5，保留原最低版本。

词汇音频焦点请求和释放按系统版本分支：API 26 及以上使用 AudioFocusRequest；API 24/25 使用原 AudioManager 接口，并保存同一个监听器用于释放。焦点被拒绝或永久丢失时清理监听器引用。

登录页将约束访问明确为 this.maxWidth。视频页区分 Kotlin ExperimentalMaterial3Api 与 AndroidX Media3 UnstableApi 的 opt-in 注解。

## 验证

最终 Android Lint 报告：0 errors，27 warnings，2 hints。截图中的 NewApi 和 BoxWithConstraints 错误均未再出现在最终报告中。

保留的提示主要为未使用资源、可更新的依赖、KTX 和样式建议。本次未通过 baseline 或关闭检查隐藏错误。

自动检查不能代替 API 24/25 设备的音频焦点、日期及活动日历验收。当前未执行旧版本设备运行测试。

## 提交范围

- app/build.gradle.kts
- app/src/main/java/com/example/seedie/data/audio/WordAudioPlayer.kt
- app/src/main/java/com/example/seedie/ui/screens/login/LoginScreen.kt
- app/src/main/java/com/example/seedie/ui/screens/learning/video/VideoLessonPlayerScreen.kt
- 本说明

不包含本机 IDE 配置、构建内存参数、Web 依赖锁文件及 hs_err/replay 日志。独立提交即可，无需改写前次提交。

最终验证：95 项 JVM 测试通过，失败/错误/跳过均为 0；APK 独立构建成功。低堆内存下将 Lint 与 Dex 合并放在同一进程造成内存压力，已结束本次验证进程，使用独立打包和临时 1536MB 堆内存完成验证，未修改 gradle.properties。调试包位于 app/build/outputs/apk/debug/app-debug.apk。
