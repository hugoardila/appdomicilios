package com.appdomicilios.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.appdomicilios.model.CourierReviewCandidate
import com.appdomicilios.ui.components.BrandBannerCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourierReviewScreen(
    isAuthenticated: Boolean,
    pendingCouriers: List<CourierReviewCandidate>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSubmitAccessKey: suspend (String) -> Boolean,
    onRefresh: suspend () -> Unit,
    onApprove: suspend (String) -> Unit,
    onReject: suspend (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var accessKey by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (isAuthenticated) "Revision de domiciliarios" else "Panel interno")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isAuthenticated) {
                        IconButton(onClick = { scope.launch { onRefresh() } }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!isAuthenticated) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BrandBannerCard()
                Text(
                    text = "Revision interna",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Desde aqui puedes revisar documento y selfie de nuevos domiciliarios antes de aprobarlos.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = accessKey,
                    onValueChange = { accessKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Clave interna") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            onSubmitAccessKey(accessKey)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isLoading) "Entrando..." else "Entrar al panel")
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Solicitudes pendientes",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${pendingCouriers.size} domiciliario(s) por revisar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onSignOut) {
                        Text("Salir")
                    }
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

            if (pendingCouriers.isEmpty()) {
                item {
                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "No hay domiciliarios pendientes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Cuando alguien se registre como domiciliario con documento y selfie, aparecerá aquí.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(pendingCouriers, key = { it.id }) { candidate ->
                    CourierReviewCard(
                        candidate = candidate,
                        isLoading = isLoading,
                        onApprove = {
                            scope.launch { onApprove(candidate.id) }
                        },
                        onReject = {
                            scope.launch { onReject(candidate.id) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CourierReviewCard(
    candidate: CourierReviewCandidate,
    isLoading: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = candidate.fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Documento: ${candidate.nationalId}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Telefono: ${candidate.phone}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Correo: ${candidate.email}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Enviado: ${candidate.submittedAtLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = candidate.documentLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            candidate.documentUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = candidate.documentLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = "Selfie",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            candidate.selfieUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Selfie del domiciliario",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onReject,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                    Text("Rechazar")
                }
                Button(
                    onClick = onApprove,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Text("Aprobar")
                }
            }
        }
    }
}
