package com.appdomicilios

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appdomicilios.notifications.ensurePushNotificationChannels
import com.appdomicilios.notifications.readPendingNotificationRoute

class MainActivity : ComponentActivity() {
    private var pendingNotificationRoute by mutableStateOf(readPendingNotificationRoute(intent))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensurePushNotificationChannels(this)
        setContent {
            AppDomiciliosApp(
                pendingNotificationRoute = pendingNotificationRoute,
                onPendingNotificationHandled = {
                    pendingNotificationRoute = null
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        pendingNotificationRoute = readPendingNotificationRoute(intent)
    }
}
