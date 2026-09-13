package com.anyi.memorial

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anyi.memorial.network.AnyiApiClient
import com.anyi.memorial.network.UploadPayload
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private val CompanionBackground = Color(0xFFF5F5F7)
private val CompanionInk = Color(0xFF1D1D1F)
private val CompanionMuted = Color(0xFF6E6E73)
private val CompanionLine = Color(0xFFE1DCCF)
private val CompanionGreen = Color(0xFF9A6B2F)
private val CompanionPaper = Color(0xFFFFFEFB)
private val CompanionButtonShape = RoundedCornerShape(12.dp)
private const val voiceConsentStoreName = "anyi_voice_consent"
private const val voiceConsentKey = "voice_message_consent_2026_09_01"
private const val maxVoiceDurationMs = 60_000L

private fun hasVoiceMessageConsent(context: Context): Boolean =
    context.getSharedPreferences(voiceConsentStoreName, Context.MODE_PRIVATE)
        .getBoolean(voiceConsentKey, false)

private fun saveVoiceMessageConsent(context: Context): Boolean =
    context.getSharedPreferences(voiceConsentStoreName, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(voiceConsentKey, true)
        .commit()

internal data class AiCompanion(
    val id: String,
    val displayName: String,
    val relation: String,
    val chatBackgroundUrl: String?,
    val avatarUrl: String?,
    val generated: Boolean,
    val updatedAt: Long,
    val latestMessage: String,
    val latestMessageAt: Long?
)

internal data class AiConversationMessage(
    val id: String,
    val sender: String,
    val content: String,
    val createdAt: Long,
    val pending: Boolean = false,
    val failed: Boolean = false,
    val messageType: String = "text",
    val audioUrl: String? = null,
    val audioLocalPath: String? = null,
    val durationMs: Long? = null
)

private data class PendingAiMessage(
    val localId: String,
    val content: String,
    val audioLocalPath: String? = null,
    val durationMs: Long? = null,
    val uploadRequestId: String? = null
)

private data class VoiceRecording(
    val companionId: String,
    val file: File,
    val durationMs: Long
)

private class VoiceRecorder(private val context: android.content.Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var activeCompanionId: String? = null
    private var startedAt: Long = 0L

    fun start(companionId: String): Boolean {
        if (recorder != null) return false
        val file = runCatching { File.createTempFile("anyi_voice_", ".m4a", context.cacheDir) }.getOrNull()
            ?: return false
        val active = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        }.getOrElse {
            file.delete()
            return false
        }
        return runCatching {
            active.setAudioSource(MediaRecorder.AudioSource.MIC)
            active.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            active.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            active.setAudioSamplingRate(16_000)
            active.setAudioEncodingBitRate(64_000)
            active.setOutputFile(file.absolutePath)
            active.prepare()
            active.start()
            recorder = active
            outputFile = file
            activeCompanionId = companionId
            startedAt = SystemClock.elapsedRealtime()
            true
        }.getOrElse {
            runCatching { active.reset() }
            runCatching { active.release() }
            file.delete()
            false
        }
    }

    fun stop(cancel: Boolean = false): VoiceRecording? {
        val active = recorder ?: return null
        recorder = null
        val file = outputFile
        outputFile = null
        val companionId = activeCompanionId
        activeCompanionId = null
        // The auto-stop coroutine can run a few milliseconds late. Clamp the
        // declared value to the server's inclusive 60-second limit so a valid
        // recording is not rejected merely because the scheduler woke up late.
        val duration = (SystemClock.elapsedRealtime() - startedAt)
            .coerceIn(1L, maxVoiceDurationMs)
        return runCatching {
            active.stop()
            active.release()
            if (cancel || file == null || companionId == null || duration < 1_000L) {
                file?.delete()
                null
            } else {
                VoiceRecording(companionId = companionId, file = file, durationMs = duration)
            }
        }.getOrElse {
            runCatching { active.release() }
            file?.delete()
            null
        }
    }

    fun cancel() { stop(cancel = true) }
}

/** A single player shared by the chat screen, matching WeChat's one-at-a-time playback. */
private class VoicePlaybackController {
    var playingMessageId by mutableStateOf<String?>(null)
        private set
    private var player: MediaPlayer? = null

    fun toggle(context: Context, token: String, message: AiConversationMessage, onError: (String) -> Unit) {
        if (playingMessageId == message.id) {
            stop()
            return
        }
        stop()
        val localPath = message.audioLocalPath?.takeIf { it.isNotBlank() }
        val remoteUrl = message.audioUrl?.takeIf { it.isNotBlank() }?.let(::absoluteVoiceAssetUrl)
        if (localPath == null && remoteUrl == null) {
            onError("这条语音暂时无法播放")
            return
        }
        runCatching {
            val next = MediaPlayer()
            if (localPath != null) {
                next.setDataSource(localPath)
            } else {
                next.setDataSource(
                    context,
                    Uri.parse(remoteUrl),
                    mapOf("Authorization" to "Bearer $token")
                )
            }
            next.setOnCompletionListener {
                if (player === next) {
                    player = null
                    playingMessageId = null
                    runCatching { next.release() }
                }
            }
            next.setOnErrorListener { _, _, _ ->
                val isCurrent = player === next
                if (isCurrent) {
                    player = null
                    playingMessageId = null
                }
                runCatching { next.release() }
                if (isCurrent) onError("语音播放失败，请稍后重试")
                true
            }
            player = next
            playingMessageId = message.id
            next.setOnPreparedListener {
                if (player === next) {
                    next.start()
                } else {
                    runCatching { next.release() }
                }
            }
            next.prepareAsync()
        }.onFailure {
            stop()
            onError("语音播放失败，请稍后重试")
        }
    }

    fun stop() {
        val active = player
        player = null
        playingMessageId = null
        if (active != null) {
            runCatching { active.stop() }
            runCatching { active.release() }
        }
    }
}

private fun absoluteVoiceAssetUrl(value: String): String {
    return when {
        value.startsWith("/") -> "${BuildConfig.API_BASE_URL.trimEnd('/')}$value"
        else -> value
    }
}

private data class AiConversationState(
    val messages: List<AiConversationMessage> = emptyList(),
    val draft: String = "",
    val pending: List<PendingAiMessage> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String = ""
)

private const val maxQueuedAiMessages = 20

private data class AiCompanionMemory(val id: String, val content: String)
private data class AiImageModel(val id: String, val name: String, val description: String)
private data class AvatarStudioMessage(
    val id: String = UUID.randomUUID().toString(),
    val mine: Boolean,
    val text: String,
    val attachmentName: String? = null,
    val imageUrl: String? = null
)

@Composable
internal fun AiCompanionScreen(
    user: AppUser,
    onBack: () -> Unit,
    onChatStateChange: (Boolean) -> Unit = {},
    onUserChanged: (AppUser) -> Unit = {}
) {
    val context = LocalContext.current
    val api = remember(user.token) { AnyiApiClient(tokenProvider = { user.token }) }
    val scope = rememberCoroutineScope()
    var companions by remember(user.token) { mutableStateOf(emptyList<AiCompanion>()) }
    var selectedId by rememberSaveable(user.token) { mutableStateOf<String?>(null) }
    var studioTarget by remember { mutableStateOf<AiCompanion?>(null) }
    var conversationStates by remember(user.token) { mutableStateOf(emptyMap<String, AiConversationState>()) }
    val workerJobs = remember(user.token) { mutableMapOf<String, Job>() }
    val historyRequests = remember(user.token) { mutableMapOf<String, String>() }
    // Kept outside the token-keyed state so logout/account switches can still
    // clean recordings that belonged to the previous account.
    val localVoicePaths = remember { mutableSetOf<String>() }
    var listLoading by remember { mutableStateOf(true) }
    var listError by remember { mutableStateOf("") }
    var editorTarget by remember { mutableStateOf<AiCompanion?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var listBackgroundSaving by remember { mutableStateOf(false) }
    var chatBackgroundSaving by remember { mutableStateOf(false) }
    var voiceAvailable by remember(user.token) { mutableStateOf(false) }
    val selected = selectedId?.let { id -> companions.firstOrNull { it.id == id } }

    fun conversationState(companionId: String): AiConversationState {
        return conversationStates[companionId] ?: AiConversationState()
    }

    fun updateConversationState(
        companionId: String,
        transform: (AiConversationState) -> AiConversationState
    ) {
        val current = conversationStates[companionId] ?: AiConversationState()
        conversationStates = conversationStates + (companionId to transform(current))
    }

    fun deleteLocalVoice(path: String?) {
        if (path.isNullOrBlank()) return
        File(path).delete()
        localVoicePaths.remove(path)
    }

    val selectedConversation = selectedId?.let(::conversationState) ?: AiConversationState()
    val latestConversationStates by rememberUpdatedState(conversationStates)

    fun replaceCompanion(updated: AiCompanion) {
        companions = (companions.filterNot { it.id == updated.id } + updated)
            .sortedByDescending { it.updatedAt }
        if (studioTarget?.id == updated.id) studioTarget = updated
    }

    fun refreshCompanions() {
        listLoading = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { api.listAiCompanions() } }
                .onSuccess { companions = parseCompanions(it); listError = "" }
                .onFailure { listError = it.companionError("陪伴对象加载失败") }
            listLoading = false
        }
    }

    LaunchedEffect(user.token) {
        refreshCompanions()
        voiceAvailable = runCatching {
            withContext(Dispatchers.IO) {
                val config = api.appConfig()
                val voice = config.optJSONObject("ai")?.optJSONObject("voice")
                voice?.optBoolean("enabled", false) == true &&
                    voice.optBoolean("asrConfigured", false)
            }
        }.getOrDefault(false)
    }

    DisposableEffect(user.token) {
        onDispose {
            workerJobs.values.toList().forEach { it.cancel() }
            workerJobs.clear()
            historyRequests.clear()
            // Voice files are kept in the app cache when the server is configured
            // not to retain audio. Remove all completed and queued local copies
            // when the account scope leaves (logout/account switch), so another
            // account on the same device cannot recover a prior user's recording.
            val localPathsToDelete = localVoicePaths.toMutableSet()
            latestConversationStates.values
                .flatMap { it.messages }
                .mapNotNull { it.audioLocalPath }
                .forEach { localPathsToDelete += it }
            localPathsToDelete.forEach { path -> File(path).delete() }
            localVoicePaths.clear()
        }
    }

    fun loadConversation(companion: AiCompanion) {
        val companionId = companion.id
        val current = conversationState(companionId)
        if (current.loaded || current.loading) return
        val requestId = UUID.randomUUID().toString()
        historyRequests[companionId] = requestId
        updateConversationState(companionId) { it.copy(loading = true, error = "") }
        scope.launch {
            val result = try {
                Result.success(withContext(Dispatchers.IO) { api.listAiCompanionMessages(companionId) })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                Result.failure<JSONArray>(failure)
            }
            if (historyRequests[companionId] != requestId) return@launch
            historyRequests.remove(companionId)
            result
                .onSuccess { response ->
                    val serverMessages = parseMessages(response)
                    updateConversationState(companionId) { state ->
                        state.copy(
                            messages = mergeAiMessages(serverMessages, state.messages),
                            loading = false,
                            loaded = true,
                            error = ""
                        )
                    }
                }
                .onFailure { failure ->
                    updateConversationState(companionId) {
                        it.copy(loading = false, error = failure.companionError("聊天记录加载失败"))
                    }
                }
        }
    }

    LaunchedEffect(selectedId, companions) {
        selected?.let(::loadConversation)
    }

    fun ensureMessageWorker(companionId: String) {
        if (workerJobs[companionId]?.isActive == true) return
        val job = scope.launch {
            while (true) {
                val pending = conversationState(companionId).pending.firstOrNull() ?: break
                val companion = companions.firstOrNull { it.id == companionId } ?: break
                val result = try {
                    Result.success(
                        withContext(Dispatchers.IO) {
                            val audioPath = pending.audioLocalPath
                            if (audioPath == null) {
                                api.sendAiCompanionMessage(companionId, pending.content)
                            } else {
                                val audioFile = File(audioPath)
                                api.sendAiCompanionVoiceMessage(
                                    companionId = companionId,
                                    audio = UploadPayload(
                                        fileName = audioFile.name,
                                        mimeType = "audio/mp4",
                                        bytes = audioFile.readBytes()
                                    ),
                                    durationMs = pending.durationMs ?: 1L,
                                    uploadRequestId = pending.uploadRequestId ?: pending.localId
                                )
                            }
                        }
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    Result.failure<JSONObject>(failure)
                }

                    result
                    .onSuccess { response ->
                        val returned = parseMessageResponse(response)
                        if (returned.isEmpty()) {
                            deleteLocalVoice(pending.audioLocalPath)
                            updateConversationState(companionId) { state ->
                                state.copy(
                                    messages = state.messages.map { message ->
                                        if (message.id == pending.localId) {
                                            message.copy(pending = false, failed = true, audioLocalPath = null)
                                        } else message
                                    },
                                    pending = state.pending.filterNot { it.localId == pending.localId },
                                    error = "AI 暂时没有返回内容，请稍后重试"
                                )
                            }
                        } else {
                            val remoteVoiceReady = returned.any {
                                it.messageType == "voice" && !it.audioUrl.isNullOrBlank()
                            }
                            updateConversationState(companionId) { state ->
                                state.copy(
                                    messages = replacePendingAiMessage(state.messages, pending.localId, returned),
                                    pending = state.pending.filterNot { it.localId == pending.localId },
                                    error = ""
                                )
                            }
                            if (remoteVoiceReady) deleteLocalVoice(pending.audioLocalPath)
                            val latest = returned.lastOrNull { it.sender == "ai" } ?: returned.last()
                            val latestCompanion = companions.firstOrNull { it.id == companionId } ?: companion
                            replaceCompanion(
                                latestCompanion.copy(
                                    latestMessage = latest.content,
                                    latestMessageAt = latest.createdAt
                                )
                            )
                        }
                    }
                    .onFailure { failure ->
                        deleteLocalVoice(pending.audioLocalPath)
                        updateConversationState(companionId) { state ->
                            state.copy(
                                messages = state.messages.map { message ->
                                    if (message.id == pending.localId) {
                                        message.copy(pending = false, failed = true, audioLocalPath = null)
                                    } else message
                                },
                                pending = state.pending.filterNot { it.localId == pending.localId },
                                error = failure.companionError("AI 回复失败")
                            )
                        }
                    }
            }
        }
        workerJobs[companionId] = job
        job.invokeOnCompletion {
            if (workerJobs[companionId] === job) workerJobs.remove(companionId)
        }
    }

    fun openChat(companion: AiCompanion) {
        selectedId = companion.id
        onChatStateChange(true)
        loadConversation(companion)
    }

    fun sendMessage() {
        val companionId = selectedId ?: return
        val state = conversationState(companionId)
        val content = state.draft.trim()
        if (content.isBlank()) return
        if (state.pending.size >= maxQueuedAiMessages) {
            updateConversationState(companionId) { it.copy(error = "消息正在排队，请稍等片刻再发送") }
            return
        }
        val localId = UUID.randomUUID().toString()
        updateConversationState(companionId) {
            it.copy(
                messages = it.messages + AiConversationMessage(
                    id = localId,
                    sender = "user",
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    pending = true
                ),
                draft = "",
                pending = it.pending + PendingAiMessage(localId, content),
                error = ""
            )
        }
        ensureMessageWorker(companionId)
    }

    fun sendVoice(recording: VoiceRecording) {
        val companionId = recording.companionId
        localVoicePaths += recording.file.absolutePath
        if (companions.none { it.id == companionId }) {
            recording.file.delete()
            return
        }
        val state = conversationState(companionId)
        if (state.pending.size >= maxQueuedAiMessages) {
            recording.file.delete()
            updateConversationState(companionId) { it.copy(error = "消息正在排队，请稍等片刻再发送") }
            return
        }
        val localId = UUID.randomUUID().toString()
        val requestId = UUID.randomUUID().toString()
        updateConversationState(companionId) {
            it.copy(
                messages = it.messages + AiConversationMessage(
                    id = localId,
                    sender = "user",
                    content = "",
                    createdAt = System.currentTimeMillis(),
                    pending = true,
                    messageType = "voice",
                    audioLocalPath = recording.file.absolutePath,
                    durationMs = recording.durationMs
                ),
                pending = it.pending + PendingAiMessage(
                    localId = localId,
                    content = "",
                    audioLocalPath = recording.file.absolutePath,
                    durationMs = recording.durationMs,
                    uploadRequestId = requestId
                ),
                error = ""
            )
        }
        ensureMessageWorker(companionId)
    }

    fun updateListBackground(backgroundUrl: String?) {
        if (listBackgroundSaving) return
        listBackgroundSaving = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.updateAiCompanionListBackground(backgroundUrl) }
            }.onSuccess {
                onUserChanged(user.copy(aiCompanionListBackgroundUrl = backgroundUrl))
                listError = ""
            }.onFailure { listError = it.companionError("列表背景更新失败") }
            listBackgroundSaving = false
        }
    }

    fun updateChatBackground(companion: AiCompanion, backgroundUrl: String?) {
        if (chatBackgroundSaving) return
        chatBackgroundSaving = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.updateAiCompanionChatBackground(companion.id, backgroundUrl)
                }
            }.onSuccess { response ->
                replaceCompanion(parseCompanion(response.optJSONObject("companion") ?: response))
                updateConversationState(companion.id) { it.copy(error = "") }
            }.onFailure { failure ->
                updateConversationState(companion.id) { state ->
                    state.copy(error = failure.companionError("聊天背景更新失败"))
                }
            }
            chatBackgroundSaving = false
        }
    }

    val listBackgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null || listBackgroundSaving) return@rememberLauncherForActivityResult
        listBackgroundSaving = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val payload = context.readUploadPayload(uri)
                    val response = api.uploadAiCompanionListBackground(payload)
                    response.getJSONObject("user").optString("aiCompanionListBackgroundUrl")
                }
            }.onSuccess { url ->
                onUserChanged(user.copy(aiCompanionListBackgroundUrl = url.takeIf { it.isNotBlank() }))
                listError = ""
            }.onFailure { listError = it.companionError("列表背景上传失败") }
            listBackgroundSaving = false
        }
    }

    val chatBackgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val companion = selected ?: return@rememberLauncherForActivityResult
        if (uri == null || chatBackgroundSaving) return@rememberLauncherForActivityResult
        chatBackgroundSaving = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val payload = context.readUploadPayload(uri)
                    api.uploadAiCompanionChatBackground(companion.id, payload)
                }
            }.onSuccess { response ->
                replaceCompanion(parseCompanion(response.optJSONObject("companion") ?: response))
                updateConversationState(companion.id) { it.copy(error = "") }
            }.onFailure { failure ->
                updateConversationState(companion.id) { state ->
                    state.copy(error = failure.companionError("聊天背景上传失败"))
                }
            }
            chatBackgroundSaving = false
        }
    }

    BackHandler(enabled = studioTarget != null || selected != null) {
        when {
            studioTarget != null -> studioTarget = null
            selected != null -> {
                selectedId = null
                onChatStateChange(false)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CompanionBackground)) {
        when {
            studioTarget != null -> AvatarStudioScreen(
                api = api,
                companion = studioTarget!!,
                onBack = { studioTarget = null },
                onAvatarUpdated = ::replaceCompanion
            )
            selected == null -> CompanionList(
                companions = companions,
                loading = listLoading,
                error = listError,
                onBack = { onChatStateChange(false); onBack() },
                onAdd = { editorTarget = null; showEditor = true },
                onRefresh = ::refreshCompanions,
                onSelect = ::openChat,
                onEdit = { editorTarget = it; showEditor = true },
                onStudio = { studioTarget = it },
                backgroundUrl = user.aiCompanionListBackgroundUrl,
                backgroundSaving = listBackgroundSaving,
                onChooseBackground = { listBackgroundPicker.launch("image/*") },
                onClearBackground = { updateListBackground(null) }
            )
            else -> CompanionChat(
                user = user,
                companion = selected!!,
                companions = companions,
                messages = selectedConversation.messages,
                draft = selectedConversation.draft,
                loading = selectedConversation.loading,
                queuedCount = selectedConversation.pending.size,
                error = selectedConversation.error,
                voiceAvailable = voiceAvailable,
                onBack = { selectedId = null; onChatStateChange(false) },
                onSelectCompanion = ::openChat,
                onDraftChange = { draft ->
                    selectedId?.let { id -> updateConversationState(id) { state -> state.copy(draft = draft.take(500)) } }
                },
                onSend = ::sendMessage,
                onVoiceRecorded = ::sendVoice,
                onEdit = { editorTarget = selected; showEditor = true },
                onStudio = { studioTarget = selected },
                backgroundSaving = chatBackgroundSaving,
                onChooseBackground = { chatBackgroundPicker.launch("image/*") },
                onClearBackground = { selected?.let { updateChatBackground(it, null) } }
            )
        }
    }

    if (showEditor) {
        CompanionEditor(
            api = api,
            companion = editorTarget,
            onDismiss = { showEditor = false; refreshCompanions() },
            onSaved = { replaceCompanion(it); showEditor = false },
            onDeleted = { deletedId ->
                companions = companions.filterNot { it.id == deletedId }
                conversationStates = conversationStates - deletedId
                workerJobs.remove(deletedId)?.cancel()
                historyRequests.remove(deletedId)
                if (selectedId == deletedId) {
                    selectedId = null
                    onChatStateChange(false)
                }
                showEditor = false
            }
        )
    }
}

@Composable
private fun CompanionList(
    companions: List<AiCompanion>, loading: Boolean, error: String,
    onBack: () -> Unit, onAdd: () -> Unit, onRefresh: () -> Unit,
    onSelect: (AiCompanion) -> Unit, onEdit: (AiCompanion) -> Unit,
    onStudio: (AiCompanion) -> Unit, backgroundUrl: String?, backgroundSaving: Boolean,
    onChooseBackground: () -> Unit, onClearBackground: () -> Unit
) {
    val background by rememberUriImage(backgroundUrl, maxDimensionPx = 2048)
    var showBackgroundMenu by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().background(CompanionBackground)) {
        BackgroundLayer(background)
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Surface(color = Color.White.copy(alpha = if (background == null) 0.94f else 0.82f), shadowElevation = 1.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = CompanionInk) }
                    Text("AI 陪伴", color = CompanionInk, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { showBackgroundMenu = true }, enabled = !backgroundSaving) {
                            if (backgroundSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = CompanionGreen)
                            else Icon(Icons.Rounded.MoreVert, "列表背景", tint = CompanionInk)
                        }
                        DropdownMenu(
                            expanded = showBackgroundMenu,
                            onDismissRequest = { showBackgroundMenu = false },
                            containerColor = Color.White
                        ) {
                            DropdownMenuItem(
                                text = { Text("更换列表背景") },
                                leadingIcon = { Icon(Icons.Rounded.Image, null) },
                                onClick = { showBackgroundMenu = false; onChooseBackground() }
                            )
                            if (!backgroundUrl.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text("恢复默认背景") },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                    onClick = { showBackgroundMenu = false; onClearBackground() }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onAdd) { Icon(Icons.Rounded.Add, "新增陪伴对象", tint = CompanionInk) }
                }
            }
            if (error.isNotBlank()) {
                Surface(color = Color.White.copy(alpha = 0.84f)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error, color = Color(0xFFB42318), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onRefresh) { Text("重试") }
                    }
                }
            }
            when {
                loading && companions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CompanionGreen)
                }
                companions.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Surface(color = Color.White.copy(alpha = 0.78f), shape = RoundedCornerShape(12.dp)) {
                        Text("还没有陪伴对象，点击右上角 + 创建", color = CompanionMuted, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                    items(companions, key = { it.id }) { companion ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
                            color = Color.White.copy(alpha = if (background == null) 0.9f else 0.78f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().clickable { onSelect(companion) }.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AiCompanionAvatar(companion.displayName, companion.avatarUrl, 54.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(companion.displayName, color = CompanionInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        companion.latestMessage.ifBlank { companion.relation },
                                        color = CompanionMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    companion.latestMessageAt?.let { Text(formatCompanionTime(it), color = CompanionMuted, fontSize = 10.sp) }
                                    Row {
                                        IconButton(onClick = { onStudio(companion) }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Rounded.AutoAwesome, "头像创作", tint = CompanionGreen, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { onEdit(companion) }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Rounded.Edit, "编辑", tint = CompanionMuted, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionChat(
    user: AppUser,
    companion: AiCompanion,
    companions: List<AiCompanion>,
    messages: List<AiConversationMessage>,
    draft: String,
    loading: Boolean,
    queuedCount: Int,
    error: String,
    voiceAvailable: Boolean,
    onBack: () -> Unit,
    onSelectCompanion: (AiCompanion) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceRecorded: (VoiceRecording) -> Unit,
    onEdit: () -> Unit,
    onStudio: () -> Unit,
    backgroundSaving: Boolean, onChooseBackground: () -> Unit, onClearBackground: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showMore by remember { mutableStateOf(false) }
    var showCompanionPicker by remember { mutableStateOf(false) }
    var voiceMode by rememberSaveable(companion.id) { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var cancelGesture by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf("") }
    var showVoicePermissionInfo by remember { mutableStateOf(false) }
    var voiceConsentAccepted by rememberSaveable { mutableStateOf(hasVoiceMessageConsent(context)) }
    val voiceRecorder = remember(context, companion.id) { VoiceRecorder(context) }
    val voicePlayback = remember(context, user.token, companion.id) { VoicePlaybackController() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) voiceError = "需要麦克风权限才能发送语音"
    }
    DisposableEffect(voiceRecorder, voicePlayback) {
        onDispose {
            voiceRecorder.cancel()
            voicePlayback.stop()
        }
    }
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner, voiceRecorder, recording) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && recording) {
                recording = false
                voiceRecorder.cancel()
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }
    BackHandler(enabled = recording) {
        recording = false
        cancelGesture = false
        voiceRecorder.cancel()
    }
    LaunchedEffect(recording) {
        if (recording) {
            delay(60_000L)
            if (recording) {
                recording = false
                voiceRecorder.stop(cancel = false)?.let(onVoiceRecorded)
            }
        }
    }
    val background by rememberUriImage(companion.chatBackgroundUrl, maxDimensionPx = 2048)
    LaunchedEffect(companion.id, messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFEDEDED))) {
        BackgroundLayer(background, overlayAlpha = 0.34f)
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Surface(color = Color(0xFFF7F7F7).copy(alpha = if (background == null) 1f else 0.9f), shadowElevation = 1.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = CompanionInk) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TextButton(
                        onClick = { if (companions.size > 1) showCompanionPicker = true },
                        enabled = companions.size > 1 && !recording,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(companion.displayName, color = CompanionInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (companions.size > 1) "${companion.relation} · 切换对象" else companion.relation,
                                    color = CompanionMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showCompanionPicker,
                            onDismissRequest = { showCompanionPicker = false },
                            containerColor = Color.White
                        ) {
                            companions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(option.displayName, color = CompanionInk, fontWeight = FontWeight.Bold)
                                            Text(option.relation, color = CompanionMuted, fontSize = 11.sp)
                                        }
                                    },
                                    leadingIcon = { AiCompanionAvatar(option.displayName, option.avatarUrl, 34.dp) },
                                    onClick = {
                                        if (recording) return@DropdownMenuItem
                                        showCompanionPicker = false
                                        onSelectCompanion(option)
                                    }
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { showMore = true }, enabled = !backgroundSaving) {
                            if (backgroundSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = CompanionGreen)
                            else Icon(Icons.Rounded.MoreVert, "更多", tint = CompanionInk)
                        }
                        DropdownMenu(
                            expanded = showMore,
                            onDismissRequest = { showMore = false },
                            containerColor = Color.White
                        ) {
                            DropdownMenuItem(
                                text = { Text("更换聊天背景") },
                                leadingIcon = { Icon(Icons.Rounded.Image, null) },
                                onClick = { showMore = false; onChooseBackground() }
                            )
                            if (!companion.chatBackgroundUrl.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text("恢复默认背景") },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                    onClick = { showMore = false; onClearBackground() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("头像创作") },
                                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null) },
                                onClick = { showMore = false; onStudio() }
                            )
                            DropdownMenuItem(
                                text = { Text("编辑陪伴对象") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = { showMore = false; onEdit() }
                            )
                        }
                    }
                }
            }
            if (error.isNotBlank() || voiceError.isNotBlank()) {
                Surface(color = Color.White.copy(alpha = 0.82f)) {
                    Text(
                        error.ifBlank { voiceError },
                        color = Color(0xFFB42318), fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(), state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (loading && messages.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CompanionGreen, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                items(messages, key = { it.id }) { message ->
                    WeChatMessageRow(
                        message = message,
                        user = user,
                        companion = companion,
                        playing = voicePlayback.playingMessageId == message.id,
                        onPlayVoice = {
                            voicePlayback.toggle(context, user.token, message) { voiceError = it }
                        }
                    )
                }
            }
            Surface(color = Color(0xFFF7F7F7).copy(alpha = if (background == null) 1f else 0.92f), shadowElevation = 4.dp) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (queuedCount > 0) {
                        Text(
                            "正在按顺序处理 $queuedCount 条消息",
                            color = CompanionMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    if (recording) {
                        Text(
                            if (cancelGesture) "松开手指，取消发送" else "松开发送，上滑取消",
                            color = Color(0xFFB42318), fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (voiceAvailable) {
                            IconButton(
                                onClick = {
                                    if (!recording) {
                                        voiceMode = !voiceMode
                                        voiceError = ""
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    if (voiceMode) Icons.Rounded.Keyboard else Icons.Rounded.Mic,
                                    if (voiceMode) "切换到键盘" else "切换到语音",
                                    tint = CompanionInk
                                )
                            }
                        }
                        if (voiceAvailable && voiceMode) {
                            VoiceRecordButton(
                                enabled = !loading && queuedCount < maxQueuedAiMessages,
                                recording = recording,
                                modifier = Modifier.weight(1f).height(48.dp),
                                onStart = {
                                    voiceError = ""
                                    cancelGesture = false
                                    if (!voiceConsentAccepted) {
                                        showVoicePermissionInfo = true
                                        false
                                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                        showVoicePermissionInfo = true
                                        false
                                    } else {
                                        val started = voiceRecorder.start(companion.id)
                                        recording = started
                                        if (!started) voiceError = "无法开始录音，请检查麦克风权限"
                                        started
                                    }
                                },
                                onFinish = { cancel ->
                                    // Stop unconditionally: ACTION_UP can arrive
                                    // before Compose has recomposed the parent
                                    // `recording` state after ACTION_DOWN.
                                    val wasRecording = recording
                                    recording = false
                                    cancelGesture = false
                                    val result = voiceRecorder.stop(cancel)
                                    if (result != null) onVoiceRecorded(result)
                                    else if (wasRecording && !cancel) {
                                        voiceError = "录音太短，请至少说 1 秒"
                                    }
                                },
                                onCancelChanged = { cancelGesture = it }
                            )
                        } else {
                            OutlinedTextField(
                                draft,
                                onDraftChange,
                                Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text("输入消息") },
                                colors = companionTextFieldColors()
                            )
                            Button(
                                onClick = onSend,
                                enabled = draft.trim().isNotBlank(),
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF07C160)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, "发送")
                            }
                        }
                    }
                }
            }
        }
    }
    if (showVoicePermissionInfo) {
        AlertDialog(
            onDismissRequest = { showVoicePermissionInfo = false },
            title = { Text("需要麦克风权限") },
            text = {
                Text(
                    "安忆只会在你按住“说话”时使用麦克风，不会后台录音。你发送的录音会上传为本人可读的私有语音消息，并交由隐私政策中公示的语音识别服务商转成文字；AI 只接收转写文字并用文字回复。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showVoicePermissionInfo = false
                    if (saveVoiceMessageConsent(context)) {
                        voiceConsentAccepted = true
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        voiceError = "语音授权记录保存失败，请重试"
                    }
                }) { Text("允许") }
            },
            dismissButton = { TextButton(onClick = { showVoicePermissionInfo = false }) { Text("暂不") } }
        )
    }
}

@Composable
private fun VoiceRecordButton(
    enabled: Boolean,
    recording: Boolean,
    modifier: Modifier = Modifier,
    onStart: () -> Boolean,
    onFinish: (cancel: Boolean) -> Unit,
    onCancelChanged: (Boolean) -> Unit = {}
) {
    val cancelDistancePx = with(LocalDensity.current) { 80.dp.toPx() }
    var pressed by remember { mutableStateOf(false) }
    var cancelGesture by remember { mutableStateOf(false) }
    var downY by remember { mutableStateOf(0f) }
    Surface(
        modifier = modifier
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!enabled) return@pointerInteropFilter false
                        downY = event.rawY
                        cancelGesture = false
                        onCancelChanged(false)
                        pressed = onStart()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (pressed) {
                            cancelGesture = downY - event.rawY > cancelDistancePx
                            onCancelChanged(cancelGesture)
                        }
                        pressed
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (pressed) onFinish(cancelGesture || event.actionMasked == MotionEvent.ACTION_CANCEL)
                        pressed = false
                        cancelGesture = false
                        onCancelChanged(false)
                        true
                    }
                    else -> pressed
                }
            },
        color = if (recording) Color(0xFFB42318) else Color(0xFFE8E8E8),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (recording) "松开发送" else "按住说话",
                color = if (recording) Color.White else CompanionInk,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BackgroundLayer(image: ImageBitmap?, overlayAlpha: Float = 0.42f) {
    if (image != null) {
        ComposeImage(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = overlayAlpha)))
    }
}

@Composable
private fun WeChatMessageRow(
    message: AiConversationMessage,
    user: AppUser,
    companion: AiCompanion,
    playing: Boolean,
    onPlayVoice: () -> Unit
) {
    val mine = message.sender == "user"
    var transcriptExpanded by rememberSaveable(message.id) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!mine) {
            AiCompanionAvatar(companion.displayName, companion.avatarUrl, 40.dp)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 280.dp),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
        ) {
            Surface(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(max = 280.dp)
                    .then(if (message.messageType == "voice") Modifier.clickable(onClick = onPlayVoice) else Modifier),
                color = if (mine) Color(0xFF95EC69) else Color.White,
                shape = RoundedCornerShape(6.dp)
            ) {
                if (message.messageType == "voice") {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = if (playing) "播放中" else "播放语音", tint = CompanionInk, modifier = Modifier.size(18.dp))
                            Text(
                                formatVoiceDuration(message.durationMs),
                                color = CompanionInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Text(
                        message.content,
                        color = CompanionInk,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
            if (message.messageType == "voice" && message.content.isNotBlank() && !message.pending) {
                TextButton(
                    onClick = { transcriptExpanded = !transcriptExpanded },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(if (transcriptExpanded) "收起文字" else "转文字", color = CompanionMuted, fontSize = 11.sp)
                }
                if (transcriptExpanded) {
                    Surface(color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            message.content,
                            color = CompanionInk,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            if (message.pending || message.failed) {
                Text(
                    if (message.pending) "排队中" else "发送失败",
                    color = if (message.failed) Color(0xFFB42318) else CompanionMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        if (mine) {
            Spacer(Modifier.width(8.dp))
            AiCompanionAvatar(user.displayName, user.avatarUrl, 40.dp)
        }
    }
}

@Composable
private fun CompanionEditor(
    api: AnyiApiClient, companion: AiCompanion?, onDismiss: () -> Unit,
    onSaved: (AiCompanion) -> Unit, onDeleted: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(companion?.id) { mutableStateOf(companion?.displayName.orEmpty()) }
    var relation by remember(companion?.id) { mutableStateOf(companion?.relation.orEmpty()) }
    var manualMemory by remember(companion?.id) { mutableStateOf("") }
    var memories by remember(companion?.id) { mutableStateOf(emptyList<AiCompanionMemory>()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingCreatedCompanion by remember(companion?.id) { mutableStateOf<AiCompanion?>(null) }
    var selectedAvatarUri by remember(companion?.id) { mutableStateOf<String?>(null) }
    var avatarUploadRequestId by remember(companion?.id) { mutableStateOf<String?>(null) }
    val displayedAvatarUrl = selectedAvatarUri ?: companion?.avatarUrl

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || saving) return@rememberLauncherForActivityResult
        context.persistReadPermission(uri)
        selectedAvatarUri = uri.toString()
        avatarUploadRequestId = UUID.randomUUID().toString()
        message = ""
    }

    LaunchedEffect(companion?.id) {
        companion?.let { active ->
            runCatching { withContext(Dispatchers.IO) { api.listAiCompanionMemories(active.id) } }
                .onSuccess { memories = parseMemories(it) }
        }
    }

    Dialog(onDismissRequest = { if (!saving) onDismiss() }) {
        Surface(Modifier.fillMaxWidth().padding(16.dp), color = CompanionPaper, shape = RoundedCornerShape(16.dp), shadowElevation = 12.dp) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (companion == null) "新增陪伴对象" else "编辑陪伴对象", color = CompanionInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, enabled = !saving) { Icon(Icons.Rounded.Close, "关闭", tint = CompanionMuted) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AiCompanionAvatar(name.ifBlank { "陪伴对象" }, displayedAvatarUrl, 78.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            when {
                                selectedAvatarUri != null -> "已选择新头像，点击保存后上传"
                                companion?.avatarUrl.isNullOrBlank() -> "还没有头像"
                                else -> "当前头像"
                            },
                            color = CompanionMuted,
                            fontSize = 12.sp
                        )
                        OutlinedButton(
                            onClick = { avatarPicker.launch(arrayOf("image/*")) },
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = CompanionButtonShape
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, "上传头像", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("上传图片")
                        }
                    }
                }
                OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("姓名或名称（必填）") }, singleLine = true, colors = companionTextFieldColors())
                OutlinedTextField(relation, { relation = it.take(30) }, Modifier.fillMaxWidth(), label = { Text("对方是我的（必填）") }, placeholder = { Text("例如：儿子、朋友、宠物或旧物") }, singleLine = true, colors = companionTextFieldColors())
                OutlinedTextField(
                    manualMemory,
                    { manualMemory = it.take(240) },
                    Modifier.fillMaxWidth(),
                    label = { Text(if (companion == null) "初始记忆（可选）" else "手动记忆（可选）") },
                    placeholder = { Text("建议写清主体，例如：陪伴对象喜欢吃梨；我喜欢吃苹果") },
                    supportingText = { Text("涉及不同人的事实，建议分开写，避免主体混淆") },
                    enabled = !saving,
                    minLines = 2,
                    maxLines = 4,
                    colors = companionTextFieldColors()
                )
                if (companion != null) {
                    OutlinedButton(
                        onClick = {
                            val content = manualMemory.trim()
                            if (content.isBlank()) { message = "请先填写记忆内容"; return@OutlinedButton }
                            saving = true
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { api.createAiCompanionMemory(companion.id, content) } }
                                    .onSuccess { response ->
                                        val saved = parseMemory(response.optJSONObject("memory") ?: response)
                                        if (saved.id.isNotBlank()) memories = memories + saved
                                        manualMemory = ""; message = "记忆已添加"
                                    }.onFailure { message = it.companionError("记忆添加失败") }
                                saving = false
                            }
                        },
                        enabled = !saving && manualMemory.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = CompanionButtonShape
                    ) { Text("添加记忆") }
                }
                if (memories.isNotEmpty()) {
                    Text("已有记忆", color = CompanionInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    memories.forEach { memory ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(memory.content, color = CompanionMuted, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = {
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { api.deleteAiCompanionMemory(companion!!.id, memory.id) } }
                                        .onSuccess { memories = memories.filterNot { it.id == memory.id } }
                                }
                            }, enabled = !saving) { Icon(Icons.Rounded.Delete, "删除记忆", tint = Color(0xFFB42318), modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
                if (message.isNotBlank()) Text(message, color = Color(0xFFB42318), fontSize = 12.sp)
                Button(
                    onClick = {
                        val cleanName = name.trim(); val cleanRelation = relation.trim()
                        if (cleanName.isBlank() || cleanRelation.isBlank()) { message = "请填写名称和关系"; return@Button }
                        if (isAmbiguousCompanionRelation(cleanRelation)) {
                            message = "请填写单向关系，例如儿子、爸爸、哥哥或妹妹"
                            return@Button
                        }
                        val avatarUri = selectedAvatarUri?.takeIf { it.isNotBlank() }
                        val avatarRequestId = avatarUri?.let { avatarUploadRequestId ?: UUID.randomUUID().toString() }
                        if (avatarRequestId != null && avatarUploadRequestId == null) {
                            avatarUploadRequestId = avatarRequestId
                        }
                        saving = true
                        scope.launch {
                            var attemptSaved: AiCompanion? = null
                            val result = runCatching {
                                withContext(Dispatchers.IO) {
                                    val persisted = companion ?: pendingCreatedCompanion
                                    val response = if (persisted == null) {
                                        api.createAiCompanion(cleanName, cleanRelation)
                                    } else {
                                        api.updateAiCompanion(persisted.id, cleanName, cleanRelation)
                                    }
                                    var saved = parseCompanion(response.optJSONObject("companion") ?: response)
                                    attemptSaved = saved
                                    if (avatarUri != null) {
                                        val payload = context.readUploadPayload(Uri.parse(avatarUri))
                                        val avatarResponse = api.uploadAiCompanionAvatar(
                                            saved.id,
                                            payload,
                                            avatarRequestId
                                        )
                                        val updatedJson = avatarResponse.optJSONObject("companion")
                                            ?: throw IllegalStateException("avatar_upload_response_invalid")
                                        saved = parseCompanion(updatedJson)
                                        if (saved.id.isBlank()) throw IllegalStateException("avatar_upload_response_invalid")
                                        attemptSaved = saved
                                    }
                                    if (companion == null && manualMemory.isNotBlank()) {
                                        api.createAiCompanionMemory(saved.id, manualMemory.trim())
                                    }
                                    saved
                                }
                            }
                            if (companion == null) attemptSaved?.let { pendingCreatedCompanion = it }
                            result.onSuccess(onSaved).onFailure { message = it.companionError("保存失败") }
                            saving = false
                        }
                    },
                    enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = CompanionButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CompanionInk)
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("保存", fontWeight = FontWeight.Bold)
                }
                if (companion != null) {
                    OutlinedButton(onClick = { confirmDelete = true }, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = CompanionButtonShape, border = BorderStroke(1.dp, Color(0xFFB42318).copy(alpha = 0.35f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB42318))) {
                        Icon(Icons.Rounded.Delete, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("删除对象")
                    }
                }
            }
        }
    }
    if (confirmDelete && companion != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false }, title = { Text("删除陪伴对象") },
            text = { Text("确定删除 ${companion.displayName} 吗？相关聊天和记忆也可能被删除。") },
            confirmButton = { TextButton(onClick = {
                saving = true
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { api.deleteAiCompanion(companion.id) } }
                        .onSuccess { onDeleted(companion.id) }.onFailure { message = it.companionError("删除失败") }
                    saving = false; confirmDelete = false
                }
            }) { Text("删除", color = Color(0xFFB42318)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun AvatarStudioScreen(
    api: AnyiApiClient, companion: AiCompanion, onBack: () -> Unit,
    onAvatarUpdated: (AiCompanion) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var current by remember(companion.id, companion.avatarUrl) { mutableStateOf(companion) }
    var messages by remember(companion.id) { mutableStateOf(emptyList<AvatarStudioMessage>()) }
    var models by remember(companion.id) { mutableStateOf(emptyList<AiImageModel>()) }
    var model by remember(companion.id) { mutableStateOf("") }
    var prompt by rememberSaveable(companion.id) { mutableStateOf("") }
    var attachment by remember(companion.id) { mutableStateOf<UploadPayload?>(null) }
    var useCurrentAvatar by remember(companion.id) { mutableStateOf(!companion.avatarUrl.isNullOrBlank()) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { context.readUploadPayload(uri) } }
                .onSuccess {
                    attachment = it
                    useCurrentAvatar = false
                    error = ""
                }
                .onFailure { error = it.companionError("照片读取失败") }
        }
    }

    LaunchedEffect(companion.id) {
        runCatching { withContext(Dispatchers.IO) { api.listAiImageModels() } }
            .onSuccess { models = parseImageModels(it); model = models.firstOrNull()?.id.orEmpty() }
            .onFailure { error = it.companionError("模型加载失败") }
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    fun submit() {
        val userPrompt = prompt
        if (userPrompt.isBlank() || model.isBlank() || sending) return
        val selectedFile = attachment
        messages = messages + AvatarStudioMessage(
            mine = true,
            text = userPrompt,
            attachmentName = selectedFile?.fileName
        )
        prompt = ""; sending = true; error = ""
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.createAiAvatarStudioImage(current.id, model, userPrompt, useCurrentAvatar, selectedFile)
                }
            }.onSuccess { response ->
                val updated = parseCompanionResponse(response, current)
                current = updated
                onAvatarUpdated(updated)
                messages = messages + AvatarStudioMessage(
                    mine = false,
                    text = response.optString("message").ifBlank { "头像已更新" },
                    imageUrl = updated.avatarUrl
                )
                attachment = null
                useCurrentAvatar = !updated.avatarUrl.isNullOrBlank()
            }.onFailure { error = it.companionError("头像创作失败") }
            sending = false
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = CompanionInk) }
            Column(Modifier.weight(1f)) {
                Text("头像创作", color = CompanionInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(current.displayName, color = CompanionMuted, fontSize = 11.sp)
            }
            AiCompanionAvatar(current.displayName, current.avatarUrl, 42.dp)
        }
        Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = Color.White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CompanionLine)) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarStudioImage(current.displayName, current.avatarUrl, 136.dp)
                Spacer(Modifier.height(6.dp)); Text("当前头像", color = CompanionMuted, fontSize = 12.sp)
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = listState,
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) item {
                Text("描述你想生成或修改的头像。可以上传参考照片，也可以基于当前头像继续调整。", color = CompanionMuted, fontSize = 13.sp, lineHeight = 19.sp)
            }
            items(messages, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (item.mine) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (item.mine) CompanionInk else Color.White, shape = RoundedCornerShape(12.dp), border = if (item.mine) null else BorderStroke(1.dp, CompanionLine)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(item.text, color = if (item.mine) Color.White else CompanionInk, fontSize = 13.sp)
                            item.attachmentName?.let { name ->
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Image, null, Modifier.size(16.dp), tint = Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text("附件：$name", color = Color.White.copy(alpha = 0.86f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            item.imageUrl?.let { url -> Spacer(Modifier.height(8.dp)); AvatarStudioImage(current.displayName, url, 196.dp) }
                        }
                    }
                }
            }
        }
        if (error.isNotBlank()) Text(error, color = Color(0xFFB42318), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Column(Modifier.fillMaxWidth().navigationBarsPadding().background(Color.White).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (models.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    items(models, key = { it.id }) { option ->
                        FilterChip(selected = model == option.id, onClick = { model = option.id }, label = { Text(option.name, maxLines = 1) }, enabled = !sending)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = useCurrentAvatar,
                    onClick = { useCurrentAvatar = !useCurrentAvatar },
                    label = { Text("基于当前头像") },
                    enabled = !current.avatarUrl.isNullOrBlank() && !sending
                )
                attachment?.let {
                    FilterChip(selected = true, onClick = { attachment = null }, label = { Text(it.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) }, trailingIcon = { Icon(Icons.Rounded.Close, "移除附件", Modifier.size(16.dp)) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { picker.launch("image/*") }, enabled = !sending) { Icon(Icons.Rounded.Image, "上传照片", tint = CompanionGreen) }
                OutlinedTextField(prompt, { prompt = it.take(2000) }, Modifier.weight(1f), placeholder = { Text("直接描述想要的头像或修改") }, minLines = 1, maxLines = 4, colors = companionTextFieldColors())
                Button(onClick = ::submit, enabled = prompt.isNotBlank() && model.isNotBlank() && !sending, modifier = Modifier.size(50.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = CompanionInk), contentPadding = PaddingValues(0.dp)) {
                    if (sending) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Rounded.Send, "发送")
                }
            }
        }
    }
}

@Composable
private fun AvatarStudioImage(name: String, imageUrl: String?, size: Dp) {
    val image by rememberUriImage(imageUrl, maxDimensionPx = 1024)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF2EBDD)),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            ComposeImage(
                bitmap = image!!,
                contentDescription = "${name}的头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(name.trim().firstOrNull()?.toString() ?: "人", color = CompanionGreen, fontSize = (size.value * 0.34f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun companionTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CompanionGreen, unfocusedBorderColor = CompanionLine,
    focusedLabelColor = CompanionGreen, cursorColor = CompanionGreen,
    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
)

private fun parseCompanions(array: JSONArray): List<AiCompanion> = List(array.length()) { parseCompanion(array.optJSONObject(it) ?: JSONObject()) }.filter { it.id.isNotBlank() }

private fun isAmbiguousCompanionRelation(value: String): Boolean {
    val normalized = value.replace(" ", "").removeSuffix("关系")
    return normalized in setOf("父子", "母子", "父女", "母女", "亲子", "兄弟", "姐妹", "兄妹", "姐弟", "祖孙", "夫妻")
}

private fun parseCompanion(item: JSONObject) = AiCompanion(
    id = item.optString("id"),
    displayName = item.optString("displayName", item.optString("name", "未命名对象")),
    relation = item.optString("relation"),
    chatBackgroundUrl = item.optString("chatBackgroundUrl").takeIf { it.isNotBlank() && it != "null" },
    avatarUrl = item.optString("avatarUrl").takeIf { it.isNotBlank() && it != "null" },
    generated = item.optBoolean("generated", false),
    updatedAt = item.optLong("updatedAt").takeIf { it > 0L } ?: System.currentTimeMillis(),
    latestMessage = item.optString("latestMessage"),
    latestMessageAt = item.optLong("latestMessageAt").takeIf { it > 0L } ?: parseCompanionTimestamp(item.optString("latestMessageAt"))
)

private fun parseCompanionResponse(response: JSONObject, fallback: AiCompanion): AiCompanion {
    response.optJSONObject("companion")?.let { return parseCompanion(it) }
    response.optJSONObject("profile")?.let { return parseCompanion(it) }
    val url = listOf("avatarUrl", "imageUrl", "url").firstNotNullOfOrNull { key -> response.optString(key).takeIf { it.isNotBlank() } }
        ?: response.optJSONObject("asset")?.optString("url")?.takeIf { it.isNotBlank() }
        ?: response.optJSONObject("avatar")?.optString("url")?.takeIf { it.isNotBlank() }
    return if (url == null) fallback else fallback.copy(avatarUrl = url, generated = true, updatedAt = System.currentTimeMillis())
}

private fun parseCompanionTimestamp(raw: String): Long? {
    val value = raw.trim().takeIf { it.isNotBlank() && it != "null" } ?: return null
    value.toLongOrNull()?.takeIf { it > 0L }?.let { return it }
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(value)?.time
    }.getOrNull()
}

private fun formatCompanionTime(timestamp: Long): String {
    val sameDay = SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(Date()) == SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(Date(timestamp))
    return SimpleDateFormat(if (sameDay) "HH:mm" else "M月d日", Locale.CHINA).format(Date(timestamp))
}

internal fun mergeAiMessages(
    serverMessages: List<AiConversationMessage>,
    localMessages: List<AiConversationMessage>
): List<AiConversationMessage> {
    val merged = LinkedHashMap<String, AiConversationMessage>()
    serverMessages.forEach { merged[it.id] = it }
    localMessages.forEach { message -> merged.putIfAbsent(message.id, message) }
    return merged.values.toList()
}

internal fun replacePendingAiMessage(
    messages: List<AiConversationMessage>,
    localId: String,
    returned: List<AiConversationMessage>
): List<AiConversationMessage> {
    if (returned.isEmpty()) return messages
    val index = messages.indexOfFirst { it.id == localId }
    if (index < 0) return mergeAiMessages(messages, returned)

    val next = messages.toMutableList()
    next.removeAt(index)
    val pending = messages[index]
    val normalized = returned.distinctBy { it.id }.map { item ->
        if (
            item.messageType == "voice" &&
            item.audioLocalPath == null &&
            item.audioUrl.isNullOrBlank()
        ) {
            item.copy(audioLocalPath = pending.audioLocalPath)
        } else item
    }
    next.addAll(index, normalized)
    return next
}

private fun parseMessages(array: JSONArray?): List<AiConversationMessage> {
    if (array == null) return emptyList()
    return List(array.length()) { index ->
        val item = array.optJSONObject(index) ?: JSONObject()
        AiConversationMessage(
            item.optString("id").ifBlank { UUID.randomUUID().toString() },
            if (item.optString("sender") == "user") "user" else "ai",
            item.optString("content"),
            item.optLong("createdAt").takeIf { it > 0L } ?: System.currentTimeMillis(),
            messageType = item.optString("messageType", "text").ifBlank { "text" },
            audioUrl = item.optString("audioUrl").takeIf { it.isNotBlank() },
            durationMs = item.optLong("durationMs").takeIf { it > 0L }
        )
    }.filter { it.messageType == "voice" || it.content.isNotBlank() }
}

private fun parseMessageResponse(response: JSONObject): List<AiConversationMessage> {
    val messages = parseMessages(response.optJSONArray("messages"))
    if (messages.isNotEmpty()) return messages
    return response.optJSONObject("voiceMessage")?.let { parseMessages(JSONArray().put(it)) }.orEmpty()
}

private fun formatVoiceDuration(durationMs: Long?): String {
    val seconds = ((durationMs ?: 0L) / 1000L).coerceAtLeast(1L)
    return "${seconds}s"
}

private fun parseMemories(array: JSONArray): List<AiCompanionMemory> = List(array.length()) { parseMemory(array.optJSONObject(it) ?: JSONObject()) }.filter { it.id.isNotBlank() && it.content.isNotBlank() }
private fun parseMemory(item: JSONObject) = AiCompanionMemory(item.optString("id"), item.optString("content"))
private fun parseImageModels(array: JSONArray): List<AiImageModel> = List(array.length()) { index ->
    val item = array.optJSONObject(index) ?: JSONObject()
    val id = item.optString("id", item.optString("model"))
    AiImageModel(id, item.optString("name", item.optString("label", id)), item.optString("description"))
}.filter { it.id.isNotBlank() }

private fun Throwable.companionError(fallback: String): String {
    val detail = message.orEmpty()
    return when {
        detail.contains("ai_provider_not_configured") -> "AI 对话暂未开放"
        detail.contains("ai_voice_disabled") -> "语音消息暂未开放"
        detail.contains("asr_provider_not_configured") -> "语音识别暂未配置"
        detail.contains("asr_provider_unsupported") -> "当前语音识别服务暂不支持"
        detail.contains("asr_upstream_unreachable") ||
            detail.contains("asr_upstream_failed") ||
            detail.contains("asr_invalid_response") -> "语音识别暂时不可用，请稍后再试"
        detail.contains("asr_empty_transcript") -> "没有识别到清晰语音，请重试"
        detail.contains("asr_audio_too_large") -> "语音文件超过识别服务限制，请缩短后重试"
        detail.contains("voice_file_required") -> "没有读取到语音，请重新录制"
        detail.contains("voice_type_invalid") || detail.contains("voice_signature_mismatch") || detail.contains("voice_audio_invalid") -> "语音格式暂不支持，请重新录制"
        detail.contains("voice_size_invalid") -> "语音文件过大，请缩短后重试"
        detail.contains("voice_duration_mismatch") -> "语音时长校验失败，请重新录制"
        detail.contains("voice_duration_invalid") || detail.contains("voice_too_long") -> "语音时长需在 1～60 秒之间"
        detail.contains("upload_request_id_required") -> "语音请求无效，请重新录制"
        detail.contains("voice_processing") -> "这条语音正在处理中，请稍后查看"
        detail.contains("rate_limit_exceeded") -> "语音发送太频繁，请稍后再试"
        detail.contains("ai_upstream_failed") -> "AI 服务暂时不可用，请稍后再试"
        detail.contains("avatar_required") || detail.contains("file_required") -> "请选择头像图片"
        detail.contains("unsupported_avatar_type") -> "头像仅支持 JPG、PNG 或 WebP"
        detail.contains("avatar_image_size_invalid") || detail.contains("file_size_invalid") -> "头像文件过大或为空"
        detail.contains("file_signature_mismatch") -> "图片格式校验失败，请重新选择"
        detail.contains("upload_content_rejected") -> "图片未通过内容检查，请更换图片"
        detail.contains("avatar_upload_rejected") -> "头像未通过审核，请更换图片"
        detail.contains("upload_request_id_conflict") -> "这张图片请求已被其他操作使用，请重新选择"
        detail.contains("avatar_asset_not_approved") -> "头像还在审核中，请稍后再试"
        detail.contains("avatar_asset_not_owned") -> "只能使用自己上传的头像"
        detail.contains("avatar_upload_response_invalid") -> "头像上传结果异常，请重试"
        detail.contains("companion_not_found") -> "该陪伴对象已不存在"
        detail.contains("relation_direction_required") -> "请填写单向关系，例如儿子、爸爸、哥哥或妹妹"
        detail.contains("rate_limit") -> "操作太频繁，请稍后再试"
        detail.isBlank() -> fallback
        else -> "$fallback：$detail"
    }
}
