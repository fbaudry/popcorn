package com.popcorn.live.ui.navigation

enum class AppSection(
    val title: String,
    val sidebarLabel: String,
) {
    Live(title = "Live TV", sidebarLabel = "Live"),
    Movies(title = "Films", sidebarLabel = "Films"),
    Series(title = "Séries", sidebarLabel = "Séries"),
}
