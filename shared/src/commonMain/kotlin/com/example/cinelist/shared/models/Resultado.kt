package com.example.cinelist.shared.models

/**
 * Recurso multiplataforma para representar respostas assíncronas no Android e iOS.
 */
sealed class CineResult<out T> {
    data class Success<T>(val data: T) : CineResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : CineResult<Nothing>()
    object Loading : CineResult<Nothing>()
}
