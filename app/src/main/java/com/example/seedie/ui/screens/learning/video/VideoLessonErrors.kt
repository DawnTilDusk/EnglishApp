package com.example.seedie.ui.screens.learning.video

internal fun mapVideoLoadError(error: Throwable): String {
    val raw = error.message.orEmpty()
    val lower = raw.lowercase()
    if (lower.contains("not_found") || lower.contains("object not found")) {
        return "视频加载失败，请稍后重试"
    }
    return raw.ifBlank { "视频加载失败" }
}
