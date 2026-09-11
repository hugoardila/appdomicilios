package com.appdomicilios.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appdomicilios.model.CourierReviewCandidate
import com.appdomicilios.network.CourierReviewApi

object CourierReviewRepository {
    var accessKey by mutableStateOf("")
        private set

    var pendingCouriers by mutableStateOf<List<CourierReviewCandidate>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    suspend fun signIn(accessKey: String): Boolean {
        isLoading = true
        val result = CourierReviewApi.fetchPendingCouriers(accessKey)
        isLoading = false
        errorMessage = result.errorMessage

        return if (result.data != null) {
            this.accessKey = accessKey.trim()
            pendingCouriers = result.data
            true
        } else {
            false
        }
    }

    suspend fun refresh() {
        if (accessKey.isBlank()) {
            errorMessage = "Escribe la clave interna para revisar domiciliarios."
            return
        }

        isLoading = true
        val result = CourierReviewApi.fetchPendingCouriers(accessKey)
        isLoading = false
        errorMessage = result.errorMessage
        result.data?.let { pendingCouriers = it }
    }

    suspend fun approve(courierId: String) {
        if (accessKey.isBlank()) return
        isLoading = true
        val result = CourierReviewApi.approveCourier(accessKey, courierId)
        isLoading = false
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            pendingCouriers = pendingCouriers.filterNot { it.id == courierId }
        }
    }

    suspend fun reject(courierId: String) {
        if (accessKey.isBlank()) return
        isLoading = true
        val result = CourierReviewApi.rejectCourier(accessKey, courierId)
        isLoading = false
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            pendingCouriers = pendingCouriers.filterNot { it.id == courierId }
        }
    }

    fun signOut() {
        accessKey = ""
        pendingCouriers = emptyList()
        errorMessage = null
        isLoading = false
    }
}
