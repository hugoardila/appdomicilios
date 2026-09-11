package com.appdomicilios.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.appdomicilios.R
import com.appdomicilios.data.DemoRepository
import com.appdomicilios.model.CourierDocumentType
import com.appdomicilios.model.CourierVerificationDraft
import com.appdomicilios.model.UserProfile
import com.appdomicilios.model.UserRole
import com.appdomicilios.network.AuthApi
import com.appdomicilios.ui.components.BrandBannerCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onOpenLogin: () -> Unit,
    onRegistered: (UserProfile) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var roleName by rememberSaveable { mutableStateOf(UserRole.CUSTOMER.name) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var nationalId by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var documentTypeName by rememberSaveable { mutableStateOf(CourierDocumentType.NATIONAL_ID_FRONT.name) }
    var documentFrontUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selfieUri by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val isCourier = roleName == UserRole.COURIER.name

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        documentFrontUri = uri?.toString()
        errorMessage = null
    }

    val selfieLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        selfieUri = uri?.toString()
        errorMessage = null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
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
                text = "Registrate para pedir o para trabajar como domiciliario.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Crea tu cuenta para pedir o trabajar vueltas dentro de Pitalito, Huila.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UserRole.entries.forEach { role ->
                    FilterChip(
                        selected = roleName == role.name,
                        onClick = { roleName = role.name },
                        label = {
                            Text(if (role == UserRole.CUSTOMER) "Cliente" else "Domiciliario")
                        },
                    )
                }
            }
            if (roleName == UserRole.COURIER.name) {
                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Revision manual",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Los domiciliarios quedan pendientes de aprobacion antes de ver pedidos y direcciones.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Validacion del domiciliario",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Para evitar abusos en los pedidos gratis, sube el frente de tu documento o licencia y una selfie clara.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CourierDocumentType.entries.forEach { type ->
                                FilterChip(
                                    selected = documentTypeName == type.name,
                                    onClick = { documentTypeName = type.name },
                                    label = { Text(type.label) },
                                )
                            }
                        }
                        UploadIdentityImageCard(
                            title = "Documento por el frente",
                            subtitle = CourierDocumentType.valueOf(documentTypeName).label,
                            imageUri = documentFrontUri,
                            onPickImage = {
                                documentLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                        UploadIdentityImageCard(
                            title = "Selfie",
                            subtitle = "Tu rostro debe verse completo y bien iluminado.",
                            imageUri = selfieUri,
                            onPickImage = {
                                selfieLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombres completos") },
            )
            OutlinedTextField(
                value = nationalId,
                onValueChange = {
                    nationalId = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Numero de identificacion") },
            )
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Telefono") },
            )
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Correo") },
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contrasena") },
                visualTransformation = PasswordVisualTransformation(),
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = {
                    if (!isLoading) {
                        if (
                            fullName.isBlank() ||
                            nationalId.isBlank() ||
                            phone.isBlank() ||
                            email.isBlank() ||
                            password.length < 6
                        ) {
                            errorMessage = "Completa todos los datos y usa una contrasena de al menos 6 caracteres."
                        } else if (isCourier && (documentFrontUri == null || selfieUri == null)) {
                            errorMessage = "Para activar el registro de domiciliario debes subir documento frontal y selfie."
                        } else {
                            isLoading = true
                            scope.launch {
                                val response = AuthApi.register(
                                    context = context,
                                    fullName = fullName,
                                    nationalId = nationalId,
                                    phone = phone,
                                    email = email,
                                    password = password,
                                    role = UserRole.valueOf(roleName),
                                    courierVerification = if (isCourier) {
                                        CourierVerificationDraft(
                                            documentType = CourierDocumentType.valueOf(documentTypeName),
                                            documentFrontUri = documentFrontUri.orEmpty(),
                                            selfieUri = selfieUri.orEmpty(),
                                        )
                                    } else {
                                        null
                                    },
                                )
                                isLoading = false

                                if (response.user != null) {
                                    DemoRepository.applyAuthenticatedUser(response.user)
                                    onRegistered(response.user)
                                } else {
                                    errorMessage = response.errorMessage
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isLoading) "Creando..." else "Crear cuenta")
            }
            TextButton(onClick = onOpenLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Ya tengo cuenta")
            }
        }
    }
}

@Composable
private fun UploadIdentityImageCard(
    title: String,
    subtitle: String,
    imageUri: String?,
    onPickImage: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (imageUri != null) {
                AsyncImage(
                    model = Uri.parse(imageUri),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit,
                )
            }
            Button(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
                Text(if (imageUri == null) "Subir foto" else "Cambiar foto")
            }
        }
    }
}
