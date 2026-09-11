package com.appdomicilios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.appdomicilios.data.DemoRepository
import com.appdomicilios.model.DeliveryOrder
import com.appdomicilios.model.OrderCategory
import com.appdomicilios.model.OrderStatus
import com.appdomicilios.model.UserProfile
import com.appdomicilios.ui.components.CompactMetric
import com.appdomicilios.ui.components.MetricCard
import com.appdomicilios.ui.components.MiniMetricCluster
import com.appdomicilios.ui.components.OrderCard
import com.appdomicilios.ui.components.ServiceCatalogGrid
import com.appdomicilios.ui.components.rememberServiceCatalogEntries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    customer: UserProfile,
    orders: List<DeliveryOrder>,
    isLoading: Boolean,
    errorMessage: String?,
    onSignOut: () -> Unit,
    onRefresh: () -> Unit,
    onCreateOrder: (OrderCategory) -> Unit,
    onTopUp: () -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    val hasPaidCoverage = customer.wallet.customerFreeOrdersRemaining > 0 ||
        customer.wallet.balance >= customer.wallet.customerOrderFee
    val activeOrdersCount = orders.count {
        it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED
    }
    val catalogEntries = rememberServiceCatalogEntries()
    val customerMetrics = listOf(
        CompactMetric(
            title = "Saldo",
            value = DemoRepository.currency(customer.wallet.balance),
            accent = MaterialTheme.colorScheme.primary,
        ),
        CompactMetric(
            title = "Activos",
            value = activeOrdersCount.toString(),
            accent = MaterialTheme.colorScheme.secondary,
        ),
        CompactMetric(
            title = "Por cobrar",
            value = DemoRepository.currency(customer.wallet.totalDeferredCharges),
            accent = MaterialTheme.colorScheme.tertiary,
        ),
    )
    var isHistoryVisible by remember { mutableStateOf(false) }

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
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
                                text = "Hola, ${customer.fullName.substringBefore(' ')}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                MiniMetricCluster(
                                    metrics = customerMetrics,
                                    modifier = Modifier.widthIn(max = 170.dp),
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.widthIn(max = 200.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TextButton(onClick = { isHistoryVisible = true }) {
                                Text("Historial de solicitudes")
                            }
                        }
                    }
                }
            }
            item {
                ServiceCatalogGrid(
                    entries = catalogEntries,
                    onEntryClick = onCreateOrder,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                MetricCard(
                    title = "Cobro por pedido",
                    value = if (customer.wallet.customerFreeOrdersRemaining > 0) {
                        "${customer.wallet.customerFreeOrdersRemaining} bienvenida"
                    } else {
                        DemoRepository.currency(customer.wallet.customerOrderFee)
                    },
                    subtitle = if (customer.wallet.customerFreeOrdersRemaining > 0) {
                        "Todavia tienes ${customer.wallet.customerFreeOrdersRemaining} pedido(s) de bienvenida. Despues, cada domicilio descuenta ${DemoRepository.currency(customer.wallet.customerOrderFee)}."
                    } else if (customer.wallet.totalDeferredCharges > 0) {
                        "Tienes ${DemoRepository.currency(customer.wallet.totalDeferredCharges)} pendientes y se descuentan en tu siguiente recarga."
                    } else {
                        "Cada nueva solicitud descuenta ${DemoRepository.currency(customer.wallet.customerOrderFee)} de tu saldo disponible."
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                TextButton(onClick = onTopUp) {
                    Text(if (hasPaidCoverage) "Recargar saldo" else "Recargar para seguir pidiendo")
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
        }

        if (isHistoryVisible) {
            CustomerHistoryDialog(
                orders = orders,
                isLoading = isLoading,
                onDismiss = { isHistoryVisible = false },
                onRefresh = onRefresh,
                onOpenOrder = { orderId ->
                    isHistoryVisible = false
                    onOpenOrder(orderId)
                },
            )
        }
    }
}

@Composable
private fun CustomerHistoryDialog(
    orders: List<DeliveryOrder>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onOpenOrder: (String) -> Unit,
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
                            text = "Historial de solicitudes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Aqui puedes revisar tus pedidos y volver a entrar al detalle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                    ) {
                        Text(
                            text = "Cargando tus solicitudes...",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else if (orders.isEmpty()) {
                    Text(
                        text = "Todavia no tienes solicitudes. Pide por categoria para crear la primera.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(orders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                onOpen = { onOpenOrder(order.id) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Actualizar")
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
