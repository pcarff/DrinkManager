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

    private val favPrefs = context.getSharedPreferences("drink_manager_favorites", Context.MODE_PRIVATE)

    private fun isBottleFavLocal(id: Int, remoteFav: Int): Int {
        val key = "bottle_$id"
        if (favPrefs.contains(key)) {
            return if (favPrefs.getBoolean(key, false)) 1 else 0
        }
        return remoteFav
    }

    private fun isCocktailFavLocal(name: String, remoteFav: Boolean): Boolean {
        val key = "cocktail_$name"
        if (favPrefs.contains(key)) {
            return favPrefs.getBoolean(key, false)
        }
        return remoteFav
    }

    private fun isPantryFavLocal(id: Int, remoteFav: Int): Int {
        val key = "pantry_$id"
        if (favPrefs.contains(key)) {
            return if (favPrefs.getBoolean(key, false)) 1 else 0
        }
        return remoteFav
    }

    private fun saveBottleFavLocal(id: Int, isFav: Boolean) {
        favPrefs.edit().putBoolean("bottle_$id", isFav).apply()
    }

    private fun saveCocktailFavLocal(name: String, isFav: Boolean) {
        favPrefs.edit().putBoolean("cocktail_$name", isFav).apply()
    }

    private fun savePantryFavLocal(id: Int, isFav: Boolean) {
        favPrefs.edit().putBoolean("pantry_$id", isFav).apply()
    }

    /**
     * Loads full 44 bottle inventory from assets/inventory.json as local cache.
     */
    private fun loadInitialLocalCache() {
        try {
            val jsonString = context.assets.open("inventory.json").bufferedReader().use { it.readText() }
            val loadedBottles = jsonFormatter.decodeFromString<List<Bottle>>(jsonString)
            _bottles.value = loadedBottles.map { b ->
                val mergedCocktails = b.cocktails.map { c ->
                    c.copy(
                        bottleId = c.effectiveBottleId,
                        bottleName = c.effectiveBottleName,
                        isMocktail = c.effectiveIsMocktail,
                        isFavorite = isCocktailFavLocal(c.name, c.effectiveIsFavorite)
                    )
                }
                b.copy(
                    subCategory = b.effectiveSubCategory,
                    abvPercent = b.effectiveAbvPercent,
                    photoFilename = b.effectivePhotoFilename,
                    allPhotos = b.effectiveAllPhotos,
                    stockStatus = b.effectiveStockStatus,
                    stockLevel = b.effectiveStockLevel,
                    isFavorite = isBottleFavLocal(b.id, b.effectiveIsFavorite),
                    cocktails = mergedCocktails
                )
            }

            val initialPantry = listOf(
                PantryItem(id = 1, name = "Rich Demerara Simple Syrup", category = "Syrup", stockStatus = "in-stock", isFavorite = isPantryFavLocal(1, 1)),
                PantryItem(id = 2, name = "Fresh Lemons & Limes", category = "Citrus", stockStatus = "in-stock", isFavorite = isPantryFavLocal(2, 1)),
                PantryItem(id = 3, name = "Angostura Aromatic Bitters", category = "Bitters", stockStatus = "in-stock", isFavorite = isPantryFavLocal(3, 1)),
                PantryItem(id = 4, name = "Fever-Tree Indian Tonic Water", category = "Mixer", stockStatus = "in-stock", isFavorite = isPantryFavLocal(4, 0)),
                PantryItem(id = 5, name = "Luxardo Maraschino Cherries", category = "Garnish", stockStatus = "low", isFavorite = isPantryFavLocal(5, 1)),
                PantryItem(id = 6, name = "Fresh Mint Leaves", category = "Garnish", stockStatus = "out-of-stock", isFavorite = isPantryFavLocal(6, 0))
            )
            _pantryItems.value = initialPantry
        } catch (_: Exception) {}
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
                
                _bottles.value = fetchedBottles.map { b ->
                    val mergedCocktails = b.cocktails.map { c ->
                        c.copy(
                            bottleId = c.effectiveBottleId,
                            bottleName = c.effectiveBottleName,
                            isMocktail = c.effectiveIsMocktail,
                            isFavorite = isCocktailFavLocal(c.name, c.effectiveIsFavorite)
                        )
                    }
                    b.copy(
                        subCategory = b.effectiveSubCategory,
                        abvPercent = b.effectiveAbvPercent,
                        photoFilename = b.effectivePhotoFilename,
                        allPhotos = b.effectiveAllPhotos,
                        stockStatus = b.effectiveStockStatus,
                        stockLevel = b.effectiveStockLevel,
                        isFavorite = isBottleFavLocal(b.id, b.effectiveIsFavorite),
                        cocktails = mergedCocktails
                    )
                }

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
                syncStatusMessage = "Sync error: ${e.localizedMessage ?: "Offline"}"
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
                val remotePantry = jsonFormatter.decodeFromString<List<PantryItem>>(jsonString)
                _pantryItems.value = remotePantry.map { item ->
                    item.copy(
                        stockStatus = item.effectiveStockStatus,
                        isFavorite = isPantryFavLocal(item.id, item.effectiveIsFavorite)
                    )
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun updateBottleStockLevel(bottleId: Int, newStockLevel: String): Boolean = withContext(Dispatchers.IO) {
        val newStatus = when (newStockLevel.lowercase()) {
            "empty" -> "out-of-stock"
            "quarter", "almost-empty", "low" -> "low"
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

    suspend fun toggleBottleFavorite(bottleId: Int) = withContext(Dispatchers.IO) {
        var newFavStatus = 0
        _bottles.value = _bottles.value.map { b ->
            if (b.id == bottleId) {
                newFavStatus = if (b.isFavorite == 1) 0 else 1
                b.copy(isFavorite = newFavStatus)
            } else b
        }
        saveBottleFavLocal(bottleId, newFavStatus == 1)

        try {
            val urlString = "${_serverConfig.value.baseUrl}/api/bottles/$bottleId/favorite"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 3000
            val payload = "{\"isFavorite\": $newFavStatus}"
            OutputStreamWriter(connection.outputStream).use { it.write(payload) }
            connection.responseCode
        } catch (_: Exception) {}
    }

    suspend fun toggleCocktailFavorite(cocktailName: String) = withContext(Dispatchers.IO) {
        var newFavStatus = false
        _bottles.value = _bottles.value.map { b ->
            val updatedCocktails = b.cocktails.map { c ->
                if (c.name.equals(cocktailName, ignoreCase = true)) {
                    newFavStatus = !c.isFavorite
                    c.copy(isFavorite = newFavStatus)
                } else c
            }
            b.copy(cocktails = updatedCocktails)
        }
        saveCocktailFavLocal(cocktailName, newFavStatus)

        try {
            val urlString = "${_serverConfig.value.baseUrl}/api/cocktails/favorite"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 3000
            val payload = "{\"name\": \"$cocktailName\", \"isFavorite\": $newFavStatus}"
            OutputStreamWriter(connection.outputStream).use { it.write(payload) }
            connection.responseCode
        } catch (_: Exception) {}
    }

    suspend fun togglePantryFavorite(itemId: Int) = withContext(Dispatchers.IO) {
        var newFavStatus = 0
        _pantryItems.value = _pantryItems.value.map { item ->
            if (item.id == itemId) {
                newFavStatus = if (item.isFavorite == 1) 0 else 1
                item.copy(isFavorite = newFavStatus)
            } else item
        }
        savePantryFavLocal(itemId, newFavStatus == 1)

        try {
            val urlString = "${_serverConfig.value.baseUrl}/api/pantry/$itemId/favorite"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 3000
            val payload = "{\"isFavorite\": $newFavStatus}"
            OutputStreamWriter(connection.outputStream).use { it.write(payload) }
            connection.responseCode
        } catch (_: Exception) {}
    }

    suspend fun saveEditedCocktail(updated: Cocktail) = withContext(Dispatchers.IO) {
        _bottles.value = _bottles.value.map { bottle ->
            if (bottle.id == updated.bottleId || bottle.name.equals(updated.bottleName, ignoreCase = true)) {
                val updatedCocktails = bottle.cocktails.map { c ->
                    if (c.name.equals(updated.name, ignoreCase = true) || c.id == updated.id) {
                        updated
                    } else c
                }
                bottle.copy(cocktails = updatedCocktails)
            } else bottle
        }
    }

    /**
     * Send a base64-encoded bottle image to the server for AI analysis.
     * Returns a map of extracted bottle metadata, or null on failure.
     */
    suspend fun analyzeBottleImage(imageBase64: String): Map<String, Any?>? = withContext(Dispatchers.IO) {
        try {
            val urlString = "${_serverConfig.value.baseUrl}/api/analyze-bottle"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 60000 // 60s — Gemini can take time
            connection.readTimeout = 60000

            val payload = buildString {
                append("{\"imageBase64\":\"")
                append(imageBase64)
                append("\"}")
            }
            OutputStreamWriter(connection.outputStream).use { it.write(payload) }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val map = mutableMapOf<String, Any?>()
                // Simple JSON parsing using kotlinx.serialization
                val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(responseText)
                if (jsonObj is kotlinx.serialization.json.JsonObject) {
                    for ((key, value) in jsonObj) {
                        map[key] = when (value) {
                            is kotlinx.serialization.json.JsonPrimitive -> {
                                if (value.isString) value.content
                                else value.content // numbers come as strings too
                            }
                            else -> value.toString()
                        }
                    }
                }
                return@withContext map
            } else {
                val errText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                android.util.Log.e("DrinkRepo", "Analyze failed ${connection.responseCode}: $errText")
                return@withContext null
            }
        } catch (e: Exception) {
            android.util.Log.e("DrinkRepo", "Analyze bottle error", e)
            return@withContext null
        }
    }

    /**
     * Create a new bottle on the server and refresh the local list.
     */
    sealed class CreateBottleResult {
        data object Success : CreateBottleResult()
        data class Duplicate(val existingName: String) : CreateBottleResult()
        data class Error(val message: String) : CreateBottleResult()
    }

    suspend fun createNewBottle(data: Map<String, Any?>, force: Boolean = false): CreateBottleResult = withContext(Dispatchers.IO) {
        try {
            val urlString = "${_serverConfig.value.baseUrl}/api/bottles"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000

            // Build JSON payload
            val json = buildString {
                append("{")
                append("\"name\":\"${escapeJson(data["name"]?.toString() ?: "Unknown")}\",")
                append("\"brand\":\"${escapeJson(data["brand"]?.toString() ?: "")}\",")
                append("\"category\":\"${escapeJson(data["category"]?.toString() ?: "other")}\",")
                append("\"subCategory\":\"${escapeJson(data["subCategory"]?.toString() ?: "")}\",")
                val proof = data["proof"]?.toString()?.toDoubleOrNull()
                if (proof != null) append("\"proof\":$proof,") else append("\"proof\":null,")
                val abv = data["abvPercent"]?.toString()?.toDoubleOrNull()
                if (abv != null) append("\"abvPercent\":$abv,") else append("\"abvPercent\":null,")
                append("\"volume\":\"${escapeJson(data["volume"]?.toString() ?: "750ml")}\",")
                append("\"notes\":\"${escapeJson(data["notes"]?.toString() ?: "")}\",")
                append("\"photoFilename\":\"${escapeJson(data["photoFilename"]?.toString() ?: "placeholder.jpg")}\",")
                append("\"stockLevel\":\"full\",")
                append("\"stockStatus\":\"in-stock\"")
                if (force) append(",\"force\":true")
                append("}")
            }

            OutputStreamWriter(connection.outputStream).use { it.write(json) }

            return@withContext when (connection.responseCode) {
                200, 201 -> {
                    refreshData()
                    CreateBottleResult.Success
                }
                409 -> {
                    val errText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "{}"
                    val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(errText)
                    val existingName = if (jsonObj is kotlinx.serialization.json.JsonObject) {
                        jsonObj["message"]?.let {
                            if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
                        } ?: "A bottle with this name already exists"
                    } else "A bottle with this name already exists"
                    CreateBottleResult.Duplicate(existingName)
                }
                else -> CreateBottleResult.Error("Server returned ${connection.responseCode}")
            }
        } catch (e: Exception) {
            android.util.Log.e("DrinkRepo", "Create bottle error", e)
            return@withContext CreateBottleResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

