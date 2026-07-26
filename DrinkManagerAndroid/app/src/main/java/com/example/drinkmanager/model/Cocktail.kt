package com.example.drinkmanager.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Cocktail(
    val id: Int? = null,
    @SerialName("bottle_id") val bottleIdSnake: Int? = null,
    val bottleId: Int? = null,
    @SerialName("bottle_name") val bottleNameSnake: String? = null,
    val bottleName: String? = null,
    val name: String,
    val glass: String? = "Rocks Glass",
    val ingredients: List<String> = emptyList(),
    val instructions: String? = null,
    @SerialName("is_mocktail") val isMocktailSnake: Boolean? = null,
    val isMocktail: Boolean = false,
    @SerialName("is_favorite") val isFavoriteSnake: Int? = null,
    val isFavorite: Boolean = false
) {
    val effectiveBottleId: Int? get() = bottleId ?: bottleIdSnake
    val effectiveBottleName: String? get() = bottleName ?: bottleNameSnake
    val effectiveIsMocktail: Boolean get() = isMocktail || (isMocktailSnake == true)
    val effectiveIsFavorite: Boolean get() = isFavorite || (isFavoriteSnake == 1)
}
