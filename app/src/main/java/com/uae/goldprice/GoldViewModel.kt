package com.uae.goldprice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    data class Success(val data: GoldPriceModel) : UiState()
    data class Error(val message: String) : UiState()
}

class GoldViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        fetchGoldPrice()
    }

    fun fetchGoldPrice() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = RetrofitClient.instance.getGoldPrice()
                val ounceToGram = 31.1034768
                val pricePerGram24k = response.price / ounceToGram

                val model = GoldPriceModel(
                    karat24 = pricePerGram24k,
                    karat22 = pricePerGram24k * (22.0 / 24.0),
                    karat21 = pricePerGram24k * (21.0 / 24.0),
                    karat18 = pricePerGram24k * (18.0 / 24.0),
                    updatedAt = response.updatedAt
                )
                _uiState.value = UiState.Success(model)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
