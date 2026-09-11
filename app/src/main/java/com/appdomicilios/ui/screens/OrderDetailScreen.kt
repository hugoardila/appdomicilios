package com.appdomicilios.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.appdomicilios.data.DemoRepository
import com.appdomicilios.data.OrdersRepository
import com.appdomicilios.location.formatCoordinate
import com.appdomicilios.location.hasLocationPermission
import com.appdomicilios.location.requestCurrentLocation
import com.appdomicilios.location.resolveAddressFromCoordinates
import com.appdomicilios.model.OrderCategory
import com.appdomicilios.model.OrderItem
import com.appdomicilios.model.OrderItemStatus
import com.appdomicilios.model.OrderRequestType
import com.appdomicilios.model.OrderResolutionReason
import com.appdomicilios.model.OrderStatus
import com.appdomicilios.model.UserRole
import com.appdomicilios.model.WalletChargeMode
import com.appdomicilios.notifications.canPostNotifications
import com.appdomicilios.notifications.showIncomingChatAlert
import com.appdomicilios.pricing.EXTRA_PURCHASE_PLACE_FEE
import com.appdomicilios.pricing.SAME_PLACE_MULTI_PRODUCT_FEE
import com.appdomicilios.pricing.URBAN_MINIMUM_DELIVERY_FEE
import com.appdomicilios.ui.components.ItemStatusChip
import com.appdomicilios.ui.components.MessageBubble
import com.appdomicilios.ui.components.RatingStars
import com.appdomicilios.ui.components.StatusChip
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    role: UserRole,
    orderId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val order = OrdersRepository.cachedOrder(orderId)
    val isCourierOwner = role == UserRole.COURIER && order?.courierId == DemoRepository.currentUser?.id
    val currentUserId = DemoRepository.currentUser?.id
    val isChatEnabled = order?.status == OrderStatus.TAKEN ||
        order?.status == OrderStatus.SHOPPING ||
        order?.status == OrderStatus.ON_THE_WAY

    var extraProduct by remember { mutableStateOf("") }
    var messageDraft by remember { mutableStateOf("") }
    var chatFeedback by remember { mutableStateOf<String?>(null) }
    var ratingFeedback by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var mapPreview by remember { mutableStateOf<MapPreviewLocation?>(null) }
    var pendingResolutionReason by remember { mutableStateOf<OrderResolutionReason?>(null) }
    var selectedRating by remember(orderId, order?.courierRatingValue) {
        mutableStateOf(order?.courierRatingValue ?: 5)
    }
    var hasInitializedIncomingTracker by remember(orderId) { mutableStateOf(false) }
    var lastIncomingMessageId by remember(orderId) { mutableStateOf<Long?>(null) }

    val sendCurrentLocationMessage = {
        if (order != null && currentUserId != null) {
            requestCurrentLocation(
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context),
                onStarted = {
                    chatFeedback = "Buscando ubicacion para enviar..."
                },
                onSuccess = { latitude, longitude ->
                    scope.launch {
                        val label = resolveAddressFromCoordinates(context, latitude, longitude)
                        val result = OrdersRepository.sendMessage(
                            orderId = order.id,
                            senderUserId = currentUserId,
                            messageText = "",
                            latitude = latitude,
                            longitude = longitude,
                            locationLabel = label,
                        )
                        chatFeedback = result.errorMessage ?: "Ubicacion compartida."
                    }
                },
                onError = { message ->
                    chatFeedback = message
                },
            )
        }
    }

    val imageMessageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null && order != null && currentUserId != null) {
            scope.launch {
                val result = OrdersRepository.sendImageMessage(
                    context = context,
                    orderId = order.id,
                    senderUserId = currentUserId,
                    messageText = messageDraft,
                    imageUri = uri,
                )
                if (result.errorMessage == null) {
                    messageDraft = ""
                    chatFeedback = "Foto enviada."
                } else {
                    chatFeedback = result.errorMessage
                }
            }
        }
    }

    LaunchedEffect(orderId) {
        OrdersRepository.refreshOrderDetail(orderId)
    }

    LaunchedEffect(orderId, isChatEnabled) {
        if (!isChatEnabled) {
            return@LaunchedEffect
        }

        while (true) {
            delay(4_000)
            OrdersRepository.refreshOrderDetail(orderId)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(isChatEnabled) {
        if (isChatEnabled && !canPostNotifications(context)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(order?.messages?.size, order?.updatedAtLabel, isChatEnabled, currentUserId) {
        if (!isChatEnabled || currentUserId == null || order == null) {
            return@LaunchedEffect
        }

        OrdersRepository.markMessagesSeen(order.id, currentUserId)
    }

    LaunchedEffect(order?.messages, role, isChatEnabled) {
        if (order == null || !isChatEnabled) {
            return@LaunchedEffect
        }

        val latestIncoming = order.messages
            .filter { message ->
                !message.isSystem &&
                    when (role) {
                        UserRole.CUSTOMER -> message.fromCourier
                        UserRole.COURIER -> !message.fromCourier
                    }
            }
            .maxByOrNull { it.id.toLongOrNull() ?: -1L }

        val latestIncomingId = latestIncoming?.id?.toLongOrNull()

        if (!hasInitializedIncomingTracker) {
            hasInitializedIncomingTracker = true
            lastIncomingMessageId = latestIncomingId
            return@LaunchedEffect
        }

        if (latestIncoming != null && latestIncomingId != null && latestIncomingId > (lastIncomingMessageId ?: -1L)) {
            val preview = when {
                latestIncoming.imageUri != null && latestIncoming.body.isNotBlank() -> "${latestIncoming.body} · Foto"
                latestIncoming.imageUri != null -> "Te envio una foto."
                latestIncoming.body.isNotBlank() -> latestIncoming.body
                latestIncoming.latitude != null && latestIncoming.longitude != null -> "Te compartio una ubicacion."
                else -> "Te envio un mensaje nuevo."
            }
            showIncomingChatAlert(
                context = context,
                orderId = order.id,
                senderName = latestIncoming.senderName,
                preview = preview,
            )
        }

        lastIncomingMessageId = latestIncomingId
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            sendCurrentLocationMessage()
        } else {
            chatFeedback = "No diste permiso de ubicacion para compartir tu punto actual."
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle del pedido") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
            ) {
                Text(
                    text = if (OrdersRepository.isLoading) {
                        "Cargando pedido..."
                    } else {
                        OrdersRepository.errorMessage ?: "No encontramos este pedido."
                    },
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusChip(order.status)
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Text(
                            text = order.category.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    order.serviceSubcategory
                        ?.takeIf { it.isNotBlank() }
                        ?.let { subcategory ->
                            Text(
                                text = "Subcategoria: $subcategory",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    Text(
                        text = order.addressText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (role == UserRole.COURIER && order.customerReputation.warningActive) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Ojo: posible cliente falso",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Comunicarse primero. Este cliente tiene ${order.customerReputation.incidentsCount} novedad(es) previa(s) y reputacion ${String.format("%.1f", order.customerReputation.score)}/5.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    if (role == UserRole.COURIER && !order.customerPhone.isNullOrBlank()) {
                        Text(
                            text = "Telefono del cliente: ${order.customerPhone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    openDialer(
                                        context = context,
                                        phone = order.customerPhone,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Llamar")
                            }
                            FilledTonalButton(
                                onClick = {
                                    openWhatsApp(
                                        context = context,
                                        phone = order.customerPhone,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("WhatsApp")
                            }
                        }
                    }
                    if (!order.addressReference.isNullOrBlank()) {
                        Text(
                            text = "Referencia GPS: ${order.addressReference}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Pago: ${order.paymentMethod.label}  |  Domicilio ${DemoRepository.currency(order.deliveryFee)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when (order.requestType) {
                        OrderRequestType.SHOPPING -> {
                            Text(
                                text = if (order.category == OrderCategory.RESTAURANTS) {
                                    "Restaurante elegido: ${order.serviceSubcategory ?: "Por confirmar"}"
                                } else {
                                    "Lugares de compra: ${order.purchasePlacesCount}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (order.category == OrderCategory.RESTAURANTS) {
                                    "Pedido de restaurante dentro del casco urbano."
                                } else {
                                    "Regla actual: 1 producto ${DemoRepository.currency(URBAN_MINIMUM_DELIVERY_FEE)}, 2 o 3 en el mismo lugar ${DemoRepository.currency(SAME_PLACE_MULTI_PRODUCT_FEE)}, y cada lugar extra suma ${DemoRepository.currency(EXTRA_PURCHASE_PLACE_FEE)}."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OrderRequestType.PICKUP_AND_DELIVER -> {
                            Text(
                                text = when (order.category) {
                                    OrderCategory.ENVIOS -> "Servicio base de envio local dentro de Pitalito."
                                    else -> "Servicio base de recoger y entregar dentro de Pitalito."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OrderRequestType.OTHER -> {
                            Text(
                                text = when (order.category) {
                                    OrderCategory.MOTOTAXI -> "Solicitud de mototaxi con tarifa base dentro del casco urbano."
                                    else -> "Vuelta especial con tarifa base dentro del casco urbano."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = buildChargeSummary(order),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (order.category == OrderCategory.ENVIOS && order.packageWeightKg != null) {
                        Text(
                            text = "Peso estimado del envio: ${formatWeightLabel(order.packageWeightKg)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (order.shareLocation) {
                        if (order.latitude != null && order.longitude != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = if (order.category == OrderCategory.MOTOTAXI) {
                                            "Origen compartido"
                                        } else {
                                            "Ubicacion compartida"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = order.addressReference ?: order.addressText,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = "${formatCoordinate(order.latitude)}, ${formatCoordinate(order.longitude)}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        mapPreview = MapPreviewLocation(
                                            latitude = order.latitude,
                                            longitude = order.longitude,
                                            label = order.addressReference ?: order.addressText,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Ver mapa aqui")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        openInGoogleMaps(
                                            context = context,
                                            latitude = order.latitude,
                                            longitude = order.longitude,
                                            label = order.addressReference ?: order.addressText,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Google Maps")
                                }
                            }
                        } else {
                            Text(
                                text = "El cliente compartio ubicacion de apoyo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(
                    when (order.requestType) {
                        OrderRequestType.SHOPPING -> if (order.category == OrderCategory.RESTAURANTS) "Pedido del restaurante" else "Encargo original"
                        OrderRequestType.PICKUP_AND_DELIVER -> if (order.category == OrderCategory.ENVIOS) "Resumen del envio" else "Resumen de la recogida"
                        OrderRequestType.OTHER -> if (order.category == OrderCategory.MOTOTAXI) "Detalle del mototaxi" else "Detalle de la vuelta"
                    },
                )
            }

            item {
                Text(
                    text = order.originalRequestText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (order.category == OrderCategory.MOTOTAXI) {
                item {
                    DetailInfoCard(
                        title = "Ruta solicitada",
                        lines = listOfNotNull(
                            order.addressReference?.let { "Origen: $it" },
                            "Destino: ${order.addressText}",
                            order.destinationReference?.let { "Referencia destino: $it" },
                        ),
                    )
                }
                if (
                    order.latitude != null &&
                    order.longitude != null &&
                    order.destinationLatitude != null &&
                    order.destinationLongitude != null
                ) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    mapPreview = MapPreviewLocation(
                                        latitude = order.latitude,
                                        longitude = order.longitude,
                                        label = order.addressReference ?: "Origen",
                                        destinationLatitude = order.destinationLatitude,
                                        destinationLongitude = order.destinationLongitude,
                                        destinationLabel = order.destinationReference ?: order.addressText,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Ver ruta aqui")
                            }
                            FilledTonalButton(
                                onClick = {
                                    openDirectionsInGoogleMaps(
                                        context = context,
                                        originLatitude = order.latitude,
                                        originLongitude = order.longitude,
                                        destinationLatitude = order.destinationLatitude,
                                        destinationLongitude = order.destinationLongitude,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Abrir ruta")
                            }
                        }
                    }
                }
            }

            if (order.requestType == OrderRequestType.PICKUP_AND_DELIVER) {
                item {
                    DetailInfoCard(
                        title = "Base de recogida",
                        lines = listOfNotNull(
                            order.pickupAddress?.let { "Direccion: $it" },
                            order.pickupContactName?.let { "Entrega: $it" },
                            order.pickupContactPhone?.let { "Telefono: $it" },
                            order.pickupPaymentAmount.takeIf { it > 0 }?.let {
                                "Valor a pagar al recoger: ${DemoRepository.currency(it)}"
                            },
                        ),
                    )
                }
                item {
                    DetailInfoCard(
                        title = "Destino final",
                        lines = listOfNotNull(
                            "Direccion: ${order.addressText}",
                            order.dropoffContactName?.let { "Recibe: $it" },
                            order.dropoffContactPhone?.let { "Telefono: $it" },
                        ),
                    )
                }
                if (order.category == OrderCategory.ENVIOS) {
                    item {
                        DetailInfoCard(
                            title = "Datos del envio",
                            lines = listOfNotNull(
                                order.serviceSubcategory?.let { "Tipo: $it" },
                                order.packageWeightKg?.let { "Peso aproximado: ${formatWeightLabel(it)}" },
                            ),
                        )
                    }
                }
            }

            if (
                order.category == OrderCategory.SHOPPING ||
                order.category == OrderCategory.RESTAURANTS ||
                order.category == OrderCategory.TRAMITES
            ) {
                item {
                    SectionTitle(
                        if (order.category == OrderCategory.SHOPPING) {
                            "Lista de productos"
                        } else if (order.category == OrderCategory.RESTAURANTS) {
                            "Lista del pedido"
                        } else {
                            "Lista de gestiones"
                        },
                    )
                }

                items(order.items, key = { "item-${it.id}" }) { item ->
                    ProductRow(
                        item = item,
                        showActions = isCourierOwner &&
                            order.status != OrderStatus.DELIVERED &&
                            order.status != OrderStatus.CANCELLED,
                        onPurchased = {
                            scope.launch {
                                OrdersRepository.updateItemStatus(
                                    orderId = order.id,
                                    itemId = item.id,
                                    status = OrderItemStatus.PURCHASED,
                                    currentUserId = currentUserId,
                                )
                            }
                        },
                        onNotFound = {
                            scope.launch {
                                OrdersRepository.updateItemStatus(
                                    orderId = order.id,
                                    itemId = item.id,
                                    status = OrderItemStatus.NOT_FOUND,
                                    currentUserId = currentUserId,
                                )
                            }
                        },
                    )
                }
            }

            if (role == UserRole.CUSTOMER && order.status == OrderStatus.WAITING) {
                item {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                currentUserId?.let { customerId ->
                                    OrdersRepository.cancelOrder(order.id, customerId)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cancelar pedido")
                    }
                }
            }

            if (
                role == UserRole.CUSTOMER &&
                order.requestType == OrderRequestType.SHOPPING &&
                order.status != OrderStatus.ON_THE_WAY &&
                order.status != OrderStatus.DELIVERED &&
                order.status != OrderStatus.CANCELLED
            ) {
                item {
                    SectionTitle("Agregar mas productos")
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = extraProduct,
                            onValueChange = { extraProduct = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Nuevo producto") },
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    OrdersRepository.addOrderItem(
                                        orderId = order.id,
                                        productName = extraProduct,
                                        currentCustomerId = currentUserId,
                                    )
                                    extraProduct = ""
                                }
                            },
                        ) {
                            Text("Agregar")
                        }
                    }
                }
            }

            if (isCourierOwner && order.status != OrderStatus.CANCELLED) {
                item {
                    SectionTitle("Acciones del domiciliario")
                }
                item {
                    CourierActions(
                        requestType = order.requestType,
                        status = order.status,
                        purchasePlacesCount = order.purchasePlacesCount,
                        onBeginWork = {
                            scope.launch {
                                val nextStatus = if (order.requestType == OrderRequestType.SHOPPING) {
                                    OrderStatus.SHOPPING
                                } else {
                                    OrderStatus.ON_THE_WAY
                                }
                                OrdersRepository.updateOrderStatus(
                                    orderId = order.id,
                                    status = nextStatus,
                                    currentUserId = currentUserId,
                                )
                                actionFeedback = if (order.requestType == OrderRequestType.SHOPPING) {
                                    "Compra iniciada."
                                } else {
                                    "Servicio en ruta."
                                }
                            }
                        },
                        onStartDelivery = {
                            scope.launch {
                                OrdersRepository.updateOrderStatus(
                                    orderId = order.id,
                                    status = OrderStatus.ON_THE_WAY,
                                    currentUserId = currentUserId,
                                )
                            }
                        },
                        onDecreasePlaces = {
                            scope.launch {
                                val updatedCount = (order.purchasePlacesCount - 1).coerceAtLeast(1)
                                val result = OrdersRepository.updatePurchasePlaces(
                                    orderId = order.id,
                                    purchasePlacesCount = updatedCount,
                                    currentUserId = currentUserId,
                                )
                                actionFeedback = result.errorMessage
                                    ?: "Tarifa actualizada para $updatedCount lugar(es) de compra."
                            }
                        },
                        onIncreasePlaces = {
                            scope.launch {
                                val updatedCount = order.purchasePlacesCount + 1
                                val result = OrdersRepository.updatePurchasePlaces(
                                    orderId = order.id,
                                    purchasePlacesCount = updatedCount,
                                    currentUserId = currentUserId,
                                )
                                actionFeedback = result.errorMessage
                                    ?: "Tarifa actualizada para $updatedCount lugar(es) de compra."
                            }
                        },
                        onResolveOutcome = { reason ->
                            pendingResolutionReason = reason
                        },
                    )
                }
                if (actionFeedback != null) {
                    item {
                        Text(
                            text = actionFeedback.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (
                role == UserRole.CUSTOMER &&
                order.status == OrderStatus.DELIVERED &&
                !order.courierId.isNullOrBlank()
            ) {
                item {
                    SectionTitle("Califica al domiciliario")
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = if (order.courierRatingValue == null) {
                                    "Tu opinion ayuda a cuidar la calidad del servicio."
                                } else {
                                    "Gracias por calificar este domicilio."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (order.courierRatingValue == null) {
                                RatingStars(
                                    selected = selectedRating,
                                    modifier = Modifier.fillMaxWidth(),
                                    onSelect = { selectedRating = it },
                                )
                                Button(
                                    onClick = {
                                        if (currentUserId == null) {
                                            ratingFeedback = "No pudimos identificar tu cuenta."
                                        } else {
                                            scope.launch {
                                                val result = OrdersRepository.rateCourier(
                                                    orderId = order.id,
                                                    customerId = currentUserId,
                                                    ratingValue = selectedRating,
                                                )
                                                ratingFeedback = result.errorMessage
                                                    ?: "Calificacion guardada con $selectedRating estrella(s)."
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Guardar calificacion")
                                }
                            } else {
                                RatingStars(
                                    selected = order.courierRatingValue,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text = "Ya registraste ${order.courierRatingValue} estrella(s) para este servicio.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (ratingFeedback != null) {
                                Text(
                                    text = ratingFeedback.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("Chat")
            }

            items(order.messages, key = { "message-${it.id}" }) { message ->
                MessageBubble(
                    message = message,
                    viewerRole = role,
                    onOpenLocation = { latitude, longitude ->
                        mapPreview = MapPreviewLocation(
                            latitude = latitude,
                            longitude = longitude,
                            label = message.locationLabel,
                        )
                    },
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isChatEnabled) {
                        Text(
                            text = when (order.status) {
                                OrderStatus.WAITING -> "El chat se habilita cuando un domiciliario tome esta orden."
                                OrderStatus.DELIVERED -> "El chat ya se cerro porque la orden fue entregada."
                                OrderStatus.CANCELLED -> "El chat no esta disponible porque la orden fue cancelada."
                                else -> "El chat no esta disponible en este momento."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = messageDraft,
                        onValueChange = { messageDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isChatEnabled,
                        label = { Text("Escribe un mensaje") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (!isChatEnabled) {
                                    chatFeedback = "El chat solo se habilita cuando la orden esta tomada, comprando o en camino."
                                } else {
                                    imageMessageLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                }
                            },
                            enabled = isChatEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                            Text("Foto")
                        }
                        FilledTonalButton(
                            onClick = {
                                if (!isChatEnabled) {
                                    chatFeedback = "El chat solo se habilita cuando la orden esta tomada, comprando o en camino."
                                } else if (currentUserId != null) {
                                    if (hasLocationPermission(context)) {
                                        sendCurrentLocationMessage()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            ),
                                        )
                                    }
                                }
                            },
                            enabled = isChatEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null)
                            Text("Ubicacion")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (!isChatEnabled) {
                                    chatFeedback = "El chat solo se habilita cuando la orden esta tomada, comprando o en camino."
                                } else if (messageDraft.isBlank() || currentUserId == null) {
                                    chatFeedback = "Escribe algo antes de enviar."
                                } else {
                                    scope.launch {
                                        val result = OrdersRepository.sendMessage(
                                            orderId = order.id,
                                            senderUserId = currentUserId,
                                            messageText = messageDraft,
                                        )
                                        if (result.errorMessage == null) {
                                            messageDraft = ""
                                            chatFeedback = null
                                        } else {
                                            chatFeedback = result.errorMessage
                                        }
                                    }
                                }
                            },
                            enabled = isChatEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                            Text("Enviar")
                        }
                    }
                    if (chatFeedback != null) {
                        Text(
                            text = chatFeedback.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (mapPreview != null) {
            LocationPreviewDialog(
                location = mapPreview!!,
                onDismiss = { mapPreview = null },
                onOpenGoogleMaps = {
                    val preview = mapPreview!!
                    if (preview.destinationLatitude != null && preview.destinationLongitude != null) {
                        openDirectionsInGoogleMaps(
                            context = context,
                            originLatitude = preview.latitude,
                            originLongitude = preview.longitude,
                            destinationLatitude = preview.destinationLatitude,
                            destinationLongitude = preview.destinationLongitude,
                        )
                    } else {
                        openInGoogleMaps(
                            context = context,
                            latitude = preview.latitude,
                            longitude = preview.longitude,
                            label = preview.label,
                        )
                    }
                },
            )
        }

        if (pendingResolutionReason != null && currentUserId != null) {
            AlertDialog(
                onDismissRequest = { pendingResolutionReason = null },
                title = {
                    Text(
                        text = when (pendingResolutionReason) {
                            OrderResolutionReason.DELIVERED -> "Confirmar entrega"
                            OrderResolutionReason.CUSTOMER_NOT_FOUND -> "Cliente no encontrado"
                            OrderResolutionReason.CUSTOMER_NONCOMPLIANCE -> "Incumplimiento del cliente"
                            OrderResolutionReason.CANCELLED_BY_CUSTOMER,
                            null -> ""
                        },
                    )
                },
                text = {
                    Text(
                        text = when (pendingResolutionReason) {
                            OrderResolutionReason.DELIVERED -> "La orden quedara cerrada como entregada."
                            OrderResolutionReason.CUSTOMER_NOT_FOUND -> "La orden se cerrara como cancelada por cliente no encontrado y al cliente se le bajara la reputacion."
                            OrderResolutionReason.CUSTOMER_NONCOMPLIANCE -> "La orden se cerrara por incumplimiento del cliente y quedara una marca de riesgo para futuros pedidos."
                            OrderResolutionReason.CANCELLED_BY_CUSTOMER,
                            null -> ""
                        },
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val resolutionReason = pendingResolutionReason ?: return@Button
                            pendingResolutionReason = null
                            scope.launch {
                                val result = OrdersRepository.resolveOrderOutcome(
                                    orderId = order.id,
                                    courierId = currentUserId,
                                    resolutionReason = resolutionReason,
                                )
                                actionFeedback = result.errorMessage
                                    ?: when (resolutionReason) {
                                        OrderResolutionReason.DELIVERED -> "Pedido cerrado como entregado."
                                        OrderResolutionReason.CUSTOMER_NOT_FOUND -> "Pedido cerrado y cliente marcado por no estar disponible."
                                        OrderResolutionReason.CUSTOMER_NONCOMPLIANCE -> "Pedido cerrado y cliente marcado por incumplimiento."
                                        OrderResolutionReason.CANCELLED_BY_CUSTOMER -> "Pedido cerrado."
                                    }
                            }
                        },
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingResolutionReason = null }) {
                        Text("Volver")
                    }
                },
            )
        }
    }
}

private fun buildChargeSummary(order: com.appdomicilios.model.DeliveryOrder): String {
    val customerSummary = when (order.customerChargeMode) {
        WalletChargeMode.FREE -> "Cliente: pedido de bienvenida pendiente"
        WalletChargeMode.PAID -> "Cliente: ${DemoRepository.currency(order.customerChargeAmount)} descontados"
        WalletChargeMode.REFUNDED -> "Cliente: cobro devuelto"
        WalletChargeMode.NONE -> "Cliente: sin cobro"
    }

    val courierSummary = when (order.courierChargeMode) {
        WalletChargeMode.FREE -> "Domiciliario: toma de bienvenida pendiente"
        WalletChargeMode.PAID -> "Domiciliario: ${DemoRepository.currency(order.courierChargeAmount)} descontados"
        WalletChargeMode.REFUNDED -> "Domiciliario: cobro devuelto"
        WalletChargeMode.NONE -> "Domiciliario: aun sin cobro"
    }

    return "$customerSummary  |  $courierSummary"
}

@Composable
private fun ProductRow(
    item: OrderItem,
    showActions: Boolean,
    onPurchased: () -> Unit,
    onNotFound: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Cantidad: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ItemStatusChip(item.status)
            }
        }
        if (showActions && item.status != OrderItemStatus.PURCHASED) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onPurchased, modifier = Modifier.weight(1f)) {
                    Text("Comprado")
                }
                FilledTonalButton(onClick = onNotFound, modifier = Modifier.weight(1f)) {
                    Text("No encontrado")
                }
            }
        }
    }
}

@Composable
private fun CourierActions(
    requestType: OrderRequestType,
    status: OrderStatus,
    purchasePlacesCount: Int,
    onBeginWork: () -> Unit,
    onStartDelivery: () -> Unit,
    onDecreasePlaces: () -> Unit,
    onIncreasePlaces: () -> Unit,
    onResolveOutcome: (OrderResolutionReason) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (requestType == OrderRequestType.SHOPPING) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Lugares de compra",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Ajusta cuantas paradas reales debes hacer para recalcular el domicilio.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = onDecreasePlaces,
                            enabled = purchasePlacesCount > 1,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("-")
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                            ) {
                                Text(
                                    text = "$purchasePlacesCount lugar(es)",
                                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Button(
                            onClick = onIncreasePlaces,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        }
        when (status) {
            OrderStatus.TAKEN -> {
                Button(onClick = onBeginWork, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when (requestType) {
                            OrderRequestType.SHOPPING -> "Iniciar compra"
                            OrderRequestType.PICKUP_AND_DELIVER -> "Iniciar recogida y entrega"
                            OrderRequestType.OTHER -> "Iniciar vuelta"
                        },
                    )
                }
            }
            OrderStatus.SHOPPING -> {
                Button(onClick = onStartDelivery, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when (requestType) {
                            OrderRequestType.SHOPPING -> "Salir a entregar"
                            OrderRequestType.PICKUP_AND_DELIVER -> "Salir a entregar"
                            OrderRequestType.OTHER -> "Continuar en ruta"
                        },
                    )
                }
            }
            OrderStatus.ON_THE_WAY -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onResolveOutcome(OrderResolutionReason.DELIVERED) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Marcar entregado")
                    }
                    FilledTonalButton(
                        onClick = { onResolveOutcome(OrderResolutionReason.CUSTOMER_NOT_FOUND) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cliente no encontrado")
                    }
                    OutlinedButton(
                        onClick = { onResolveOutcome(OrderResolutionReason.CUSTOMER_NONCOMPLIANCE) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Incumplimiento del cliente")
                    }
                }
            }
            else -> {
                Text(
                    text = "Sin acciones manuales para este estado.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DetailInfoCard(
    title: String,
    lines: List<String>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            lines.filter { it.isNotBlank() }.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

private data class MapPreviewLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String?,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val destinationLabel: String? = null,
)

@Composable
private fun LocationPreviewDialog(
    location: MapPreviewLocation,
    onDismiss: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (location.destinationLatitude != null && location.destinationLongitude != null) {
                        "Ruta solicitada"
                    } else {
                        "Ubicacion compartida"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (!location.label.isNullOrBlank()) {
                    Text(
                        text = location.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!location.destinationLabel.isNullOrBlank()) {
                    Text(
                        text = "Destino: ${location.destinationLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                ) {
                    DisposableEffect(Unit) {
                        val preferences = context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
                        Configuration.getInstance().load(context, preferences)
                        Configuration.getInstance().userAgentValue = context.packageName

                        onDispose { }
                    }
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        factory = { context ->
                            MapView(context).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(16.0)
                                val point = GeoPoint(location.latitude, location.longitude)
                                controller.setCenter(point)

                                overlays.clear()
                                Marker(this).apply {
                                    position = point
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = location.label ?: "Ubicacion compartida"
                                }.also(overlays::add)
                            }
                        },
                        update = { mapView ->
                            val point = GeoPoint(location.latitude, location.longitude)
                            mapView.overlays.clear()
                            Marker(mapView).apply {
                                position = point
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = location.label ?: "Ubicacion compartida"
                            }.also(mapView.overlays::add)

                            val destinationPoint = if (
                                location.destinationLatitude != null &&
                                location.destinationLongitude != null
                            ) {
                                GeoPoint(location.destinationLatitude, location.destinationLongitude)
                            } else {
                                null
                            }

                            if (destinationPoint != null) {
                                Marker(mapView).apply {
                                    position = destinationPoint
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = location.destinationLabel ?: "Destino"
                                }.also(mapView.overlays::add)

                                Polyline(mapView).apply {
                                    setPoints(listOf(point, destinationPoint))
                                }.also(mapView.overlays::add)

                                val bounds = BoundingBox.fromGeoPointsSafe(listOf(point, destinationPoint))
                                mapView.zoomToBoundingBox(bounds, true, 90)
                            } else {
                                mapView.controller.setZoom(16.0)
                                mapView.controller.setCenter(point)
                            }
                            mapView.invalidate()
                        },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cerrar")
                    }
                    Button(
                        onClick = onOpenGoogleMaps,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (location.destinationLatitude != null && location.destinationLongitude != null) {
                                "Abrir ruta"
                            } else {
                                "Abrir en Maps"
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun formatWeightLabel(weightKg: Double): String {
    return "${String.format("%.1f", weightKg)} kg"
}

private fun openInGoogleMaps(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
    label: String?,
) {
    val encodedLabel = Uri.encode(label ?: "$latitude,$longitude")
    val googleMapsUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
    val googleMapsIntent = Intent(Intent.ACTION_VIEW, googleMapsUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    if (googleMapsIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(googleMapsIntent)
        return
    }

    val fallbackUri = Uri.parse(
        "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude",
    )
    context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
}

private fun openDirectionsInGoogleMaps(
    context: android.content.Context,
    originLatitude: Double,
    originLongitude: Double,
    destinationLatitude: Double,
    destinationLongitude: Double,
) {
    val googleMapsUri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&origin=$originLatitude,$originLongitude&destination=$destinationLatitude,$destinationLongitude&travelmode=driving",
    )
    context.startActivity(Intent(Intent.ACTION_VIEW, googleMapsUri))
}

private fun openDialer(
    context: android.content.Context,
    phone: String,
) {
    val normalizedPhone = phone.filter { it.isDigit() || it == '+' }
    if (normalizedPhone.isBlank()) {
        return
    }

    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalizedPhone"))
    context.startActivity(dialIntent)
}

private fun openWhatsApp(
    context: android.content.Context,
    phone: String,
) {
    val rawDigits = phone.filter(Char::isDigit)
    val normalizedPhone = when {
        rawDigits.startsWith("57") -> rawDigits
        rawDigits.length == 10 -> "57$rawDigits"
        else -> rawDigits
    }

    if (normalizedPhone.isBlank()) {
        return
    }

    val whatsappUri = Uri.parse("https://wa.me/$normalizedPhone")
    context.startActivity(Intent(Intent.ACTION_VIEW, whatsappUri))
}
