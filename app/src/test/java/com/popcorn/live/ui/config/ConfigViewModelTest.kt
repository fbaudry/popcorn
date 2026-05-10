package com.popcorn.live.ui.config

import com.popcorn.live.config.XtreamConnectionConfig
import com.popcorn.live.config.XtreamConnectionConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigViewModelTest {
    @Test
    fun missingConfigExposesConfigurationRequiredState() = runTest {
        withMainDispatcher { dispatcher ->
            val repository = FakeXtreamConnectionConfigRepository()
            val viewModel = ConfigViewModel(
                configRepository = repository,
                saveConfig = repository::save,
                dispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isConfigured)
            assertFalse(viewModel.uiState.value.canSave)
        }
    }

    @Test
    fun existingConfigPrefillsDraftAndMarksAppConfigured() = runTest {
        withMainDispatcher { dispatcher ->
            val repository = FakeXtreamConnectionConfigRepository(
                initialConfig = XtreamConnectionConfig(
                    baseUrl = "https://iptv.example.com",
                    username = "Mike",
                    password = "1234",
                ),
            )
            val viewModel = ConfigViewModel(
                configRepository = repository,
                saveConfig = repository::save,
                dispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isConfigured)
            assertEquals("https://iptv.example.com", viewModel.uiState.value.baseUrl)
            assertEquals("Mike", viewModel.uiState.value.username)
            assertEquals("1234", viewModel.uiState.value.password)
        }
    }

    @Test
    fun savePersistsNormalizedConnectionConfig() = runTest {
        withMainDispatcher { dispatcher ->
            val repository = FakeXtreamConnectionConfigRepository()
            val viewModel = ConfigViewModel(
                configRepository = repository,
                saveConfig = repository::save,
                dispatcher = dispatcher,
            )

            viewModel.onBaseUrlChanged(" https://iptv.example.com:8080/// ")
            viewModel.onUsernameChanged(" Mike ")
            viewModel.onPasswordChanged(" 1234 ")
            viewModel.save()
            advanceUntilIdle()

            assertEquals(
                XtreamConnectionConfig(
                    baseUrl = "https://iptv.example.com:8080",
                    username = "Mike",
                    password = "1234",
                ),
                repository.savedConfig,
            )
            assertTrue(viewModel.uiState.value.isConfigured)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.withMainDispatcher(
    block: suspend TestScope.(TestDispatcher) -> Unit,
) {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
        block(dispatcher)
    } finally {
        Dispatchers.resetMain()
    }
}

private class FakeXtreamConnectionConfigRepository(
    initialConfig: XtreamConnectionConfig? = null,
) : XtreamConnectionConfigRepository {
    private val mutableConfig = MutableStateFlow(initialConfig)
    var savedConfig: XtreamConnectionConfig? = null
        private set

    override val config = mutableConfig

    override suspend fun save(config: XtreamConnectionConfig) {
        val normalizedConfig = config.normalized()
        savedConfig = normalizedConfig
        mutableConfig.value = normalizedConfig
    }
}
