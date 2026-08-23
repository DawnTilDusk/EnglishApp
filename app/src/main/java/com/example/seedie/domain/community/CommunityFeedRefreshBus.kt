package com.example.seedie.domain.community

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Signals community feed to reload (profile grade change, tab revisit, etc.). */
@Singleton
class CommunityFeedRefreshBus @Inject constructor() {
    private val _ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val ticks: SharedFlow<Unit> = _ticks.asSharedFlow()

    fun requestRefresh() {
        _ticks.tryEmit(Unit)
    }
}
