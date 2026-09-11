package com.appdomicilios.network

import com.appdomicilios.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object PushApi {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrls = ApiConfig.baseUrls

    suspend fun registerDeviceToken(
        userId: String,
        appInstanceId: String,
        fcmToken: String,
        deviceLabel: String,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        post(
            endpoint = "register_push_token.php",
            payload = JSONObject()
                .put("user_id", userId)
                .put("app_instance_id", appInstanceId)
                .put("fcm_token", fcmToken)
                .put("platform", "android")
                .put("device_label", deviceLabel),
        )
    }

    suspend fun unregisterDeviceToken(
        userId: String,
        appInstanceId: String,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        post(
            endpoint = "unregister_push_token.php",
            payload = JSONObject()
                .put("user_id", userId)
                .put("app_instance_id", appInstanceId),
        )
    }

    private fun post(
        endpoint: String,
        payload: JSONObject,
    ): ApiResult<Unit> {
        var lastError: String? = null

        for (baseUrl in baseUrls) {
            val attempt = runCatching {
                val request = Request.Builder()
                    .url(baseUrl + endpoint)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    parseResult(response.body?.string().orEmpty())
                }
            }

            if (attempt.isSuccess) {
                return attempt.getOrThrow()
            }

            lastError = attempt.exceptionOrNull()?.message
        }

        return ApiResult(errorMessage = lastError ?: "No fue posible registrar las notificaciones del dispositivo.")
    }

    private fun parseResult(body: String): ApiResult<Unit> {
        val json = if (body.isBlank()) JSONObject() else JSONObject(body)
        val success = json.optBoolean("success", false)
        val message = json.optString("message").ifBlank { "Operacion no disponible." }

        return if (success) {
            ApiResult(data = Unit)
        } else {
            ApiResult(errorMessage = message)
        }
    }
}
