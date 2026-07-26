package com.example.drinkmanager.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Bottle(
    val id: Int,
    val name: String,
    val category: String,
    @SerialName("sub_category") val subCategorySnake: String? = null,
    val subCategory: String? = null,
    val brand: String? = null,
    val proof: Double? = null,
    @SerialName("abv_percent") val abvPercentSnake: Double? = null,
    val abvPercent: Double? = null,
    val volume: String? = null,
    @SerialName("photo_filename") val photoFilenameSnake: String? = null,
    val photoFilename: String? = null,
    @SerialName("all_photos") val allPhotosSnake: List<String> = emptyList(),
    val allPhotos: List<String> = emptyList(),
    val notes: String? = null,
    @SerialName("stock_status") val stockStatusSnake: String? = null,
    val stockStatus: String = "in-stock",
    @SerialName("stock_level") val stockLevelSnake: String? = null,
    val stockLevel: String = "full",
    @SerialName("is_favorite") val isFavoriteSnake: Int? = null,
    val isFavorite: Int = 0,
    val location: String? = "Home Bar",
    val cocktails: List<Cocktail> = emptyList(),
    val mocktail: Mocktail? = null
) {
    val effectiveSubCategory: String? get() = subCategory ?: subCategorySnake
    val effectiveAbvPercent: Double? get() = abvPercent ?: abvPercentSnake
    val effectivePhotoFilename: String? get() = photoFilename ?: photoFilenameSnake
    val effectiveAllPhotos: List<String> get() = if (allPhotos.isNotEmpty()) allPhotos else allPhotosSnake
    val effectiveStockStatus: String get() = if (stockStatus != "in-stock") stockStatus else (stockStatusSnake ?: "in-stock")
    val effectiveStockLevel: String get() = if (stockLevel != "full") stockLevel else (stockLevelSnake ?: "full")
    val effectiveIsFavorite: Int get() = if (isFavorite != 0) isFavorite else (isFavoriteSnake ?: 0)
}

@Serializable
data class Mocktail(
    val name: String,
    val glass: String? = null,
    val ingredients: List<String> = emptyList(),
    val instructions: String? = null
)
