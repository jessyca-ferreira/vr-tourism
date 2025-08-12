package com.example.ra_quadrado.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ra_quadrado.viewmodels.AttractionsViewModel
import com.example.ra_quadrado.viewmodels.LocationViewModel
import com.example.ra_quadrado.widgets.AttractionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionsScreen(
    attractionsViewModel: AttractionsViewModel,
    onNavigateToDetail: (attractionId: String) -> Unit
) {
    // We can also get a ViewModel instance scoped to this screen if needed
    val locationViewModel: LocationViewModel = viewModel()

    val searchQuery by attractionsViewModel.searchQuery.collectAsState()
    val selectedCategory by attractionsViewModel.selectedCategory.collectAsState()
    val categories by attractionsViewModel.categories.collectAsState()
    val attractions by attractionsViewModel.filteredAttractions.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Atrações Artísticas", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, // Placeholder
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = attractionsViewModel::searchAttractions
                )
            }

            // Category Filters
            item {
                CategoryFilters(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = attractionsViewModel::filterByCategory
                )
            }

            // Results Count and Clear Button
            item {
                ResultsHeader(
                    count = attractions.size,
                    showClearButton = selectedCategory != "Todos" || searchQuery.isNotEmpty(),
                    onClearClick = attractionsViewModel::clearFilters
                )
            }

            // Attractions List
            if (attractions.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(attractions) { attraction ->
                    AttractionCard(
                        attraction = attraction,
                        distance = locationViewModel.calculateDistance(attraction.location),
                        onTap = { onNavigateToDetail(attraction.id) } // TODO: Navigate to detail
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Pesquisar atrações...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent
        ),
        singleLine = true
    )
}

@Composable
private fun CategoryFilters(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
private fun ResultsHeader(count: Int, showClearButton: Boolean, onClearClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$count atrações encontradas",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (showClearButton) {
            TextButton(onClick = onClearClick) {
                Text("Limpar filtros")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Nenhuma atração encontrada", style = MaterialTheme.typography.titleLarge)
        Text("Tente ajustar sua pesquisa ou filtros", style = MaterialTheme.typography.bodyMedium)
    }
}