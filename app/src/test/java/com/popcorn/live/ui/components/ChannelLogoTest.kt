package com.popcorn.live.ui.components

import android.graphics.Bitmap
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class ChannelLogoTest {
    @Test
    fun decodeBoundedChannelLogoScalesLargeImage() {
        val source = Bitmap.createBitmap(1_200, 600, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        source.compress(Bitmap.CompressFormat.PNG, 100, output)

        val decoded = decodeBoundedChannelLogo(output.toByteArray(), maxDimension = 512)

        assertNotNull(decoded)
        assertEquals(512, decoded!!.width)
        assertTrue(decoded.height <= 512)
    }

    @Test
    fun readAtMostReturnsBytesWhenStreamFitsLimit() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        val result = ByteArrayInputStream(bytes).readAtMost(maxBytes = 4)

        assertArrayEquals(bytes, result)
    }

    @Test
    fun readAtMostRejectsStreamsAboveLimit() {
        val result = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5))
            .readAtMost(maxBytes = 4)

        assertNull(result)
    }
}
