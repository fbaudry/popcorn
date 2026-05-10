package com.popcorn.live.xtream

import java.nio.charset.StandardCharsets

class XtreamUrlFactory(
    baseUrl: String,
    username: String,
    password: String,
) {
    private val cleanBaseUrl = baseUrl.trim().trimEnd('/')
    private val encodedUsername = username.urlEncode()
    private val encodedPassword = password.urlEncode()

    fun accountInfoUrl(): String =
        "$cleanBaseUrl/player_api.php?username=$encodedUsername&password=$encodedPassword"

    fun liveCategoriesUrl(): String =
        "${accountInfoUrl()}&action=get_live_categories"

    fun liveStreamsUrl(categoryId: String? = null): String =
        actionUrl(action = "get_live_streams", categoryId = categoryId)

    fun vodCategoriesUrl(): String =
        "${accountInfoUrl()}&action=get_vod_categories"

    fun vodStreamsUrl(categoryId: String? = null): String =
        actionUrl(action = "get_vod_streams", categoryId = categoryId)

    fun vodInfoUrl(vodId: Int): String =
        "${accountInfoUrl()}&action=get_vod_info&vod_id=$vodId"

    fun seriesCategoriesUrl(): String =
        "${accountInfoUrl()}&action=get_series_categories"

    fun seriesUrl(categoryId: String? = null): String =
        actionUrl(action = "get_series", categoryId = categoryId)

    fun seriesInfoUrl(seriesId: Int): String =
        "${accountInfoUrl()}&action=get_series_info&series_id=$seriesId"

    fun hlsPlaybackUrl(streamId: Int): String =
        "$cleanBaseUrl/live/$encodedUsername/$encodedPassword/$streamId.m3u8"

    fun tsPlaybackUrl(streamId: Int): String =
        "$cleanBaseUrl/live/$encodedUsername/$encodedPassword/$streamId.ts"

    fun moviePlaybackUrl(streamId: Int, extension: String): String =
        "$cleanBaseUrl/movie/$encodedUsername/$encodedPassword/$streamId.${extension.toPathExtension()}"

    fun seriesPlaybackUrl(episodeId: String, extension: String): String =
        "$cleanBaseUrl/series/$encodedUsername/$encodedPassword/${episodeId.urlEncode()}.${extension.toPathExtension()}"

    private fun actionUrl(action: String, categoryId: String? = null): String =
        buildString {
            append(accountInfoUrl())
            append("&action=")
            append(action)
            if (!categoryId.isNullOrBlank()) {
                append("&category_id=")
                append(categoryId.urlEncode())
            }
        }

    private fun String.toPathExtension(): String =
        trim()
            .trimStart('.')
            .takeIf(String::isNotBlank)
            ?.urlEncode()
            ?: DEFAULT_MEDIA_EXTENSION

    private fun String.urlEncode(): String =
        buildString {
            for (byte in toByteArray(StandardCharsets.UTF_8)) {
                val value = byte.toInt() and 0xff
                val char = value.toChar()
                if (char.isUrlUnreserved()) {
                    append(char)
                } else {
                    append('%')
                    append(hex[value ushr 4])
                    append(hex[value and 0x0f])
                }
            }
        }

    private fun Char.isUrlUnreserved(): Boolean =
        this in 'A'..'Z' ||
            this in 'a'..'z' ||
            this in '0'..'9' ||
            this == '-' ||
            this == '.' ||
            this == '_' ||
            this == '~'

    private companion object {
        const val hex = "0123456789ABCDEF"
        const val DEFAULT_MEDIA_EXTENSION = "mp4"
    }
}
