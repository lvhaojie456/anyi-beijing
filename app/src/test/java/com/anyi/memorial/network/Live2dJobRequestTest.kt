package com.anyi.memorial.network

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test

class Live2dJobRequestTest {
    @Test
    fun jobCreationUsesStableIdempotencyKeyAndImageMultipart() {
        val id="0533712f-e198-4867-abcd-664a0f65fac6"
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        var key=""; var token=""; var body=""
        server.createContext("/ai/companions/$id/live2d/jobs") { exchange ->
            key=exchange.requestHeaders.getFirst("Idempotency-Key")
            token=exchange.requestHeaders.getFirst("Authorization")
            body=exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            val result="""{"job":{"id":"$id","status":"queued"}}""".toByteArray()
            exchange.sendResponseHeaders(202,result.size.toLong())
            exchange.responseBody.use { it.write(result) };exchange.close()
        }
        server.start()
        try {
            val api=AnyiApiClient("http://127.0.0.1:${server.address.port}") { "test-token" }
            val result=api.createLive2dJob(id,"",UploadPayload("source.png","image/png",byteArrayOf(1,2,3)),id)
            assertEquals("queued",result.getString("status"))
            assertEquals(id,key);assertEquals("Bearer test-token",token)
            assertTrue(body.contains("filename=\"source.png\""))
        } finally { server.stop(0) }
    }

    @Test
    fun privateFilesRejectTraversalAndDoNotForwardCredentialsOnRedirect() {
        val id="0533712f-e198-4867-abcd-664a0f65fac6"
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        var escaped=false
        server.createContext("/ai/live2d/jobs/$id/files/project.zip") { exchange ->
            exchange.responseHeaders.set("Location","/unexpected")
            exchange.sendResponseHeaders(302,-1);exchange.close()
        }
        server.createContext("/unexpected") { exchange -> escaped=true;exchange.sendResponseHeaders(200,-1);exchange.close() }
        server.start()
        try {
            val api=AnyiApiClient("http://127.0.0.1:${server.address.port}") { "private" }
            assertThrows(IllegalArgumentException::class.java) { api.readLive2dFile(id,"../secret") }
            assertThrows(Exception::class.java) { api.downloadLive2dFile(id,"project.zip",ByteArrayOutputStream()) }
            assertFalse(escaped)
        } finally { server.stop(0) }
    }
}
