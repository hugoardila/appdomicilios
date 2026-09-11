package com.appdomicilios.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appdomicilios.catalog.CatalogSubcategory
import com.appdomicilios.catalog.ExpandedServiceCatalog
import coil.compose.AsyncImage
import com.appdomicilios.data.DemoRepository
import com.appdomicilios.location.PITALITO_SERVICE_AREA_CITY
import com.appdomicilios.location.PITALITO_SERVICE_AREA_NAME
import com.appdomicilios.location.PITALITO_SERVICE_AREA_RADIUS_METERS
import com.appdomicilios.location.checkPitalitoCoverage
import com.appdomicilios.location.formatDistanceMeters
import com.appdomicilios.location.formatCoordinate
import com.appdomicilios.location.hasLocationPermission
import com.appdomicilios.location.requestCurrentLocation
import com.appdomicilios.location.resolveCoordinatesFromAddress
import com.appdomicilios.location.resolveAddressFromCoordinates
import com.appdomicilios.model.OrderCategory
import com.appdomicilios.model.OrderRequestType
import com.appdomicilios.model.PaymentMethod
import com.appdomicilios.model.RequestedOrderItem
import com.appdomicilios.model.WalletInfo
import com.appdomicilios.model.defaultRequestType
import com.appdomicilios.pricing.SHIPPING_BASE_WEIGHT_KG
import com.appdomicilios.pricing.SHIPPING_EXTRA_KILO_FEE
import com.appdomicilios.pricing.URBAN_MINIMUM_DELIVERY_FEE
import com.appdomicilios.pricing.calculateCategoryDeliveryFee
import com.appdomicilios.pricing.deliveryFeeRuleSummary
import com.appdomicilios.pricing.shippingFeeRuleSummary
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

data class CreateOrderDraft(
    val category: OrderCategory,
    val requestType: OrderRequestType,
    val serviceSubcategory: String? = null,
    val items: List<RequestedOrderItem> = emptyList(),
    val details: String? = null,
    val address: String,
    val shareLocation: Boolean,
    val addressReference: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val destinationReference: String? = null,
    val paymentMethod: PaymentMethod,
    val referenceImages: List<String>,
    val pickupAddress: String? = null,
    val pickupContactName: String? = null,
    val pickupContactPhone: String? = null,
    val dropoffContactName: String? = null,
    val dropoffContactPhone: String? = null,
    val pickupPaymentAmount: Int = 0,
    val packageWeightKg: Double? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    initialCategory: OrderCategory,
    walletInfo: WalletInfo,
    serverErrorMessage: String?,
    onBack: () -> Unit,
    onTopUp: () -> Unit,
    onSubmit: (CreateOrderDraft) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val category = initialCategory
    val requestType = category.defaultRequestType()
    val availableSubcategories = remember(category) { ExpandedServiceCatalog.subcategoriesFor(category) }
    val defaultSubcategoryId = availableSubcategories.firstOrNull()?.id.orEmpty()

    var catalogSearchQuery by rememberSaveable(category.name) { mutableStateOf("") }
    var selectedItems by remember(category.name) { mutableStateOf<List<RequestedOrderItem>>(emptyList()) }
    var selectedSubcategoryId by rememberSaveable(category.name) { mutableStateOf(defaultSubcategoryId) }
    var otherDetails by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var pickupAddress by rememberSaveable { mutableStateOf("") }
    var pickupContactName by rememberSaveable { mutableStateOf("") }
    var pickupContactPhone by rememberSaveable { mutableStateOf("") }
    var dropoffContactName by rememberSaveable { mutableStateOf("") }
    var dropoffContactPhone by rememberSaveable { mutableStateOf("") }
    var pickupPaymentAmountText by rememberSaveable { mutableStateOf("") }
    var packageWeightText by rememberSaveable { mutableStateOf("") }
    var shareLocation by rememberSaveable(category.name) { mutableStateOf(category != OrderCategory.MOTOTAXI) }
    var paymentMethod by rememberSaveable { mutableStateOf(PaymentMethod.CASH.name) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var isResolvingDestination by remember { mutableStateOf(false) }
    var locationMessage by remember {
        mutableStateOf("Confirma tu ubicacion actual para validar que el pedido si esta dentro de Pitalito.")
    }
    var locationReference by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var selectedLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var destinationReference by rememberSaveable { mutableStateOf<String?>(null) }
    var destinationLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var destinationLongitude by rememberSaveable { mutableStateOf<Double?>(null) }

    val canCreateOrder = walletInfo.customerFreeOrdersRemaining > 0 ||
        walletInfo.balance >= walletInfo.customerOrderFee
    val selectedSubcategory = availableSubcategories.firstOrNull { it.id == selectedSubcategoryId }
    val packageWeightKg = packageWeightText.replace(',', '.').toDoubleOrNull()
    val draftProductCount = selectedItems.sumOf { it.quantity.coerceAtLeast(1) }
    val estimatedDeliveryFee = calculateCategoryDeliveryFee(
        category = category,
        requestType = requestType,
        productCount = draftProductCount,
        purchasePlacesCount = 1,
        packageWeightKg = packageWeightKg,
    )
    val coverageCheck = if (selectedLatitude != null && selectedLongitude != null) {
        checkPitalitoCoverage(
            latitude = selectedLatitude!!,
            longitude = selectedLongitude!!,
        )
    } else {
        null
    }
    val destinationCoverageCheck = if (destinationLatitude != null && destinationLongitude != null) {
        checkPitalitoCoverage(
            latitude = destinationLatitude!!,
            longitude = destinationLongitude!!,
        )
    } else {
        null
    }
    val filteredCatalogSuggestions = remember(
        category,
        selectedSubcategoryId,
        catalogSearchQuery,
    ) {
        val normalizedQuery = catalogSearchQuery.trim().lowercase()
        val allSuggestions = ExpandedServiceCatalog
            .suggestionsFor(category, selectedSubcategoryId)
        allSuggestions
            .filter { suggestion ->
                normalizedQuery.isBlank() || suggestion.lowercase().contains(normalizedQuery)
            }
            .take(if (normalizedQuery.isBlank()) 3 else 8)
    }

    fun addRequestedItem(name: String) {
        val cleanedName = name.trim()
        if (cleanedName.isBlank()) {
            validationError = "Escribe o selecciona un item para agregarlo."
            return
        }

        selectedItems = selectedItems
            .toMutableList()
            .apply {
                val existingIndex = indexOfFirst { it.name.equals(cleanedName, ignoreCase = true) }
                if (existingIndex >= 0) {
                    val currentItem = this[existingIndex]
                    this[existingIndex] = currentItem.copy(quantity = currentItem.quantity + 1)
                } else {
                    add(RequestedOrderItem(name = cleanedName, quantity = 1))
                }
            }
            .toList()
        catalogSearchQuery = ""
        validationError = null
    }

    fun updateRequestedItemQuantity(name: String, delta: Int) {
        selectedItems = selectedItems
            .mapNotNull { item ->
                if (!item.name.equals(name, ignoreCase = true)) {
                    item
                } else {
                    val nextQuantity = (item.quantity + delta).coerceAtLeast(0)
                    if (nextQuantity == 0) null else item.copy(quantity = nextQuantity)
                }
            }
    }

    fun normalizeIntegerInput(rawValue: String): String {
        return rawValue.filter { it.isDigit() }
    }

    fun normalizeWeightInput(rawValue: String): String {
        val normalized = rawValue.replace(',', '.')
        val builder = StringBuilder()
        var dotFound = false
        normalized.forEach { character ->
            when {
                character.isDigit() -> builder.append(character)
                character == '.' && !dotFound -> {
                    builder.append(character)
                    dotFound = true
                }
            }
        }
        return builder.toString()
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
    ) { uris ->
        selectedUris = uris
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            requestCurrentLocation(
                fusedLocationClient = fusedLocationClient,
                onStarted = {
                    isFetchingLocation = true
                    locationMessage = "Buscando tu ubicacion actual para validar cobertura..."
                },
                onSuccess = { latitude, longitude ->
                    selectedLatitude = latitude
                    selectedLongitude = longitude
                    scope.launch {
                        locationReference = resolveAddressFromCoordinates(context, latitude, longitude)
                        isFetchingLocation = false
                        locationMessage = buildLocationMessage(
                            latitude = latitude,
                            longitude = longitude,
                            reference = locationReference,
                            shareLocation = shareLocation,
                        )
                    }
                },
                onError = { message ->
                    isFetchingLocation = false
                    locationReference = null
                    locationMessage = message
                },
            )
        } else {
            shareLocation = false
            isFetchingLocation = false
            locationReference = null
            selectedLatitude = null
            selectedLongitude = null
            locationMessage = "Sin ubicacion no podemos validar si el pedido esta dentro de Pitalito."
        }
    }

    fun startLocationCapture() {
        if (hasLocationPermission(context)) {
            requestCurrentLocation(
                fusedLocationClient = fusedLocationClient,
                onStarted = {
                    isFetchingLocation = true
                    locationMessage = "Buscando tu ubicacion actual para validar cobertura..."
                },
                onSuccess = { latitude, longitude ->
                    selectedLatitude = latitude
                    selectedLongitude = longitude
                    scope.launch {
                        locationReference = resolveAddressFromCoordinates(context, latitude, longitude)
                        isFetchingLocation = false
                        locationMessage = buildLocationMessage(
                            latitude = latitude,
                            longitude = longitude,
                            reference = locationReference,
                            shareLocation = shareLocation,
                        )
                    }
                },
                onError = { message ->
                    isFetchingLocation = false
                    locationReference = null
                    locationMessage = message
                },
            )
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    fun resolveDestinationFromAddress() {
        val trimmedAddress = address.trim()
        if (trimmedAddress.isBlank()) {
            validationError = "Escribe la direccion destino para ubicarla en el mapa."
            return
        }

        scope.launch {
            isResolvingDestination = true
            val coordinates = resolveCoordinatesFromAddress(context, trimmedAddress)
            if (coordinates == null) {
                destinationLatitude = null
                destinationLongitude = null
                destinationReference = null
                validationError = "No pudimos ubicar ese destino. Revisa la direccion e intenta otra vez."
            } else {
                destinationLatitude = coordinates.first
                destinationLongitude = coordinates.second
                destinationReference = resolveAddressFromCoordinates(
                    context = context,
                    latitude = coordinates.first,
                    longitude = coordinates.second,
                )
                validationError = null
            }
            isResolvingDestination = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crear pedido") },
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
                    text = category.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    text = if (walletInfo.customerFreeOrdersRemaining > 0) {
                        "Tienes ${walletInfo.customerFreeOrdersRemaining} pedido(s) de bienvenida disponibles. Lo usado se cobrara en tu primera recarga."
                    } else {
                        "Tu saldo real es ${DemoRepository.currency(walletInfo.balance)}. Cada pedido descuenta ${DemoRepository.currency(walletInfo.customerOrderFee)}."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    text = "Pendiente por cobrar: ${DemoRepository.currency(walletInfo.totalDeferredCharges)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (
                category == OrderCategory.SHOPPING ||
                category == OrderCategory.RESTAURANTS ||
                category == OrderCategory.TRAMITES
            ) {
                item {
                    SectionCard(
                        title = when (category) {
                            OrderCategory.SHOPPING -> "Arma tu pedido"
                            OrderCategory.RESTAURANTS -> "Arma tu pedido del restaurante"
                            else -> "Selecciona el tramite"
                        },
                        subtitle = when (category) {
                            OrderCategory.SHOPPING -> "Escoge una subcategoria, busca productos en tiempo real y agrega las cantidades que necesites."
                            OrderCategory.RESTAURANTS -> "Escoge el restaurante, escribe el plato o bebida y agrega las cantidades que quieres pedir."
                            else -> "Escoge la entidad o tipo de gestion, filtra el servicio y agregalo con la cantidad necesaria."
                        },
                    ) {
                        if (availableSubcategories.isNotEmpty()) {
                            SubcategoryDropdown(
                                label = "Subcategoria",
                                selectedSubcategory = selectedSubcategory,
                                options = availableSubcategories,
                                onSelected = {
                                    selectedSubcategoryId = it.id
                                    catalogSearchQuery = ""
                                    validationError = null
                                },
                            )
                        }
                        OutlinedTextField(
                            value = catalogSearchQuery,
                            onValueChange = {
                                catalogSearchQuery = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (category == OrderCategory.SHOPPING) {
                                        "Buscar producto"
                                    } else if (category == OrderCategory.RESTAURANTS) {
                                        "Buscar plato o bebida"
                                    } else {
                                        "Buscar tramite o servicio"
                                    },
                                )
                            },
                            placeholder = {
                                Text(
                                    if (category == OrderCategory.SHOPPING) {
                                        "Escribe iniciales: arroz, leche, huevo..."
                                    } else if (category == OrderCategory.RESTAURANTS) {
                                        "Escribe el plato, combo, hamburguesa, bebida..."
                                    } else {
                                        "Escribe iniciales: cita, recibo, autenticacion..."
                                    },
                                )
                            },
                        )
                        if (filteredCatalogSuggestions.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (catalogSearchQuery.isBlank()) {
                                        "Sugerencias rapidas"
                                    } else {
                                        "Resultados sugeridos"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                filteredCatalogSuggestions.forEach { suggestion ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { addRequestedItem(suggestion) },
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = suggestion,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = "Agregar",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = if (catalogSearchQuery.isBlank()) {
                                    if (category == OrderCategory.RESTAURANTS) {
                                        "Puedes escribir el plato manualmente o elegir una sugerencia cuando parametrices ese restaurante."
                                    } else {
                                        "Empieza a escribir para filtrar el listado de la subcategoria."
                                    }
                                } else {
                                    "No encontramos coincidencias exactas. Puedes agregarlo manualmente con el mismo texto."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { addRequestedItem(catalogSearchQuery) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Agregar al pedido")
                            }
                        }
                        if (category == OrderCategory.TRAMITES) {
                            OutlinedTextField(
                                value = otherDetails,
                                onValueChange = {
                                    otherDetails = it
                                    validationError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Observaciones (opcional)") },
                                placeholder = { Text("Ej: Llevar fotocopia de la cedula o reclamar comprobante") },
                                minLines = 2,
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = if (selectedItems.isEmpty()) {
                            if (category == OrderCategory.SHOPPING) {
                                "Todavia no has agregado productos."
                            } else if (category == OrderCategory.RESTAURANTS) {
                                "Todavia no has agregado platos o bebidas."
                            } else {
                                "Todavia no has agregado tramites."
                            }
                        } else {
                            "${selectedItems.size} item(s) listos y ${selectedItems.sumOf { it.quantity }} unidad(es) en total."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(selectedItems, key = { "draft-item-${it.name}" }) { item ->
                    RequestedItemEditorRow(
                        item = item,
                        onDecrease = { updateRequestedItemQuantity(item.name, -1) },
                        onIncrease = { updateRequestedItemQuantity(item.name, 1) },
                        onRemove = { updateRequestedItemQuantity(item.name, -item.quantity) },
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            validationError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                if (category == OrderCategory.SHOPPING) {
                                    "Direccion de entrega"
                                } else if (category == OrderCategory.RESTAURANTS) {
                                    "Direccion de entrega del pedido"
                                } else {
                                    "Direccion de referencia o entrega"
                                },
                            )
                        },
                        minLines = 2,
                    )
                }
            }
            if (requestType == OrderRequestType.PICKUP_AND_DELIVER) {
                if (category == OrderCategory.ENVIOS) {
                    item {
                        SectionCard(
                            title = "Datos del envio",
                            subtitle = "Define el tipo de envio y el peso para calcular la tarifa automatica.",
                        ) {
                            if (availableSubcategories.isNotEmpty()) {
                                SubcategoryDropdown(
                                    label = "Tipo de envio",
                                    selectedSubcategory = selectedSubcategory,
                                    options = availableSubcategories,
                                    onSelected = {
                                        selectedSubcategoryId = it.id
                                        validationError = null
                                    },
                                )
                            }
                            OutlinedTextField(
                                value = packageWeightText,
                                onValueChange = {
                                    packageWeightText = normalizeWeightInput(it)
                                    validationError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Peso aproximado (kg)") },
                                placeholder = { Text("Ej: 2 o 3.5") },
                            )
                            Text(
                                text = shippingFeeRuleSummary(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    SectionCard(
                        title = pickupSectionTitle(category),
                        subtitle = pickupSectionSubtitle(category),
                    ) {
                        OutlinedTextField(
                            value = pickupAddress,
                            onValueChange = {
                                pickupAddress = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(pickupAddressLabel(category)) },
                            minLines = 2,
                        )
                        OutlinedTextField(
                            value = pickupContactName,
                            onValueChange = {
                                pickupContactName = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(pickupContactNameLabel(category)) },
                        )
                        OutlinedTextField(
                            value = pickupContactPhone,
                            onValueChange = {
                                pickupContactPhone = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(pickupContactPhoneLabel(category)) },
                        )
                        OutlinedTextField(
                            value = pickupPaymentAmountText,
                            onValueChange = {
                                pickupPaymentAmountText = normalizeIntegerInput(it)
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(pickupPaymentLabel(category)) },
                            placeholder = { Text("Ej: 25000") },
                        )
                    }
                }
                item {
                    SectionCard(
                        title = dropoffSectionTitle(category),
                        subtitle = dropoffSectionSubtitle(category),
                    ) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(dropoffAddressLabel(category)) },
                            minLines = 2,
                        )
                        OutlinedTextField(
                            value = dropoffContactName,
                            onValueChange = {
                                dropoffContactName = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(dropoffContactNameLabel(category)) },
                        )
                        OutlinedTextField(
                            value = dropoffContactPhone,
                            onValueChange = {
                                dropoffContactPhone = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(dropoffContactPhoneLabel(category)) },
                        )
                    }
                }
            }
            if (category == OrderCategory.MOTOTAXI) {
                item {
                    SectionCard(
                        title = "Ruta del mototaxi",
                        subtitle = "Tu ubicacion actual sera el punto de partida. Escribe el destino y fijalo en el mapa.",
                    ) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                destinationLatitude = null
                                destinationLongitude = null
                                destinationReference = null
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Destino") },
                            placeholder = { Text("Ej: Terminal de transporte, Calamo o calle exacta") },
                            minLines = 2,
                        )
                        OutlinedTextField(
                            value = otherDetails,
                            onValueChange = {
                                otherDetails = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Notas para el conductor (opcional)") },
                            placeholder = { Text("Ej: Estoy en la esquina de la tienda o voy con maleta") },
                            minLines = 2,
                        )
                        OutlinedButton(
                            onClick = { resolveDestinationFromAddress() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (isResolvingDestination) {
                                    "Ubicando destino..."
                                } else if (destinationLatitude != null && destinationLongitude != null) {
                                    "Actualizar destino en el mapa"
                                } else {
                                    "Fijar destino"
                                },
                            )
                        }
                        if (destinationLatitude != null && destinationLongitude != null) {
                            Text(
                                text = destinationReference ?: "${formatCoordinate(destinationLatitude!!)}, ${formatCoordinate(destinationLongitude!!)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (category == OrderCategory.TRAMITES) {
                item {
                    Text(
                        text = "Puedes agregar varios tramites dentro de la misma entidad o tipo de gestion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Text(
                    text = "Por seguridad, antes de enviar validamos que tu pedido si quede dentro del area de cobertura de $PITALITO_SERVICE_AREA_CITY.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    text = "Domicilio estimado inicial: ${DemoRepository.currency(estimatedDeliveryFee)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                Text(
                    text = when (requestType) {
                        OrderRequestType.SHOPPING -> deliveryFeeRuleSummary()
                        OrderRequestType.PICKUP_AND_DELIVER -> if (category == OrderCategory.ENVIOS) {
                            shippingFeeRuleSummary()
                        } else {
                            "${category.shortLabel}: base de domicilios $${URBAN_MINIMUM_DELIVERY_FEE} dentro del casco urbano."
                        }
                        OrderRequestType.OTHER -> "${category.shortLabel}: base de domicilios $${URBAN_MINIMUM_DELIVERY_FEE} dentro del casco urbano."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Metodo de pago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method.name,
                                onClick = { paymentMethod = method.name },
                                label = { Text(method.label) },
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (category == OrderCategory.MOTOTAXI) {
                                "Punto de partida del servicio"
                            } else {
                                "Compartir este punto con el domiciliario"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (category == OrderCategory.MOTOTAXI) {
                                "Tu ubicacion actual se usara como origen del trayecto y se mostrara al domiciliario para que vea la ruta antes de aceptar."
                            } else {
                                "Tu ubicacion actual siempre se usa para validar cobertura. Si apagas esta opcion, confirmamos que estas en Pitalito pero no mostramos el punto exacto al domiciliario."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = shareLocation,
                        enabled = category != OrderCategory.MOTOTAXI,
                        onCheckedChange = {
                            shareLocation = it
                            validationError = null

                            if (selectedLatitude != null && selectedLongitude != null) {
                                locationMessage = buildLocationMessage(
                                    latitude = selectedLatitude!!,
                                    longitude = selectedLongitude!!,
                                    reference = locationReference,
                                    shareLocation = it,
                                )
                            } else if (it) {
                                startLocationCapture()
                            } else {
                                locationMessage = "Confirma tu ubicacion actual para validar que el pedido si esta dentro de Pitalito."
                            }
                        },
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ElevatedAssistChip(
                        onClick = {
                            imageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        label = { Text("Agregar fotos de referencia") },
                        leadingIcon = {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ElevatedAssistChip(
                            onClick = { startLocationCapture() },
                            label = {
                                Text(
                                    if (isFetchingLocation) {
                                        "Tomando ubicacion..."
                                    } else if (selectedLatitude != null && selectedLongitude != null) {
                                        if (category == OrderCategory.MOTOTAXI) {
                                            "Origen confirmado"
                                        } else {
                                            "Ubicacion confirmada"
                                        }
                                    } else {
                                        if (category == OrderCategory.MOTOTAXI) {
                                            "Confirmar origen"
                                        } else {
                                            "Confirmar mi ubicacion"
                                        }
                                    },
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.MyLocation, contentDescription = null)
                            },
                        )
                        if (selectedLatitude != null && selectedLongitude != null) {
                            OutlinedButton(onClick = { startLocationCapture() }) {
                                Text("Actualizar")
                            }
                        }
                    }
                    Text(
                        text = locationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    coverageCheck?.let { check ->
                        Text(
                            text = if (check.isInside) {
                                "Estas dentro de $PITALITO_SERVICE_AREA_NAME. Distancia aproximada al centro: ${formatDistanceMeters(check.distanceMeters)}."
                            } else {
                                "Tu punto actual esta fuera del area de cobertura de $PITALITO_SERVICE_AREA_NAME. Distancia aproximada al centro: ${formatDistanceMeters(check.distanceMeters)}."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (check.isInside) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    if (!locationReference.isNullOrBlank()) {
                        Text(
                            text = if (category == OrderCategory.MOTOTAXI) {
                                "Origen resuelto: $locationReference"
                            } else {
                                "Punto resuelto: $locationReference"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (category == OrderCategory.MOTOTAXI && destinationCoverageCheck != null) {
                        Text(
                            text = if (destinationCoverageCheck.isInside) {
                                "Destino dentro de $PITALITO_SERVICE_AREA_NAME."
                            } else {
                                "El destino esta fuera del area de cobertura de $PITALITO_SERVICE_AREA_NAME."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (destinationCoverageCheck.isInside) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    if (category == OrderCategory.MOTOTAXI && !destinationReference.isNullOrBlank()) {
                        Text(
                            text = "Destino resuelto: $destinationReference",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Cobertura inicial: ${PITALITO_SERVICE_AREA_RADIUS_METERS / 1000} km alrededor del centro urbano de Pitalito.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (selectedUris.isNotEmpty()) {
                items(selectedUris, key = { it.toString() }) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Foto de referencia",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                }
            }
            if (validationError != null) {
                item {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (!serverErrorMessage.isNullOrBlank()) {
                item {
                    Text(
                        text = serverErrorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                Button(
                    enabled = canCreateOrder,
                    onClick = {
                        val pickupPaymentAmount = pickupPaymentAmountText.toIntOrNull() ?: 0
                        val effectiveShareLocation = if (category == OrderCategory.MOTOTAXI) true else shareLocation
                        val trimmedDetails = otherDetails.trim().ifBlank { null }

                        when {
                            selectedLatitude == null || selectedLongitude == null -> {
                                validationError = "Necesitamos validar tu ubicacion actual para confirmar que la solicitud si esta dentro de Pitalito."
                            }
                            coverageCheck != null && !coverageCheck.isInside -> {
                                validationError = "Tu ubicacion actual aparece fuera del area de cobertura de Pitalito. Por ahora no podemos recibir esta solicitud."
                            }
                            (category == OrderCategory.SHOPPING || category == OrderCategory.RESTAURANTS) &&
                                (selectedSubcategory == null || selectedItems.isEmpty() || address.isBlank()) -> {
                                validationError = if (category == OrderCategory.RESTAURANTS) {
                                    "Para restaurantes debes escoger un restaurante, agregar platos o bebidas y dejar la direccion de entrega."
                                } else {
                                    "Para compras debes escoger una subcategoria, agregar productos y dejar la direccion de entrega."
                                }
                            }
                            category == OrderCategory.TRAMITES &&
                                (selectedSubcategory == null || selectedItems.isEmpty() || address.isBlank()) -> {
                                validationError = "Para tramites debes escoger una subcategoria, agregar las gestiones y dejar una direccion de referencia."
                            }
                            requestType == OrderRequestType.PICKUP_AND_DELIVER &&
                                (
                                    pickupAddress.isBlank() ||
                                        pickupContactName.isBlank() ||
                                        pickupContactPhone.isBlank() ||
                                        address.isBlank() ||
                                        dropoffContactName.isBlank() ||
                                        dropoffContactPhone.isBlank()
                                    ) -> {
                                validationError = "Completa los datos de recogida y entrega para esta solicitud."
                            }
                            category == OrderCategory.ENVIOS &&
                                (selectedSubcategory == null || packageWeightKg == null || packageWeightKg <= 0.0) -> {
                                validationError = "En envios debes escoger el tipo de paquete y registrar el peso aproximado."
                            }
                            category == OrderCategory.MOTOTAXI &&
                                (address.isBlank() || destinationLatitude == null || destinationLongitude == null) -> {
                                validationError = "Para mototaxi debes escribir y fijar el destino en el mapa."
                            }
                            category == OrderCategory.MOTOTAXI &&
                                destinationCoverageCheck != null &&
                                !destinationCoverageCheck.isInside -> {
                                validationError = "El destino del mototaxi aparece fuera del area de cobertura de Pitalito."
                            }
                            requestType == OrderRequestType.OTHER &&
                                category != OrderCategory.TRAMITES &&
                                category != OrderCategory.MOTOTAXI &&
                                (otherDetails.isBlank() || address.isBlank()) -> {
                                validationError = "Describe la vuelta y deja una direccion de referencia o entrega."
                            }
                            else -> {
                                onSubmit(
                                    CreateOrderDraft(
                                        category = category,
                                        serviceSubcategory = selectedSubcategory?.label,
                                        requestType = requestType,
                                        items = selectedItems,
                                        details = trimmedDetails,
                                        address = address.trim(),
                                        shareLocation = effectiveShareLocation,
                                        addressReference = locationReference,
                                        latitude = selectedLatitude,
                                        longitude = selectedLongitude,
                                        destinationLatitude = destinationLatitude,
                                        destinationLongitude = destinationLongitude,
                                        destinationReference = destinationReference,
                                        paymentMethod = PaymentMethod.valueOf(paymentMethod),
                                        referenceImages = selectedUris.map(Uri::toString),
                                        pickupAddress = pickupAddress.trim().ifBlank { null },
                                        pickupContactName = pickupContactName.trim().ifBlank { null },
                                        pickupContactPhone = pickupContactPhone.trim().ifBlank { null },
                                        dropoffContactName = dropoffContactName.trim().ifBlank { null },
                                        dropoffContactPhone = dropoffContactPhone.trim().ifBlank { null },
                                        pickupPaymentAmount = pickupPaymentAmount,
                                        packageWeightKg = packageWeightKg,
                                    ),
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text(
                        submitLabel(category),
                    )
                }
            }
            if (!canCreateOrder) {
                item {
                    Text(
                        text = "Ya se acabaron tus pedidos iniciales y no tienes saldo suficiente para crear otro pedido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                item {
                    TextButton(onClick = onTopUp) {
                        Text("Ir a recargar saldo")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun SubcategoryDropdown(
    label: String,
    selectedSubcategory: CatalogSubcategory?,
    options: List<CatalogSubcategory>,
    onSelected: (CatalogSubcategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedSubcategory?.label ?: "Selecciona una opcion",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RequestedItemEditorRow(
    item: RequestedOrderItem,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onRemove: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDecrease) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Quitar uno")
                    }
                    Text(
                        text = item.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onIncrease) {
                        Icon(Icons.Outlined.Add, contentDescription = "Agregar uno")
                    }
                }
                TextButton(onClick = onRemove) {
                    Text("Quitar")
                }
            }
        }
    }
}

private fun buildLocationMessage(
    latitude: Double,
    longitude: Double,
    reference: String?,
    shareLocation: Boolean,
): String {
    val baseMessage = if (reference.isNullOrBlank()) {
        "Ubicacion confirmada: ${formatCoordinate(latitude)}, ${formatCoordinate(longitude)}"
    } else {
        "Ubicacion confirmada: ${formatCoordinate(latitude)}, ${formatCoordinate(longitude)}"
    }

    return if (shareLocation) {
        "$baseMessage. Tambien compartiremos este punto con el domiciliario."
    } else {
        "$baseMessage. Solo lo usaremos para validar cobertura, sin mostrar el punto exacto al domiciliario."
    }
}

private fun pickupSectionTitle(category: OrderCategory): String {
    return when (category) {
        OrderCategory.DOMICILIOS -> "Recoger"
        OrderCategory.ENVIOS -> "Recoger envio"
        else -> "Punto de salida"
    }
}

private fun pickupSectionSubtitle(category: OrderCategory): String {
    return when (category) {
        OrderCategory.DOMICILIOS -> "Completa la informacion del lugar donde se recoge el encargo."
        OrderCategory.ENVIOS -> "Indica donde recogen el paquete o articulo que se va a enviar."
        else -> "Completa el punto inicial de esta solicitud."
    }
}

private fun pickupAddressLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.ENVIOS -> "Direccion de recogida del envio"
        else -> "Direccion de recogida"
    }
}

private fun pickupContactNameLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.ENVIOS -> "Nombre de quien entrega el envio"
        else -> "Nombre de quien entrega"
    }
}

private fun pickupContactPhoneLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.ENVIOS -> "Telefono de quien entrega el envio"
        else -> "Telefono de quien entrega"
    }
}

private fun pickupPaymentLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.ENVIOS -> "Valor a cancelar si el envio requiere pago"
        else -> "Valor a cancelar si toca pagar algo"
    }
}

private fun dropoffSectionTitle(category: OrderCategory): String {
    return when (category) {
        OrderCategory.ENVIOS -> "Entregar envio"
        else -> "Entregar"
    }
}

private fun dropoffSectionSubtitle(category: OrderCategory): String {
    return when (category) {
        OrderCategory.ENVIOS -> "Aqui va el destino final del paquete o articulo."
        else -> "Aqui va el destino final de la recogida."
    }
}

private fun dropoffAddressLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.ENVIOS -> "Direccion de entrega del envio"
        else -> "Direccion de entrega"
    }
}

private fun dropoffContactNameLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.ENVIOS -> "Nombre de quien recibe el envio"
        else -> "Nombre de quien recibe"
    }
}

private fun dropoffContactPhoneLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.ENVIOS -> "Telefono de quien recibe el envio"
        else -> "Telefono de quien recibe"
    }
}

private fun otherSectionTitle(category: OrderCategory): String {
    return when (category) {
        OrderCategory.TRAMITES -> "Tramite o diligencia"
        OrderCategory.MOTOTAXI -> "Solicitud de mototaxi"
        else -> "Otra vuelta"
    }
}

private fun otherSectionSubtitle(category: OrderCategory): String {
    return when (category) {
        OrderCategory.TRAMITES -> "Describe el tramite, pago de servicio, recibo u otra gestion que necesitas."
        OrderCategory.MOTOTAXI -> "Cuéntanos origen, destino y cualquier dato clave del traslado para coordinarlo mejor."
        else -> "Describe la gestion que necesitas."
    }
}

private fun otherDetailsLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.TRAMITES -> "Describe el tramite"
        OrderCategory.MOTOTAXI -> "Describe la ruta o solicitud"
        else -> "Describe la vuelta"
    }
}

private fun otherDetailsPlaceholder(category: OrderCategory): String {
    return when (category) {
        OrderCategory.TRAMITES -> "Ej: Pagar recibo de energia y traer el comprobante"
        OrderCategory.MOTOTAXI -> "Ej: Recoger pasajero en el parque principal y llevarlo a Cálamo"
        else -> "Ej: Ayudame con esta diligencia"
    }
}

private fun otherAddressLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.MOTOTAXI -> "Direccion de referencia o destino"
        else -> "Direccion de referencia o entrega"
    }
}

private fun submitLabel(category: OrderCategory): String {
    return when (category) {
        OrderCategory.SHOPPING -> "Enviar compra"
        OrderCategory.RESTAURANTS -> "Enviar pedido"
        OrderCategory.DOMICILIOS -> "Solicitar domicilio"
        OrderCategory.TRAMITES -> "Enviar tramite"
        OrderCategory.MOTOTAXI -> "Solicitar mototaxi"
        OrderCategory.ENVIOS -> "Solicitar envio"
    }
}
