package com.example.seedie.ui.screens.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.ConvertTokensToDewsResult
import com.example.seedie.domain.model.GardenSpeciesCatalog
import com.example.seedie.domain.repository.DewManager
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.usecase.Currency
import com.example.seedie.domain.usecase.GardenEngine
import com.example.seedie.domain.usecase.GardenSpeciesUi
import com.example.seedie.domain.usecase.GardenUnlockResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlantPickerViewModel @Inject constructor(
    private val gardenEngine: GardenEngine,
    economyManager: EconomyManager,
    private val dewManager: DewManager
) : ViewModel() {

    val catalog: StateFlow<List<GardenSpeciesUi>> = gardenEngine.observeSpeciesCatalog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tokenBalance: StateFlow<Int> = economyManager.totalTokens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dewBalance: StateFlow<Int> = dewManager.totalDews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedSpeciesId = MutableStateFlow<String?>(null)
    val selectedSpeciesId: StateFlow<String?> = _selectedSpeciesId.asStateFlow()

    private val _unlockMessage = MutableStateFlow<String?>(null)
    val unlockMessage: StateFlow<String?> = _unlockMessage.asStateFlow()

    private val _convertMessage = MutableStateFlow<String?>(null)
    val convertMessage: StateFlow<String?> = _convertMessage.asStateFlow()

    fun bootstrap() {
        viewModelScope.launch {
            gardenEngine.ensureDefaultUnlocks()
            val last = gardenEngine.getLastSelectedSpeciesId()
            _selectedSpeciesId.value = last
        }
    }

    fun selectSpecies(speciesId: String) {
        _selectedSpeciesId.value = speciesId
        _unlockMessage.value = null
    }

    suspend fun confirmSelection() {
        val id = _selectedSpeciesId.value ?: GardenSpeciesCatalog.DEFAULT_SPECIES_ID
        gardenEngine.setLastSelectedSpeciesId(id)
    }

    suspend fun unlockSpecies(speciesId: String, currency: Currency = Currency.DEW): GardenUnlockResult {
        val result = gardenEngine.unlockSpecies(speciesId, currency)
        _unlockMessage.value = when (result) {
            GardenUnlockResult.Success -> null
            GardenUnlockResult.AlreadyUnlocked -> null
            GardenUnlockResult.InsufficientTokens -> "代币不足，先去背单词攒一些吧"
            GardenUnlockResult.InsufficientDews -> "露水不够，今天多学一会儿再来试试吧"
            GardenUnlockResult.NotLoggedIn -> "请先登录"
            GardenUnlockResult.UnknownSpecies -> "未知树种"
        }
        return result
    }

    suspend fun convertTokensToDews(tokenAmount: Int): ConvertTokensToDewsResult {
        val result = dewManager.convertTokensToDews(tokenAmount)
        _convertMessage.value = when (result) {
            ConvertTokensToDewsResult.Success -> "兑换成功"
            ConvertTokensToDewsResult.NotEnoughTokens -> "代币不足"
            ConvertTokensToDewsResult.DailyConvertCapReached -> "今日兑换额度已用完（最多 10 代币）"
            ConvertTokensToDewsResult.NotLoggedIn -> "请先登录"
            ConvertTokensToDewsResult.InvalidAmount -> "数量无效"
        }
        return result
    }
}
