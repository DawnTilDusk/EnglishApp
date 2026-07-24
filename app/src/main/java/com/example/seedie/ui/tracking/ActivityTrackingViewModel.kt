package com.example.seedie.ui.tracking

import androidx.lifecycle.ViewModel
import com.example.seedie.domain.usecase.GlobalActivityTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ActivityTrackingViewModel @Inject constructor(
    private val globalActivityTracker: GlobalActivityTracker
) : ViewModel() {

    fun enterModule(moduleId: String) {
        globalActivityTracker.enter(moduleId)
    }
}
