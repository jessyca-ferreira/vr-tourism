package com.example.ra_quadrado.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ra_quadrado.models.Attraction
import com.example.ra_quadrado.viewmodels.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AttractionDetailScreen(
    attraction: Attraction,
    locationViewModel: LocationViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        bottomBar = { DetailBottomBar() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
            // The LazyColumn should go under the status bar, so we don't apply padding.
        ) {
            item {
                DetailHeader(
                    attraction = attraction,
                    distance = locationViewModel.calculateDistance(attraction.location),
                    onBackClick = onBackClick,
                    onShareClick = { /* TODO */ }
                )
            }

            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoCards(attraction)
                    Spacer(Modifier.height(24.dp))

                    TextSection("Sobre", attraction.description)
                    TextSection("História", attraction.history)
                    TextSection("Importância Cultural", attraction.importance)

                    TagsSection(attraction.tags)
                    Spacer(Modifier.height(24.dp))

                    Text("Informações Práticas", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    InfoRow(icon = Icons.Default.LocationOn, title = "Endereço", value = attraction.address)
                    InfoRow(
                        icon = Icons.Default.Phone, title = "Telefone", value = attraction.phoneNumber,
                        onClick = { launchUrl(context, "tel:${attraction.phoneNumber}") }
                    )
                    // Using "Public" icon as it's in the default set
                    InfoRow(
                        icon = Icons.Default.Public, title = "Website", value = attraction.website,
                        onClick = { launchUrl(context, attraction.website) }
                    )
                    Spacer(Modifier.height(24.dp))

                    Text("Horário de Funcionamento", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    attraction.openingHours.forEach { (day, hours) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(day, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(hours, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(
    attraction: Attraction,
    distance: Double,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(modifier = Modifier.height(350.dp)) {
        AsyncImage(
            model = attraction.imageUrl,
            contentDescription = attraction.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 400f
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick)
            CircleButton(icon = Icons.Default.Share, onClick = onShareClick)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(attraction.name, style = MaterialTheme.typography.headlineLarge, color = Color.White, fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(4.dp))
                Text(LocationViewModel().formatDistance(distance), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null, // Corrected parameter name
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(attraction.rating.toString(), style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        }
    }
}

@Composable
private fun CircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun InfoCards(attraction: Attraction) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.AccessTime,
            title = "Duração",
            value = "${attraction.visitDuration} min"
        )
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.AttachMoney,
            title = "Preço",
            value = if (attraction.price == 0.0) "Grátis" else "R$${attraction.price.toInt()}"
        )
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Category,
            title = "Categoria",
            value = attraction.category
        )
    }
}

@Composable
private fun InfoCard(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TextSection(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(tags: List<String>) {
    Column {
        Text("Destaques", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        // Using FlowRow to allow tags to wrap to the next line
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                AssistChip(onClick = {}, label = { Text(tag) })
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, onClick: (() -> Unit)? = null) {
    val rowModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            )
        }
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
private fun DetailBottomBar() {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { /*TODO*/ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Como Chegar")
            }
            Button(
                onClick = { /*TODO*/ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reservar")
            }
        }
    }
}

private fun launchUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle case where no app can handle the URL, e.g., show a Toast
    }
}