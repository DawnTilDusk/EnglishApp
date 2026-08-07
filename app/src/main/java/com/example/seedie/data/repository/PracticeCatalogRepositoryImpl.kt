package com.example.seedie.data.repository

import com.example.seedie.data.remote.ListeningRemoteDataSource
import com.example.seedie.data.remote.PracticeCatalogRemoteDataSource
import com.example.seedie.data.remote.ReadingRemoteDataSource
import com.example.seedie.domain.model.PracticeCatalogItem
import com.example.seedie.domain.model.PracticeCatalogLoad
import com.example.seedie.domain.reading.ReadingGradeBands
import com.example.seedie.domain.repository.PracticeCatalogRepository
import com.example.seedie.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeCatalogRepositoryImpl @Inject constructor(
    private val readingRemote: ReadingRemoteDataSource,
    private val listeningRemote: ListeningRemoteDataSource,
    private val catalogRemote: PracticeCatalogRemoteDataSource,
    private val profileRepository: ProfileRepository
) : PracticeCatalogRepository {

    override suspend fun listCatalog(moduleId: String): PracticeCatalogLoad {
        val completed = catalogRemote.fetchCompletedItemRefs(moduleId)
        return when (moduleId) {
            "reading" -> listReadingCatalog(completed)
            "listening" -> PracticeCatalogLoad(
                items = listeningRemote.fetchAllMaterials().map { material ->
                    val primary = material.title_zh?.takeIf { it.isNotBlank() }
                        ?: material.title?.takeIf { it.isNotBlank() }
                        ?: material.material_id
                    val secondary = material.title?.takeIf {
                        it.isNotBlank() && it != primary
                    }
                    PracticeCatalogItem(
                        itemRef = material.material_id,
                        title = primary,
                        subtitle = buildString {
                            if (secondary != null) append(secondary)
                            if (material.estimated_seconds > 0) {
                                if (isNotEmpty()) append(" · ")
                                append("约 ")
                                append(material.estimated_seconds)
                                append(" 秒")
                            }
                        }.ifBlank { null },
                        completed = material.material_id in completed
                    )
                }
            )
            else -> PracticeCatalogLoad(items = emptyList())
        }
    }

    private suspend fun listReadingCatalog(completed: Set<String>): PracticeCatalogLoad {
        val profile = runCatching { profileRepository.getMyProfile() }.getOrNull()
        val bands = ReadingGradeBands.bandsForProfileGrade(profile?.grade)
        if (bands.isEmpty()) {
            return PracticeCatalogLoad(
                items = emptyList(),
                emptyMessage = ReadingGradeBands.EMPTY_NON_JUNIOR_MESSAGE
            )
        }
        val items = readingRemote.fetchAllSets()
            .filter { it.grade_band in bands }
            .map { set ->
                val bandLabel = ReadingGradeBands.displayName(set.grade_band)
                PracticeCatalogItem(
                    itemRef = set.set_id,
                    title = set.title_zh?.takeIf { it.isNotBlank() } ?: set.title,
                    subtitle = buildString {
                        if (bandLabel != null) {
                            append(bandLabel)
                            append(" · ")
                        }
                        append(set.title)
                        if (set.difficulty.isNotBlank()) {
                            append(" · ")
                            append(set.difficulty)
                        }
                        if (set.estimated_minutes > 0) {
                            append(" · 约 ")
                            append(set.estimated_minutes)
                            append(" 分钟")
                        }
                    },
                    completed = set.set_id in completed
                )
            }
        return PracticeCatalogLoad(
            items = items,
            emptyMessage = ReadingGradeBands.EMPTY_CATALOG_MESSAGE
        )
    }

    override suspend fun markCompleted(moduleId: String, itemRefs: List<String>) {
        catalogRemote.markCompleted(moduleId, itemRefs)
    }
}
