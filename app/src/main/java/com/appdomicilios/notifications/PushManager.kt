package com.appdomicilios.notifications

import android.content.Context
import android.os.Build
import com.appdomicilios.data.SessionStore
import com.appdomicilios.model.UserProfile
import com.appdomicilios.network.PushApi
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.util.UUID

object PushManager {
    private const val preferencesName = "xpertgopitalito_push"
    private const val installationIdKey = "installation_id"

    suspend fun syncCurrentUser(context: Context, user: UserProfile) {
        if (!isFirebaseConfigured(context)) {
            return
        }

        val token = runCatching { FirebaseMessaging.getInstance().token.await() }
            .getOrNull()
            ?.trim()
            .orEmpty()

        if (token.isBlank()) {
            return
        }

        registerToken(
            context = context,
            user = user,
            token = token,
        )
    }

    suspend fun syncStoredUserToken(
        context: Context,
        token: String,
    ) {
        val user = SessionStore.loadUser(context) ?: return
        if (token.isBlank()) {
            return
        }

        registerToken(
            context = context,
            user = user,
            token = token.trim(),
        )
    }

    suspend fun unregisterCurrentUser(
        context: Context,
        userId: String?,
    ) {
        if (userId.isNullOrBlank()) {
            return
        }

        PushApi.unregisterDeviceToken(
            userId = userId,
            appInstanceId = installationId(context),
        )
    }

    fun isFirebaseConfigured(context: Context): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            return true
        }

        return runCatching { FirebaseApp.initializeApp(context) != null }
            .getOrDefault(false)
    }

    private suspend fun registerToken(
        context: Context,
        user: UserProfile,
        token: String,
    ) {
        PushApi.registerDeviceToken(
            userId = user.id,
            appInstanceId = installationId(context),
            fcmToken = token,
            deviceLabel = buildDeviceLabel(),
        )
    }

    private fun installationId(context: Context): String {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val currentValue = preferences.getString(installationIdKey, null)
        if (!currentValue.isNullOrBlank()) {
            return currentValue
        }

        val newValue = UUID.randomUUID().toString()
        preferences
            .edit()
            .putString(installationIdKey, newValue)
            .apply()
        return newValue
    }

    private fun buildDeviceLabel(): String {
        return listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .take(120)
            .ifBlank { "Android" }
    }
}
