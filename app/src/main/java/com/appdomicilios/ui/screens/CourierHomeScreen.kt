package com.appdomicilios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appdomicilios.data.DemoRepository
import com.appdomicilios.model.CourierApprovalStatus
import com.appdomicilios.model.DeliveryOrder
import com.appdomicilios.model.UserProfile
import com.appdomicilios.ui.components.CompactMetric
import com.appdomicilios.ui.components.MetricCard
import com.appdomicilios.ui.components.MiniMetricCluster
import com.appdomicilios.ui.components.OrderCard
import com.appdomicilios.ui.components.PrimaryActionCard
import com.appdomicilios.ui.components.RatingStars
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourierHomeScreen(
    courier: UserProfile,
    activeOrders: List<DeliveryOrder>,
    availableOrders: List<DeliveryOrder>,
    isLoading: Boolean,
    errorMessage: String?,
    onSignOut: () -> Unit,
    onTopUp: () -> Unit,
    onRefresh: () -> Unit,
    onTakeOrder: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    val slotsLeft = (3 - activeOrders.size).coerceAtLeast(0)
    val approved = courier.approvalStatus == CourierApprovalStatus.APPROVED
    val hasWalletCoverage = courier.wallet.courierFreeTakesRemaining > 0 ||
        courier.wallet.balance >= courier.wallet.courierTakeFee
    val ratingAverage = courier.courierRating.average
    val ratingCount = courier.courierRating.count
    val courierMetrics = listOf(
        CompactMetric(
            title = "Cupos",
            value = slotsLeft.toString(),
            accent = MaterialTheme.colorScheme.primary,
        ),
        CompactMetric(
            title = "Saldo",
            value = DemoRepository.currency(courier.wallet.balance),
            accent = MaterialTheme.colorScheme.secondary,
        ),
        CompactMetric(
            title = "Por cobrar",
            value = DemoRepository.currency(courier.wallet.totalDeferredCharges),
            accent = MaterialTheme.colorScheme.tertiary,
        ),
    )
    var isAccountDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("XpertGoPitalito") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    TextButton(onClick = onTopUp) {
                        Text("Recargar")
                    }
                    TextButton(onClick = onSignOut) {
                        Text("Salir")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Hola, ${courier.fullName.substringBefore(' ')}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Revisa la zona, toma pedidos y mueve tus vueltas del dia sin tanto ruido en pantalla.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Column(
                            modifier = Modifier.widthIn(max = 165.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TextButton(onClick = { isAccountDialogVisible = true }) {
                                Text("Estado de tu cuenta")
                            }
                            MiniMetricCluster(
                                metrics = courierMetrics,
                                modifier = Modifier.align(Alignment.End),
                            )
                        }
                    }
                }
            }
            if (courier.courierRating.warningActive) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Riesgo de desactivacion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Tu promedio esta en ${String.format("%.1f", ratingAverage)}. Si no mejoras el comportamiento de entrega, tu cuenta sera desactivada.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            item {
                PrimaryActionCard(
                    title = if (hasWalletCoverage) "Zona disponible" else "Recarga para seguir tomando",
                    subtitle = if (hasWalletCoverage) {
                        "Hay ${availableOrders.size} pedido(s) en espera para revisar y tomar desde esta zona."
                    } else {
                        "Ya agotaste tus tomas iniciales o tu saldo actual no alcanza para tomar otro pedido."
                    },
                    actionLabel = if (hasWalletCoverage) "Actualizar zona" else "Recargar ahora",
                    onAction = if (hasWalletCoverage) onRefresh else onTopUp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                TextButton(onClick = onTopUp) {
                    Text(if (hasWalletCoverage) "Recargar saldo" else "Recargar para seguir tomando")
                }
            }
            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                Text(
                    text = "Activos en ruta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (activeOrders.isEmpty()) {
                item {
                    Text(
                        text = "Todavia no has tomado pedidos. Cuando tomes uno, te apareceran aqui tus vueltas activas.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(activeOrders.chunked(2), key = { chunk -> chunk.joinToString("-") { it.id } }) { rowOrders ->
                    CourierOrderGridRow(
                        orders = rowOrders,
                        onTakeOrder = null,
                        canTakeOrder = false,
                        onOpenOrder = onOpenOrder,
                    )
                }
            }
            item {
                Text(
                    text = "Vueltas disponibles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (isLoading) {
                item {
                    Text(
                        text = "Cargando pedidos de la zona...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (availableOrders.isEmpty()) {
                item {
                    Text(
                        text = "En este momento no hay pedidos esperando domiciliario en la zona.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item {
                    TextButton(onClick = onRefresh) {
                        Text("Actualizar")
                    }
                }
            } else {
                items(availableOrders.chunked(2), key = { chunk -> chunk.joinToString("-") { it.id } }) { rowOrders ->
                    CourierOrderGridRow(
                        orders = rowOrders,
                        onTakeOrder = onTakeOrder,
                        canTakeOrder = approved && slotsLeft > 0 && hasWalletCoverage,
                        onOpenOrder = onOpenOrder,
                    )
                }
                item {
                    TextButton(onClick = onRefresh) {
                        Text("Actualizar lista")
                    }
                }
            }
        }

        if (isAccountDialogVisible) {
            CourierAccountDialog(
                approved = approved,
                ratingAverage = ratingAverage,
                ratingCount = ratingCount,
                courier = courier,
                onDismiss = { isAccountDialogVisible = false },
                onTopUp = onTopUp,
            )
        }
    }
}

@Composable
private fun CourierOrderGridRow(
    orders: List<DeliveryOrder>,
    onTakeOrder: ((String) -> Unit)?,
    canTakeOrder: Boolean,
    onOpenOrder: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        orders.forEach { order ->
            OrderCard(
                order = order,
                primaryActionLabel = if (canTakeOrder && onTakeOrder != null) "Tomar pedido" else null,
                onPrimaryAction = if (canTakeOrder && onTakeOrder != null) {
                    { onTakeOrder(order.id) }
                } else {
                    null
                },
                showCourierCollectionSummary = true,
                compact = true,
                onOpen = { onOpenOrder(order.id) },
                modifier = Modifier.weight(1f),
            )
        }
        repeat(2 - orders.size) {
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CourierAccountDialog(
    approved: Boolean,
    ratingAverage: Double,
    ratingCount: Int,
    courier: UserProfile,
    onDismiss: () -> Unit,
    onTopUp: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Estado de tu cuenta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Aqui ves tu aprobacion, reputacion y cobros de uso.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                }

                MetricCard(
                    title = "Cuenta",
                    value = if (approved) "Aprobado" else "Pendiente",
                    subtitle = if (courier.wallet.courierFreeTakesRemaining > 0) {
                        "Tienes ${courier.wallet.courierFreeTakesRemaining} toma(s) de bienvenida. Luego, cada pedido nuevo descuenta ${DemoRepository.currency(courier.wallet.courierTakeFee)}."
                    } else if (courier.wallet.totalDeferredCharges > 0) {
                        "Tienes ${DemoRepository.currency(courier.wallet.totalDeferredCharges)} pendientes y se descuentan en tu siguiente recarga."
                    } else {
                        "Tu cuenta esta lista para seguir tomando pedidos de la zona."
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                MetricCard(
                    title = "Calificacion",
                    value = if (ratingCount > 0) String.format("%.1f / 5", ratingAverage) else "Sin calificar",
                    subtitle = if (ratingCount > 0) {
                        "Llevas $ratingCount calificacion(es) registradas por clientes."
                    } else {
                        "Todavia no tienes calificaciones. Entrega bien y cuida la comunicacion para construir una buena reputacion."
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                RatingStars(
                    selected = ratingAverage.roundToInt().coerceIn(0, 5),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onTopUp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Recargar")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Listo")
                    }
                }
            }
        }
    }
}
