package com.example.ra_quadrado.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent // Import MotionEvent
import android.view.View // Import View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ra_quadrado.R
import com.example.ra_quadrado.ar.ARRenderer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

// Updated data class to match the Flask API's /nearest_curiosity response
@Serializable
data class CuriosityResponse(
    val nearest_place: String,
    val curiosity: String,
    val image_urls: List<String>? = null, // NEW: Optional list of image URLs from the API
    val input_coordinates: Map<String, Double>? = null
)

@Composable
fun MuseumInfoCard(
    modifier: Modifier = Modifier,
    nearestPlace: String,
    curiosityText: String,
    isLoading: Boolean,
    error: String?
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Carregando curiosidade...", style = MaterialTheme.typography.bodyMedium)
                } else if (error != null) {
                    Text("Erro ao carregar: $error", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                } else {
                    Column {
                        Text(
                            text = "Você está em: ${nearestPlace.ifEmpty { "" }}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = curiosityText.ifEmpty { "." },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ARScreen() {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var showMuseumInfo by remember { mutableStateOf(false) }
    var nearestPlaceName by remember { mutableStateOf("") }
    var curiosityText by remember { mutableStateOf("") }
    var fetchedImageUrls by remember { mutableStateOf<List<String>?>(null) } // NEW: State for fetched image_urls (List<String>)
    var isLoadingCuriosity by remember { mutableStateOf(false) }
    var curiosityError by remember { mutableStateOf<String?>(null) }

    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    val httpClient = remember { OkHttpClient() }
    val json = remember { Json { ignoreUnknownKeys = true } }

    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    isFetchingLocation = false
                    Log.d("ARScreen", "Location update received: Lat=${currentLatitude}, Lon=${currentLongitude}")
                    fusedLocationClient.removeLocationUpdates(this)
                } ?: run {
                    Log.w("ARScreen", "Location update result is null.")
                }
            }
        }
    }

    val requestLocationUpdates: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            isFetchingLocation = true
            locationError = null

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(2000)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            Log.d("ARScreen", "Requesting location updates...")
        } else {
            locationError = "Permissão de localização não concedida."
            Log.e("ARScreen", "Location permission not granted when trying to request updates.")
        }
    }

    val getLocation: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        isFetchingLocation = false
                        Log.d("ARScreen", "Last known location: Lat=${currentLatitude}, Lon=${currentLongitude}")
                    } else {
                        Log.w("ARScreen", "Last known location is null. Requesting new updates.")
                        requestLocationUpdates()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ARScreen", "Error getting last known location: ${e.message}. Requesting new updates.")
                    locationError = "Erro ao obter última localização: ${e.message}"
                    requestLocationUpdates()
                }
        } else {
            locationError = "Permissão de localização não concedida."
            Log.e("ARScreen", "Location permission not granted when trying to get location.")
        }
    }


    LaunchedEffect(showMuseumInfo) {
        if (showMuseumInfo) {
            if (!locationPermissionState.status.isGranted) {
                locationPermissionState.launchPermissionRequest()
            } else {
                getLocation()
            }
        } else {
            curiosityText = ""
            nearestPlaceName = ""
            fetchedImageUrls = null // Reset image_urls
            curiosityError = null
            isLoadingCuriosity = false
            currentLatitude = null
            currentLongitude = null
            isFetchingLocation = false
            locationError = null
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    LaunchedEffect(currentLatitude, currentLongitude, showMuseumInfo) {
        if (showMuseumInfo && currentLatitude != null && currentLongitude != null && !isLoadingCuriosity) {
            isLoadingCuriosity = true
            curiosityError = null
            val baseUrl = "https://c82a7042092f.ngrok-free.app" // Make sure this is your current ngrok URL
            val url = "$baseUrl/nearest_curiosity".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("lat", currentLatitude.toString())
                ?.addQueryParameter("lon", currentLongitude.toString())
                ?.build()

            if (url == null) {
                curiosityError = "Falha ao construir URL da API."
                isLoadingCuriosity = false
                Log.e("ARScreen", "Failed to build URL for API call.")
                return@LaunchedEffect
            }

            val request = Request.Builder().url(url).build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    curiosityError = e.message
                    isLoadingCuriosity = false
                    Log.e("ARScreen", "API call failed: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!response.isSuccessful) {
                                throw IOException("HTTP ${response.code}: ${response.message}. Body: ${response.body?.string()}")
                            }
                            val body = response.body?.string()
                            if (body != null) {
                                val parsed = json.decodeFromString(CuriosityResponse.serializer(), body)
                                nearestPlaceName = parsed.nearest_place
                                curiosityText = parsed.curiosity
                                fetchedImageUrls = parsed.image_urls // Capture the list of image URLs
                                Log.d("ARScreen", "API Response: Nearest Place=${nearestPlaceName}, Curiosity=${curiosityText}, Image URLs=${fetchedImageUrls}")
                            } else {
                                curiosityError = "Resposta da API vazia."
                                Log.e("ARScreen", "Empty API response body.")
                            }
                        } catch (ex: Exception) {
                            curiosityError = ex.message
                            Log.e("ARScreen", "Error parsing JSON or API response: ${ex.message}")
                        } finally {
                            isLoadingCuriosity = false
                        }
                    }
                }
            })
        }
    }

    val arSession = remember {
        Session(context).apply {
            val config = Config(this)
            val db = try {
                val stream = context.assets.open("imgs2.imgdb")
                AugmentedImageDatabase.deserialize(this, stream)
            } catch (_: IOException) {
                null
            }
            if (db != null) config.augmentedImageDatabase = db
            configure(config)
        }
    }

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        }
    }

    val arRenderer = remember {
        ARRenderer(arSession, context, glSurfaceView) { tracked ->
            showMuseumInfo = tracked
        }
    }

    DisposableEffect(glSurfaceView, arRenderer) {
        glSurfaceView.setRenderer(arRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    arRenderer.onScreenTapped()
                    Log.d("ARScreen", "Screen tapped! Requesting new random image.")
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    LaunchedEffect(nearestPlaceName) {
        arRenderer.updateTitleText(nearestPlaceName)
    }

    LaunchedEffect(curiosityText) {
        arRenderer.updateDescriptionText(curiosityText)
    }

    // NEW: LaunchedEffect to update the image in ARRenderer when fetchedImageUrls changes
    LaunchedEffect(fetchedImageUrls) {
        fetchedImageUrls?.firstOrNull()?.let { imageUrl -> // Take the first URL from the list
            arRenderer.updateAugmentedImage(imageUrl) // Call the new method in ARRenderer
        } ?: run {
            // Optionally, load a default placeholder if no image URLs are found
            arRenderer.updateAugmentedImage(null) // Pass null to indicate no specific image
        }
    }

    DisposableEffect(arSession) {
        onDispose { arSession.close() }
    }

    val allPermissionsGranted = cameraPermissionState.status.isGranted && locationPermissionState.status.isGranted

    if (allPermissionsGranted) {
        Box(Modifier.fillMaxSize()) {
            ARCoreView(
                modifier = Modifier.fillMaxSize(),
                arSession = arSession,
                arRenderer = arRenderer,
                glSurfaceView = glSurfaceView
            )

            AnimatedVisibility(
                visible = showMuseumInfo,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MuseumInfoCard(
                    nearestPlace = nearestPlaceName,
                    curiosityText = curiosityText,
                    isLoading = isLoadingCuriosity || isFetchingLocation,
                    error = curiosityError ?: locationError
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val cameraText = if (cameraPermissionState.status.shouldShowRationale) {
                    "A câmera é importante para AR."
                } else {
                    "Permita acesso à câmera para AR."
                }
                Text(cameraText, modifier = Modifier.padding(bottom = 8.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Permitir Câmera")
                }
                Spacer(modifier = Modifier.height(16.dp))

                val locationText = if (locationPermissionState.status.shouldShowRationale) {
                    "A localização é necessária para encontrar locais próximos."
                } else {
                    "Permita acesso à localização para obter curiosidades."
                }
                Text(locationText, modifier = Modifier.padding(bottom = 8.dp))
                Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                    Text("Permitir Localização")
                }
            }
        }
    }
}

@Composable
fun ARCoreView(
    modifier: Modifier = Modifier,
    arSession: Session,
    arRenderer: ARRenderer,
    glSurfaceView: GLSurfaceView
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { glSurfaceView }
    )

    DisposableEffect(lifecycleOwner, arSession, glSurfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        glSurfaceView.onResume()
                        arSession.resume()
                    } catch (e: Exception) {
                        Log.e("ARCoreView", "Failed to resume AR session", e)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    glSurfaceView.onPause()
                    arSession.pause()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}