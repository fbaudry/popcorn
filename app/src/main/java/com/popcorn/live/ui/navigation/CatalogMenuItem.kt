package com.popcorn.live.ui.navigation

enum class CatalogMenuItemType {
    Favorites,
    Resume,
    Category,
}

data class CatalogMenuItem(
    val id: String,
    val title: String,
    val type: CatalogMenuItemType,
    val categoryId: String? = null,
)

object CatalogMenuIds {
    const val FAVORITES = "library:favorites"
    const val RESUME = "library:resume"

    fun category(categoryId: String): String = "category:$categoryId"

    fun categoryId(menuItemId: String): String? =
        menuItemId.removePrefix("category:").takeIf { id -> id != menuItemId }
}
