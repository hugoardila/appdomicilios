package com.appdomicilios.notifications

import com.appdomicilios.model.UserRole
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FirebasePushService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            PushManager.syncStoredUserToken(applicationContext, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"].orEmpty()
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"].orEmpty()
        val orderId = remoteMessage.data["order_id"].orEmpty()
        val role = runCatching {
            UserRole.valueOf(remoteMessage.data["target_role"]?.uppercase().orEmpty())
        }.getOrDefault(UserRole.COURIER)

        when (remoteMessage.data["notification_type"]) {
            "new_pending_order" -> {
                if (orderId.isNotBlank()) {
                    showPendingOrderAlert(
                        context = applicationContext,
                        orderId = orderId,
                        customerName = remoteMessage.data["customer_name"].orEmpty(),
                        body = body.ifBlank { "Hay un nuevo domicilio pendiente en la zona." },
                        viewerRole = role,
                    )
                }
            }

            "order_chat_message" -> {
                if (orderId.isNotBlank()) {
                    showIncomingChatAlert(
                        context = applicationContext,
                        orderId = orderId,
                        senderName = title.ifBlank { remoteMessage.data["sender_name"].orEmpty() },
                        preview = body.ifBlank { "Tienes un mensaje nuevo." },
                        viewerRole = role,
                    )
                }
            }
        }
    }
}
