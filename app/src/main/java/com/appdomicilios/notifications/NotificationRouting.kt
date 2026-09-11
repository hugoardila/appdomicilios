package com.appdomicilios.notifications

import android.content.Intent
import com.appdomicilios.model.UserRole

const val extraNotificationOrderId = "notification_order_id"
const val extraNotificationRole = "notification_role"

data class PendingNotificationRoute(
    val orderId: String,
    val role: UserRole,
)

fun readPendingNotificationRoute(intent: Intent?): PendingNotificationRoute? {
    val orderId = intent?.getStringExtra(extraNotificationOrderId)?.trim().orEmpty()
    if (orderId.isBlank()) {
        return null
    }

    val role = runCatching {
        UserRole.valueOf(
            intent?.getStringExtra(extraNotificationRole)
                ?.trim()
                ?.uppercase()
                .orEmpty(),
        )
    }.getOrDefault(UserRole.CUSTOMER)

    return PendingNotificationRoute(orderId = orderId, role = role)
}
