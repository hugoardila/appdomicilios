package com.appdomicilios.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.appdomicilios.R
import com.appdomicilios.data.DemoRepository
import com.appdomicilios.model.DeliveryOrder
import com.appdomicilios.model.OrderCategory
import com.appdomicilios.model.OrderItemStatus
import com.appdomicilios.model.OrderMessage
import com.appdomicilios.model.OrderRequestType
import com.appdomicilios.model.OrderResolutionReason
import com.appdomicilios.model.OrderStatus
import com.appdomicilios.model.UserRole
import com.appdomicilios.model.WalletChargeMode

@Composable
fun BrandBannerCard(modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_brand_banner),
            contentDescription = "Logo XpertGoPitalito",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentScale = ContentScale.FillWidth,
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

data class CompactMetric(
    val title: String,
    val value: String,
    val accent: Color,
)

@Composable
fun MiniMetricCluster(
    metrics: List<CompactMetric>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        metrics.forEach { metric ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = metric.accent,
                    contentColor = metric.accent,
                ) {}
                Text(
                    text = metric.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

data class ServiceCatalogEntry(
    val category: OrderCategory,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
fun CompactMetricStrip(
    metrics: List<CompactMetric>,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            metrics.forEachIndexed { index, metric ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = metric.accent,
                        contentColor = metric.accent,
                    ) {}
                    Text(
                        text = metric.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = metric.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }

                if (index != metrics.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                    )
                }
            }
        }
    }
}

@Composable
fun PrimaryActionCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ServiceCatalogGrid(
    entries: List<ServiceCatalogEntry>,
    onEntryClick: (OrderCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Pide por categoria",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Escoge el servicio que necesitas y entra directo al formulario correcto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entries.chunked(3).forEach { rowEntries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowEntries.forEach { entry ->
                        ServiceCatalogButton(
                            entry = entry,
                            onClick = { onEntryClick(entry.category) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowEntries.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCatalogButton(
    entry: ServiceCatalogEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(999.dp),
                color = entry.accent.copy(alpha = 0.18f),
                contentColor = entry.accent,
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = entry.category.label,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(32.dp),
                )
            }
            Text(
                text = entry.category.shortLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                text = entry.category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
        }
    }
}

@Composable
fun rememberServiceCatalogEntries(): List<ServiceCatalogEntry> {
    return listOf(
        ServiceCatalogEntry(
            category = OrderCategory.SHOPPING,
            icon = Icons.Outlined.ShoppingCart,
            accent = Color(0xFF0A66C2),
        ),
        ServiceCatalogEntry(
            category = OrderCategory.RESTAURANTS,
            icon = Icons.Outlined.RestaurantMenu,
            accent = Color(0xFFE66A00),
        ),
        ServiceCatalogEntry(
            category = OrderCategory.DOMICILIOS,
            icon = Icons.Outlined.DeliveryDining,
            accent = Color(0xFFFF7A00),
        ),
        ServiceCatalogEntry(
            category = OrderCategory.TRAMITES,
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            accent = Color(0xFF1F8A70),
        ),
        ServiceCatalogEntry(
            category = OrderCategory.MOTOTAXI,
            icon = Icons.Outlined.TwoWheeler,
            accent = Color(0xFFE65100),
        ),
        ServiceCatalogEntry(
            category = OrderCategory.ENVIOS,
            icon = Icons.Outlined.LocalShipping,
            accent = Color(0xFF455A64),
        ),
    )
}

@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val colors = when (status) {
        OrderStatus.WAITING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.TAKEN -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.SHOPPING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.ON_THE_WAY -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = colors.first,
        contentColor = colors.second,
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ItemStatusChip(status: OrderItemStatus, modifier: Modifier = Modifier) {
    val background = when (status) {
        OrderItemStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
        OrderItemStatus.PURCHASED -> MaterialTheme.colorScheme.primaryContainer
        OrderItemStatus.NOT_FOUND -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (status) {
        OrderItemStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        OrderItemStatus.PURCHASED -> MaterialTheme.colorScheme.onPrimaryContainer
        OrderItemStatus.NOT_FOUND -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = background,
        contentColor = content,
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun OrderCard(
    order: DeliveryOrder,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    showCourierCollectionSummary: Boolean = false,
    compact: Boolean = false,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(status = order.status)
                Text(
                    text = order.updatedAtLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = order.customerName,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = order.category.label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = order.addressText,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = orderCardSummary(order),
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = orderCardMeta(order),
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (order.customerReputation.warningActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (compact) "Posible cliente falso" else "Ojo: posible cliente falso",
                            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!compact) {
                            Text(
                                text = "Comunicarse primero. Tiene ${order.customerReputation.incidentsCount} novedad(es) previa(s) y reputacion ${String.format("%.1f", order.customerReputation.score)}/5.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (showCourierCollectionSummary) {
                if (compact) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = "Cliente paga ${DemoRepository.currency(order.deliveryFee)}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Cliente paga al domiciliario: ${DemoRepository.currency(order.deliveryFee)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = courierCollectionSummary(order),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (!compact) {
                Text(
                    text = orderFeeLabel(order),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            order.resolutionReason
                ?.takeIf { it != OrderResolutionReason.DELIVERED }
                ?.let { resolution ->
                    Text(
                        text = "Cierre: ${resolution.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                        Text("Detalle")
                    }
                    if (primaryActionLabel != null && onPrimaryAction != null) {
                        Button(onClick = onPrimaryAction, modifier = Modifier.fillMaxWidth()) {
                            Text(primaryActionLabel.replace("pedido", "").trim().ifBlank { primaryActionLabel })
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                        Text("Ver detalle")
                    }
                    if (primaryActionLabel != null && onPrimaryAction != null) {
                        Button(onClick = onPrimaryAction, modifier = Modifier.weight(1f)) {
                            Text(primaryActionLabel)
                        }
                    }
                }
            }
        }
    }
}

private fun orderFeeLabel(order: DeliveryOrder): String {
    return when {
        order.customerChargeMode == WalletChargeMode.FREE -> "Bienvenida pendiente"
        order.customerChargeMode == WalletChargeMode.REFUNDED -> "Cobro devuelto"
        order.serviceFee > 0 -> "Uso app ${DemoRepository.currency(order.serviceFee)}"
        else -> "Sin cobro app"
    }
}

private fun orderCardSummary(order: DeliveryOrder): String {
    return when (order.category) {
        OrderCategory.SHOPPING,
        OrderCategory.RESTAURANTS,
        OrderCategory.TRAMITES,
        -> buildString {
            order.serviceSubcategory?.takeIf { it.isNotBlank() }?.let {
                append(it)
                append(" | ")
            }
            append(order.originalRequestText)
        }
        OrderCategory.DOMICILIOS -> buildString {
            append("Recoger en ${order.pickupAddress ?: "punto por confirmar"}")
            order.dropoffContactName?.takeIf { it.isNotBlank() }?.let {
                append(" y entregar a $it")
            }
        }
        OrderCategory.MOTOTAXI -> buildString {
            append("Origen: ")
            append(order.addressReference ?: "punto actual")
            order.destinationReference?.takeIf { it.isNotBlank() }?.let {
                append(" | Destino: ")
                append(it)
            } ?: append(" | Destino: ${order.addressText}")
        }
        OrderCategory.ENVIOS -> buildString {
            order.serviceSubcategory?.takeIf { it.isNotBlank() }?.let {
                append(it)
                append(" | ")
            }
            append("Recoger en ${order.pickupAddress ?: "punto por confirmar"}")
            order.packageWeightKg?.let {
                append(" | ${formatWeightLabel(it)}")
            }
        }
    }
}

private fun orderCardMeta(order: DeliveryOrder): String {
    return when (order.category) {
        OrderCategory.SHOPPING -> "${order.items.sumOf { it.quantity }} unidad(es)  |  ${order.purchasePlacesCount} lugar(es)  |  Domicilio ${DemoRepository.currency(order.deliveryFee)}"
        OrderCategory.RESTAURANTS -> "${order.items.sumOf { it.quantity }} producto(s)  |  Pedido de restaurante  |  Domicilio ${DemoRepository.currency(order.deliveryFee)}"
        OrderCategory.DOMICILIOS -> "Recogida y entrega  |  Domicilio ${DemoRepository.currency(order.deliveryFee)}"
        OrderCategory.TRAMITES -> "${order.items.sumOf { it.quantity }} gestion(es)  |  Domicilio ${DemoRepository.currency(order.deliveryFee)}"
        OrderCategory.MOTOTAXI -> "Servicio mototaxi  |  Domicilio ${DemoRepository.currency(order.deliveryFee)}"
        OrderCategory.ENVIOS -> "${order.packageWeightKg?.let(::formatWeightLabel) ?: "Peso por confirmar"}  |  Domicilio ${DemoRepository.currency(order.deliveryFee)}"
    }
}

private fun courierCollectionSummary(order: DeliveryOrder): String {
    return when (order.category) {
        OrderCategory.SHOPPING -> "Productos aparte segun la compra. Metodo: ${order.paymentMethod.label}."
        OrderCategory.RESTAURANTS -> "Pedido de restaurante. Metodo: ${order.paymentMethod.label}."
        OrderCategory.DOMICILIOS -> {
            val pickupPayment = if (order.pickupPaymentAmount > 0) {
                " Debe llevar ${DemoRepository.currency(order.pickupPaymentAmount)} si toca pagar al recoger."
            } else {
                ""
            }
            "Recoge y entrega. Metodo: ${order.paymentMethod.label}.$pickupPayment"
        }
        OrderCategory.TRAMITES -> "Gestion especial. Metodo: ${order.paymentMethod.label}."
        OrderCategory.MOTOTAXI -> "Traslado solicitado. Metodo: ${order.paymentMethod.label}."
        OrderCategory.ENVIOS -> {
            val pickupPayment = if (order.pickupPaymentAmount > 0) {
                " Debe llevar ${DemoRepository.currency(order.pickupPaymentAmount)} si toca pagar al recoger."
            } else {
                ""
            }
            "Envio ${order.serviceSubcategory?.lowercase().orEmpty()} ${order.packageWeightKg?.let(::formatWeightLabel) ?: ""}. Metodo: ${order.paymentMethod.label}.$pickupPayment".trim()
        }
    }
}

private fun formatWeightLabel(weightKg: Double): String {
    return "${String.format("%.1f", weightKg)} kg"
}

@Composable
fun MessageBubble(
    message: OrderMessage,
    viewerRole: UserRole,
    modifier: Modifier = Modifier,
    onOpenLocation: ((Double, Double) -> Unit)? = null,
) {
    val background = if (message.fromCourier) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (message.fromCourier) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.fromCourier) Alignment.End else Alignment.Start,
    ) {
        val isOutgoing = when (viewerRole) {
            UserRole.CUSTOMER -> !message.fromCourier && !message.isSystem
            UserRole.COURIER -> message.fromCourier && !message.isSystem
        }
        val seenLabel = when {
            !isOutgoing -> null
            message.fromCourier && message.seenByCustomer -> {
                if (message.seenByCustomerLabel.isNotBlank()) "Visto ${message.seenByCustomerLabel}" else "Visto"
            }
            !message.fromCourier && message.seenByCourier -> {
                if (message.seenByCourierLabel.isNotBlank()) "Visto ${message.seenByCourierLabel}" else "Visto"
            }
            else -> "Enviado"
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = background,
            contentColor = content,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (message.body.isNotBlank()) {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (message.imageUri != null) {
                    AsyncImage(
                        model = message.imageUri,
                        contentDescription = "Imagen adjunta",
                        modifier = Modifier
                            .width(180.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
                if (message.latitude != null && message.longitude != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable(enabled = onOpenLocation != null) {
                            onOpenLocation?.invoke(message.latitude, message.longitude)
                        },
                    ) {
                        Column(
                            modifier = Modifier
                                .width(220.dp)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Ubicacion compartida",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Toca para verla dentro de la app o abrirla en Maps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = message.locationLabel ?: "${message.latitude}, ${message.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    text = message.sentAtLabel,
                    style = MaterialTheme.typography.labelSmall,
                )
                if (!seenLabel.isNullOrBlank()) {
                    Text(
                        text = seenLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun RatingStars(
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: ((Int) -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (value in 1..5) {
            val tint = if (value <= selected) {
                Color(0xFFFF9800)
            } else {
                MaterialTheme.colorScheme.outline
            }

            Icon(
                imageVector = if (value <= selected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "$value estrellas",
                tint = tint,
                modifier = Modifier
                    .size(28.dp)
                    .let { base ->
                        if (onSelect != null) {
                            base.clickable { onSelect(value) }
                        } else {
                            base
                        }
                    },
            )
        }
    }
}
