package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.StickerEntity
import com.example.data.repository.SudokuRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StickersUiState(
    val stickers: List<StickerEntity> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val errorMessage: String? = null
)

class StickersViewModel(
    repository: SudokuRepository
) : ViewModel() {

    val uiState: StateFlow<StickersUiState> = repository.stickers
        .map { list ->
            val displayList = list.ifEmpty { SudokuRepository.defaultStickerCatalog() }
            StickersUiState(
                stickers = displayList,
                unlockedCount = displayList.count { it.unlocked },
                totalCount = displayList.size
            )
        }
        .catch { error ->
            Log.w("StickersViewModel", "Error observing stickers", error)
            val fallback = SudokuRepository.defaultStickerCatalog()
            emit(
                StickersUiState(
                    stickers = fallback,
                    unlockedCount = 0,
                    totalCount = fallback.size,
                    errorMessage = error.localizedMessage
                )
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StickersUiState(
                stickers = SudokuRepository.defaultStickerCatalog(),
                totalCount = SudokuRepository.defaultStickerCatalog().size
            )
        )
}
