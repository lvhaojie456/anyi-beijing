package com.anyi.memorial.network

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnyiApiClientRequestIdTest {
    private var server: HttpServer? = null

    @After
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun uploadAsset_sendsRequestIdAsMultipartFieldAndIdempotencyHeader() {
        var capturedHeader = ""
        var capturedBody = ""
        val runningServer = startServer("/assets", """{"asset":{"id":"asset-1"}}""") { exchange ->
            capturedHeader = exchange.requestHeaders.getFirst("Idempotency-Key").orEmpty()
            capturedBody = exchange.requestBody.readBytes().toString(Charsets.ISO_8859_1)
        }
        val requestId = "2be015f2-aefd-43ec-a193-a29bf3fa71ac"

        AnyiApiClient(
            baseUrl = "http://127.0.0.1:${runningServer.address.port}",
            tokenProvider = { "token" }
        ).uploadAsset(
            scope = "community/volunteer",
            fileName = "cover.jpg",
            mimeType = "image/jpeg",
            bytes = byteArrayOf(1, 2, 3),
            uploadRequestId = requestId
        )

        assertEquals(requestId, capturedHeader)
        assertTrue(capturedBody.contains("name=\"uploadRequestId\"\r\n\r\n$requestId"))
        assertTrue(capturedBody.contains("name=\"scope\"\r\n\r\ncommunity/volunteer"))
    }

    @Test
    fun createCommunityVolunteer_sendsRequestIdAsJsonFieldAndIdempotencyHeader() {
        var capturedHeader = ""
        var capturedBody = ""
        val runningServer = startServer(
            "/community/volunteer",
            """{"volunteer":{"id":"volunteer-1"}}"""
        ) { exchange ->
            capturedHeader = exchange.requestHeaders.getFirst("Idempotency-Key").orEmpty()
            capturedBody = exchange.requestBody.bufferedReader().use { it.readText() }
        }
        val requestId = "28b26761-fae7-412d-b2d4-a77ec2bbf1e1"

        AnyiApiClient(
            baseUrl = "http://127.0.0.1:${runningServer.address.port}",
            tokenProvider = { "token" }
        ).createCommunityVolunteer(
            title = "Volunteer title",
            body = "Volunteer description",
            contact = "Contact details",
            volunteerCreateRequestId = requestId
        )

        assertEquals(requestId, capturedHeader)
        assertEquals(requestId, JSONObject(capturedBody).getString("volunteerCreateRequestId"))
    }

    @Test
    fun uploadAiCompanionAvatar_sendsImageAndRequestId() {
        var capturedHeader = ""
        var capturedBody = ""
        val runningServer = startServer(
            "/ai/companions/companion-1/avatar",
            """{"asset":{"id":"asset-1","reviewStatus":"pending"},"companion":{"id":"companion-1"}}"""
        ) { exchange ->
            capturedHeader = exchange.requestHeaders.getFirst("Idempotency-Key").orEmpty()
            capturedBody = exchange.requestBody.readBytes().toString(Charsets.ISO_8859_1)
        }
        val requestId = "9d1b3a8e-7d47-4a4e-9d87-9ad0d1c5f782"

        AnyiApiClient(
            baseUrl = "http://127.0.0.1:${runningServer.address.port}",
            tokenProvider = { "token" }
        ).uploadAiCompanionAvatar(
            companionId = "companion-1",
            file = UploadPayload("avatar.png", "image/png", byteArrayOf(1, 2, 3)),
            uploadRequestId = requestId
        )

        assertEquals(requestId, capturedHeader)
        assertTrue(capturedBody.contains("name=\"uploadRequestId\"\r\n\r\n$requestId"))
        assertTrue(capturedBody.contains("filename=\"avatar.png\""))
        assertTrue(capturedBody.contains("Content-Type: image/png"))
    }

    @Test
    fun createAiCompanion_includesOptionalAvatarUrl() {
        var capturedBody = ""
        val runningServer = startServer(
            "/ai/companions",
            """{"companion":{"id":"companion-1"}}"""
        ) { exchange ->
            capturedBody = exchange.requestBody.bufferedReader().use { it.readText() }
        }
        val avatarUrl = "https://api.anyibj.cn/assets/user-1%2Fai%2Favatar%2Fone.png"

        AnyiApiClient(
            baseUrl = "http://127.0.0.1:${runningServer.address.port}",
            tokenProvider = { "token" }
        ).createAiCompanion("小猫", "宠物", avatarUrl)

        assertEquals(avatarUrl, JSONObject(capturedBody).getString("avatarUrl"))
    }

    @Test
    fun sendAiCompanionVoiceMessage_sendsAudioAndRequestMetadata() {
        var capturedHeader = ""
        var capturedBody = ""
        val runningServer = startServer(
            "/ai/companions/companion-1/voice-messages",
            """{"transcript":"你好","messages":[]}"""
        ) { exchange ->
            capturedHeader = exchange.requestHeaders.getFirst("Idempotency-Key").orEmpty()
            capturedBody = exchange.requestBody.readBytes().toString(Charsets.ISO_8859_1)
        }
        val requestId = "f1d2b4b2-3c95-41ec-a95a-3da8c7d85b2e"

        AnyiApiClient(
            baseUrl = "http://127.0.0.1:${runningServer.address.port}",
            tokenProvider = { "token" }
        ).sendAiCompanionVoiceMessage(
            companionId = "companion-1",
            audio = UploadPayload("voice.m4a", "audio/mp4", byteArrayOf(1, 2, 3)),
            durationMs = 2_100L,
            uploadRequestId = requestId
        )

        assertEquals(requestId, capturedHeader)
        assertTrue(capturedBody.contains("name=\"uploadRequestId\"\r\n\r\n$requestId"))
        assertTrue(capturedBody.contains("name=\"durationMs\"\r\n\r\n2100"))
        assertTrue(capturedBody.contains("filename=\"voice.m4a\""))
        assertTrue(capturedBody.contains("Content-Type: audio/mp4"))
    }

    private fun startServer(
        path: String,
        responseBody: String,
        capture: (HttpExchange) -> Unit
    ): HttpServer {
        return HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext(path) { exchange ->
                try {
                    capture(exchange)
                    val responseBytes = responseBody.toByteArray()
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, responseBytes.size.toLong())
                    exchange.responseBody.use { it.write(responseBytes) }
                } finally {
                    exchange.close()
                }
            }
            start()
            server = this
        }
    }

}
