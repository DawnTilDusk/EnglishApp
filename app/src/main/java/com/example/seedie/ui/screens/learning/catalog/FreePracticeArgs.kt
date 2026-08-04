package com.example.seedie.ui.screens.learning.catalog

data class FreePracticeArgs(
    val moduleId: String,
    val itemRef: String
) {
    val sessionId: String get() = "free_${moduleId}:$itemRef"
}
