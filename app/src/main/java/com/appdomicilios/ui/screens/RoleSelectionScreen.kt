package com.appdomicilios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RoleSelectionScreen(
    onCustomerClick: () -> Unit,
    onCourierClick: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "App de domicilios para Pitalito",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Una sola app, dos experiencias: cliente y domiciliario. Esta base ya refleja pedidos manuales, chat, fotos y control de hasta 3 pedidos activos para el domiciliario.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoleCard(
                title = "Version cliente",
                subtitle = "Crear pedidos libres por texto, adjuntar fotos, compartir direccion y seguir el estado del domicilio.",
                actionLabel = "Entrar como cliente",
                onClick = onCustomerClick,
            )

            RoleCard(
                title = "Version domiciliario",
                subtitle = "Ver pedidos en espera, tomar maximo 3, marcar productos y hablar con el cliente en tiempo real.",
                actionLabel = "Entrar como domiciliario",
                onClick = onCourierClick,
            )

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "MVP acordado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    SummaryLine("Cobertura inicial solo para Pitalito, Huila.")
                    SummaryLine("Domiciliarios con aprobacion manual antes de activar pedidos.")
                    SummaryLine("Cliente puede agregar productos mientras no esten comprados.")
                    SummaryLine("La app cobra una tarifa de servicio adicional al domicilio.")
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onClick) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(text: String) {
    Text(
        text = "- $text",
        style = MaterialTheme.typography.bodyMedium,
    )
}
