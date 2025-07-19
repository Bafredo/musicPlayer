package com.muyoma.thapab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyoma.thapab.ui.state.AuthUIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUIState())
    val uiState: StateFlow<AuthUIState> = _uiState

    private fun setLoading(value: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = value)
    }

    private fun setError(message: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    private fun setSuccess(value: Boolean) {
        _uiState.value = _uiState.value.copy(isSuccess = value)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            isSuccess = false
        )
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)

            delay(1000) // Simulate network call

            if (username.isBlank() || password.isBlank()) {
                setError("Username or password cannot be empty")
                setLoading(false)
                return@launch
            }

            if (username == "admin" && password == "admin") {
                setSuccess(true)
            } else {
                setError("Invalid credentials")
            }

            setLoading(false)
        }
    }

    fun signUp(username: String, password: String) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)

            delay(1500) // Simulate network call

            if (username.length < 3 || password.length < 6) {
                setError("Username must be at least 3 characters and password at least 6")
                setLoading(false)
                return@launch
            }

            setSuccess(true)
            setLoading(false)
        }
    }
}
