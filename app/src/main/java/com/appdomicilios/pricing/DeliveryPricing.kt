package com.appdomicilios.pricing

import com.appdomicilios.model.OrderCategory
import com.appdomicilios.model.OrderRequestType

const val URBAN_MINIMUM_DELIVERY_FEE = 5000
const val SAME_PLACE_MULTI_PRODUCT_FEE = 7000
const val EXTRA_PURCHASE_PLACE_FEE = 2000
const val SHIPPING_BASE_WEIGHT_KG = 2.0
const val SHIPPING_EXTRA_KILO_FEE = 2000

fun calculateDeliveryFee(
    productCount: Int,
    purchasePlacesCount: Int,
): Int {
    val normalizedProducts = productCount.coerceAtLeast(1)
    val normalizedPlaces = purchasePlacesCount.coerceAtLeast(1)

    val baseFee = if (normalizedProducts <= 1) {
        URBAN_MINIMUM_DELIVERY_FEE
    } else {
        SAME_PLACE_MULTI_PRODUCT_FEE
    }

    return baseFee + ((normalizedPlaces - 1) * EXTRA_PURCHASE_PLACE_FEE)
}

fun calculateDeliveryFee(
    requestType: OrderRequestType,
    productCount: Int,
    purchasePlacesCount: Int,
): Int {
    return when (requestType) {
        OrderRequestType.SHOPPING -> calculateDeliveryFee(productCount, purchasePlacesCount)
        OrderRequestType.PICKUP_AND_DELIVER,
        OrderRequestType.OTHER,
        -> URBAN_MINIMUM_DELIVERY_FEE
    }
}

fun calculateCategoryDeliveryFee(
    category: OrderCategory,
    requestType: OrderRequestType,
    productCount: Int,
    purchasePlacesCount: Int,
    packageWeightKg: Double? = null,
): Int {
    return when (category) {
        OrderCategory.ENVIOS -> calculateShippingFee(packageWeightKg)
        else -> calculateDeliveryFee(
            requestType = requestType,
            productCount = productCount,
            purchasePlacesCount = purchasePlacesCount,
        )
    }
}

fun calculateShippingFee(packageWeightKg: Double?): Int {
    val safeWeight = (packageWeightKg ?: 0.0).coerceAtLeast(0.0)
    if (safeWeight <= SHIPPING_BASE_WEIGHT_KG) {
        return URBAN_MINIMUM_DELIVERY_FEE
    }

    val additionalKilos = kotlin.math.ceil(safeWeight - SHIPPING_BASE_WEIGHT_KG).toInt()
    return URBAN_MINIMUM_DELIVERY_FEE + (additionalKilos * SHIPPING_EXTRA_KILO_FEE)
}

fun deliveryFeeRuleSummary(): String {
    return "1 producto $${URBAN_MINIMUM_DELIVERY_FEE}, 2 o 3 productos en el mismo lugar $${SAME_PLACE_MULTI_PRODUCT_FEE}, y cada lugar extra suma $${EXTRA_PURCHASE_PLACE_FEE}."
}

fun shippingFeeRuleSummary(): String {
    return "Hasta ${SHIPPING_BASE_WEIGHT_KG} kg pagas $${URBAN_MINIMUM_DELIVERY_FEE}; por cada kilo adicional se suman $${SHIPPING_EXTRA_KILO_FEE}."
}
