package com.appdomicilios.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appdomicilios.model.ApiResult
import com.appdomicilios.model.CourierOrdersPayload
import com.appdomicilios.model.DeliveryOrder
import com.appdomicilios.model.OrderCategory
import com.appdomicilios.model.OrderItemStatus
import com.appdomicilios.model.OrderRequestType
import com.appdomicilios.model.OrderResolutionReason
import com.appdomicilios.model.OrderStatus
import com.appdomicilios.model.PaymentMethod
import com.appdomicilios.model.RequestedOrderItem
import com.appdomicilios.network.AuthApi
import com.appdomicilios.network.OrdersApi

object OrdersRepository {
    var customerOrders by mutableStateOf<List<DeliveryOrder>>(emptyList())
        private set

    var courierActiveOrders by mutableStateOf<List<DeliveryOrder>>(emptyList())
        private set

    var courierAvailableOrders by mutableStateOf<List<DeliveryOrder>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val orderCache = mutableStateMapOf<String, DeliveryOrder>()

    suspend fun refreshCustomerOrders(customerId: String) {
        isLoading = true
        val result = OrdersApi.fetchCustomerOrders(customerId)
        isLoading = false
        errorMessage = result.errorMessage
        result.data?.let { orders ->
            customerOrders = orders
            orders.forEach { orderCache[it.id] = it }
        }
    }

    suspend fun refreshCourierOrders(courierId: String) {
        isLoading = true
        val result = OrdersApi.fetchCourierOrders(courierId)
        isLoading = false
        errorMessage = result.errorMessage
        result.data?.let { payload ->
            courierActiveOrders = payload.activeOrders
            courierAvailableOrders = payload.availableOrders
            (payload.activeOrders + payload.availableOrders).forEach { orderCache[it.id] = it }
        }
    }

    suspend fun refreshOrderDetail(orderId: String): ApiResult<DeliveryOrder> {
        val result = OrdersApi.fetchOrderDetail(orderId)
        errorMessage = result.errorMessage
        result.data?.let { orderCache[orderId] = it }
        return result
    }

    fun cachedOrder(orderId: String): DeliveryOrder? = orderCache[orderId]

    suspend fun createOrder(
        customerId: String,
        category: OrderCategory,
        serviceSubcategory: String?,
        requestType: OrderRequestType,
        items: List<RequestedOrderItem>,
        details: String?,
        address: String,
        shareLocation: Boolean,
        addressReference: String?,
        latitude: Double?,
        longitude: Double?,
        destinationLatitude: Double?,
        destinationLongitude: Double?,
        destinationReference: String?,
        paymentMethod: PaymentMethod,
        pickupAddress: String?,
        pickupContactName: String?,
        pickupContactPhone: String?,
        dropoffContactName: String?,
        dropoffContactPhone: String?,
        pickupPaymentAmount: Int,
        packageWeightKg: Double?,
    ): ApiResult<String> {
        val result = OrdersApi.createOrder(
            customerId = customerId,
            category = category,
            serviceSubcategory = serviceSubcategory,
            requestType = requestType,
            items = items,
            details = details,
            address = address,
            shareLocation = shareLocation,
            addressReference = addressReference,
            latitude = latitude,
            longitude = longitude,
            destinationLatitude = destinationLatitude,
            destinationLongitude = destinationLongitude,
            destinationReference = destinationReference,
            paymentMethod = paymentMethod,
            pickupAddress = pickupAddress,
            pickupContactName = pickupContactName,
            pickupContactPhone = pickupContactPhone,
            dropoffContactName = dropoffContactName,
            dropoffContactPhone = dropoffContactPhone,
            pickupPaymentAmount = pickupPaymentAmount,
            packageWeightKg = packageWeightKg,
        )
        errorMessage = result.errorMessage
        if (result.data != null) {
            refreshCurrentUser(customerId)
            refreshCustomerOrders(customerId)
            refreshOrderDetail(result.data)
        }
        return result
    }

    suspend fun takeOrder(orderId: String, courierId: String): ApiResult<Unit> {
        val result = OrdersApi.takeOrder(orderId, courierId)
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshCurrentUser(courierId)
            refreshCourierOrders(courierId)
            refreshOrderDetail(orderId)
        }
        return result
    }

    suspend fun updateItemStatus(
        orderId: String,
        itemId: String,
        status: OrderItemStatus,
        currentUserId: String?,
    ): ApiResult<Unit> {
        val result = OrdersApi.updateItemStatus(orderId, itemId, status)
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshOrderDetail(orderId)
            currentUserId?.let { refreshCourierOrders(it) }
        }
        return result
    }

    suspend fun updateOrderStatus(
        orderId: String,
        status: OrderStatus,
        currentUserId: String?,
    ): ApiResult<Unit> {
        val result = OrdersApi.updateOrderStatus(orderId, status)
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshOrderDetail(orderId)
            currentUserId?.let { refreshCourierOrders(it) }
        }
        return result
    }

    suspend fun resolveOrderOutcome(
        orderId: String,
        courierId: String,
        resolutionReason: OrderResolutionReason,
    ): ApiResult<Unit> {
        val result = OrdersApi.resolveOrderOutcome(
            orderId = orderId,
            courierId = courierId,
            resolutionReason = resolutionReason,
        )
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshOrderDetail(orderId)
            refreshCourierOrders(courierId)
        }
        return result
    }

    suspend fun addOrderItem(
        orderId: String,
        productName: String,
        currentCustomerId: String?,
    ): ApiResult<Unit> {
        val result = OrdersApi.addOrderItem(orderId, productName)
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshOrderDetail(orderId)
            currentCustomerId?.let { refreshCustomerOrders(it) }
        }
        return result
    }

    suspend fun updatePurchasePlaces(
        orderId: String,
        purchasePlacesCount: Int,
        currentUserId: String?,
    ): ApiResult<Unit> {
        val result = OrdersApi.updatePurchasePlaces(orderId, purchasePlacesCount)
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshOrderDetail(orderId)
            currentUserId?.let { refreshCourierOrders(it) }
        }
        return result
    }

    suspend fun cancelOrder(
        orderId: String,
        customerId: String,
    ): ApiResult<Unit> {
        val result = OrdersApi.cancelOrder(orderId, customerId)
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshCurrentUser(customerId)
            refreshCustomerOrders(customerId)
            refreshOrderDetail(orderId)
        }
        return result
    }

    suspend fun sendMessage(
        orderId: String,
        senderUserId: String,
        messageText: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String? = null,
    ): ApiResult<Unit> {
        val result = OrdersApi.sendMessage(
            orderId = orderId,
            senderUserId = senderUserId,
            messageText = messageText,
            latitude = latitude,
            longitude = longitude,
            locationLabel = locationLabel,
        )
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshOrderDetail(orderId)
        }
        return result
    }

    suspend fun sendImageMessage(
        context: Context,
        orderId: String,
        senderUserId: String,
        messageText: String,
        imageUri: Uri,
    ): ApiResult<Unit> {
        val result = OrdersApi.sendImageMessage(
            context = context,
            orderId = orderId,
            senderUserId = senderUserId,
            messageText = messageText,
            imageUri = imageUri,
        )
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshOrderDetail(orderId)
        }
        return result
    }

    suspend fun markMessagesSeen(
        orderId: String,
        viewerUserId: String,
    ): ApiResult<Int> {
        val result = OrdersApi.markMessagesSeen(orderId, viewerUserId)
        errorMessage = result.errorMessage
        if (result.errorMessage == null && (result.data ?: 0) > 0) {
            refreshOrderDetail(orderId)
        }
        return result
    }

    suspend fun rateCourier(
        orderId: String,
        customerId: String,
        ratingValue: Int,
    ): ApiResult<Unit> {
        val result = OrdersApi.rateCourier(
            orderId = orderId,
            customerId = customerId,
            ratingValue = ratingValue,
        )
        errorMessage = result.errorMessage
        if (result.errorMessage == null) {
            refreshOrderDetail(orderId)
            refreshCustomerOrders(customerId)
        }
        return result
    }

    fun clear() {
        customerOrders = emptyList()
        courierActiveOrders = emptyList()
        courierAvailableOrders = emptyList()
        orderCache.clear()
        errorMessage = null
        isLoading = false
    }

    suspend fun refreshCurrentUser(userId: String) {
        val authResult = AuthApi.fetchUserProfile(userId)
        authResult.user?.let { DemoRepository.applyAuthenticatedUser(it) }
    }

    fun clearError() {
        errorMessage = null
    }
}
