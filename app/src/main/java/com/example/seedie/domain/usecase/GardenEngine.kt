package com.example.seedie.domain.usecase

import com.example.seedie.data.local.DevicePreferencesRepository
import com.example.seedie.data.local.dao.GardenPlantDao
import com.example.seedie.data.local.dao.GardenUnlockDao
import com.example.seedie.data.local.entity.GardenPlantEntity
import com.example.seedie.data.local.entity.GardenUnlockEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.GardenSpecies
import com.example.seedie.domain.model.GardenSpeciesCatalog
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.DewManager
import com.example.seedie.domain.repository.EconomyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed class GardenUnlockResult {
    data object Success : GardenUnlockResult()
    data object AlreadyUnlocked : GardenUnlockResult()
    data object InsufficientDews : GardenUnlockResult()
    data object NotLoggedIn : GardenUnlockResult()
    data object UnknownSpecies : GardenUnlockResult()
}

data class GardenSpeciesUi(
    val species: GardenSpecies,
    val unlocked: Boolean
)

@Singleton
class GardenEngine @Inject constructor(
    private val gardenPlantDao: GardenPlantDao,
    private val gardenUnlockDao: GardenUnlockDao,
    private val economyManager: EconomyManager,
    private val dewManager: DewManager,
    private val rewardEventBus: RewardEventBus,
    private val authService: AuthService,
    private val devicePreferencesRepository: DevicePreferencesRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePlantsForDate(localDate: String): Flow<List<GardenPlantEntity>> =
        authService.currentSession.flatMapLatest { session ->
            val userId = session?.userId
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else gardenPlantDao.observePlantsForDate(userId, localDate)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePlantsBetween(startDate: String, endDate: String): Flow<List<GardenPlantEntity>> =
        authService.currentSession.flatMapLatest { session ->
            val userId = session?.userId
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else gardenPlantDao.observePlantsBetween(userId, startDate, endDate)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSpeciesCatalog(): Flow<List<GardenSpeciesUi>> =
        authService.currentSession.flatMapLatest { session ->
            val userId = session?.userId
            if (userId.isNullOrBlank()) {
                flowOf(defaultCatalogUi())
            } else {
                gardenUnlockDao.observeUnlocks(userId).map { unlocks ->
                    val unlockedIds = unlocks.map { it.speciesId }.toSet()
                    GardenSpeciesCatalog.all.map { species ->
                        GardenSpeciesUi(
                            species = species,
                            unlocked = species.unlockedByDefault || species.id in unlockedIds
                        )
                    }
                }
            }
        }

    suspend fun ensureDefaultUnlocks() {
        val userId = authService.currentSession.value?.userId ?: return
        GardenSpeciesCatalog.all.filter { it.unlockedByDefault }.forEach { species ->
            gardenUnlockDao.insertUnlock(
                GardenUnlockEntity(
                    userId = userId,
                    speciesId = species.id,
                    unlockedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun isSpeciesUnlocked(speciesId: String): Boolean {
        val species = GardenSpeciesCatalog.byId(speciesId) ?: return false
        if (species.unlockedByDefault) return true
        val userId = authService.currentSession.value?.userId ?: return false
        return gardenUnlockDao.findUnlock(userId, speciesId) != null
    }

    suspend fun unlockSpecies(speciesId: String): GardenUnlockResult {
        val species = GardenSpeciesCatalog.byId(speciesId) ?: return GardenUnlockResult.UnknownSpecies
        val userId = authService.currentSession.value?.userId
            ?: return GardenUnlockResult.NotLoggedIn
        if (species.unlockedByDefault || gardenUnlockDao.findUnlock(userId, speciesId) != null) {
            return GardenUnlockResult.AlreadyUnlocked
        }
        val cost = species.unlockCostDew
        if (cost <= 0) {
            gardenUnlockDao.insertUnlock(
                GardenUnlockEntity(userId, speciesId, System.currentTimeMillis())
            )
            return GardenUnlockResult.Success
        }
        val spent = dewManager.spendDews(
            amount = cost,
            item = "Garden species ${species.id}",
            refId = "garden_unlock_dew:$userId:${species.id}"
        )
        if (!spent) return GardenUnlockResult.InsufficientDews
        gardenUnlockDao.insertUnlock(
            GardenUnlockEntity(userId, speciesId, System.currentTimeMillis())
        )
        return GardenUnlockResult.Success
    }

    suspend fun getLastSelectedSpeciesId(): String {
        val saved = devicePreferencesRepository.getLastGardenSpeciesId()
        if (saved != null && isSpeciesUnlocked(saved)) return saved
        return GardenSpeciesCatalog.DEFAULT_SPECIES_ID
    }

    suspend fun setLastSelectedSpeciesId(speciesId: String) {
        if (isSpeciesUnlocked(speciesId)) {
            devicePreferencesRepository.setLastGardenSpeciesId(speciesId)
        }
    }

    /**
     * Records a plant from a study session.
     * @return status written, or null if skipped / duplicate.
     */
    suspend fun recordFromStudyResult(result: StudyResult): String? {
        val now = System.currentTimeMillis()
        val status = GardenForestRules.statusFor(
            isCompleted = result.isCompleted,
            questionCount = result.completedQuestionCount,
            abandonElapsedMs = GardenForestRules.abandonElapsedMs(
                sessionOpenedAtMillis = result.sessionOpenedAtMillis,
                nowMillis = now
            )
        ) ?: return null
        val userId = authService.currentSession.value?.userId ?: return null
        val speciesId = resolveSpeciesId(result.selectedSpeciesId)
        val existing = gardenPlantDao.findBySessionId(userId, result.sessionId)
        if (existing != null) {
            if (existing.status == GardenPlantEntity.STATUS_WITHERED &&
                status == GardenPlantEntity.STATUS_ALIVE
            ) {
                val upgraded = existing.copy(
                    status = GardenPlantEntity.STATUS_ALIVE,
                    speciesId = speciesId,
                    completedQuestionCount = result.completedQuestionCount,
                    correctCount = result.correctCount,
                    studyDurationSec = result.studyDurationSec,
                    syncStatus = "PENDING",
                    syncedAt = null
                )
                gardenPlantDao.updatePlant(upgraded)
                rewardEventBus.emit(RewardEvent.PlantGrown(speciesId = speciesId, status = status))
                return status
            }
            return null
        }

        val plant = GardenPlantEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            sessionId = result.sessionId,
            moduleId = result.moduleId,
            speciesId = speciesId,
            status = status,
            completedQuestionCount = result.completedQuestionCount,
            correctCount = result.correctCount,
            studyDurationSec = result.studyDurationSec,
            createdAt = now,
            localDate = dateFormat.format(Date(now)),
            syncStatus = "PENDING",
            syncedAt = null
        )
        val inserted = gardenPlantDao.insertPlant(plant)
        if (inserted == -1L) return null
        rewardEventBus.emit(RewardEvent.PlantGrown(speciesId = speciesId, status = status))
        return status
    }

    sealed class RemoveWitheredResult {
        data object Success : RemoveWitheredResult()
        data object NotFound : RemoveWitheredResult()
        data object NotWithered : RemoveWitheredResult()
        data object InsufficientTokens : RemoveWitheredResult()
        data object NotLoggedIn : RemoveWitheredResult()
    }

    suspend fun removeWitheredPlant(plantId: String): RemoveWitheredResult {
        val userId = authService.currentSession.value?.userId
            ?: return RemoveWitheredResult.NotLoggedIn
        val plant = gardenPlantDao.findById(userId, plantId)
            ?: return RemoveWitheredResult.NotFound
        if (plant.status != GardenPlantEntity.STATUS_WITHERED) {
            return RemoveWitheredResult.NotWithered
        }
        val spent = economyManager.spendTokens(
            amount = GardenForestRules.REMOVE_WITHERED_COST,
            item = "Remove withered plant ${plant.id}",
            refId = "garden_remove:$userId:${plant.id}"
        )
        if (!spent) return RemoveWitheredResult.InsufficientTokens
        gardenPlantDao.deleteWitheredPlant(userId, plantId)
        return RemoveWitheredResult.Success
    }

    private suspend fun resolveSpeciesId(requested: String): String {
        val id = requested.ifBlank { GardenSpeciesCatalog.DEFAULT_SPECIES_ID }
        return if (isSpeciesUnlocked(id)) id else GardenSpeciesCatalog.DEFAULT_SPECIES_ID
    }

    private fun defaultCatalogUi(): List<GardenSpeciesUi> =
        GardenSpeciesCatalog.all.map { species ->
            GardenSpeciesUi(species = species, unlocked = species.unlockedByDefault)
        }
}
