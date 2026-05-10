package com.popcorn.live.catalog

data class LiveCategory(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

data class LiveChannel(
    val streamId: Int,
    val name: String,
    val categoryId: String,
    val streamIcon: String?,
    val streamType: String,
    val added: String?,
    val sortOrder: Int,
)
