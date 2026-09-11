package com.appdomicilios.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.appdomicilios.data.DemoRepository
import com.appdomicilios.model.UserProfile
import com.appdomicilios.network.WalletApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletTopUpScreen(
    user: UserProfile,
    onBack: () -> Unit,
    onRefreshWallet: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val suggestedAmounts = listOf(10000, 20000, 30000, 50000)

    var selectedAmount by remember { mutableIntStateOf(user.wallet.minimumTopUp) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var helperMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, user.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshWallet()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recargar saldo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
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
                Text(
                    text = "Saldo actual: ${DemoRepository.currency(user.wallet.balance)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                Text(
                    text = "Las recargas se abren en ePayco. La minima es ${DemoRepository.currency(user.wallet.minimumTopUp)}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    text = if (user.wallet.totalDeferredCharges > 0) {
                        "Tienes ${DemoRepository.currency(user.wallet.totalDeferredCharges)} pendientes por tus beneficios iniciales. La recarga los descontara primero y el resto quedara como saldo real."
                    } else {
                        "No tienes cobros pendientes. Todo lo que recargues entrara como saldo real."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    suggestedAmounts.forEach { amount ->
                        FilterChip(
                            selected = selectedAmount == amount,
                            onClick = { selectedAmount = amount },
                            label = { Text(DemoRepository.currency(amount)) },
                        )
                    }
                }
            }
            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (helperMessage != null) {
                item {
                    Text(
                        text = helperMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        errorMessage = null
                        helperMessage = null
                        isLoading = true

                        scope.launch {
                            val result = WalletApi.createTopUpSession(user.id, selectedAmount)
                            isLoading = false
                            if (result.data != null) {
                                helperMessage = if (result.data.confirmationReady) {
                                    "Abriendo ePayco para terminar la recarga."
                                } else {
                                    "Abriendo ePayco. Todavia falta exponer una URL publica para confirmar automaticamente el saldo."
                                }

                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.data.checkoutPageUrl))
                                context.startActivity(intent)
                            } else {
                                errorMessage = result.errorMessage
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isLoading) "Preparando recarga..." else "Ir a ePayco")
                }
            }
            item {
                OutlinedButton(
                    onClick = onRefreshWallet,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Actualizar saldo")
                }
            }
            item {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Volver")
                }
            }
        }
    }
}
