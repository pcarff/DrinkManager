package com.example.drinkmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.drinkmanager.model.Bottle
import com.example.drinkmanager.model.Cocktail
import com.example.drinkmanager.model.PantryItem
import com.example.drinkmanager.model.ServerConfig
import com.example.drinkmanager.repository.DrinkManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DrinkManagerRepository(application)

    val serverConfig: StateFlow<ServerConfig> = repository.serverConfig
    val isLoading: StateFlow<Boolean> = repository.isLoading

    val rawBottles: StateFlow<List<Bottle>> = repository.bottles
    val pantryItems: StateFlow<List<PantryItem>> = repository.pantryItems

    // UI state filters
    private val _selectedTab = MutableStateFlow(0) // 0: Inventory, 1: Cocktails, 2: Pantry, 3: Settings
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedStockFilter = MutableStateFlow("All") // "All", "In Stock", "Low Stock", "Out of Stock"
    val selectedStockFilter: StateFlow<String> = _selectedStockFilter.asStateFlow()

    private val _selectedBottle = MutableStateFlow<Bottle?>(null)
    val selectedBottle: StateFlow<Bottle?> = _selectedBottle.asStateFlow()

    private val _selectedCocktail = MutableStateFlow<Cocktail?>(null)
    val selectedCocktail: StateFlow<Cocktail?> = _selectedCocktail.asStateFlow()

    private val _canMakeOnlyFilter = MutableStateFlow(false)
    val canMakeOnlyFilter: StateFlow<Boolean> = _canMakeOnlyFilter.asStateFlow()

    private val _favoritesOnlyFilter = MutableStateFlow(false)
    val favoritesOnlyFilter: StateFlow<Boolean> = _favoritesOnlyFilter.asStateFlow()

    private val _activeRecipeDialogCocktail = MutableStateFlow<Cocktail?>(null)
    val activeRecipeDialogCocktail: StateFlow<Cocktail?> = _activeRecipeDialogCocktail.asStateFlow()

    private val _showAddBottle = MutableStateFlow(false)
    val showAddBottle: StateFlow<Boolean> = _showAddBottle.asStateFlow()

    val filteredBottles: StateFlow<List<Bottle>> = combine(
        rawBottles,
        searchQuery,
        selectedCategory,
        selectedStockFilter,
        favoritesOnlyFilter
    ) { bottles, query, category, stock, favoritesOnly ->
        bottles.filter { bottle ->
            val matchesQuery = query.isEmpty() ||
                    bottle.name.contains(query, ignoreCase = true) ||
                    (bottle.brand?.contains(query, ignoreCase = true) == true) ||
                    (bottle.notes?.contains(query, ignoreCase = true) == true)

            val matchesCategory = category == "All" ||
                    bottle.category.contains(category, ignoreCase = true) ||
                    (bottle.subCategory?.contains(category, ignoreCase = true) == true)

            val matchesStock = when (stock) {
                "In Stock" -> bottle.stockStatus == "in-stock"
                "Low Stock" -> bottle.stockStatus == "low"
                "Out of Stock" -> bottle.stockStatus == "out-of-stock"
                else -> true
            }

            val matchesFavorite = !favoritesOnly || bottle.isFavorite == 1

            matchesQuery && matchesCategory && matchesStock && matchesFavorite
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCocktails: StateFlow<List<Cocktail>> = combine(
        rawBottles,
        searchQuery,
        canMakeOnlyFilter,
        favoritesOnlyFilter
    ) { bottles, query, canMake, favoritesOnly ->
        val list = mutableListOf<Cocktail>()
        bottles.forEach { bottle ->
            bottle.cocktails.forEach { c ->
                val fullCocktail = c.copy(bottleId = bottle.id, bottleName = bottle.name)
                val matchesQuery = query.isEmpty() || c.name.contains(query, ignoreCase = true)
                val matchesCanMake = !canMake || bottle.stockStatus != "out-of-stock"
                val matchesFavorite = !favoritesOnly || c.isFavorite
                if (matchesQuery && matchesCanMake && matchesFavorite) {
                    list.add(fullCocktail)
                }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            repository.bottles.collect { bottles ->
                if (bottles.isNotEmpty() && _selectedBottle.value == null) {
                    _selectedBottle.value = bottles.first()
                }
                // Keep selectedBottle in sync when underlying list changes
                val currentId = _selectedBottle.value?.id
                if (currentId != null) {
                    val updated = bottles.find { it.id == currentId }
                    if (updated != null && updated != _selectedBottle.value) {
                        _selectedBottle.value = updated
                    }
                }
            }
        }
        refreshServerData()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _searchQuery.value = ""
    }

    fun selectStockFilter(filter: String) {
        _selectedStockFilter.value = filter
    }

    fun selectBottle(bottle: Bottle?) {
        _selectedBottle.value = bottle
    }

    fun selectCocktail(cocktail: Cocktail?) {
        _selectedCocktail.value = cocktail
        _activeRecipeDialogCocktail.value = cocktail
    }

    fun toggleCanMakeOnly() {
        _canMakeOnlyFilter.value = !_canMakeOnlyFilter.value
    }

    fun toggleFavoritesOnly() {
        _favoritesOnlyFilter.value = !_favoritesOnlyFilter.value
    }

    fun toggleBottleFavorite(bottleId: Int) {
        viewModelScope.launch {
            repository.toggleBottleFavorite(bottleId)
        }
    }

    fun toggleCocktailFavorite(cocktail: Cocktail) {
        viewModelScope.launch {
            repository.toggleCocktailFavorite(cocktail.name)
            if (_activeRecipeDialogCocktail.value?.name.equals(cocktail.name, ignoreCase = true)) {
                val current = _activeRecipeDialogCocktail.value
                if (current != null) {
                    _activeRecipeDialogCocktail.value = current.copy(isFavorite = !current.isFavorite)
                }
            }
        }
    }

    fun togglePantryFavorite(itemId: Int) {
        viewModelScope.launch {
            repository.togglePantryFavorite(itemId)
        }
    }

    fun openRecipeDialog(cocktail: Cocktail) {
        _activeRecipeDialogCocktail.value = cocktail
    }

    fun closeRecipeDialog() {
        _activeRecipeDialogCocktail.value = null
    }

    fun saveRecipe(updated: Cocktail) {
        viewModelScope.launch {
            repository.saveEditedCocktail(updated)
            _activeRecipeDialogCocktail.value = updated
        }
    }

    fun updateBottleStock(bottleId: Int, newLevel: String) {
        viewModelScope.launch {
            repository.updateBottleStockLevel(bottleId, newLevel)
        }
    }

    fun togglePantryStock(itemId: Int) {
        viewModelScope.launch {
            repository.togglePantryStock(itemId)
        }
    }

    fun refreshServerData() {
        viewModelScope.launch {
            repository.refreshData()
        }
    }

    fun saveServerUrl(newUrl: String) {
        repository.updateServerUrl(newUrl)
        refreshServerData()
    }

    fun showAddBottleDialog() {
        _showAddBottle.value = true
    }

    fun hideAddBottleDialog() {
        _showAddBottle.value = false
    }

    fun analyzeBottleImage(imageBase64: String, callback: (com.example.drinkmanager.ui.screens.BottleFormData?) -> Unit) {
        viewModelScope.launch {
            val result = repository.analyzeBottleImage(imageBase64)
            if (result != null) {
                val formData = com.example.drinkmanager.ui.screens.BottleFormData(
                    name = result["name"]?.toString() ?: "",
                    brand = result["brand"]?.toString() ?: "",
                    category = result["category"]?.toString() ?: "",
                    subCategory = result["subCategory"]?.toString() ?: "",
                    proof = result["proof"]?.toString()?.takeIf { it != "null" } ?: "",
                    abvPercent = result["abvPercent"]?.toString()?.takeIf { it != "null" } ?: "",
                    volume = result["volume"]?.toString()?.takeIf { it != "null" } ?: "",
                    notes = result["notes"]?.toString() ?: "",
                    photoFilename = result["photoFilename"]?.toString() ?: ""
                )
                callback(formData)
            } else {
                callback(null)
            }
        }
    }

    fun saveNewBottle(formData: com.example.drinkmanager.ui.screens.BottleFormData) {
        viewModelScope.launch {
            val data = mutableMapOf<String, Any?>(
                "name" to formData.name,
                "brand" to formData.brand,
                "category" to formData.category,
                "subCategory" to formData.subCategory,
                "proof" to formData.proof.toDoubleOrNull(),
                "abvPercent" to formData.abvPercent.toDoubleOrNull(),
                "volume" to formData.volume,
                "notes" to formData.notes,
                "photoFilename" to formData.photoFilename
            )
            val success = repository.createNewBottle(data)
            if (success) {
                _showAddBottle.value = false
            }
        }
    }
}
