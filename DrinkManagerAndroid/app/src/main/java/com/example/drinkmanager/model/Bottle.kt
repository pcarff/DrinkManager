package com.example.drinkmanager.model

import kotlinx.serialization.Serializable

@Serializable
data class Bottle(
    val id: Int,
    val name: String,
    val category: String,
    val subCategory: String? = null,
    val brand: String? = null,
    val proof: Double? = null,
    val abvPercent: Double? = null,
    val volume: String? = null,
    val photoFilename: String? = null,
    val allPhotos: List<String> = emptyList(),
    val notes: String? = null,
    val stockStatus: String = "in-stock", // "in-stock", "low", "out-of-stock"
    val stockLevel: String = "full",     // "full", "high", "medium", "low", "empty"
    val isFavorite: Int = 0,
    val location: String? = "Home Bar",
    val cocktails: List<Cocktail> = emptyList(),
    val mocktail: Mocktail? = null
)

@Serializable
data class Mocktail(
    val name: String,
    val glass: String? = null,
    val ingredients: List<String> = emptyList(),
    val instructions: String? = null
)
