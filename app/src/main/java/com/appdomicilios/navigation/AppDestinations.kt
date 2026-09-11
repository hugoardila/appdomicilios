package com.appdomicilios.navigation

import com.appdomicilios.model.OrderCategory
import com.appdomicilios.model.UserRole

sealed class AppDestinations(val route: String) {
    data object Welcome : AppDestinations("welcome")
    data object Login : AppDestinations("login")
    data object Register : AppDestinations("register")
    data object CourierReview : AppDestinations("courier_review")
    data object PendingApproval : AppDestinations("pending_approval")
    data object CustomerHome : AppDestinations("customer_home")
    data object CourierHome : AppDestinations("courier_home")
    data object CreateOrder : AppDestinations("create_order/{category}") {
        const val categoryArg = "category"

        fun createRoute(category: OrderCategory): String {
            return "create_order/${category.name.lowercase()}"
        }
    }
    data object WalletTopUp : AppDestinations("wallet_top_up")

    data object OrderDetail : AppDestinations("order_detail/{role}/{orderId}") {
        const val roleArg = "role"
        const val orderIdArg = "orderId"

        fun createRoute(role: UserRole, orderId: String): String {
            return "order_detail/${role.name.lowercase()}/$orderId"
        }
    }
}
