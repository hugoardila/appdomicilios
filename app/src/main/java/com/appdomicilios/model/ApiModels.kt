package com.appdomicilios.model

data class ApiResult<T>(
    val data: T? = null,
    val errorMessage: String? = null,
)

data class CourierOrdersPayload(
    val activeOrders: List<DeliveryOrder>,
    val availableOrders: List<DeliveryOrder>,
)

data class WalletPolicy(
    val customerFreeOrders: Int,
    val courierFreeOrders: Int,
    val minimumTopUp: Int,
    val customerOrderFee: Int,
    val courierOrderFee: Int,
)

data class TopUpSession(
    val reference: String,
    val checkoutPageUrl: String,
    val confirmationReady: Boolean,
)

data class CourierReviewCandidate(
    val id: String,
    val fullName: String,
    val nationalId: String,
    val phone: String,
    val email: String,
    val documentLabel: String,
    val documentUrl: String?,
    val selfieUrl: String?,
    val submittedAtLabel: String,
)
