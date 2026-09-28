package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.FriendEntity
import com.example.data.database.entity.StreakRecordEntity
import com.example.data.repository.SudokuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SocialUiState(
    val myName: String = "Obsidian Solver",
    val myFriendCode: String = "OBSIDIAN-9428",
    val showAddFriendDialog: Boolean = false,
    val addFriendCodeInput: String = "",
    val addFriendNameInput: String = "",
    val feedbackMessage: String? = null
)

class SocialViewModel(
    private val repository: SudokuRepository
) : ViewModel() {

    val friends: StateFlow<List<FriendEntity>> = repository.friends
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

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    fun openAddFriendDialog() {
        _uiState.update {
            it.copy(
                showAddFriendDialog = true,
                addFriendCodeInput = "",
                addFriendNameInput = "",
                feedbackMessage = null
            )
        }
    }

    fun closeAddFriendDialog() {
        _uiState.update { it.copy(showAddFriendDialog = false, feedbackMessage = null) }
    }

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(addFriendCodeInput = code) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(addFriendNameInput = name) }
    }

    fun addFriend() {
        val state = _uiState.value
        val name = state.addFriendNameInput.trim().ifEmpty { "Contender #${(100..999).random()}" }
        val code = state.addFriendCodeInput.trim().ifEmpty { "SUDOKU-${(1000..9999).random()}" }

        viewModelScope.launch {
            repository.addFriend(name, code)
            _uiState.update {
                it.copy(
                    showAddFriendDialog = false,
                    feedbackMessage = "Added $name to your friends list!"
                )
            }
        }
    }

    fun sendKudos(friend: FriendEntity) {
        viewModelScope.launch {
            repository.sendKudosToFriend(friend)
            _uiState.update {
                it.copy(feedbackMessage = "Sent a streak cheer to ${friend.name}! 🔥")
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
