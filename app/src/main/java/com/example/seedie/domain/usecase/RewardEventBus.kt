package com.example.seedie.domain.usecase

import com.example.seedie.domain.model.RewardEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<RewardEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: RewardEvent) {
        _events.emit(event)
    }
}