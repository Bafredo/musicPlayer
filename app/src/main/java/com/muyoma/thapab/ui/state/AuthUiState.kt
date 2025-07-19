package com.muyoma.thapab.ui.state

data class AuthUIState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val username: String = "",
    val password: String = ""
)
