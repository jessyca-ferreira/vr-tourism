package com.example.ra_quadrado.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ra_quadrado.ar.ARRenderer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.ar.core.Session
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit // NEW: Import for TimeUnit

// This data class matches your existing /nearest_curiosity endpoint
@Serializable
data class CuriosityResponse(
    val nearest_place: String,
    val curiosity_text: String,
    val image_urls: List<String>? = null,
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
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Carregando curiosidade...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (error != null) {
                Text("Erro ao carregar: $error", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            } else {
                Column {
                    Text(
                        text = "Você está perto de: $nearestPlace",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = curiosityText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ARScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // --- State Variables ---
    var showMuseumInfo by remember { mutableStateOf(false) }
    var nearestPlaceName by remember { mutableStateOf("") }
    var curiosityText by remember { mutableStateOf("") }
    var fetchedImageUrls by remember { mutableStateOf<List<String>?>(null) }
    var isLoadingCuriosity by remember { mutableStateOf(false) }
    var curiosityError by remember { mutableStateOf<String?>(null) }
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // --- Audio State ---
    val mediaPlayer = remember { MediaPlayer() }
    var localAudioPath by remember { mutableStateOf<String?>(null) }

    // --- HTTP and Location Clients ---
    // This client is for fast, normal requests
    val httpClient = remember { OkHttpClient() }
    // NEW: This second client has a long timeout specifically for the slow audio generation
    val longTimeoutHttpClient = remember {
        OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val json = remember { Json { ignoreUnknownKeys = true } }
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // --- Audio Control Functions to play from local file ---
    val playAudioFromPath: (String) -> Unit = { path ->
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()
            mediaPlayer.setDataSource(path) // Set data source to the local file path
            mediaPlayer.prepare() // Use synchronous prepare for local files, it's fast.
            mediaPlayer.start()
            Log.d("ARScreen", "MediaPlayer started playing from local path: $path")
        } catch (e: Exception) {
            Log.e("ARScreen", "Failed to play from local path", e)
            curiosityError = "Erro ao tocar o áudio."
        }
    }

    val stopAndResetAudio: () -> Unit = {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()
        // Clean up the old audio file when we're done
        localAudioPath?.let {
            try {
                File(it).delete()
                Log.d("ARScreen", "Deleted local audio cache file: $it")
            } catch (e: Exception) {
                Log.e("ARScreen", "Failed to delete cache file", e)
            }
        }
        localAudioPath = null
        Log.d("ARScreen", "MediaPlayer stopped and local audio cache cleared.")
    }

    // --- Refresh and AR Renderer Setup ---
    val triggerRefresh: () -> Unit = {
        refreshTrigger++
        stopAndResetAudio()
        nearestPlaceName = ""
        curiosityText = ""
        fetchedImageUrls = null
        currentLatitude = null
        currentLongitude = null
        curiosityError = null
        locationError = null
        Log.d("ARScreen", "Refresh triggered - cleared data and stopped audio.")
    }

    val arSession = remember { Session(context) }

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        }
    }

    val arRenderer = remember {
        ARRenderer(
            session = arSession,
            context = context,
            glSurfaceView = glSurfaceView,
            onAnchorPlaced = {
                // Play audio from the local path once the anchor is placed
                localAudioPath?.let { path ->
                    Log.d("ARScreen", "Anchor placed, attempting to play audio from local path.")
                    playAudioFromPath(path)
                }
            },
            onRefreshNeeded = { triggerRefresh() }
        )
    }

    // --- Location and API Logic ---
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    isFetchingLocation = false
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }
    }

    val requestLocationUpdates: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isFetchingLocation = true
            locationError = null
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            locationError = "Permissão de localização não concedida."
        }
    }

    val getLocation: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        isFetchingLocation = false
                    } else {
                        requestLocationUpdates()
                    }
                }
                .addOnFailureListener { requestLocationUpdates() }
        } else {
            locationError = "Permissão de localização não concedida."
        }
    }

    LaunchedEffect(showMuseumInfo, refreshTrigger) {
        if (showMuseumInfo) {
            if (locationPermissionState.status.isGranted) {
                getLocation()
            }
        }
    }

    // --- API call and audio download logic ---
    LaunchedEffect(currentLatitude, currentLongitude, showMuseumInfo, refreshTrigger) {
        if (showMuseumInfo && currentLatitude != null && currentLongitude != null && !isLoadingCuriosity) {
            isLoadingCuriosity = true
            curiosityError = null

            // IMPORTANT: Replace this with your active ngrok URL every time you restart it
            val baseUrl = "https://192ebbf9bc4c.ngrok-free.app"

            val curiosityUrl = "$baseUrl/nearest_curiosity".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("lat", currentLatitude.toString())
                ?.addQueryParameter("lon", currentLongitude.toString())
                ?.build()

            if (curiosityUrl == null) {
                curiosityError = "Falha ao construir URL da API."
                isLoadingCuriosity = false
                return@LaunchedEffect
            }
            val request = Request.Builder().url(curiosityUrl).build()

            // --- Main API Call for Text/Image ---
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    curiosityError = e.message
                    isLoadingCuriosity = false
                }

                override fun onResponse(call: Call, response: Response) {
                    // Use a coroutine to avoid blocking the network thread and to update UI
                    coroutineScope.launch {
                        try {
                            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                            val body = response.body?.string()
                            if (body != null) {
                                val parsed = json.decodeFromString(CuriosityResponse.serializer(), body)

                                // Update UI State on the Main thread
                                withContext(Dispatchers.Main) {
                                    nearestPlaceName = parsed.nearest_place
                                    curiosityText = parsed.curiosity_text
                                    fetchedImageUrls = parsed.image_urls
                                }

                                // Update Renderer (can be done from this background thread)
                                arRenderer.updateTitleText(parsed.nearest_place)
                                arRenderer.updateDescriptionText(parsed.curiosity_text)
                                arRenderer.updateAugmentedImage(parsed.image_urls?.firstOrNull())

                                // --- CONCURRENT AUDIO DOWNLOAD ---
                                // Now that we have the place name, start downloading the audio
                                if (parsed.nearest_place.isNotEmpty()) {
                                    val placeForUrl = Uri.encode(parsed.nearest_place)
                                    val audioUrl = "$baseUrl/curiosidade_audio?place=$placeForUrl"

                                    Log.d("ARScreen", "Starting audio download from: $audioUrl")
                                    val audioRequest = Request.Builder().url(audioUrl).build()

                                    // UPDATED: Use the client with the long timeout for this call
                                    longTimeoutHttpClient.newCall(audioRequest).enqueue(object: Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            Log.e("ARScreen", "Audio download failed", e)
                                        }

                                        override fun onResponse(call: Call, response: Response) {
                                            if (response.isSuccessful) {
                                                try {
                                                    // Save the downloaded audio to a temporary file
                                                    val tempFile = File.createTempFile("curiosity_audio", ".wav", context.cacheDir)
                                                    response.body?.byteStream()?.use { input ->
                                                        FileOutputStream(tempFile).use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                    // Store the local file path to be used by the player
                                                    localAudioPath = tempFile.absolutePath
                                                    Log.d("ARScreen", "Audio downloaded and cached at: $localAudioPath")
                                                } catch (e: Exception) {
                                                    Log.e("ARScreen", "Failed to save audio cache file", e)
                                                }
                                            } else {
                                                Log.e("ARScreen", "Audio download request was not successful: ${response.code}")
                                            }
                                        }
                                    })
                                }

                                // Signal AR Renderer that data is ready and it can start looking for planes
                                arRenderer.signalDataIsReady()

                            } else {
                                curiosityError = "Resposta da API vazia."
                            }
                        } catch (ex: Exception) {
                            Log.e("ARScreen", "API response parsing error", ex)
                            curiosityError = ex.message ?: "Erro desconhecido ao processar dados."
                        } finally {
                            withContext(Dispatchers.Main) {
                                isLoadingCuriosity = false
                            }
                        }
                    }
                }
            })
        }
    }

    // --- Lifecycle and Permission Logic ---
    DisposableEffect(glSurfaceView) {
        glSurfaceView.setRenderer(arRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
            stopAndResetAudio() // Make sure cache is cleaned up on exit
            Log.d("ARScreen", "MediaPlayer released on dispose.")
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    val allPermissionsGranted = cameraPermissionState.status.isGranted && locationPermissionState.status.isGranted

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            showMuseumInfo = true
        }
    }

    DisposableEffect(arSession) {
        onDispose {
            arSession.close()
        }
    }

    // --- UI Drawing Logic ---
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
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MuseumInfoCard(
                    nearestPlace = nearestPlaceName,
                    curiosityText = curiosityText,
                    isLoading = isLoadingCuriosity,
                    error = curiosityError ?: locationError
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(
                    "É necessário permitir o acesso à Câmera e à Localização para a experiência de Realidade Aumentada.",
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    cameraPermissionState.launchPermissionRequest()
                    locationPermissionState.launchPermissionRequest()
                }) {
                    Text("Conceder Permissões")
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
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { glSurfaceView }
    )
    DisposableEffect(lifecycleOwner, arSession) {
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