package com.anyi.memorial.network

import com.anyi.memorial.BuildConfig
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class UploadPayload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

class AnyiApiClient(
    private val baseUrl: String = BuildConfig.API_BASE_URL,
    private val tokenProvider: () -> String? = { null }
) {
    companion object {
        @Volatile
        private var unauthorizedHandler: (() -> Unit)? = null

        fun setUnauthorizedHandler(handler: (() -> Unit)?) {
            unauthorizedHandler = handler
        }
    }

    private val baseUrls = apiBaseUrls(baseUrl)

    fun health(): JSONObject {
        return request(method = "GET", path = "/health")
    }

    fun appConfig(): JSONObject {
        return request(method = "GET", path = "/app/config")
    }

    fun listAiCompanions(): JSONArray {
        return request(method = "GET", path = "/ai/companions", authorized = true)
            .getJSONArray("companions")
    }

    fun createAiCompanion(
        displayName: String,
        relation: String,
        avatarUrl: String? = null
    ): JSONObject {
        require(displayName.isNotBlank()) { "ai_companion_name_required" }
        require(relation.isNotBlank()) { "ai_companion_relation_required" }
        val body = JSONObject()
            .put("displayName", displayName)
            .put("relation", relation)
        if (!avatarUrl.isNullOrBlank()) body.put("avatarUrl", avatarUrl)
        return request(
            method = "POST",
            path = "/ai/companions",
            authorized = true,
            body = body
        )
    }

    fun updateAiCompanion(
        companionId: String,
        displayName: String,
        relation: String,
        avatarUrl: String? = null
    ): JSONObject {
        require(displayName.isNotBlank()) { "ai_companion_name_required" }
        require(relation.isNotBlank()) { "ai_companion_relation_required" }
        val body = JSONObject()
            .put("displayName", displayName)
            .put("relation", relation)
        if (!avatarUrl.isNullOrBlank()) body.put("avatarUrl", avatarUrl)
        return request(
            method = "PATCH",
            path = "/ai/companions/$companionId",
            authorized = true,
            body = body
        )
    }

    fun deleteAiCompanion(companionId: String): JSONObject {
        return request(
            method = "DELETE",
            path = "/ai/companions/$companionId",
            authorized = true
        )
    }

    fun uploadAiCompanionAvatar(
        companionId: String,
        file: UploadPayload,
        uploadRequestId: String? = null
    ): JSONObject {
        val fields = if (uploadRequestId.isNullOrBlank()) {
            emptyMap()
        } else {
            mapOf("uploadRequestId" to uploadRequestId)
        }
        return multipartRequest(
            path = "/ai/companions/$companionId/avatar",
            fields = fields,
            files = listOf(file),
            idempotencyKey = uploadRequestId
        )
    }

    fun updateAiCompanionChatBackground(companionId: String, backgroundUrl: String?): JSONObject {
        return request(
            method = "PATCH",
            path = "/ai/companions/$companionId/background",
            authorized = true,
            body = JSONObject().put("backgroundUrl", backgroundUrl)
        )
    }

    fun updateAiCompanionLive2dModel(companionId: String, live2dModel: String?): JSONObject {
        return request(
            method = "PATCH",
            path = "/ai/companions/$companionId/live2d",
            authorized = true,
            body = JSONObject().put("live2dModel", live2dModel ?: JSONObject.NULL)
        )
    }

    fun uploadAiCompanionChatBackground(companionId: String, file: UploadPayload): JSONObject {
        return multipartRequest(
            path = "/ai/companions/$companionId/background",
            fields = emptyMap(),
            files = listOf(file)
        )
    }

    fun listAiCompanionMessages(companionId: String): JSONArray {
        return request(
            method = "GET",
            path = "/ai/companions/$companionId/messages",
            authorized = true
        ).getJSONArray("messages")
    }

    fun sendAiCompanionMessage(companionId: String, content: String): JSONObject {
        return request(
            method = "POST",
            path = "/ai/companions/$companionId/messages",
            authorized = true,
            body = JSONObject().put("content", content)
        )
    }

    fun sendAiCompanionVoiceMessage(
        companionId: String,
        audio: UploadPayload,
        durationMs: Long,
        uploadRequestId: String
    ): JSONObject {
        require(durationMs > 0L) { "voice_duration_invalid" }
        require(uploadRequestId.isNotBlank()) { "upload_request_id_required" }
        return multipartRequest(
            path = "/ai/companions/$companionId/voice-messages",
            fields = mapOf(
                "durationMs" to durationMs.toString(),
                "uploadRequestId" to uploadRequestId
            ),
            files = listOf(audio),
            idempotencyKey = uploadRequestId
        )
    }

    fun listAiCompanionMemories(companionId: String): JSONArray {
        return request(
            method = "GET",
            path = "/ai/companions/$companionId/memories",
            authorized = true
        ).getJSONArray("memories")
    }

    fun createAiCompanionMemory(companionId: String, content: String): JSONObject {
        return request(
            method = "POST",
            path = "/ai/companions/$companionId/memories",
            authorized = true,
            body = JSONObject().put("content", content)
        )
    }

    fun deleteAiCompanionMemory(companionId: String, memoryId: String): JSONObject {
        return request(
            method = "DELETE",
            path = "/ai/companions/$companionId/memories/$memoryId",
            authorized = true
        )
    }

    fun listAiImageModels(): JSONArray {
        val response = request(method = "GET", path = "/ai/image-models", authorized = true)
        return response.optJSONArray("models")
            ?: response.optJSONArray("imageModels")
            ?: JSONArray()
    }

    fun createAiAvatarStudioImage(
        companionId: String,
        model: String,
        prompt: String,
        useCurrentAvatar: Boolean,
        file: UploadPayload? = null
    ): JSONObject {
        require(model.isNotBlank()) { "ai_image_model_required" }
        require(prompt.isNotBlank()) { "ai_avatar_prompt_required" }
        return multipartRequest(
            path = "/ai/companions/$companionId/avatar/studio",
            fields = mapOf(
                "model" to model,
                "prompt" to prompt,
                "useCurrentAvatar" to useCurrentAvatar.toString()
            ),
            files = listOfNotNull(file)
        )
    }

    fun register(
        username: String,
        password: String,
        displayName: String,
        gender: String,
        avatar: UploadPayload,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean
    ): JSONObject {
        val normalizedGender = normalizeUserGender(gender)
        require(displayName.isNotBlank()) { "display_name_required" }
        return multipartRequest(
            path = "/auth/register",
            fields = mapOf(
                "username" to username,
                "password" to password,
                "displayName" to displayName,
                "gender" to normalizedGender,
                "acceptedTerms" to acceptedTerms.toString(),
                "acceptedPrivacy" to acceptedPrivacy.toString()
            ),
            files = listOf(avatar),
            authorized = false
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

    fun loginWithWechat(
        code: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean
    ): JSONObject {
        return request(
            method = "POST",
            path = "/auth/wechat",
            body = JSONObject()
                .put("code", code)
                .put("acceptedTerms", acceptedTerms)
                .put("acceptedPrivacy", acceptedPrivacy)
        )
    }

    fun currentUser(): JSONObject {
        return request(method = "GET", path = "/me", authorized = true)
    }

    fun updateCurrentUser(displayName: String, avatarUrl: String?, gender: String? = null): JSONObject {
        val body = JSONObject()
            .put("displayName", displayName)
            .put("avatarUrl", avatarUrl)
        gender?.let { body.put("gender", normalizeUserGender(it)) }
        return request(
            method = "PATCH",
            path = "/me",
            authorized = true,
            body = body
        )
    }

    fun updateAiCompanionListBackground(backgroundUrl: String?): JSONObject {
        return request(
            method = "PATCH",
            path = "/me/ai-companion-background",
            authorized = true,
            body = JSONObject().put("backgroundUrl", backgroundUrl)
        )
    }

    fun uploadAiCompanionListBackground(file: UploadPayload): JSONObject {
        return multipartRequest(
            path = "/me/ai-companion-background",
            fields = emptyMap(),
            files = listOf(file)
        )
    }

    fun listCommunityPosts(): JSONArray {
        return request(method = "GET", path = "/community/posts", authorized = true)
            .getJSONArray("posts")
    }

    fun createCommunityPost(
        content: String,
        imageUrls: List<String> = emptyList(),
        idempotencyKey: String? = null
    ): JSONObject {
        return request(
            method = "POST",
            path = "/community/posts",
            authorized = true,
            body = JSONObject()
                .put("content", content)
                .put("imageUrls", JSONArray(imageUrls)),
            idempotencyKey = idempotencyKey
        )
    }

    fun deleteCommunityPost(postId: String): JSONObject {
        return request(
            method = "DELETE",
            path = "/community/posts/$postId",
            authorized = true
        )
    }

    fun likeCommunityPost(postId: String): JSONObject {
        return request(
            method = "POST",
            path = "/community/posts/$postId/like",
            authorized = true
        )
    }

    fun listCommunityPostComments(postId: String): JSONArray {
        return request(
            method = "GET",
            path = "/community/posts/$postId/comments",
            authorized = true
        )
            .getJSONArray("comments")
    }

    fun createCommunityPostComment(postId: String, content: String): JSONObject {
        return request(
            method = "POST",
            path = "/community/posts/$postId/comments",
            authorized = true,
            body = JSONObject().put("content", content)
        )
    }

    fun deleteCommunityPostComment(postId: String, commentId: String): JSONObject {
        return request(
            method = "DELETE",
            path = "/community/posts/$postId/comments/$commentId",
            authorized = true
        )
    }

    fun reportCommunityContent(targetType: String, targetId: String, reason: String): JSONObject {
        return request(
            method = "POST",
            path = "/community/reports",
            authorized = true,
            body = JSONObject()
                .put("targetType", targetType)
                .put("targetId", targetId)
                .put("reason", reason)
        )
    }

    fun communityVolunteerInfo(): JSONObject {
        return request(method = "GET", path = "/community/volunteer", authorized = true)
    }

    fun createCommunityVolunteer(
        title: String,
        body: String,
        contact: String,
        imageUrl: String? = null,
        deadlineAt: String? = null,
        volunteerCreateRequestId: String? = null
    ): JSONObject {
        val requestBody = JSONObject()
            .put("title", title)
            .put("body", body)
            .put("contact", contact)
        if (!imageUrl.isNullOrBlank()) {
            requestBody.put("imageUrl", imageUrl)
        }
        if (!deadlineAt.isNullOrBlank()) {
            requestBody.put("deadlineAt", deadlineAt)
        }
        if (!volunteerCreateRequestId.isNullOrBlank()) {
            requestBody.put("volunteerCreateRequestId", volunteerCreateRequestId)
        }
        return request(
            method = "POST",
            path = "/community/volunteer",
            authorized = true,
            body = requestBody,
            idempotencyKey = volunteerCreateRequestId
        ).getJSONObject("volunteer")
    }

    fun applyForCommunityVolunteer(
        volunteerId: String,
        name: String,
        phone: String,
        note: String
    ): JSONObject {
        return request(
            method = "POST",
            path = "/community/volunteer/$volunteerId/applications",
            authorized = true,
            body = JSONObject()
                .put("name", name)
                .put("phone", phone)
                .put("note", note)
        ).getJSONObject("application")
    }

    fun listCommunityVolunteerApplications(status: String = "all"): JSONArray {
        return request(
            method = "GET",
            path = "/community/volunteer/applications?status=$status",
            authorized = true
        ).getJSONArray("applications")
    }

    fun reviewCommunityVolunteerApplication(applicationId: String, status: String): JSONObject {
        return request(
            method = "PATCH",
            path = "/community/volunteer/applications/$applicationId",
            authorized = true,
            body = JSONObject().put("status", status)
        ).getJSONObject("application")
    }

    fun cancelCommunityVolunteerApplication(applicationId: String): JSONObject {
        return request(
            method = "DELETE",
            path = "/community/volunteer/applications/$applicationId",
            authorized = true
        ).getJSONObject("application")
    }

    fun updateCommunityVolunteerStatus(volunteerId: String, status: String): JSONObject {
        return request(
            method = "PATCH",
            path = "/community/volunteer/$volunteerId",
            authorized = true,
            body = JSONObject().put("status", status)
        ).getJSONObject("volunteer")
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
        return request(method = "GET", path = "/memorials", authorized = true)
            .getJSONArray("memorials")
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

    fun uploadAsset(
        scope: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        idempotencyKey: String? = null,
        uploadRequestId: String? = null
    ): JSONObject {
        val requestId = uploadRequestId ?: idempotencyKey
        val fields = mutableMapOf("scope" to scope)
        if (!uploadRequestId.isNullOrBlank()) {
            fields["uploadRequestId"] = uploadRequestId
        }
        return multipartRequest(
            path = "/assets",
            fields = fields,
            files = listOf(UploadPayload(fileName, mimeType, bytes)),
            idempotencyKey = requestId
        )
    }

    fun assetReviewStatus(assetId: String): JSONObject {
        require(assetId.matches(Regex("[A-Za-z0-9-]{1,80}"))) { "invalid_asset_id" }
        return request(
            method = "GET",
            path = "/asset-reviews/$assetId",
            authorized = true
        ).getJSONObject("review")
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

    private fun request(
        method: String,
        path: String,
        authorized: Boolean = false,
        body: JSONObject? = null,
        idempotencyKey: String? = null
    ): JSONObject {
        val execute: (String) -> JSONObject = { currentBaseUrl ->
            val connection = openConnection(currentBaseUrl, path, method, authorized)
            try {
                if (!idempotencyKey.isNullOrBlank()) {
                    connection.setRequestProperty("Idempotency-Key", idempotencyKey)
                }
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
        // Only replay safe reads against another configured endpoint. A write may have
        // reached the server before the connection failed, so replaying it can duplicate data.
        return if (method.equals("GET", ignoreCase = true) || method.equals("HEAD", ignoreCase = true)) {
            withApiFallback(execute)
        } else {
            execute(baseUrls.first())
        }
    }

    private fun openConnection(
        currentBaseUrl: String,
        path: String,
        method: String,
        authorized: Boolean
    ): HttpURLConnection {
        val url = URL("${currentBaseUrl.trimEnd('/')}$path")
        if (!BuildConfig.DEBUG && url.protocol != "https") {
            throw IOException("cleartext_api_blocked")
        }
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
        files: List<UploadPayload>,
        idempotencyKey: String? = null,
        authorized: Boolean = true
    ): JSONObject {
        val currentBaseUrl = baseUrls.first()
        val connection = openConnection(
            currentBaseUrl,
            path,
            method = "POST",
            authorized = authorized
        )
        return try {
            if (!idempotencyKey.isNullOrBlank()) {
                connection.setRequestProperty("Idempotency-Key", idempotencyKey)
            }
            val boundary = "AnyiBoundary${UUID.randomUUID()}"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.doOutput = true

            DataOutputStream(connection.outputStream).use { stream ->
                fields.forEach { (name, value) ->
                    stream.writeBytes("--$boundary\r\n")
                    stream.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    stream.write(value.toByteArray(Charsets.UTF_8))
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
            if (status == 401) {
                unauthorizedHandler?.invoke()
            }
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
            path == "/assets" || path.endsWith("/assets") || path.endsWith("/acceptance") -> 120_000
            path.endsWith("/messages") || path.endsWith("/voice-messages") || path.endsWith("/avatar") || path.endsWith("/avatar/studio") -> 120_000
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
        val secureCandidates = candidates
            .distinct()
            .filter { BuildConfig.DEBUG || it.startsWith("https://") }
        return secureCandidates.ifEmpty { listOf(primary.trim().trimEnd('/')) }
    }
}

private fun normalizeUserGender(value: String): String {
    return when (value.trim()) {
        "男" -> "男"
        "女" -> "女"
        else -> throw IllegalArgumentException("user_gender_invalid")
    }
}

class AnyiApiException(
    val statusCode: Int,
    val code: String,
    val payload: JSONObject
) : RuntimeException(code)
