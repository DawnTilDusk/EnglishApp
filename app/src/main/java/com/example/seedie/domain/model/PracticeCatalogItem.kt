package com.example.seedie.domain.model

data class PracticeCatalogItem(
    val itemRef: String,
    val title: String,
    val subtitle: String? = null,
    val completed: Boolean = false
)

data class PracticeCatalogLoad(
    val items: List<PracticeCatalogItem>,
    val emptyMessage: String = "题库暂无内容"
)
