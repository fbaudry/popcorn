package com.popcorn.live.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorn.live.config.XtreamConnectionConfig
import com.popcorn.live.config.XtreamConnectionConfigRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfigViewModel(
    configRepository: XtreamConnectionConfigRepository,
    private val saveConfig: suspend (XtreamConnectionConfig) -> Unit,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val draft = MutableStateFlow(configRepository.config.value ?: XtreamConnectionConfig("", "", ""))
    private val isSaving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        configRepository.config,
        draft,
        isSaving,
        errorMessage,
    ) { savedConfig, draft, isSaving, errorMessage ->
        ConfigUiState(
            isConfigured = savedConfig != null,
            baseUrl = draft.baseUrl,
            username = draft.username,
            password = draft.password,
            isSaving = isSaving,
            errorMessage = errorMessage,
            canSave = draft.normalized().isValid && !isSaving,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConfigUiState())

    fun onBaseUrlChanged(value: String) {
        updateDraft { config -> config.copy(baseUrl = value) }
    }

    fun onUsernameChanged(value: String) {
        updateDraft { config -> config.copy(username = value) }
    }

    fun onPasswordChanged(value: String) {
        updateDraft { config -> config.copy(password = value) }
    }

    fun save() {
        val config = draft.value.normalized()
        if (!config.isValid || isSaving.value) {
            errorMessage.value = "Renseigne l'URL, l'identifiant et le mot de passe Xtream."
            return
        }

        viewModelScope.launch {
            isSaving.value = true
            errorMessage.value = null
            try {
                withContext(dispatcher) {
                    saveConfig(config)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                errorMessage.value = throwable.message
                    ?.takeIf(String::isNotBlank)
                    ?: "Impossible d'enregistrer la configuration Xtream."
            } finally {
                isSaving.value = false
            }
        }
    }

    private fun updateDraft(block: (XtreamConnectionConfig) -> XtreamConnectionConfig) {
        draft.value = block(draft.value)
        errorMessage.value = null
    }
}

data class ConfigUiState(
    val isConfigured: Boolean = false,
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val canSave: Boolean = false,
)
