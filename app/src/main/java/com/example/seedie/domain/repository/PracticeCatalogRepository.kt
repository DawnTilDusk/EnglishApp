package com.example.seedie.domain.repository

import com.example.seedie.domain.model.PracticeCatalogLoad

interface PracticeCatalogRepository {
    suspend fun listCatalog(moduleId: String): PracticeCatalogLoad
    suspend fun markCompleted(moduleId: String, itemRefs: List<String>)
}
