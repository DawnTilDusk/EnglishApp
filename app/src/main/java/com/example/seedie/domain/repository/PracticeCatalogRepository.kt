package com.example.seedie.domain.repository

import com.example.seedie.domain.model.PracticeCatalogItem

interface PracticeCatalogRepository {
    suspend fun listCatalog(moduleId: String): List<PracticeCatalogItem>
    suspend fun markCompleted(moduleId: String, itemRefs: List<String>)
}
