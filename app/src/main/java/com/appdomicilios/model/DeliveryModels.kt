package com.appdomicilios.model

enum class UserRole {
    CUSTOMER,
    COURIER,
}

enum class CourierApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
}

enum class CourierDocumentType(val label: String, val backendValue: String) {
    NATIONAL_ID_FRONT("Cedula (frente)", "national_id_front"),
    DRIVER_LICENSE_FRONT("Licencia (frente)", "driver_license_front"),
}

enum class OrderStatus(val label: String) {
    WAITING("En espera"),
    TAKEN("Tomado"),
    SHOPPING("Comprando"),
    ON_THE_WAY("En camino"),
    DELIVERED("Entregado"),
    CANCELLED("Cancelado"),
}

enum class OrderResolutionReason(val label: String) {
    DELIVERED("Entregado"),
    CUSTOMER_NOT_FOUND("Cliente no encontrado"),
    CUSTOMER_NONCOMPLIANCE("Incumplimiento del cliente"),
    CANCELLED_BY_CUSTOMER("Cancelado por el cliente"),
}

enum class OrderItemStatus(val label: String) {
    PENDING("Pendiente"),
    PURCHASED("Comprado"),
    NOT_FOUND("No encontrado"),
}

enum class PaymentMethod(val label: String) {
    CASH("Efectivo"),
    TRANSFER("Transferencia"),
    DIGITAL("Pago digital"),
}

enum class OrderRequestType(val label: String) {
    SHOPPING("Comprar"),
    PICKUP_AND_DELIVER("Recoger y entregar"),
    OTHER("Otra vuelta"),
}

enum class OrderCategory(
    val label: String,
    val shortLabel: String,
    val description: String,
) {
    SHOPPING(
        label = "Compras",
        shortLabel = "Compras",
        description = "Tiendas, mercado, farmacia y encargos de compra.",
    ),
    RESTAURANTS(
        label = "Restaurantes",
        shortLabel = "Restaurantes",
        description = "Comidas, platos y bebidas preparadas de restaurantes locales.",
    ),
    DOMICILIOS(
        label = "Domicilios",
        shortLabel = "Domicilios",
        description = "Recoger algo en un punto y llevarlo a otro.",
    ),
    TRAMITES(
        label = "Tramites",
        shortLabel = "Tramites",
        description = "Pagos, recibos, diligencias y vueltas especiales.",
    ),
    MOTOTAXI(
        label = "Mototaxi",
        shortLabel = "Mototaxi",
        description = "Solicitudes de traslado en moto dentro de Pitalito.",
    ),
    ENVIOS(
        label = "Envios",
        shortLabel = "Envios",
        description = "Paquetes, sobres y articulos que se deban llevar.",
    ),
}

fun OrderCategory.defaultRequestType(): OrderRequestType {
    return when (this) {
        OrderCategory.SHOPPING,
        OrderCategory.RESTAURANTS,
        -> OrderRequestType.SHOPPING
        OrderCategory.DOMICILIOS,
        OrderCategory.ENVIOS,
        -> OrderRequestType.PICKUP_AND_DELIVER
        OrderCategory.TRAMITES,
        OrderCategory.MOTOTAXI,
        -> OrderRequestType.OTHER
    }
}

enum class WalletChargeMode(val label: String) {
    NONE("Sin cobro"),
    FREE("Gratis"),
    PAID("Descontado"),
    REFUNDED("Devuelto"),
}

data class WalletInfo(
    val balance: Int = 0,
    val minimumTopUp: Int = 10000,
    val customerFreeOrdersRemaining: Int = 0,
    val courierFreeTakesRemaining: Int = 0,
    val customerOrderFee: Int = 1500,
    val courierTakeFee: Int = 1000,
    val customerDeferredCharges: Int = 0,
    val courierDeferredCharges: Int = 0,
    val totalDeferredCharges: Int = 0,
)

data class CourierRatingSummary(
    val average: Double = 0.0,
    val count: Int = 0,
    val warningActive: Boolean = false,
)

data class CustomerReputationSummary(
    val score: Double = 5.0,
    val incidentsCount: Int = 0,
    val warningActive: Boolean = false,
)

data class UserProfile(
    val id: String,
    val fullName: String,
    val nationalId: String,
    val phone: String,
    val email: String,
    val role: UserRole,
    val city: String,
    val approvalStatus: CourierApprovalStatus = CourierApprovalStatus.APPROVED,
    val wallet: WalletInfo = WalletInfo(),
    val courierRating: CourierRatingSummary = CourierRatingSummary(),
)

data class CourierVerificationDraft(
    val documentType: CourierDocumentType,
    val documentFrontUri: String,
    val selfieUri: String,
)

data class AuthResponse(
    val user: UserProfile? = null,
    val errorMessage: String? = null,
)

data class RequestedOrderItem(
    val name: String,
    val quantity: Int = 1,
)

data class OrderItem(
    val id: String,
    val name: String,
    val quantity: Int = 1,
    val status: OrderItemStatus = OrderItemStatus.PENDING,
)

data class OrderMessage(
    val id: String,
    val senderUserId: String? = null,
    val senderName: String,
    val fromCourier: Boolean,
    val isSystem: Boolean = false,
    val body: String,
    val imageUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String? = null,
    val seenByCustomer: Boolean = false,
    val seenByCourier: Boolean = false,
    val seenByCustomerLabel: String = "",
    val seenByCourierLabel: String = "",
    val sentAtLabel: String,
)

data class DeliveryOrder(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String? = null,
    val courierId: String? = null,
    val courierName: String? = null,
    val category: OrderCategory = OrderCategory.SHOPPING,
    val serviceSubcategory: String? = null,
    val requestType: OrderRequestType = OrderRequestType.SHOPPING,
    val status: OrderStatus,
    val addressText: String,
    val addressReference: String? = null,
    val shareLocation: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val destinationReference: String? = null,
    val pickupAddress: String? = null,
    val pickupContactName: String? = null,
    val pickupContactPhone: String? = null,
    val dropoffContactName: String? = null,
    val dropoffContactPhone: String? = null,
    val pickupPaymentAmount: Int = 0,
    val packageWeightKg: Double? = null,
    val city: String,
    val paymentMethod: PaymentMethod,
    val serviceFee: Int,
    val deliveryFee: Int,
    val purchasePlacesCount: Int = 1,
    val customerChargeMode: WalletChargeMode = WalletChargeMode.NONE,
    val customerChargeAmount: Int = 0,
    val courierChargeMode: WalletChargeMode = WalletChargeMode.NONE,
    val courierChargeAmount: Int = 0,
    val resolutionReason: OrderResolutionReason? = null,
    val courierRatingValue: Int? = null,
    val customerReputation: CustomerReputationSummary = CustomerReputationSummary(),
    val originalRequestText: String,
    val referenceImages: List<String>,
    val items: List<OrderItem>,
    val messages: List<OrderMessage>,
    val createdAtLabel: String,
    val updatedAtLabel: String,
)
