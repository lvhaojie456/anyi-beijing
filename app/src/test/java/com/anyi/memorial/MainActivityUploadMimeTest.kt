package com.anyi.memorial

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityUploadMimeTest {
    @Test
    fun missingProviderMimeTypeUsesImageSignatureBeforeExtension() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)

        assertEquals("image/jpeg", inferUploadMimeType(null, "photo.bin", jpeg))
    }

    @Test
    fun genericProviderMimeTypeUsesFilenameWhenBytesAreNotReadableYet() {
        assertEquals("image/png", inferUploadMimeType("application/octet-stream", "portrait.png", byteArrayOf()))
    }

    @Test
    fun wildcardProviderMimeTypeUsesTheDetectedImageType() {
        val png = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)

        assertEquals("image/png", inferUploadMimeType("image/*", "portrait.data", png))
    }
}
