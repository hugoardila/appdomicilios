package com.appdomicilios.data

import android.content.Context
import com.appdomicilios.model.CourierApprovalStatus
import com.appdomicilios.model.CourierRatingSummary
import com.appdomicilios.model.UserProfile
import com.appdomicilios.model.UserRole
import com.appdomicilios.model.WalletInfo
import org.json.JSONObject

object SessionStore {
    private const val preferencesName = "xpertgopitalito_session"
    private const val currentUserKey = "current_user"

    fun saveUser(context: Context, user: UserProfile) {
        prefs(context)
            .edit()
            .putString(currentUserKey, userToJson(user).toString())
            .apply()
    }

    fun loadUser(context: Context): UserProfile? {
        val raw = prefs(context).getString(currentUserKey, null) ?: return null
        return runCatching { parseUser(JSONObject(raw)) }.getOrNull()
    }

    fun clear(context: Context) {
        prefs(context)
            .edit()
            .remove(currentUserKey)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    private fun userToJson(user: UserProfile): JSONObject {
        return JSONObject()
            .put("id", user.id)
            .put("full_name", user.fullName)
            .put("national_id", user.nationalId)
            .put("phone", user.phone)
            .put("email", user.email)
            .put("role", user.role.name)
            .put("city", user.city)
            .put("approval_status", user.approvalStatus.name)
            .put(
                "wallet",
                JSONObject()
                    .put("balance", user.wallet.balance)
                    .put("minimum_top_up", user.wallet.minimumTopUp)
                    .put("customer_free_orders_remaining", user.wallet.customerFreeOrdersRemaining)
                    .put("courier_free_takes_remaining", user.wallet.courierFreeTakesRemaining)
                    .put("customer_order_fee", user.wallet.customerOrderFee)
                    .put("courier_take_fee", user.wallet.courierTakeFee)
                    .put("customer_deferred_charges", user.wallet.customerDeferredCharges)
                    .put("courier_deferred_charges", user.wallet.courierDeferredCharges)
                    .put("total_deferred_charges", user.wallet.totalDeferredCharges),
            )
            .put(
                "courier_rating",
                JSONObject()
                    .put("average", user.courierRating.average)
                    .put("count", user.courierRating.count)
                    .put("warning_active", user.courierRating.warningActive),
            )
    }

    private fun parseUser(json: JSONObject): UserProfile {
        val walletJson = json.optJSONObject("wallet")
        val ratingJson = json.optJSONObject("courier_rating")

        return UserProfile(
            id = json.optString("id"),
            fullName = json.optString("full_name"),
            nationalId = json.optString("national_id"),
            phone = json.optString("phone"),
            email = json.optString("email"),
            role = runCatching { UserRole.valueOf(json.optString("role")) }.getOrDefault(UserRole.CUSTOMER),
            city = json.optString("city", "Pitalito, Huila"),
            approvalStatus = runCatching {
                CourierApprovalStatus.valueOf(json.optString("approval_status"))
            }.getOrDefault(CourierApprovalStatus.APPROVED),
            wallet = WalletInfo(
                balance = walletJson?.optInt("balance", 0) ?: 0,
                minimumTopUp = walletJson?.optInt("minimum_top_up", 10000) ?: 10000,
                customerFreeOrdersRemaining = walletJson?.optInt("customer_free_orders_remaining", 0) ?: 0,
                courierFreeTakesRemaining = walletJson?.optInt("courier_free_takes_remaining", 0) ?: 0,
                customerOrderFee = walletJson?.optInt("customer_order_fee", 1500) ?: 1500,
                courierTakeFee = walletJson?.optInt("courier_take_fee", 1000) ?: 1000,
                customerDeferredCharges = walletJson?.optInt("customer_deferred_charges", 0) ?: 0,
                courierDeferredCharges = walletJson?.optInt("courier_deferred_charges", 0) ?: 0,
                totalDeferredCharges = walletJson?.optInt("total_deferred_charges", 0) ?: 0,
            ),
            courierRating = CourierRatingSummary(
                average = ratingJson?.optDouble("average", 0.0) ?: 0.0,
                count = ratingJson?.optInt("count", 0) ?: 0,
                warningActive = ratingJson?.optBoolean("warning_active", false) ?: false,
            ),
        )
    }
}
