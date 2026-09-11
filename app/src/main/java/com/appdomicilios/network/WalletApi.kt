package com.appdomicilios.network

import com.appdomicilios.model.ApiResult
import com.appdomicilios.model.TopUpSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object WalletApi {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrls = ApiConfig.baseUrls

    suspend fun createTopUpSession(userId: String, amount: Int): ApiResult<TopUpSession> = withContext(Dispatchers.IO) {
        var lastError: String? = null

        for (baseUrl in baseUrls) {
            val attempt = runCatching {
                val request = Request.Builder()
                    .url(baseUrl + "create_topup_session.php")
                    .post(
                        JSONObject()
                            .put("user_id", userId)
                            .put("amount", amount)
                            .toString()
                            .toRequestBody(jsonMediaType),
                    )
                    .build()

                client.newCall(request).execute().use { response ->
                    parseResult(response.body?.string().orEmpty())
                }
            }

            if (attempt.isSuccess) {
                return@withContext attempt.getOrThrow()
            }

            lastError = attempt.exceptionOrNull()?.message
        }

        ApiResult(errorMessage = lastError ?: "No fue posible iniciar la recarga.")
    }

    private fun parseResult(body: String): ApiResult<TopUpSession> {
        val json = if (body.isBlank()) JSONObject() else JSONObject(body)
        val success = json.optBoolean("success", false)
        val message = json.optString("message").ifBlank { "Operacion no disponible." }

        if (!success) {
            return ApiResult(errorMessage = message)
        }

        return ApiResult(
            data = TopUpSession(
                reference = json.optString("topup_reference"),
                checkoutPageUrl = json.optString("checkout_page_url"),
                confirmationReady = json.optBoolean("confirmation_ready", false),
            ),
        )
    }
}
