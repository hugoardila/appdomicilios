package com.appdomicilios.network

import com.appdomicilios.model.ApiResult
import com.appdomicilios.model.CourierReviewCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object CourierReviewApi {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrls = ApiConfig.baseUrls

    suspend fun fetchPendingCouriers(accessKey: String): ApiResult<List<CourierReviewCandidate>> =
        withContext(Dispatchers.IO) {
            postList(
                endpoint = "review_couriers.php",
                payload = JSONObject().put("access_key", accessKey.trim()),
            )
        }

    suspend fun approveCourier(accessKey: String, courierId: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            postAction(
                endpoint = "review_courier_action.php",
                payload = JSONObject()
                    .put("access_key", accessKey.trim())
                    .put("courier_id", courierId)
                    .put("decision", "approve"),
            )
        }

    suspend fun rejectCourier(accessKey: String, courierId: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            postAction(
                endpoint = "review_courier_action.php",
                payload = JSONObject()
                    .put("access_key", accessKey.trim())
                    .put("courier_id", courierId)
                    .put("decision", "reject"),
            )
        }

    private fun postList(
        endpoint: String,
        payload: JSONObject,
    ): ApiResult<List<CourierReviewCandidate>> {
        var lastError: String? = null

        for (baseUrl in baseUrls) {
            val attempt = runCatching {
                val request = Request.Builder()
                    .url(baseUrl + endpoint)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    parseCourierListResponse(body)
                }
            }

            if (attempt.isSuccess) {
                return attempt.getOrThrow()
            }

            lastError = attempt.exceptionOrNull()?.message
        }

        return ApiResult(errorMessage = lastError ?: "No fue posible conectar con la revision interna.")
    }

    private fun postAction(
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
                    val json = JSONObject(response.body?.string().orEmpty().ifBlank { "{}" })
                    if (!json.optBoolean("success", false)) {
                        ApiResult<Unit>(errorMessage = json.optString("message").ifBlank { "No fue posible actualizar la revision." })
                    } else {
                        ApiResult(Unit)
                    }
                }
            }

            if (attempt.isSuccess) {
                return attempt.getOrThrow()
            }

            lastError = attempt.exceptionOrNull()?.message
        }

        return ApiResult(errorMessage = lastError ?: "No fue posible conectar con la revision interna.")
    }

    private fun parseCourierListResponse(body: String): ApiResult<List<CourierReviewCandidate>> {
        val json = JSONObject(body.ifBlank { "{}" })
        if (!json.optBoolean("success", false)) {
            return ApiResult(
                errorMessage = json.optString("message").ifBlank { "No fue posible cargar la revision interna." },
            )
        }

        val couriersJson = json.optJSONArray("couriers")
        val couriers = buildList {
            if (couriersJson != null) {
                for (index in 0 until couriersJson.length()) {
                    val item = couriersJson.optJSONObject(index) ?: continue
                    add(
                        CourierReviewCandidate(
                            id = item.optString("id"),
                            fullName = item.optString("full_name"),
                            nationalId = item.optString("national_id"),
                            phone = item.optString("phone"),
                            email = item.optString("email"),
                            documentLabel = item.optString("document_label").ifBlank { "Documento" },
                            documentUrl = item.optString("document_url").takeIf { it.isNotBlank() },
                            selfieUrl = item.optString("selfie_url").takeIf { it.isNotBlank() },
                            submittedAtLabel = item.optString("submitted_at"),
                        ),
                    )
                }
            }
        }

        return ApiResult(data = couriers)
    }
}
