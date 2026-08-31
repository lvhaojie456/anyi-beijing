package com.anyi.memorial.network

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationRequestTest {
    private var server: HttpServer? = null

    @After
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun registerSendsRequiredProfileAndAvatarAsMultipart() {
        var capturedBody = ""
        var authorization = ""
        val runningServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/auth/register") { exchange ->
                authorization = exchange.requestHeaders.getFirst("Authorization").orEmpty()
                capturedBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                val response = """{"user":{"id":"1","username":"tester","displayName":"吕豪杰","gender":"男","role":"user","avatarUrl":"/assets/avatar.png"},"token":"token"}""".toByteArray()
                exchange.sendResponseHeaders(201, response.size.toLong())
                exchange.responseBody.use { it.write(response) }
                exchange.close()
            }
            start()
            server = this
        }

        AnyiApiClient(
            baseUrl = "http://127.0.0.1:${runningServer.address.port}",
            tokenProvider = { "must-not-be-sent" }
        ).register(
            username = "tester",
            password = "password123",
            displayName = "吕豪杰",
            gender = "男",
            avatar = UploadPayload("avatar.png", "image/png", byteArrayOf(1, 2, 3)),
            acceptedTerms = true,
            acceptedPrivacy = true
        )

        assertFalse(capturedBody.isBlank())
        assertTrue(capturedBody.contains("name=\"displayName\"\r\n\r\n吕豪杰"))
        assertTrue(capturedBody.contains("name=\"gender\"\r\n\r\n男"))
        assertTrue(capturedBody.contains("name=\"acceptedTerms\"\r\n\r\ntrue"))
        assertTrue(capturedBody.contains("filename=\"avatar.png\""))
        assertTrue(authorization.isBlank())
    }
}
