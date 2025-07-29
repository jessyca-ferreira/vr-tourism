package com.example.ra_quadrado.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ra_quadrado.data.SampleAttractions
import com.example.ra_quadrado.models.Attraction
import kotlinx.coroutines.flow.*

class AttractionsViewModel : ViewModel() {

    // Internal state holders
    private val _isLoading = MutableStateFlow(false)
    private val _allAttractions = MutableStateFlow<List<Attraction>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("Todos")

    // Publicly exposed states for the UI to observe
    val isLoading = _isLoading.asStateFlow()
    val searchQuery = _searchQuery.asStateFlow()
    val selectedCategory = _selectedCategory.asStateFlow()

    // The list of available categories, derived from the full attractions list
    val categories: StateFlow<List<String>> = _allAttractions.map { attractions ->
        listOf("Todos") + attractions.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Todos"))

    // The final filtered list of attractions, recalculated whenever the source list,
    // search query, or selected category changes. This is the reactive approach.
    val filteredAttractions: StateFlow<List<Attraction>> = combine(
        _allAttractions,
        _searchQuery,
        _selectedCategory
    ) { attractions, query, category ->
        _isLoading.value = true
        val filtered = attractions.filter { attraction ->
            val matchesCategory = category == "Todos" || attraction.category == category
            val matchesSearch = query.isBlank() ||
                    attraction.name.contains(query, ignoreCase = true) ||
                    attraction.description.contains(query, ignoreCase = true) ||
                    attraction.tags.any { it.contains(query, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
        _isLoading.value = false
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load the initial data when the ViewModel is created
        _allAttractions.value = SampleAttractions.get()
    }

    // --- Actions called by the UI ---

    fun searchAttractions(query: String) {
        _searchQuery.value = query
    }

    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "Todos"
    }

    fun getAttractionById(id: String): Attraction? {
        return _allAttractions.value.find { it.id == id }
    }
}
