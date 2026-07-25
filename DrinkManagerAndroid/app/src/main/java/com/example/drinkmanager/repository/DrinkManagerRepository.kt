package com.example.drinkmanager.repository

import android.content.Context
import com.example.drinkmanager.model.Bottle
import com.example.drinkmanager.model.Cocktail
import com.example.drinkmanager.model.Mocktail
import com.example.drinkmanager.model.PantryItem
import com.example.drinkmanager.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DrinkManagerRepository(private val context: Context) {

    private val jsonFormatter = Json { ignoreUnknownKeys = true }

    private val _serverConfig = MutableStateFlow(
        ServerConfig(baseUrl = loadSavedBaseUrl())
    )
    val serverConfig: StateFlow<ServerConfig> = _serverConfig.asStateFlow()

    private val _bottles = MutableStateFlow<List<Bottle>>(emptyList())
    val bottles: StateFlow<List<Bottle>> = _bottles.asStateFlow()

    private val _pantryItems = MutableStateFlow<List<PantryItem>>(emptyList())
    val pantryItems: StateFlow<List<PantryItem>> = _pantryItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadInitialLocalCache()
    }

    private fun loadSavedBaseUrl(): String {
        val prefs = context.getSharedPreferences("drink_manager_prefs", Context.MODE_PRIVATE)
        return prefs.getString("server_url", "http://10.19.5.115:3005") ?: "http://10.19.5.115:3005"
    }

    fun updateServerUrl(newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl.dropLast(1) else newUrl
        val prefs = context.getSharedPreferences("drink_manager_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("server_url", formatted).apply()
        _serverConfig.value = _serverConfig.value.copy(baseUrl = formatted)
    }

    /**
     * Loads full 44 bottle inventory from assets/inventory.json as local cache.
     */
    private fun loadInitialLocalCache() {
        try {
            val jsonString = context.assets.open("inventory.json").bufferedReader().use { it.readText() }
            val loadedBottles = jsonFormatter.decodeFromString<List<Bottle>>(jsonString)
            _bottles.value = loadedBottles
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val initialPantry = listOf(
            PantryItem(id = 1, name = "Rich Demerara Simple Syrup", category = "Syrup", stockStatus = "in-stock", isFavorite = 1),
            PantryItem(id = 2, name = "Fresh Lemons & Limes", category = "Citrus", stockStatus = "in-stock", isFavorite = 1),
            PantryItem(id = 3, name = "Angostura Aromatic Bitters", category = "Bitters", stockStatus = "in-stock", isFavorite = 1),
            PantryItem(id = 4, name = "Fever-Tree Indian Tonic Water", category = "Mixer", stockStatus = "in-stock", isFavorite = 0),
            PantryItem(id = 5, name = "Luxardo Maraschino Cherries", category = "Garnish", stockStatus = "low", isFavorite = 1),
            PantryItem(id = 6, name = "Fresh Mint Leaves", category = "Garnish", stockStatus = "out-of-stock", isFavorite = 0)
        )
        _pantryItems.value = initialPantry
    }

    suspend fun refreshData(): Boolean = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val urlString = "${_serverConfig.value.baseUrl}/api/bottles"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val fetchedBottles = jsonFormatter.decodeFromString<List<Bottle>>(jsonString)
                _bottles.value = fetchedBottles

                fetchPantryItemsRemote()

                _serverConfig.value = _serverConfig.value.copy(
                    isOnline = true,
                    lastSyncedAt = System.currentTimeMillis(),
                    syncStatusMessage = "Connected & Synced (${fetchedBottles.size} bottles)"
                )
                _isLoading.value = false
                return@withContext true
            } else {
                _serverConfig.value = _serverConfig.value.copy(
                    isOnline = false,
                    syncStatusMessage = "Server code ${connection.responseCode}. Using local database (${_bottles.value.size} bottles)."
                )
            }
        } catch (e: Exception) {
            _serverConfig.value = _serverConfig.value.copy(
                isOnline = false,
                syncStatusMessage = "Offline Mode (${_bottles.value.size} bottles loaded)"
            )
        } finally {
            _isLoading.value = false
        }
        return@withContext false
    }

    private fun fetchPantryItemsRemote() {
        try {
            val urlString = "${_serverConfig.value.baseUrl}/api/pantry"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val fetched = jsonFormatter.decodeFromString<List<PantryItem>>(jsonString)
                _pantryItems.value = fetched
            }
        } catch (_: Exception) {}
    }

    suspend fun updateBottleStockLevel(bottleId: Int, newStockLevel: String): Boolean = withContext(Dispatchers.IO) {
        val newStatus = when (newStockLevel.lowercase()) {
            "empty" -> "out-of-stock"
            "low" -> "low"
            else -> "in-stock"
        }

        _bottles.value = _bottles.value.map { b ->
            if (b.id == bottleId) {
                b.copy(stockLevel = newStockLevel, stockStatus = newStatus)
            } else b
        }

        try {
            val urlString = "${_serverConfig.value.baseUrl}/api/bottles/$bottleId/stock"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 3000

            val payload = "{\"stockLevel\": \"$newStockLevel\"}"
            OutputStreamWriter(connection.outputStream).use { it.write(payload) }

            return@withContext (connection.responseCode == 200)
        } catch (_: Exception) {
            return@withContext true
        }
    }

    suspend fun togglePantryStock(itemId: Int) = withContext(Dispatchers.IO) {
        _pantryItems.value = _pantryItems.value.map { item ->
            if (item.id == itemId) {
                val nextStatus = when (item.stockStatus) {
                    "in-stock" -> "low"
                    "low" -> "out-of-stock"
                    else -> "in-stock"
                }
                item.copy(stockStatus = nextStatus)
            } else item
        }
    }
}
