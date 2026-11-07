package com.example.seedie.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Background time must not count as active practice. */
@Composable
fun PracticeLifecycle(onActiveChanged: (Boolean) -> Unit) {
    val owner = LocalLifecycleOwner.current
    val callback = rememberUpdatedState(onActiveChanged)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) callback.value(true)
            if (event == Lifecycle.Event.ON_STOP) callback.value(false)
        }
        owner.lifecycle.addObserver(observer)
        callback.value(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            callback.value(false)
            owner.lifecycle.removeObserver(observer)
        }
    }
}
