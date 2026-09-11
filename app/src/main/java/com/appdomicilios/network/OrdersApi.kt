package com.appdomicilios.network

import android.content.Context
import android.net.Uri
import com.appdomicilios.model.ApiResult
import com.appdomicilios.model.CustomerReputationSummary
import com.appdomicilios.model.CourierOrdersPayload
import com.appdomicilios.model.DeliveryOrder
import com.appdomicilios.model.OrderCategory
import com.appdomicilios.model.OrderItem
import com.appdomicilios.model.OrderItemStatus
import com.appdomicilios.model.OrderMessage
import com.appdomicilios.model.OrderRequestType
import com.appdomicilios.model.OrderResolutionReason
import com.appdomicilios.model.OrderStatus
import com.appdomicilios.model.PaymentMethod
import com.appdomicilios.model.RequestedOrderItem
import com.appdomicilios.model.WalletChargeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object OrdersApi {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrls = ApiConfig.baseUrls

    suspend fun fetchCustomerOrders(customerId: String): ApiResult<List<DeliveryOrder>> = withContext(Dispatchers.IO) {
        getOrders(
            endpoint = "orders.php?role=customer&user_id=$customerId",
            mapper = { json ->
                json.optJSONArray("orders")?.toOrderList().orEmpty()
            },
        )
    }

    suspend fun fetchCourierOrders(courierId: String): ApiResult<CourierOrdersPayload> = withContext(Dispatchers.IO) {
        getOrders(
            endpoint = "orders.php?role=courier&user_id=$courierId",
            mapper = { json ->
                CourierOrdersPayload(
                    activeOrders = json.optJSONArray("active_orders")?.toOrderList().orEmpty(),
                    availableOrders = json.optJSONArray("available_orders")?.toOrderList().orEmpty(),
                )
            },
        )
    }

    suspend fun fetchOrderDetail(orderId: String): ApiResult<DeliveryOrder> = withContext(Dispatchers.IO) {
        getOrders(
            endpoint = "order_detail.php?order_id=$orderId",
            mapper = { json ->
                parseOrder(json.getJSONObject("order"))
            },
        )
    }

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
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        val sanitizedItems = items
            .map { item ->
                RequestedOrderItem(
                    name = item.name.trim(),
                    quantity = item.quantity.coerceAtLeast(1),
                )
            }
            .filter { it.name.isNotBlank() }
        val sanitizedDetails = details?.trim().orEmpty()
        val description = when (requestType) {
            OrderRequestType.SHOPPING -> sanitizedItems.joinToString("\n") { "${it.quantity} x ${it.name}" }
            OrderRequestType.PICKUP_AND_DELIVER -> "Recoger y entregar"
            OrderRequestType.OTHER -> sanitizedDetails
        }

        postOperation(
            endpoint = "create_order.php",
            payload = JSONObject()
                .put("customer_id", customerId)
                .put("service_category", category.toApiValue())
                .put("service_subcategory", serviceSubcategory)
                .put("request_type", requestType.toApiValue())
                .put("description", description)
                .put("details", sanitizedDetails.ifBlank { JSONObject.NULL })
                .put(
                    "items",
                    JSONArray().apply {
                        sanitizedItems.forEach { item ->
                            put(
                                JSONObject()
                                    .put("name", item.name)
                                    .put("quantity", item.quantity),
                            )
                        }
                    },
                )
                .put("address_text", address)
                .put("share_location", shareLocation)
                .put("address_reference", addressReference)
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("destination_latitude", destinationLatitude)
                .put("destination_longitude", destinationLongitude)
                .put("destination_reference", destinationReference)
                .put("payment_method", paymentMethod.name.lowercase())
                .put("pickup_address", pickupAddress)
                .put("pickup_contact_name", pickupContactName)
                .put("pickup_contact_phone", pickupContactPhone)
                .put("dropoff_contact_name", dropoffContactName)
                .put("dropoff_contact_phone", dropoffContactPhone)
                .put("pickup_payment_amount", pickupPaymentAmount)
                .put("package_weight_kg", packageWeightKg),
        ) { json ->
            json.optString("order_id")
        }
    }

    suspend fun sendMessage(
        orderId: String,
        senderUserId: String,
        messageText: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String? = null,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "send_order_message.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("sender_user_id", senderUserId)
                .put("message_text", messageText)
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("location_label", locationLabel),
        ) { Unit }
    }

    suspend fun sendImageMessage(
        context: Context,
        orderId: String,
        senderUserId: String,
        messageText: String,
        imageUri: Uri,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postMultipart(
            context = context,
            endpoint = "send_order_message.php",
            fields = mapOf(
                "order_id" to orderId,
                "sender_user_id" to senderUserId,
                "message_text" to messageText,
            ),
            files = listOf(
                UploadFile(
                    fieldName = "image",
                    uri = imageUri,
                    prefix = "chat_message",
                ),
            ),
        ) { Unit }
    }

    suspend fun takeOrder(orderId: String, courierId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "take_order.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("courier_id", courierId),
        ) { Unit }
    }

    suspend fun updateItemStatus(
        orderId: String,
        itemId: String,
        status: OrderItemStatus,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "update_item_status.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("item_id", itemId)
                .put("status", status.toApiValue()),
        ) { Unit }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        status: OrderStatus,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "update_order_status.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("status", status.toApiValue()),
        ) { Unit }
    }

    suspend fun resolveOrderOutcome(
        orderId: String,
        courierId: String,
        resolutionReason: OrderResolutionReason,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "resolve_order_outcome.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("courier_id", courierId)
                .put("resolution_reason", resolutionReason.toApiValue()),
        ) { Unit }
    }

    suspend fun addOrderItem(orderId: String, productName: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "add_order_item.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("name", productName),
        ) { Unit }
    }

    suspend fun updatePurchasePlaces(orderId: String, purchasePlacesCount: Int): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "update_purchase_places.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("purchase_places_count", purchasePlacesCount),
        ) { Unit }
    }

    suspend fun cancelOrder(orderId: String, customerId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "cancel_order.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("customer_id", customerId),
        ) { Unit }
    }

    suspend fun rateCourier(
        orderId: String,
        customerId: String,
        ratingValue: Int,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "rate_courier.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("customer_id", customerId)
                .put("rating_value", ratingValue),
        ) { Unit }
    }

    suspend fun markMessagesSeen(
        orderId: String,
        viewerUserId: String,
    ): ApiResult<Int> = withContext(Dispatchers.IO) {
        postOperation(
            endpoint = "mark_order_messages_seen.php",
            payload = JSONObject()
                .put("order_id", orderId)
                .put("viewer_user_id", viewerUserId),
        ) { json ->
            json.optInt("updated_count", 0)
        }
    }

    private fun <T> getOrders(
        endpoint: String,
        mapper: (JSONObject) -> T,
    ): ApiResult<T> {
        return request(
            endpoint = endpoint,
            method = "GET",
            payload = null,
            mapper = mapper,
        )
    }

    private fun <T> postOperation(
        endpoint: String,
        payload: JSONObject,
        mapper: (JSONObject) -> T,
    ): ApiResult<T> {
        return request(
            endpoint = endpoint,
            method = "POST",
            payload = payload,
            mapper = mapper,
        )
    }

    private fun <T> postMultipart(
        context: Context,
        endpoint: String,
        fields: Map<String, String>,
        files: List<UploadFile>,
        mapper: (JSONObject) -> T,
    ): ApiResult<T> {
        var lastError: String? = null

        for (baseUrl in baseUrls) {
            val tempFiles = mutableListOf<File>()
            val attempt = runCatching {
                val form = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)

                fields.forEach { (key, value) ->
                    form.addFormDataPart(key, value)
                }

                files.forEach { upload ->
                    val stagedFile = stageUriToTempFile(context, upload)
                    tempFiles += stagedFile
                    val mimeType = context.contentResolver.getType(upload.uri) ?: "image/jpeg"
                    form.addFormDataPart(
                        upload.fieldName,
                        stagedFile.name,
                        stagedFile.asRequestBody(mimeType.toMediaTypeOrNull()),
                    )
                }

                val request = Request.Builder()
                    .url(baseUrl + endpoint)
                    .post(form.build())
                    .build()

                client.newCall(request).execute().use { response ->
                    parseResult(response.body?.string().orEmpty(), mapper)
                }
            }

            tempFiles.forEach { staged ->
                runCatching { staged.delete() }
            }

            if (attempt.isSuccess) {
                return attempt.getOrThrow()
            }

            lastError = attempt.exceptionOrNull()?.message
        }

        return ApiResult(errorMessage = lastError ?: "No fue posible conectar con la API del servidor.")
    }

    private fun <T> request(
        endpoint: String,
        method: String,
        payload: JSONObject?,
        mapper: (JSONObject) -> T,
    ): ApiResult<T> {
        var lastError: String? = null

        for (baseUrl in baseUrls) {
            val attempt = runCatching {
                val builder = Request.Builder().url(baseUrl + endpoint)
                if (method == "POST" && payload != null) {
                    builder.post(payload.toString().toRequestBody(jsonMediaType))
                }
                val request = builder.build()

                client.newCall(request).execute().use { response ->
                    parseResult(response.body?.string().orEmpty(), mapper)
                }
            }

            if (attempt.isSuccess) {
                return attempt.getOrThrow()
            }

            lastError = attempt.exceptionOrNull()?.message
        }

        return ApiResult(errorMessage = lastError ?: "No fue posible conectar con la API del servidor.")
    }

    private fun <T> parseResult(
        body: String,
        mapper: (JSONObject) -> T,
    ): ApiResult<T> {
        val json = if (body.isBlank()) JSONObject() else JSONObject(body)
        val success = json.optBoolean("success", false)
        val message = json.optString("message").ifBlank { "Operacion no disponible." }

        if (!success) {
            return ApiResult(errorMessage = message)
        }

        return ApiResult(data = mapper(json))
    }

    private fun JSONArray.toOrderList(): List<DeliveryOrder> {
        return buildList(length()) {
            for (index in 0 until length()) {
                val orderJson = optJSONObject(index) ?: continue
                add(parseOrder(orderJson))
            }
        }
    }

    private fun parseOrder(orderJson: JSONObject): DeliveryOrder {
        return DeliveryOrder(
            id = orderJson.optString("id"),
            customerId = orderJson.optString("customer_id"),
            customerName = orderJson.optString("customer_name"),
            customerPhone = orderJson.optString("customer_phone").nullIfBlank(),
            courierId = orderJson.optString("courier_id").nullIfBlank(),
            courierName = orderJson.optString("courier_name").nullIfBlank(),
            category = orderJson.optString("service_category").toOrderCategory(
                fallbackRequestType = orderJson.optString("request_type").toRequestType(),
            ),
            serviceSubcategory = orderJson.optString("service_subcategory").nullIfBlank(),
            requestType = orderJson.optString("request_type").toRequestType(),
            status = orderJson.optString("status").toOrderStatus(),
            addressText = orderJson.optString("address_text"),
            addressReference = orderJson.optString("address_reference").nullIfBlank(),
            shareLocation = orderJson.optBoolean("share_location", false),
            latitude = orderJson.optNullableDouble("latitude"),
            longitude = orderJson.optNullableDouble("longitude"),
            destinationLatitude = orderJson.optNullableDouble("destination_latitude"),
            destinationLongitude = orderJson.optNullableDouble("destination_longitude"),
            destinationReference = orderJson.optString("destination_reference").nullIfBlank(),
            pickupAddress = orderJson.optString("pickup_address").nullIfBlank(),
            pickupContactName = orderJson.optString("pickup_contact_name").nullIfBlank(),
            pickupContactPhone = orderJson.optString("pickup_contact_phone").nullIfBlank(),
            dropoffContactName = orderJson.optString("dropoff_contact_name").nullIfBlank(),
            dropoffContactPhone = orderJson.optString("dropoff_contact_phone").nullIfBlank(),
            pickupPaymentAmount = orderJson.optInt("pickup_payment_amount", 0),
            packageWeightKg = orderJson.optNullableDouble("package_weight_kg"),
            city = orderJson.optString("city", "Pitalito, Huila"),
            paymentMethod = orderJson.optString("payment_method").toPaymentMethod(),
            serviceFee = orderJson.optInt("service_fee", 0),
            deliveryFee = orderJson.optInt("delivery_fee", 0),
            purchasePlacesCount = orderJson.optInt("purchase_places_count", 1).coerceAtLeast(1),
            customerChargeMode = orderJson.optString("customer_fee_mode").toWalletChargeMode(
                reversed = orderJson.optBoolean("customer_fee_reversed", false),
            ),
            customerChargeAmount = orderJson.optInt("customer_fee_amount", 0),
            courierChargeMode = orderJson.optString("courier_fee_mode").toWalletChargeMode(
                reversed = orderJson.optBoolean("courier_fee_reversed", false),
            ),
            courierChargeAmount = orderJson.optInt("courier_fee_amount", 0),
            resolutionReason = orderJson.optString("resolution_reason").toResolutionReason(),
            courierRatingValue = if (orderJson.isNull("courier_rating_value")) null else orderJson.optInt("courier_rating_value"),
            customerReputation = parseCustomerReputation(orderJson.optJSONObject("customer_reputation")),
            originalRequestText = orderJson.optString("original_request_text"),
            referenceImages = orderJson.optJSONArray("reference_images")?.let { images ->
                buildList(images.length()) {
                    for (index in 0 until images.length()) {
                        ApiConfig.resolveMediaUrl(images.optString(index))?.let(::add)
                    }
                }
            }.orEmpty(),
            items = orderJson.optJSONArray("items")?.let { itemsJson ->
                buildList(itemsJson.length()) {
                    for (index in 0 until itemsJson.length()) {
                        val itemJson = itemsJson.optJSONObject(index) ?: continue
                        add(
                            OrderItem(
                                id = itemJson.optString("id"),
                                name = itemJson.optString("name"),
                                quantity = itemJson.optInt("quantity", 1).coerceAtLeast(1),
                                status = itemJson.optString("status").toOrderItemStatus(),
                            ),
                        )
                    }
                }
            }.orEmpty(),
            messages = orderJson.optJSONArray("messages")?.let { messagesJson ->
                buildList(messagesJson.length()) {
                    for (index in 0 until messagesJson.length()) {
                        val messageJson = messagesJson.optJSONObject(index) ?: continue
                        add(
                            OrderMessage(
                                id = messageJson.optString("id"),
                                senderUserId = messageJson.optString("sender_user_id").nullIfBlank(),
                                senderName = messageJson.optString("sender_name"),
                                fromCourier = messageJson.optBoolean("from_courier", false),
                                isSystem = messageJson.optString("sender_name") == "Sistema",
                                body = messageJson.optString("message_text"),
                                imageUri = ApiConfig.resolveMediaUrl(messageJson.optString("image_path").nullIfBlank()),
                                latitude = messageJson.optNullableDouble("latitude"),
                                longitude = messageJson.optNullableDouble("longitude"),
                                locationLabel = messageJson.optString("location_label").nullIfBlank(),
                                seenByCustomer = messageJson.optBoolean("seen_by_customer", false),
                                seenByCourier = messageJson.optBoolean("seen_by_courier", false),
                                seenByCustomerLabel = messageJson.optString("seen_by_customer_label"),
                                seenByCourierLabel = messageJson.optString("seen_by_courier_label"),
                                sentAtLabel = messageJson.optString("created_at_label"),
                            ),
                        )
                    }
                }
            }.orEmpty(),
            createdAtLabel = orderJson.optString("created_at_label"),
            updatedAtLabel = orderJson.optString("updated_at_label"),
        )
    }

    private fun String.toOrderStatus(): OrderStatus {
        return when (this) {
            "taken" -> OrderStatus.TAKEN
            "shopping" -> OrderStatus.SHOPPING
            "on_the_way" -> OrderStatus.ON_THE_WAY
            "delivered" -> OrderStatus.DELIVERED
            "cancelled" -> OrderStatus.CANCELLED
            else -> OrderStatus.WAITING
        }
    }

    private fun String.toOrderItemStatus(): OrderItemStatus {
        return when (this) {
            "purchased" -> OrderItemStatus.PURCHASED
            "not_found" -> OrderItemStatus.NOT_FOUND
            else -> OrderItemStatus.PENDING
        }
    }

    private fun String.toPaymentMethod(): PaymentMethod {
        return when (this) {
            "transfer" -> PaymentMethod.TRANSFER
            "digital" -> PaymentMethod.DIGITAL
            else -> PaymentMethod.CASH
        }
    }

    private fun String.toRequestType(): OrderRequestType {
        return when (this) {
            "pickup_delivery", "pickup_and_deliver" -> OrderRequestType.PICKUP_AND_DELIVER
            "other" -> OrderRequestType.OTHER
            else -> OrderRequestType.SHOPPING
        }
    }

    private fun String.toOrderCategory(fallbackRequestType: OrderRequestType): OrderCategory {
        return when (this) {
            "restaurantes", "restaurants" -> OrderCategory.RESTAURANTS
            "domicilios" -> OrderCategory.DOMICILIOS
            "tramites" -> OrderCategory.TRAMITES
            "mototaxi" -> OrderCategory.MOTOTAXI
            "envios" -> OrderCategory.ENVIOS
            "shopping", "compras" -> OrderCategory.SHOPPING
            else -> when (fallbackRequestType) {
                OrderRequestType.SHOPPING -> OrderCategory.SHOPPING
                OrderRequestType.PICKUP_AND_DELIVER -> OrderCategory.DOMICILIOS
                OrderRequestType.OTHER -> OrderCategory.TRAMITES
            }
        }
    }

    private fun OrderItemStatus.toApiValue(): String {
        return when (this) {
            OrderItemStatus.PURCHASED -> "purchased"
            OrderItemStatus.NOT_FOUND -> "not_found"
            OrderItemStatus.PENDING -> "pending"
        }
    }

    private fun OrderStatus.toApiValue(): String {
        return when (this) {
            OrderStatus.WAITING -> "waiting"
            OrderStatus.TAKEN -> "taken"
            OrderStatus.SHOPPING -> "shopping"
            OrderStatus.ON_THE_WAY -> "on_the_way"
            OrderStatus.DELIVERED -> "delivered"
            OrderStatus.CANCELLED -> "cancelled"
        }
    }

    private fun OrderRequestType.toApiValue(): String {
        return when (this) {
            OrderRequestType.SHOPPING -> "shopping"
            OrderRequestType.PICKUP_AND_DELIVER -> "pickup_delivery"
            OrderRequestType.OTHER -> "other"
        }
    }

    private fun OrderCategory.toApiValue(): String {
        return when (this) {
            OrderCategory.SHOPPING -> "shopping"
            OrderCategory.RESTAURANTS -> "restaurantes"
            OrderCategory.DOMICILIOS -> "domicilios"
            OrderCategory.TRAMITES -> "tramites"
            OrderCategory.MOTOTAXI -> "mototaxi"
            OrderCategory.ENVIOS -> "envios"
        }
    }

    private fun String.toWalletChargeMode(reversed: Boolean): WalletChargeMode {
        return when {
            reversed -> WalletChargeMode.REFUNDED
            this == "free" -> WalletChargeMode.FREE
            this == "paid" -> WalletChargeMode.PAID
            else -> WalletChargeMode.NONE
        }
    }

    private fun String.toResolutionReason(): OrderResolutionReason? {
        return when (this) {
            "delivered" -> OrderResolutionReason.DELIVERED
            "customer_not_found" -> OrderResolutionReason.CUSTOMER_NOT_FOUND
            "customer_noncompliance" -> OrderResolutionReason.CUSTOMER_NONCOMPLIANCE
            "cancelled_by_customer" -> OrderResolutionReason.CANCELLED_BY_CUSTOMER
            else -> null
        }
    }

    private fun OrderResolutionReason.toApiValue(): String {
        return when (this) {
            OrderResolutionReason.DELIVERED -> "delivered"
            OrderResolutionReason.CUSTOMER_NOT_FOUND -> "customer_not_found"
            OrderResolutionReason.CUSTOMER_NONCOMPLIANCE -> "customer_noncompliance"
            OrderResolutionReason.CANCELLED_BY_CUSTOMER -> "cancelled_by_customer"
        }
    }

    private fun parseCustomerReputation(customerReputationJson: JSONObject?): CustomerReputationSummary {
        if (customerReputationJson == null) {
            return CustomerReputationSummary()
        }

        return CustomerReputationSummary(
            score = customerReputationJson.optDouble("score", 5.0),
            incidentsCount = customerReputationJson.optInt("incidents_count", 0),
            warningActive = customerReputationJson.optBoolean("warning_active", false),
        )
    }

    private fun String.nullIfBlank(): String? = if (isBlank()) null else this

    private fun JSONObject.optNullableDouble(key: String): Double? {
        return if (isNull(key)) null else optDouble(key)
    }

    private fun stageUriToTempFile(context: Context, upload: UploadFile): File {
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(upload.uri)
            ?: error("No pudimos leer una de las imagenes seleccionadas.")

        val mimeType = resolver.getType(upload.uri) ?: "image/jpeg"
        val extension = when (mimeType.lowercase()) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> ".jpg"
        }

        return inputStream.use { stream ->
            val file = File.createTempFile(upload.prefix, extension, context.cacheDir)
            file.sink().buffer().use { sink ->
                sink.writeAll(stream.source())
            }
            file
        }
    }

    private data class UploadFile(
        val fieldName: String,
        val uri: Uri,
        val prefix: String,
    )
}
