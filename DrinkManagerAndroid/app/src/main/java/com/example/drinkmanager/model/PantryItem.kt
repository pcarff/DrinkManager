package com.example.drinkmanager.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PantryItem(
    val id: Int = 0,
    val name: String,
    val category: String = "Mixer",
    @SerialName("stock_status") val stockStatusSnake: String? = null,
    val stockStatus: String = "in-stock",
    @SerialName("is_favorite") val isFavoriteSnake: Int? = null,
    val isFavorite: Int = 0,
    val notes: String? = null
) {
    val effectiveStockStatus: String get() = if (stockStatus != "in-stock") stockStatus else (stockStatusSnake ?: "in-stock")
    val effectiveIsFavorite: Int get() = if (isFavorite != 0) isFavorite else (isFavoriteSnake ?: 0)
}
