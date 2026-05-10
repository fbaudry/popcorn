package com.popcorn.live.di

import android.content.Context
import com.popcorn.live.config.XtreamConnectionConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppContainerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("popcorn_xtream_config", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.deleteDatabase("popcorn.db")
        context.deleteDatabase("popcorn-media.db")
        context.deleteDatabase("popcorn-user.db")
    }

    @Test
    fun missingConfigDoesNotCreateXtreamServices() {
        val container = AppContainer(context)

        assertThrows(IllegalStateException::class.java) {
            container.requireXtreamServices()
        }
    }

    @Test
    fun savedConfigCreatesXtreamUrlFactory() = runTest {
        val container = AppContainer(context)

        container.saveXtreamConnectionConfig(
            XtreamConnectionConfig(
                baseUrl = " https://iptv.example.com/// ",
                username = "Mike",
                password = "1234",
            ),
        )

        assertEquals(
            "https://iptv.example.com/live/Mike/1234/42.m3u8",
            container.requireXtreamServices().xtreamUrlFactory.hlsPlaybackUrl(streamId = 42),
        )
    }
}
