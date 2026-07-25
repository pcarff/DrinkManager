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

    val filteredBottles: StateFlow<List<Bottle>> = combine(
        rawBottles,
        searchQuery,
        selectedCategory,
        selectedStockFilter
    ) { bottles, query, category, stock ->
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

            matchesQuery && matchesCategory && matchesStock
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCocktails: StateFlow<List<Cocktail>> = combine(
        rawBottles,
        searchQuery,
        canMakeOnlyFilter
    ) { bottles, query, canMake ->
        val list = mutableListOf<Cocktail>()
        bottles.forEach { bottle ->
            bottle.cocktails.forEach { c ->
                val fullCocktail = c.copy(bottleId = bottle.id, bottleName = bottle.name)
                val matchesQuery = query.isEmpty() || c.name.contains(query, ignoreCase = true)
                val matchesCanMake = !canMake || bottle.stockStatus != "out-of-stock"
                if (matchesQuery && matchesCanMake) {
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
    }

    fun toggleCanMakeOnly() {
        _canMakeOnlyFilter.value = !_canMakeOnlyFilter.value
    }

    fun updateStockLevel(bottleId: Int, newStockLevel: String) {
        viewModelScope.launch {
            repository.updateBottleStockLevel(bottleId, newStockLevel)
            // Refresh selected bottle reference if it matches
            _selectedBottle.value = _selectedBottle.value?.let { b ->
                if (b.id == bottleId) b.copy(stockLevel = newStockLevel) else b
            }
        }
    }

    fun togglePantryItem(itemId: Int) {
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
}
