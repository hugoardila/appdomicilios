package com.appdomicilios.network

import android.content.Context
import android.net.Uri
import com.appdomicilios.model.AuthResponse
import com.appdomicilios.model.CourierApprovalStatus
import com.appdomicilios.model.CourierRatingSummary
import com.appdomicilios.model.CourierVerificationDraft
import com.appdomicilios.model.UserProfile
import com.appdomicilios.model.UserRole
import com.appdomicilios.model.WalletInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import okio.source
import org.json.JSONObject
import java.io.File

object AuthApi {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrls = ApiConfig.baseUrls

    suspend fun login(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        post(
            endpoint = "login.php",
            payload = JSONObject()
                .put("email", email.trim())
                .put("password", password),
        )
    }

    suspend fun register(
        context: Context,
        fullName: String,
        nationalId: String,
        phone: String,
        email: String,
        password: String,
        role: UserRole,
        courierVerification: CourierVerificationDraft? = null,
    ): AuthResponse = withContext(Dispatchers.IO) {
        if (role == UserRole.COURIER && courierVerification != null) {
            postMultipart(
                context = context,
                endpoint = "register.php",
                fields = mapOf(
                    "full_name" to fullName.trim(),
                    "national_id" to nationalId.trim(),
                    "phone" to phone.trim(),
                    "email" to email.trim(),
                    "password" to password,
                    "role" to "courier",
                    "document_type" to courierVerification.documentType.backendValue,
                ),
                files = listOf(
                    UploadFile(
                        fieldName = "document_front",
                        uri = Uri.parse(courierVerification.documentFrontUri),
                        prefix = "document_front",
                    ),
                    UploadFile(
                        fieldName = "selfie_photo",
                        uri = Uri.parse(courierVerification.selfieUri),
                        prefix = "selfie",
                    ),
                ),
            )
        } else {
            post(
                endpoint = "register.php",
                payload = JSONObject()
                    .put("full_name", fullName.trim())
                    .put("national_id", nationalId.trim())
                    .put("phone", phone.trim())
                    .put("email", email.trim())
                    .put("password", password)
                    .put("role", if (role == UserRole.CUSTOMER) "customer" else "courier"),
            )
        }
    }

    suspend fun fetchUserProfile(userId: String): AuthResponse = withContext(Dispatchers.IO) {
        get(endpoint = "wallet_summary.php?user_id=$userId")
    }

    private fun post(endpoint: String, payload: JSONObject): AuthResponse {
        var lastError: String? = null

        for (baseUrl in baseUrls) {
            val attempt = runCatching {
                val request = Request.Builder()
                    .url(baseUrl + endpoint)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    parseAuthResponse(response.body?.string().orEmpty())
                }
            }

            if (attempt.isSuccess) {
                return attempt.getOrThrow()
            }

            lastError = attempt.exceptionOrNull()?.message
        }

        return AuthResponse(
            errorMessage = lastError ?: "No fue posible conectar con la API del servidor.",
        )
    }

    private fun postMultipart(
        context: Context,
        endpoint: String,
        fields: Map<String, String>,
        files: List<UploadFile>,
    ): AuthResponse {
        var lastError: String? = null

        for (baseUrl in baseUrls) {
            val tempFiles = mutableListOf<File>()
            val attempt = runCatching {
                val form = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)

                fields.forEach { (key, value) ->
                    form.addFormDataPart(key, value)
                }

                files.forEach { upload ->
                    val stagedFile = stageUriToTempFile(context, upload)
                    tempFiles += stagedFile
                    val mimeType = context.contentResolver.getType(upload.uri) ?: "image/jpeg"
                    form.addFormDataPart(
                        upload.fieldName,
                        stagedFile.name,
                        stagedFile.asRequestBody(mimeType.toMediaTypeOrNull()),
                    )
                }

                val request = Request.Builder()
                    .url(baseUrl + endpoint)
                    .post(form.build())
                    .build()

                client.newCall(request).execute().use { response ->
                    parseAuthResponse(response.body?.string().orEmpty())
                }
            }

            tempFiles.forEach { staged ->
                runCatching { staged.delete() }
            }

            if (attempt.isSuccess) {
                return attempt.getOrThrow()
            }

            lastError = attempt.exceptionOrNull()?.message
        }

        return AuthResponse(
            errorMessage = lastError ?: "No fue posible conectar con la API del servidor.",
        )
    }

    private fun get(endpoint: String): AuthResponse {
        var lastError: String? = null

        for (baseUrl in baseUrls) {
            val attempt = runCatching {
                val request = Request.Builder()
                    .url(baseUrl + endpoint)
                    .build()

                client.newCall(request).execute().use { response ->
                    parseAuthResponse(response.body?.string().orEmpty())
                }
            }

            if (attempt.isSuccess) {
                return attempt.getOrThrow()
            }

            lastError = attempt.exceptionOrNull()?.message
        }

        return AuthResponse(
            errorMessage = lastError ?: "No fue posible conectar con la API del servidor.",
        )
    }

    private fun parseAuthResponse(body: String): AuthResponse {
        val json = if (body.isBlank()) JSONObject() else JSONObject(body)
        val success = json.optBoolean("success", false)
        val message = json.optString("message").ifBlank { "Operacion no disponible." }

        if (!success) {
            return AuthResponse(errorMessage = message)
        }

        val userJson = json.optJSONObject("user")
            ?: return AuthResponse(errorMessage = "La respuesta del servidor no incluyo usuario.")

        return AuthResponse(user = parseUser(userJson))
    }

    private fun parseUser(userJson: JSONObject): UserProfile {
        val role = when (userJson.optString("role")) {
            "courier" -> UserRole.COURIER
            else -> UserRole.CUSTOMER
        }

        val approval = when (userJson.optString("approval_status")) {
            "pending" -> CourierApprovalStatus.PENDING
            "rejected" -> CourierApprovalStatus.REJECTED
            else -> CourierApprovalStatus.APPROVED
        }

        return UserProfile(
            id = userJson.optString("id"),
            fullName = userJson.optString("full_name"),
            nationalId = userJson.optString("national_id"),
            phone = userJson.optString("phone"),
            email = userJson.optString("email"),
            role = role,
            city = userJson.optString("city", "Pitalito, Huila"),
            approvalStatus = approval,
            wallet = parseWallet(userJson.optJSONObject("wallet")),
            courierRating = parseCourierRating(userJson.optJSONObject("courier_rating")),
        )
    }

    private fun parseWallet(walletJson: JSONObject?): WalletInfo {
        if (walletJson == null) {
            return WalletInfo()
        }

        return WalletInfo(
            balance = walletJson.optInt("balance", 0),
            minimumTopUp = walletJson.optInt("minimum_top_up", 10000),
            customerFreeOrdersRemaining = walletJson.optInt("customer_free_orders_remaining", 0),
            courierFreeTakesRemaining = walletJson.optInt("courier_free_takes_remaining", 0),
            customerOrderFee = walletJson.optInt("customer_order_fee", 1500),
            courierTakeFee = walletJson.optInt("courier_take_fee", 1000),
            customerDeferredCharges = walletJson.optInt("customer_deferred_charges", 0),
            courierDeferredCharges = walletJson.optInt("courier_deferred_charges", 0),
            totalDeferredCharges = walletJson.optInt("total_deferred_charges", 0),
        )
    }

    private fun parseCourierRating(ratingJson: JSONObject?): CourierRatingSummary {
        if (ratingJson == null) {
            return CourierRatingSummary()
        }

        return CourierRatingSummary(
            average = ratingJson.optDouble("average", 0.0),
            count = ratingJson.optInt("count", 0),
            warningActive = ratingJson.optBoolean("warning_active", false),
        )
    }

    private fun stageUriToTempFile(context: Context, upload: UploadFile): File {
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(upload.uri)
            ?: error("No pudimos leer una de las imagenes seleccionadas.")

        val mimeType = resolver.getType(upload.uri) ?: "image/jpeg"
        val extension = when (mimeType.lowercase()) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> ".jpg"
        }

        return inputStream.use { stream ->
            val file = File.createTempFile(upload.prefix, extension, context.cacheDir)
            file.sink().buffer().use { sink ->
                sink.writeAll(stream.source())
            }
            file
        }
    }

    private data class UploadFile(
        val fieldName: String,
        val uri: Uri,
        val prefix: String,
    )
}
