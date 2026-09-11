package com.appdomicilios.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale

const val PITALITO_SERVICE_AREA_NAME = "Pitalito urbano"
const val PITALITO_SERVICE_AREA_CITY = "Pitalito, Huila"
const val PITALITO_SERVICE_AREA_CENTER_LATITUDE = 1.8537
const val PITALITO_SERVICE_AREA_CENTER_LONGITUDE = -76.0507
const val PITALITO_SERVICE_AREA_RADIUS_METERS = 10_000

data class ServiceAreaCheck(
    val distanceMeters: Int,
    val isInside: Boolean,
)

fun hasLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    return fineGranted || coarseGranted
}

@SuppressLint("MissingPermission")
fun requestCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onStarted: () -> Unit,
    onSuccess: (Double, Double) -> Unit,
    onError: (String) -> Unit,
) {
    onStarted()

    val request = CurrentLocationRequest.Builder()
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setDurationMillis(10_000)
        .build()

    fusedLocationClient
        .getCurrentLocation(request, null)
        .addOnSuccessListener { location ->
            if (location != null) {
                onSuccess(location.latitude, location.longitude)
            } else {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            onSuccess(lastLocation.latitude, lastLocation.longitude)
                        } else {
                            onError("No pudimos obtener tu ubicacion. Puedes intentar otra vez.")
                        }
                    }
                    .addOnFailureListener {
                        onError("No pudimos obtener tu ubicacion. Revisa GPS y vuelve a intentar.")
                    }
            }
        }
        .addOnFailureListener {
            onError("No pudimos obtener tu ubicacion. Revisa GPS y vuelve a intentar.")
        }
}

suspend fun resolveAddressFromCoordinates(
    context: Context,
    latitude: Double,
    longitude: Double,
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val geocoder = Geocoder(context, Locale("es", "CO"))
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        val address = addresses?.firstOrNull() ?: return@runCatching null

        listOfNotNull(
            address.thoroughfare?.takeIf { it.isNotBlank() },
            address.subLocality?.takeIf { it.isNotBlank() },
            address.locality?.takeIf { it.isNotBlank() },
        ).joinToString(", ").ifBlank { address.getAddressLine(0) }
    }.getOrNull()
}

suspend fun resolveCoordinatesFromAddress(
    context: Context,
    query: String,
): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
        return@withContext null
    }

    runCatching {
        val geocoder = Geocoder(context, Locale("es", "CO"))
        val addresses = geocoder.getFromLocationName(normalizedQuery, 1)
        val address = addresses?.firstOrNull() ?: return@runCatching null
        Pair(address.latitude, address.longitude)
    }.getOrNull()
}

fun formatCoordinate(value: Double): String {
    return String.format(Locale.US, "%.5f", value)
}

fun checkPitalitoCoverage(
    latitude: Double,
    longitude: Double,
): ServiceAreaCheck {
    val distance = haversineMeters(
        latitudeOne = latitude,
        longitudeOne = longitude,
        latitudeTwo = PITALITO_SERVICE_AREA_CENTER_LATITUDE,
        longitudeTwo = PITALITO_SERVICE_AREA_CENTER_LONGITUDE,
    )

    return ServiceAreaCheck(
        distanceMeters = distance.roundToInt(),
        isInside = distance <= PITALITO_SERVICE_AREA_RADIUS_METERS,
    )
}

fun formatDistanceMeters(distanceMeters: Int): String {
    return if (distanceMeters >= 1000) {
        String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
    } else {
        "$distanceMeters m"
    }
}

private fun haversineMeters(
    latitudeOne: Double,
    longitudeOne: Double,
    latitudeTwo: Double,
    longitudeTwo: Double,
): Double {
    val earthRadiusMeters = 6_371_000.0
    val latitudeDistance = Math.toRadians(latitudeTwo - latitudeOne)
    val longitudeDistance = Math.toRadians(longitudeTwo - longitudeOne)
    val startLatitude = Math.toRadians(latitudeOne)
    val endLatitude = Math.toRadians(latitudeTwo)

    val a = sin(latitudeDistance / 2) * sin(latitudeDistance / 2) +
        cos(startLatitude) * cos(endLatitude) *
        sin(longitudeDistance / 2) * sin(longitudeDistance / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadiusMeters * c
}

fun staticMapUrl(
    latitude: Double,
    longitude: Double,
    width: Int = 600,
    height: Int = 260,
): String {
    return "https://staticmap.openstreetmap.de/staticmap.php" +
        "?center=$latitude,$longitude" +
        "&zoom=15" +
        "&size=${width}x$height" +
        "&markers=$latitude,$longitude,orange-pushpin"
}

fun openStreetMapEmbedUrl(
    latitude: Double,
    longitude: Double,
): String {
    val latDelta = 0.0035
    val lonDelta = 0.0045
    val left = longitude - lonDelta
    val right = longitude + lonDelta
    val bottom = latitude - latDelta
    val top = latitude + latDelta

    return "https://www.openstreetmap.org/export/embed.html" +
        "?bbox=${formatCoordinate(left)}%2C${formatCoordinate(bottom)}%2C${formatCoordinate(right)}%2C${formatCoordinate(top)}" +
        "&layer=mapnik" +
        "&marker=${formatCoordinate(latitude)}%2C${formatCoordinate(longitude)}"
}
