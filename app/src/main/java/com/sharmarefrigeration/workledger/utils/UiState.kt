package com.sharmarefrigeration.workledger.utils

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val action: (() -> Unit)? = null) : UiState<Nothing>()
}