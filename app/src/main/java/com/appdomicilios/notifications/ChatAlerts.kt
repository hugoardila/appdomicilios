package com.appdomicilios.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.appdomicilios.MainActivity
import com.appdomicilios.R
import com.appdomicilios.data.SessionStore
import com.appdomicilios.model.UserRole

private const val CHAT_CHANNEL_ID = "xpertgopitalito_chat_messages"
private const val ORDER_CHANNEL_ID = "xpertgopitalito_available_orders"

fun ensurePushNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return
    }

    val chatChannel = NotificationChannel(
        CHAT_CHANNEL_ID,
        "Mensajes del chat",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Avisos de mensajes nuevos entre cliente y domiciliario."
        enableVibration(true)
        setSound(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            null,
        )
    }

    val orderChannel = NotificationChannel(
        ORDER_CHANNEL_ID,
        "Pedidos disponibles",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Avisos de nuevas ordenes pendientes para domiciliarios."
        enableVibration(true)
        setSound(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            null,
        )
    }

    val manager = context.getSystemService(NotificationManager::class.java)
    manager?.createNotificationChannels(listOf(chatChannel, orderChannel))
}

fun ensureChatNotificationChannel(context: Context) {
    ensurePushNotificationChannels(context)
}

fun showIncomingChatAlert(
    context: Context,
    orderId: String,
    senderName: String,
    preview: String,
    viewerRole: UserRole = SessionStore.loadUser(context)?.role ?: UserRole.CUSTOMER,
) {
    playIncomingMessageTone()

    if (!canPostNotifications(context)) {
        return
    }

    val notification = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(senderName)
        .setContentText(preview.ifBlank { "Te envio un mensaje nuevo." })
        .setStyle(NotificationCompat.BigTextStyle().bigText(preview.ifBlank { "Te envio un mensaje nuevo." }))
        .setAutoCancel(true)
        .setContentIntent(orderPendingIntent(context, orderId, viewerRole))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(context).notify(orderId.hashCode(), notification)
}

fun showPendingOrderAlert(
    context: Context,
    orderId: String,
    customerName: String,
    body: String,
    viewerRole: UserRole = UserRole.COURIER,
) {
    playIncomingMessageTone()

    if (!canPostNotifications(context)) {
        return
    }

    val title = if (customerName.isBlank()) {
        "Nuevo domicilio disponible"
    } else {
        "Nueva orden de $customerName"
    }

    val notification = NotificationCompat.Builder(context, ORDER_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .setContentIntent(orderPendingIntent(context, orderId, viewerRole))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    NotificationManagerCompat.from(context).notify(("order-$orderId").hashCode(), notification)
}

fun canPostNotifications(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun playIncomingMessageTone() {
    runCatching {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90).startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
    }
}

private fun orderPendingIntent(
    context: Context,
    orderId: String,
    viewerRole: UserRole,
): PendingIntent {
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(extraNotificationOrderId, orderId)
        putExtra(extraNotificationRole, viewerRole.name.lowercase())
    }

    return PendingIntent.getActivity(
        context,
        "$orderId-${viewerRole.name}".hashCode(),
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
