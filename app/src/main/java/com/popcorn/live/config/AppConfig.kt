package com.popcorn.live.config

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class XtreamConnectionConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
) {
    val isValid: Boolean
        get() = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    fun normalized(): XtreamConnectionConfig =
        copy(
            baseUrl = baseUrl.trim().trimEnd('/'),
            username = username.trim(),
            password = password.trim(),
        )
}

interface XtreamConnectionConfigRepository {
    val config: StateFlow<XtreamConnectionConfig?>

    suspend fun save(config: XtreamConnectionConfig)
}

class SharedPreferencesXtreamConnectionConfigRepository(
    context: Context,
) : XtreamConnectionConfigRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _config = MutableStateFlow(readConfig())

    override val config: StateFlow<XtreamConnectionConfig?> = _config.asStateFlow()

    override suspend fun save(config: XtreamConnectionConfig) {
        val normalizedConfig = config.normalized()
        require(normalizedConfig.isValid) {
            "Xtream connection config must contain a base URL, username, and password."
        }

        preferences.edit()
            .putString(KEY_BASE_URL, normalizedConfig.baseUrl)
            .putString(KEY_USERNAME, normalizedConfig.username)
            .putString(KEY_PASSWORD, normalizedConfig.password)
            .apply()
        _config.value = normalizedConfig
    }

    private fun readConfig(): XtreamConnectionConfig? {
        val config = XtreamConnectionConfig(
            baseUrl = preferences.getString(KEY_BASE_URL, null).orEmpty(),
            username = preferences.getString(KEY_USERNAME, null).orEmpty(),
            password = preferences.getString(KEY_PASSWORD, null).orEmpty(),
        ).normalized()

        return config.takeIf(XtreamConnectionConfig::isValid)
    }

    private companion object {
        const val PREFERENCES_NAME = "popcorn_xtream_config"
        const val KEY_BASE_URL = "base_url"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
    }
}
