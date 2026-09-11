package com.appdomicilios

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appdomicilios.data.CourierReviewRepository
import com.appdomicilios.data.DemoRepository
import com.appdomicilios.data.OrdersRepository
import com.appdomicilios.model.OrderCategory
import com.appdomicilios.model.UserRole
import com.appdomicilios.navigation.AppDestinations
import com.appdomicilios.notifications.PendingNotificationRoute
import com.appdomicilios.notifications.PushManager
import com.appdomicilios.notifications.canPostNotifications
import com.appdomicilios.ui.screens.CourierReviewScreen
import com.appdomicilios.ui.screens.CourierHomeScreen
import com.appdomicilios.ui.screens.CreateOrderScreen
import com.appdomicilios.ui.screens.CustomerHomeScreen
import com.appdomicilios.ui.screens.LoginScreen
import com.appdomicilios.ui.screens.OrderDetailScreen
import com.appdomicilios.ui.screens.PendingApprovalScreen
import com.appdomicilios.ui.screens.RegisterScreen
import com.appdomicilios.ui.screens.WalletTopUpScreen
import com.appdomicilios.ui.screens.WelcomeScreen
import com.appdomicilios.ui.theme.AppDomiciliosTheme
import kotlinx.coroutines.launch

@Composable
fun AppDomiciliosApp(
    modifier: Modifier = Modifier,
    pendingNotificationRoute: PendingNotificationRoute? = null,
    onPendingNotificationHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appScope = rememberCoroutineScope()
    val currentUser = DemoRepository.currentUser

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(currentUser?.id, currentUser?.role, currentUser?.approvalStatus) {
        val user = currentUser ?: return@LaunchedEffect
        PushManager.syncCurrentUser(context, user)
    }

    LaunchedEffect(currentUser?.id) {
        if (
            currentUser != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !canPostNotifications(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(currentUser?.id, pendingNotificationRoute?.orderId, pendingNotificationRoute?.role) {
        val user = currentUser ?: return@LaunchedEffect
        val pending = pendingNotificationRoute ?: return@LaunchedEffect

        val destination = if (pending.role == user.role) {
            AppDestinations.OrderDetail.createRoute(
                role = pending.role,
                orderId = pending.orderId,
            )
        } else {
            destinationForUser(user)
        }

        navController.navigate(destination) {
            launchSingleTop = true
        }
        onPendingNotificationHandled()
    }

    fun signOutCurrentUser(userId: String?) {
        appScope.launch {
            PushManager.unregisterCurrentUser(context, userId)
            DemoRepository.logout()
            OrdersRepository.clear()
            navigateToWelcome(navController)
        }
    }

    AppDomiciliosTheme {
        NavHost(
            navController = navController,
            startDestination = AppDestinations.Welcome.route,
            modifier = modifier,
        ) {
            composable(AppDestinations.Welcome.route) {
                if (currentUser != null) {
                    LaunchedEffect(currentUser.id) {
                        navigateToTopLevelHome(
                            navController = navController,
                            route = destinationForUser(currentUser),
                        )
                    }
                }

                WelcomeScreen(
                    onLoginClick = { navController.navigate(AppDestinations.Login.route) },
                    onRegisterClick = { navController.navigate(AppDestinations.Register.route) },
                    onOpenCourierReview = { navController.navigate(AppDestinations.CourierReview.route) },
                )
            }

            composable(AppDestinations.Login.route) {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRegister = { navController.navigate(AppDestinations.Register.route) },
                    onLoggedIn = { user ->
                        navigateToTopLevelHome(
                            navController = navController,
                            route = destinationForUser(user),
                        )
                    },
                )
            }

            composable(AppDestinations.Register.route) {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLogin = { navController.navigate(AppDestinations.Login.route) },
                    onRegistered = { user ->
                        navigateToTopLevelHome(
                            navController = navController,
                            route = destinationForUser(user),
                        )
                    },
                )
            }

            composable(AppDestinations.CourierReview.route) {
                CourierReviewScreen(
                    isAuthenticated = CourierReviewRepository.accessKey.isNotBlank(),
                    pendingCouriers = CourierReviewRepository.pendingCouriers,
                    isLoading = CourierReviewRepository.isLoading,
                    errorMessage = CourierReviewRepository.errorMessage,
                    onBack = { navController.popBackStack() },
                    onSubmitAccessKey = { accessKey ->
                        CourierReviewRepository.signIn(accessKey)
                    },
                    onRefresh = {
                        CourierReviewRepository.refresh()
                    },
                    onApprove = { courierId ->
                        CourierReviewRepository.approve(courierId)
                    },
                    onReject = { courierId ->
                        CourierReviewRepository.reject(courierId)
                    },
                    onSignOut = {
                        CourierReviewRepository.signOut()
                        navController.popBackStack()
                    },
                )
            }

            composable(AppDestinations.PendingApproval.route) {
                if (currentUser == null || currentUser.role != UserRole.COURIER) {
                    LaunchedEffect(currentUser?.id) {
                        navigateToWelcome(navController)
                    }
                } else {
                    PendingApprovalScreen(
                        courier = currentUser,
                        onSignOut = {
                            signOutCurrentUser(currentUser.id)
                        },
                    )
                }
            }

            composable(AppDestinations.CustomerHome.route) {
                if (currentUser == null || currentUser.role != UserRole.CUSTOMER) {
                    LaunchedEffect(currentUser?.id) {
                        navigateToWelcome(navController)
                    }
                } else {
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(currentUser.id) {
                        OrdersRepository.refreshCurrentUser(currentUser.id)
                        OrdersRepository.refreshCustomerOrders(currentUser.id)
                    }

                    CustomerHomeScreen(
                        customer = currentUser,
                        orders = OrdersRepository.customerOrders,
                        isLoading = OrdersRepository.isLoading,
                        errorMessage = OrdersRepository.errorMessage,
                        onSignOut = {
                            signOutCurrentUser(currentUser.id)
                        },
                        onCreateOrder = { category ->
                            navController.navigate(AppDestinations.CreateOrder.createRoute(category))
                        },
                        onTopUp = { navController.navigate(AppDestinations.WalletTopUp.route) },
                        onRefresh = {
                            scope.launch {
                                OrdersRepository.refreshCurrentUser(currentUser.id)
                                OrdersRepository.refreshCustomerOrders(currentUser.id)
                            }
                        },
                        onOpenOrder = {
                            navController.navigate(
                                AppDestinations.OrderDetail.createRoute(
                                    role = UserRole.CUSTOMER,
                                    orderId = it,
                                ),
                            )
                        },
                    )
                }
            }

            composable(AppDestinations.CourierHome.route) {
                when {
                    currentUser == null -> {
                        LaunchedEffect(Unit) {
                            navigateToWelcome(navController)
                        }
                    }

                    currentUser.role != UserRole.COURIER -> {
                        LaunchedEffect(currentUser.id) {
                            navigateToTopLevelHome(
                                navController = navController,
                                route = destinationForUser(currentUser),
                            )
                        }
                    }

                    !isApprovedCourier(currentUser) -> {
                        LaunchedEffect(currentUser.id) {
                            navController.navigate(AppDestinations.PendingApproval.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }

                    else -> {
                        val scope = rememberCoroutineScope()

                        LaunchedEffect(currentUser.id) {
                            OrdersRepository.refreshCurrentUser(currentUser.id)
                            OrdersRepository.refreshCourierOrders(currentUser.id)
                        }

                        CourierHomeScreen(
                            courier = currentUser,
                            activeOrders = OrdersRepository.courierActiveOrders,
                            availableOrders = OrdersRepository.courierAvailableOrders,
                            isLoading = OrdersRepository.isLoading,
                            errorMessage = OrdersRepository.errorMessage,
                            onSignOut = {
                                signOutCurrentUser(currentUser.id)
                            },
                            onTopUp = { navController.navigate(AppDestinations.WalletTopUp.route) },
                            onRefresh = {
                                scope.launch {
                                    OrdersRepository.refreshCurrentUser(currentUser.id)
                                    OrdersRepository.refreshCourierOrders(currentUser.id)
                                }
                            },
                            onTakeOrder = { orderId ->
                                scope.launch {
                                    OrdersRepository.takeOrder(orderId, currentUser.id)
                                }
                            },
                            onOpenOrder = {
                                navController.navigate(
                                    AppDestinations.OrderDetail.createRoute(
                                        role = UserRole.COURIER,
                                        orderId = it,
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            composable(
                route = AppDestinations.CreateOrder.route,
                arguments = listOf(
                    navArgument(AppDestinations.CreateOrder.categoryArg) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                if (currentUser == null || currentUser.role != UserRole.CUSTOMER) {
                    LaunchedEffect(currentUser?.id) {
                        navigateToWelcome(navController)
                    }
                } else {
                    val scope = rememberCoroutineScope()
                    val categoryArg = backStackEntry.arguments
                        ?.getString(AppDestinations.CreateOrder.categoryArg)
                        ?.uppercase()
                    val initialCategory = runCatching {
                        OrderCategory.valueOf(categoryArg ?: OrderCategory.SHOPPING.name)
                    }.getOrDefault(OrderCategory.SHOPPING)

                    LaunchedEffect(currentUser.id) {
                        OrdersRepository.clearError()
                    }

                    CreateOrderScreen(
                        initialCategory = initialCategory,
                        walletInfo = currentUser.wallet,
                        serverErrorMessage = OrdersRepository.errorMessage,
                        onBack = { navController.popBackStack() },
                        onTopUp = { navController.navigate(AppDestinations.WalletTopUp.route) },
                        onSubmit = { draft ->
                            scope.launch {
                                val result = OrdersRepository.createOrder(
                                    customerId = currentUser.id,
                                    category = draft.category,
                                    serviceSubcategory = draft.serviceSubcategory,
                                    requestType = draft.requestType,
                                    items = draft.items,
                                    details = draft.details,
                                    address = draft.address,
                                    shareLocation = draft.shareLocation,
                                    addressReference = draft.addressReference,
                                    latitude = draft.latitude,
                                    longitude = draft.longitude,
                                    destinationLatitude = draft.destinationLatitude,
                                    destinationLongitude = draft.destinationLongitude,
                                    destinationReference = draft.destinationReference,
                                    paymentMethod = draft.paymentMethod,
                                    pickupAddress = draft.pickupAddress,
                                    pickupContactName = draft.pickupContactName,
                                    pickupContactPhone = draft.pickupContactPhone,
                                    dropoffContactName = draft.dropoffContactName,
                                    dropoffContactPhone = draft.dropoffContactPhone,
                                    pickupPaymentAmount = draft.pickupPaymentAmount,
                                    packageWeightKg = draft.packageWeightKg,
                                )

                                result.data?.let { newOrderId ->
                                    navController.navigate(
                                        AppDestinations.OrderDetail.createRoute(
                                            role = UserRole.CUSTOMER,
                                            orderId = newOrderId,
                                        ),
                                    ) {
                                        popUpTo(AppDestinations.CustomerHome.route)
                                    }
                                }
                            }
                        },
                    )
                }
            }

            composable(AppDestinations.WalletTopUp.route) {
                if (currentUser == null) {
                    LaunchedEffect(Unit) {
                        navigateToWelcome(navController)
                    }
                } else {
                    val scope = rememberCoroutineScope()

                    WalletTopUpScreen(
                        user = currentUser,
                        onBack = { navController.popBackStack() },
                        onRefreshWallet = {
                            scope.launch {
                                OrdersRepository.refreshCurrentUser(currentUser.id)
                            }
                        },
                    )
                }
            }

            composable(
                route = AppDestinations.OrderDetail.route,
                arguments = listOf(
                    navArgument(AppDestinations.OrderDetail.roleArg) { type = NavType.StringType },
                    navArgument(AppDestinations.OrderDetail.orderIdArg) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val roleArg = backStackEntry.arguments?.getString(AppDestinations.OrderDetail.roleArg)
                    ?: UserRole.CUSTOMER.name.lowercase()
                val orderId = backStackEntry.arguments?.getString(AppDestinations.OrderDetail.orderIdArg)
                    .orEmpty()

                OrderDetailScreen(
                    role = UserRole.valueOf(roleArg.uppercase()),
                    orderId = orderId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun destinationForUser(user: com.appdomicilios.model.UserProfile): String {
    return when {
        user.role == UserRole.CUSTOMER -> AppDestinations.CustomerHome.route
        isApprovedCourier(user) -> AppDestinations.CourierHome.route
        else -> AppDestinations.PendingApproval.route
    }
}

private fun isApprovedCourier(user: com.appdomicilios.model.UserProfile): Boolean {
    return user.role == UserRole.COURIER &&
        user.approvalStatus == com.appdomicilios.model.CourierApprovalStatus.APPROVED
}

private fun navigateToTopLevelHome(
    navController: androidx.navigation.NavHostController,
    route: String,
) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

private fun navigateToWelcome(navController: androidx.navigation.NavHostController) {
    navController.navigate(AppDestinations.Welcome.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}
