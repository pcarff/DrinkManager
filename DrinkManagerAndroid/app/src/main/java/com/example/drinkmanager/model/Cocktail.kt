package com.example.drinkmanager.model

import kotlinx.serialization.Serializable

@Serializable
data class Cocktail(
    val id: Int? = null,
    val bottleId: Int? = null,
    val bottleName: String? = null,
    val name: String,
    val glass: String? = "Rocks Glass",
    val ingredients: List<String> = emptyList(),
    val instructions: String? = null,
    val isMocktail: Boolean = false,
    val isFavorite: Boolean = false
)
