package com.appdomicilios.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appdomicilios.AppContextHolder
import com.appdomicilios.model.AuthResponse
import com.appdomicilios.model.CourierApprovalStatus
import com.appdomicilios.model.DeliveryOrder
import com.appdomicilios.model.OrderItem
import com.appdomicilios.model.OrderItemStatus
import com.appdomicilios.model.OrderMessage
import com.appdomicilios.model.OrderStatus
import com.appdomicilios.model.PaymentMethod
import com.appdomicilios.model.UserProfile
import com.appdomicilios.model.UserRole
import com.appdomicilios.model.WalletChargeMode
import com.appdomicilios.model.WalletInfo
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class DemoAccount(
    val profile: UserProfile,
    val password: String,
)

object DemoRepository {
    private const val defaultCity = "Pitalito, Huila"

    private val demoCustomer = UserProfile(
        id = "customer-1",
        fullName = "Laura Trujillo",
        nationalId = "1.111.222.333",
        phone = "3100000001",
        email = "laura@example.com",
        role = UserRole.CUSTOMER,
        city = defaultCity,
        wallet = WalletInfo(
            balance = 0,
            customerFreeOrdersRemaining = 2,
        ),
    )

    private val demoCourier = UserProfile(
        id = "courier-1",
        fullName = "Andres Imbachi",
        nationalId = "1.222.333.444",
        phone = "3100000002",
        email = "andres@example.com",
        role = UserRole.COURIER,
        city = defaultCity,
        approvalStatus = CourierApprovalStatus.APPROVED,
        wallet = WalletInfo(
            balance = 0,
            courierFreeTakesRemaining = 3,
        ),
    )

    val demoCustomerEmail = demoCustomer.email
    val demoCustomerPassword = "cliente123"
    val demoCourierEmail = demoCourier.email
    val demoCourierPassword = "domi123"

    private val accounts = mutableStateListOf(
        DemoAccount(profile = demoCustomer, password = demoCustomerPassword),
        DemoAccount(profile = demoCourier, password = demoCourierPassword),
    )

    var currentUser by mutableStateOf<UserProfile?>(null)
        private set

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "CO"))

    private val orders = mutableStateListOf<DeliveryOrder>().apply {
        addAll(
            listOf(
                DeliveryOrder(
                    id = "order-101",
                    customerId = demoCustomer.id,
                    customerName = demoCustomer.fullName,
                    status = OrderStatus.WAITING,
                    addressText = "Cra 4 #12-18, barrio Los Lagos",
                    shareLocation = true,
                    city = defaultCity,
                    paymentMethod = PaymentMethod.CASH,
                    serviceFee = 0,
                    deliveryFee = 6000,
                    customerChargeMode = WalletChargeMode.FREE,
                    originalRequestText = "Arroz Diana 1kg\nHuevos por cubeta\nTomates maduros",
                    referenceImages = emptyList(),
                    items = listOf(
                        OrderItem(id = "item-1", name = "Arroz Diana 1kg"),
                        OrderItem(id = "item-2", name = "Huevos por cubeta"),
                        OrderItem(id = "item-3", name = "Tomates maduros"),
                    ),
                    messages = listOf(
                        systemMessage("Pedido creado y esperando un domiciliario."),
                    ),
                    createdAtLabel = currentTimestamp(),
                    updatedAtLabel = currentTimestamp(),
                ),
                DeliveryOrder(
                    id = "order-102",
                    customerId = demoCustomer.id,
                    customerName = demoCustomer.fullName,
                    courierId = demoCourier.id,
                    courierName = demoCourier.fullName,
                    status = OrderStatus.SHOPPING,
                    addressText = "Mz B Casa 8, conjunto Campo Real",
                    shareLocation = false,
                    city = defaultCity,
                    paymentMethod = PaymentMethod.TRANSFER,
                    serviceFee = 1500,
                    deliveryFee = 7000,
                    customerChargeMode = WalletChargeMode.PAID,
                    customerChargeAmount = 1500,
                    courierChargeMode = WalletChargeMode.FREE,
                    originalRequestText = "Leche deslactosada\nPan integral\nBanano",
                    referenceImages = emptyList(),
                    items = listOf(
                        OrderItem(id = "item-4", name = "Leche deslactosada", status = OrderItemStatus.PURCHASED),
                        OrderItem(id = "item-5", name = "Pan integral"),
                        OrderItem(id = "item-6", name = "Banano"),
                    ),
                    messages = listOf(
                        systemMessage("Pedido tomado por Andres Imbachi."),
                        OrderMessage(
                            id = UUID.randomUUID().toString(),
                            senderName = demoCourier.fullName,
                            fromCourier = true,
                            body = "Voy saliendo a la galeria. Si no encuentro el pan integral te escribo.",
                            sentAtLabel = currentTimestamp(),
                        ),
                    ),
                    createdAtLabel = currentTimestamp(),
                    updatedAtLabel = currentTimestamp(),
                ),
                DeliveryOrder(
                    id = "order-103",
                    customerId = "customer-2",
                    customerName = "Martha Cuellar",
                    status = OrderStatus.WAITING,
                    addressText = "Calle 7 #3-25, centro",
                    shareLocation = true,
                    city = defaultCity,
                    paymentMethod = PaymentMethod.DIGITAL,
                    serviceFee = 0,
                    deliveryFee = 5000,
                    customerChargeMode = WalletChargeMode.FREE,
                    originalRequestText = "Acetaminofen 500mg\nVitamina C\nAgua micelar",
                    referenceImages = emptyList(),
                    items = listOf(
                        OrderItem(id = "item-7", name = "Acetaminofen 500mg"),
                        OrderItem(id = "item-8", name = "Vitamina C"),
                        OrderItem(id = "item-9", name = "Agua micelar"),
                    ),
                    messages = listOf(
                        systemMessage("Pedido creado y esperando un domiciliario."),
                    ),
                    createdAtLabel = currentTimestamp(),
                    updatedAtLabel = currentTimestamp(),
                ),
            ),
        )
    }

    fun currency(value: Int): String = currencyFormat.format(value)

    fun login(email: String, password: String): AuthResponse {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val account = accounts.firstOrNull {
            it.profile.email.equals(cleanEmail, ignoreCase = true) && it.password == cleanPassword
        } ?: return AuthResponse(errorMessage = "No encontramos una cuenta con esos datos.")

        currentUser = account.profile
        return AuthResponse(user = account.profile)
    }

    fun register(
        fullName: String,
        nationalId: String,
        phone: String,
        email: String,
        password: String,
        role: UserRole,
    ): AuthResponse {
        val cleanEmail = email.trim()
        if (accounts.any { it.profile.email.equals(cleanEmail, ignoreCase = true) }) {
            return AuthResponse(errorMessage = "Ese correo ya esta registrado.")
        }

        val profile = UserProfile(
            id = "${role.name.lowercase()}-${UUID.randomUUID().toString().take(8)}",
            fullName = fullName.trim(),
            nationalId = nationalId.trim(),
            phone = phone.trim(),
            email = cleanEmail,
            role = role,
            city = defaultCity,
            approvalStatus = if (role == UserRole.COURIER) {
                CourierApprovalStatus.PENDING
            } else {
                CourierApprovalStatus.APPROVED
            },
        )

        accounts.add(
            DemoAccount(
                profile = profile,
                password = password,
            ),
        )
        currentUser = profile
        return AuthResponse(user = profile)
    }

    fun applyAuthenticatedUser(user: UserProfile) {
        currentUser = user
        if (AppContextHolder.isReady) {
            SessionStore.saveUser(AppContextHolder.applicationContext, user)
        }
        val existingIndex = accounts.indexOfFirst { it.profile.id == user.id }
        val account = DemoAccount(profile = user, password = "")

        if (existingIndex >= 0) {
            accounts[existingIndex] = account
        } else {
            accounts.add(account)
        }
    }

    fun logout() {
        currentUser = null
        if (AppContextHolder.isReady) {
            SessionStore.clear(AppContextHolder.applicationContext)
        }
    }

    fun customerOrders(customerId: String): List<DeliveryOrder> {
        return orders.filter { it.customerId == customerId }.sortedByDescending { it.updatedAtLabel }
    }

    fun courierOrders(courierId: String): List<DeliveryOrder> {
        return orders.filter {
            it.courierId == courierId &&
                it.status != OrderStatus.DELIVERED &&
                it.status != OrderStatus.CANCELLED
        }
    }

    fun availableOrders(): List<DeliveryOrder> {
        return orders.filter { it.status == OrderStatus.WAITING }
    }

    fun orderById(orderId: String): DeliveryOrder? {
        return orders.firstOrNull { it.id == orderId }
    }

    fun createOrder(
        customerId: String,
        customerName: String,
        description: String,
        address: String,
        shareLocation: Boolean,
        paymentMethod: PaymentMethod,
        referenceImages: List<String>,
    ): DeliveryOrder {
        val cleanDescription = description.trim()
        val items = cleanDescription
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, product ->
                OrderItem(
                    id = "item-${UUID.randomUUID()}-$index",
                    name = product,
                )
            }
            .toList()

        val timestamp = currentTimestamp()
        val order = DeliveryOrder(
            id = "order-${UUID.randomUUID().toString().take(8)}",
            customerId = customerId,
            customerName = customerName,
            status = OrderStatus.WAITING,
            addressText = address.trim(),
            shareLocation = shareLocation,
            city = defaultCity,
            paymentMethod = paymentMethod,
            serviceFee = 0,
            deliveryFee = 6000,
            customerChargeMode = WalletChargeMode.FREE,
            originalRequestText = cleanDescription,
            referenceImages = referenceImages,
            items = items,
            messages = listOf(systemMessage("Pedido creado y esperando un domiciliario.")),
            createdAtLabel = timestamp,
            updatedAtLabel = timestamp,
        )
        orders.add(0, order)
        return order
    }

    fun takeOrder(orderId: String, courierId: String): Boolean {
        val courierProfile = userById(courierId) ?: return false
        val activeOrders = courierOrders(courierId)
        if (activeOrders.size >= 3) return false

        val order = orderById(orderId) ?: return false
        if (order.status != OrderStatus.WAITING) return false

        updateOrder(orderId) {
            it.copy(
                courierId = courierId,
                courierName = courierProfile.fullName,
                status = OrderStatus.TAKEN,
                messages = it.messages + systemMessage("Pedido tomado por ${courierProfile.fullName}."),
                updatedAtLabel = currentTimestamp(),
            )
        }
        return true
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        updateOrder(orderId) { current ->
            current.copy(
                status = newStatus,
                messages = current.messages + systemMessage("Estado actualizado a ${newStatus.label}."),
                updatedAtLabel = currentTimestamp(),
            )
        }
    }

    fun updateItemStatus(orderId: String, itemId: String, newStatus: OrderItemStatus) {
        updateOrder(orderId) { current ->
            val updatedItems = current.items.map { item ->
                if (item.id == itemId) item.copy(status = newStatus) else item
            }

            val hasPending = updatedItems.any { it.status == OrderItemStatus.PENDING }
            val nextStatus = when {
                current.status == OrderStatus.TAKEN -> OrderStatus.SHOPPING
                !hasPending && current.status == OrderStatus.SHOPPING -> OrderStatus.ON_THE_WAY
                else -> current.status
            }

            current.copy(
                items = updatedItems,
                status = nextStatus,
                updatedAtLabel = currentTimestamp(),
            )
        }
    }

    fun addExtraItem(orderId: String, productName: String) {
        val cleanName = productName.trim()
        if (cleanName.isEmpty()) return

        updateOrder(orderId) { current ->
            if (current.status == OrderStatus.ON_THE_WAY || current.status == OrderStatus.DELIVERED) {
                return@updateOrder current
            }

            val updatedText = buildString {
                append(current.originalRequestText)
                if (current.originalRequestText.isNotBlank()) append("\n")
                append(cleanName)
            }

            current.copy(
                originalRequestText = updatedText,
                items = current.items + OrderItem(
                    id = "item-${UUID.randomUUID()}",
                    name = cleanName,
                ),
                messages = current.messages + systemMessage("El cliente agrego: $cleanName"),
                updatedAtLabel = currentTimestamp(),
            )
        }
    }

    fun sendMessage(
        orderId: String,
        senderName: String,
        fromCourier: Boolean,
        body: String,
        imageUri: String? = null,
    ) {
        val text = body.trim()
        if (text.isEmpty() && imageUri == null) return

        updateOrder(orderId) { current ->
            current.copy(
                messages = current.messages + OrderMessage(
                    id = UUID.randomUUID().toString(),
                    senderName = senderName,
                    fromCourier = fromCourier,
                    body = text,
                    imageUri = imageUri,
                    sentAtLabel = currentTimestamp(),
                ),
                updatedAtLabel = currentTimestamp(),
            )
        }
    }

    private fun userById(userId: String): UserProfile? {
        return accounts.firstOrNull { it.profile.id == userId }?.profile
    }

    private fun updateOrder(orderId: String, transform: (DeliveryOrder) -> DeliveryOrder) {
        val index = orders.indexOfFirst { it.id == orderId }
        if (index == -1) return
        orders[index] = transform(orders[index])
    }

    private fun systemMessage(text: String): OrderMessage {
        return OrderMessage(
            id = UUID.randomUUID().toString(),
            senderName = "Sistema",
            fromCourier = false,
            body = text,
            sentAtLabel = currentTimestamp(),
        )
    }

    private fun currentTimestamp(): String = dateFormat.format(Date())
}
