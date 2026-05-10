package com.popcorn.live.config

import com.popcorn.live.BuildConfig

data class AppConfig(
    val xtreamBaseUrl: String,
    val xtreamUsername: String,
    val xtreamPassword: String,
) {
    companion object {
        fun fromBuildConfig(): AppConfig = AppConfig(
            xtreamBaseUrl = BuildConfig.XTREAM_BASE_URL,
            xtreamUsername = BuildConfig.XTREAM_USERNAME,
            xtreamPassword = BuildConfig.XTREAM_PASSWORD,
        )
    }
}
