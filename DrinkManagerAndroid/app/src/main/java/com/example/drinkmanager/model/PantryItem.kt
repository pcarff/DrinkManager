package com.example.drinkmanager.model

import kotlinx.serialization.Serializable

@Serializable
data class PantryItem(
    val id: Int = 0,
    val name: String,
    val category: String = "Mixer", // Mixer, Syrup, Citrus, Garnish, Bitters, Ice
    val stockStatus: String = "in-stock", // in-stock, low, out-of-stock
    val isFavorite: Int = 0,
    val notes: String? = null
)
