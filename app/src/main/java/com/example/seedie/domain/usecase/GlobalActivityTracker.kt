package com.example.seedie.domain.usecase

import com.example.seedie.di.ApplicationScope
import com.example.seedie.domain.model.ActivityModule
import com.example.seedie.domain.repository.ActivityTrackingRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class GlobalActivityTracker @Inject constructor(
    private val activityTrackingRepository: ActivityTrackingRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    private val clock: Clock = Clock.systemDefaultZone()
    private val lock = Any()
    private var currentModuleId: String? = null
    private var currentSegmentStartMillis: Long? = null
    private var isInForeground = true

    fun enter(moduleId: String) {
        synchronized(lock) {
            updateTrackedModuleLocked(moduleId = moduleId, nowMillis = clock.millis())
        }
    }

    fun trackModule(module: ActivityModule?) {
        synchronized(lock) {
            updateTrackedModuleLocked(
                moduleId = module?.id,
                nowMillis = clock.millis()
            )
        }
    }

    fun onAppForeground() {
        onAppForegrounded()
    }

    fun onAppForegrounded() {
        synchronized(lock) {
            if (isInForeground) return
            isInForeground = true
            if (currentModuleId != null) {
                currentSegmentStartMillis = clock.millis()
            }
        }
    }

    fun onAppBackground() {
        onAppBackgrounded()
    }

    fun onAppBackgrounded() {
        synchronized(lock) {
            if (!isInForeground) return
            isInForeground = false
            flushLocked(clock.millis())
            currentSegmentStartMillis = null
        }
    }

    private fun updateTrackedModuleLocked(moduleId: String?, nowMillis: Long) {
        if (moduleId != null && moduleId.isBlank()) return
        if (currentModuleId == moduleId) {
            if (moduleId == null || !isInForeground || currentSegmentStartMillis != null) return
        }

        flushLocked(nowMillis)
        currentModuleId = moduleId
        currentSegmentStartMillis = if (isInForeground && moduleId != null) nowMillis else null
    }

    private fun flushLocked(nowMillis: Long) {
        val moduleId = currentModuleId ?: return
        val startMillis = currentSegmentStartMillis ?: return
        if (nowMillis <= startMillis) return

        applicationScope.launch {
            activityTrackingRepository.recordSegment(
                moduleId = moduleId,
                startTimeMillis = startMillis,
                endTimeMillis = nowMillis
            )
        }
    }
}
