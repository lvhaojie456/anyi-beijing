package com.anyi.memorial.network

import com.anyi.memorial.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class UploadPayload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

class AnyiApiClient(
    private val baseUrl: String = BuildConfig.API_BASE_URL,
    private val tokenProvider: () -> String? = { null }
) {
    private val baseUrls = apiBaseUrls(baseUrl)

    fun health(): JSONObject {
        return request(method = "GET", path = "/health")
    }

    fun register(username: String, password: String, displayName: String): JSONObject {
        return request(
            method = "POST",
            path = "/auth/register",
            body = JSONObject()
                .put("username", username)
                .put("password", password)
                .put("displayName", displayName)
        )
    }

    fun login(username: String, password: String): JSONObject {
        return request(
            method = "POST",
            path = "/auth/login",
            body = JSONObject()
                .put("username", username)
                .put("password", password)
        )
    }

    fun loginWithWechat(code: String): JSONObject {
        return request(
            method = "POST",
            path = "/auth/wechat",
            body = JSONObject().put("code", code)
        )
    }

    fun currentUser(): JSONObject {
        return request(method = "GET", path = "/me", authorized = true)
    }

    fun updateCurrentUser(displayName: String, avatarUrl: String?): JSONObject {
        return request(
            method = "PATCH",
            path = "/me",
            authorized = true,
            body = JSONObject()
                .put("displayName", displayName)
                .put("avatarUrl", avatarUrl)
        )
    }

    fun deleteAccount(): JSONObject {
        return request(method = "DELETE", path = "/me", authorized = true)
    }

    fun reportCrash(
        platform: String,
        appVersion: String,
        deviceModel: String,
        osVersion: String,
        errorType: String,
        message: String?,
        stackTrace: String?
    ): JSONObject {
        return request(
            method = "POST",
            path = "/crash-reports",
            authorized = !tokenProvider().isNullOrBlank(),
            body = JSONObject()
                .put("platform", platform)
                .put("appVersion", appVersion)
                .put("deviceModel", deviceModel)
                .put("osVersion", osVersion)
                .put("errorType", errorType)
                .put("message", message)
                .put("stackTrace", stackTrace)
        )
    }

    fun listMemorials(): JSONArray {
        return request(method = "GET", path = "/memorials", authorized = true).getJSONArray("memorials")
    }

    fun createMemorial(name: String, imageUrl: String?): JSONObject {
        return request(
            method = "POST",
            path = "/memorials",
            authorized = true,
            body = JSONObject()
                .put("name", name)
                .put("imageUrl", imageUrl)
        )
    }

    fun updateMemorial(id: String, name: String, imageUrl: String?): JSONObject {
        return request(
            method = "PATCH",
            path = "/memorials/$id",
            authorized = true,
            body = JSONObject()
                .put("name", name)
                .put("imageUrl", imageUrl)
        )
    }

    fun offerFlower(memorialId: String, flowerType: String): JSONObject {
        return request(
            method = "POST",
            path = "/memorials/$memorialId/flowers",
            authorized = true,
            body = JSONObject().put("type", flowerType)
        )
    }

    fun lightCandle(memorialId: String): JSONObject {
        return request(method = "POST", path = "/memorials/$memorialId/candle", authorized = true)
    }

    fun lightIncense(memorialId: String): JSONObject {
        return request(method = "POST", path = "/memorials/$memorialId/incense", authorized = true)
    }

    fun offerFruit(memorialId: String, fruitType: String): JSONObject {
        return request(
            method = "POST",
            path = "/memorials/$memorialId/fruits",
            authorized = true,
            body = JSONObject().put("type", fruitType)
        )
    }

    fun listCommunityPosts(): JSONArray {
        return request(method = "GET", path = "/community/posts", authorized = true)
            .getJSONArray("posts")
    }

    fun createCommunityPost(content: String, imageUrls: List<String> = emptyList()): JSONObject {
        return request(
            method = "POST",
            path = "/community/posts",
            authorized = true,
            body = JSONObject()
                .put("content", content)
                .put("imageUrls", JSONArray(imageUrls))
        ).getJSONObject("post")
    }

    fun communityVolunteerInfo(): JSONObject {
        return request(method = "GET", path = "/community/volunteer", authorized = true)
    }

    fun uploadAsset(scope: String, fileName: String, mimeType: String, bytes: ByteArray): JSONObject {
        return multipartRequest(
            path = "/assets",
            fields = mapOf("scope" to scope),
            files = listOf(UploadPayload(fileName, mimeType, bytes))
        )
    }

    fun featureUnlocked(feature: String): Boolean {
        return request(
            method = "GET",
            path = "/feature-unlocks/$feature",
            authorized = true
        ).optBoolean("unlocked", false)
    }

    fun unlockFeature(feature: String): JSONObject {
        return request(method = "POST", path = "/feature-unlocks/$feature", authorized = true)
    }

    fun aiProfile(): JSONObject {
        return request(method = "GET", path = "/ai/profile", authorized = true).getJSONObject("profile")
    }

    fun listAiCompanions(): JSONArray {
        return request(method = "GET", path = "/ai/companions", authorized = true).getJSONArray("companions")
    }

    fun createAiCompanion(displayName: String, gender: String, relation: String): JSONObject {
        return request(
            method = "POST",
            path = "/ai/companions",
            authorized = true,
            body = JSONObject()
                .put("displayName", displayName)
                .put("gender", gender)
                .put("relation", relation)
        ).getJSONObject("companion")
    }

    fun updateAiCompanion(
        companionId: String,
        displayName: String,
        gender: String,
        relation: String,
        generated: Boolean? = null
    ): JSONObject {
        val body = JSONObject()
            .put("displayName", displayName)
            .put("gender", gender)
            .put("relation", relation)
        if (generated != null) {
            body.put("generated", generated)
        }
        return request(method = "PATCH", path = "/ai/companions/$companionId", authorized = true, body = body)
            .getJSONObject("companion")
    }

    fun updateAiProfile(gender: String, relation: String, generated: Boolean? = null): JSONObject {
        val body = JSONObject()
            .put("gender", gender)
            .put("relation", relation)
        if (generated != null) {
            body.put("generated", generated)
        }
        return request(method = "PATCH", path = "/ai/profile", authorized = true, body = body)
            .getJSONObject("profile")
    }

    fun unlockAi(): JSONObject {
        return request(method = "POST", path = "/ai/unlock", authorized = true).getJSONObject("profile")
    }

    fun unlockAiCompanion(companionId: String): JSONObject {
        return request(method = "POST", path = "/ai/companions/$companionId/unlock", authorized = true)
            .getJSONObject("companion")
    }

    fun uploadAiAsset(kind: String, fileName: String, mimeType: String, bytes: ByteArray): JSONObject {
        return multipartRequest(
            path = "/ai/assets",
            fields = mapOf("kind" to kind),
            files = listOf(UploadPayload(fileName, mimeType, bytes))
        )
    }

    fun uploadAiCompanionAsset(companionId: String, kind: String, fileName: String, mimeType: String, bytes: ByteArray): JSONObject {
        return multipartRequest(
            path = "/ai/companions/$companionId/assets",
            fields = mapOf("kind" to kind),
            files = listOf(UploadPayload(fileName, mimeType, bytes))
        )
    }

    fun listAiMessages(): JSONArray {
        return request(method = "GET", path = "/ai/messages", authorized = true).getJSONArray("messages")
    }

    fun listAiMessages(companionId: String): JSONArray {
        return request(method = "GET", path = "/ai/companions/$companionId/messages", authorized = true)
            .getJSONArray("messages")
    }

    fun sendAiMessage(content: String): JSONArray {
        return request(
            method = "POST",
            path = "/ai/messages",
            authorized = true,
            body = JSONObject().put("content", content)
        ).getJSONArray("messages")
    }

    fun sendAiMessage(companionId: String, content: String): JSONArray {
        return request(
            method = "POST",
            path = "/ai/companions/$companionId/messages",
            authorized = true,
            body = JSONObject().put("content", content)
        ).getJSONArray("messages")
    }

    private fun request(
        method: String,
        path: String,
        authorized: Boolean = false,
        body: JSONObject? = null
    ): JSONObject {
        return withApiFallback { currentBaseUrl ->
            val connection = openConnection(currentBaseUrl, path, method, authorized)
            try {
                if (body != null) {
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.doOutput = true
                    connection.outputStream.use { output ->
                        output.write(body.toString().toByteArray(Charsets.UTF_8))
                    }
                }
                parseResponse(connection)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun openConnection(currentBaseUrl: String, path: String, method: String, authorized: Boolean): HttpURLConnection {
        val url = URL("${currentBaseUrl.trimEnd('/')}$path")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = readTimeoutFor(path, method)
            setRequestProperty("Accept", "application/json")
        }
        if (authorized) {
            val token = tokenProvider()
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
        }
        return connection
    }

    private fun multipartRequest(
        path: String,
        fields: Map<String, String>,
        files: List<UploadPayload>
    ): JSONObject {
        return withApiFallback { currentBaseUrl ->
            val boundary = "AnyiBoundary${UUID.randomUUID()}"
            val connection = openConnection(currentBaseUrl, path, method = "POST", authorized = true)
            try {
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.doOutput = true

                DataOutputStream(connection.outputStream).use { stream ->
                    fields.forEach { (name, value) ->
                        stream.writeBytes("--$boundary\r\n")
                        stream.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                        stream.writeBytes(value)
                        stream.writeBytes("\r\n")
                    }
                    files.forEach { file ->
                        stream.writeBytes("--$boundary\r\n")
                        stream.writeBytes(
                            "Content-Disposition: form-data; name=\"file\"; filename=\"${file.fileName}\"\r\n"
                        )
                        stream.writeBytes("Content-Type: ${file.mimeType}\r\n\r\n")
                        stream.write(file.bytes)
                        stream.writeBytes("\r\n")
                    }
                    stream.writeBytes("--$boundary--\r\n")
                }

                parseResponse(connection)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseResponse(connection: HttpURLConnection): JSONObject {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val json = if (text.isBlank()) {
            JSONObject()
        } else {
            runCatching { JSONObject(text) }.getOrElse { JSONObject().put("error", text) }
        }
        if (status !in 200..299) {
            throw AnyiApiException(status, json.optString("error", "request_failed"), json)
        }
        return json
    }

    private inline fun withApiFallback(block: (String) -> JSONObject): JSONObject {
        var lastFailure: Throwable? = null
        baseUrls.forEachIndexed { index, currentBaseUrl ->
            try {
                return block(currentBaseUrl)
            } catch (error: Throwable) {
                if (index == baseUrls.lastIndex || !shouldTryNextApi(error)) {
                    throw error
                }
                lastFailure = error
            }
        }
        throw lastFailure ?: IllegalStateException("no_api_base_url")
    }

    private fun shouldTryNextApi(error: Throwable): Boolean {
        return error is IOException ||
            (error is AnyiApiException && error.statusCode in listOf(502, 503, 504))
    }

    private fun readTimeoutFor(path: String, method: String): Int {
        return when {
            path == "/ai/messages" && method == "POST" -> 65_000
            path.startsWith("/ai/companions/") && path.endsWith("/messages") && method == "POST" -> 65_000
            path == "/assets" || path == "/ai/assets" || path.endsWith("/acceptance") -> 120_000
            path.startsWith("/ai/companions/") && path.endsWith("/assets") -> 120_000
            else -> 30_000
        }
    }

    private fun apiBaseUrls(primary: String): List<String> {
        val configured = BuildConfig.API_BASE_URLS
            .split(',')
            .map { it.trim().trimEnd('/') }
            .filter { it.startsWith("https://") || it.startsWith("http://") }
        val candidates = if (primary == BuildConfig.API_BASE_URL) {
            configured
        } else {
            listOf(primary.trim().trimEnd('/')) + configured
        }
        return candidates.distinct().ifEmpty { listOf(primary.trim().trimEnd('/')) }
    }
}

class AnyiApiException(
    val statusCode: Int,
    val code: String,
    val payload: JSONObject
) : RuntimeException(code)
