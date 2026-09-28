package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.StickerEntity
import com.example.data.database.entity.StreakRecordEntity
import com.example.data.repository.SudokuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StickersUiState(
    val selectedSticker: StickerEntity? = null,
    val showDetailModal: Boolean = false,
    val filterType: String = "ALL"
)

class StickersViewModel(
    private val repository: SudokuRepository
) : ViewModel() {

    val stickers: StateFlow<List<StickerEntity>> = repository.stickers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val streak: StateFlow<StreakRecordEntity?> = repository.streak
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow(StickersUiState())
    val uiState: StateFlow<StickersUiState> = _uiState.asStateFlow()

    fun selectSticker(sticker: StickerEntity) {
        _uiState.update { it.copy(selectedSticker = sticker, showDetailModal = true) }
    }

    fun dismissDetailModal() {
        _uiState.update { it.copy(showDetailModal = false) }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(filterType = filter) }
    }

    fun equipSticker(sticker: StickerEntity) {
        viewModelScope.launch {
            repository.setFeaturedSticker(sticker.id)
            _uiState.update { it.copy(showDetailModal = false) }
        }
    }
}
