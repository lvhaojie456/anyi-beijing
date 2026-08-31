package com.anyi.memorial.network

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAvatarStudioRequestTest {
    private var server: HttpServer? = null

    @After
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun avatarStudio_sendsUserPromptAndOptionalFileAsMultipart() {
        var capturedBody = ""
        val runningServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/ai/companions/person-1/avatar/studio") { exchange ->
                capturedBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                val response = """{"avatarUrl":"/assets/generated.png"}""".toByteArray()
                exchange.sendResponseHeaders(200, response.size.toLong())
                exchange.responseBody.use { it.write(response) }
                exchange.close()
            }
            start()
            server = this
        }

        AnyiApiClient(
            baseUrl = "http://127.0.0.1:${runningServer.address.port}",
            tokenProvider = { "token" }
        ).createAiAvatarStudioImage(
            companionId = "person-1",
            model = "image-model-1",
            prompt = "  保留用户原文  ",
            useCurrentAvatar = true,
            file = UploadPayload("reference.png", "image/png", byteArrayOf(1, 2, 3))
        )

        assertTrue(capturedBody.contains("name=\"model\"\r\n\r\nimage-model-1"))
        assertTrue(capturedBody.contains("name=\"prompt\"\r\n\r\n  保留用户原文  \r\n"))
        assertTrue(capturedBody.contains("name=\"useCurrentAvatar\"\r\n\r\ntrue"))
        assertTrue(capturedBody.contains("filename=\"reference.png\""))
    }
}
