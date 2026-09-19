package com.anyi.memorial

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay

/**
 * Immersive chat: the companion's bound Live2D avatar fills the upper part of
 * the screen and a compact chat sits below it.
 *
 * This composable owns no conversation state of its own. Messages, draft,
 * queue and the send actions are the SAME objects that drive the normal
 * CompanionChat, so history and the per-companion FIFO worker are shared and
 * nothing is duplicated when the user toggles modes. Voice input reuses the
 * same recorder, consent record and upload path as the normal chat.
 */
@Composable
internal fun Live2dChatScreen(
    api: com.anyi.memorial.network.AnyiApiClient,
    user: AppUser,
    companion: AiCompanion,
    modelId: String,
    messages: List<AiConversationMessage>,
    draft: String,
    loading: Boolean,
    queuedCount: Int,
    error: String,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    voiceAvailable: Boolean,
    onVoiceRecorded: (VoiceRecording) -> Unit,
    onChangeAvatar: () -> Unit
) {
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var avatarError by remember(modelId) { mutableStateOf("") }
    var mouthOpenness by remember(companion.id) { mutableStateOf<Float?>(null) }
    val speech = remember(companion.id) {
        AiSpeechQueue(
            cacheDir = java.io.File(context.cacheDir, "speech"),
            scope = scope,
            onPlayingChanged = { playing -> if (!playing) mouthOpenness = null },
            onLipsync = { openness ->
                // MediaPlayer exposes no amplitude, so the mouth follows an envelope while audible.
                mouthOpenness = if (openness <= 0f) null else openness
            },
            onError = { message -> avatarError = message }
        )
    }
    DisposableEffect(companion.id) { onDispose { speech.stop() } }

    // Voice input: identical gesture, consent and permission flow to the normal chat.
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

    // Talk animation is triggered by the newest AI reply. Track its id so a
    // recomposition with the same list does not replay the animation.
    var lastSpokenId by remember(companion.id) { mutableStateOf<String?>(null) }
    var speakText by remember(companion.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(messages.lastOrNull()?.id, messages.size) {
        val latest = messages.lastOrNull() ?: return@LaunchedEffect
        if (latest.sender == "ai" && !latest.pending && latest.id != lastSpokenId) {
            lastSpokenId = latest.id
            speakText = latest.content
            speakReply(api, companion, latest, speech, scope) { message -> avatarError = message }
        }
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEDE7DA), Color(0xFFF7F5EF), Color(0xFFF7F7F7))
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Avatar stage (upper ~55%).
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1.15f)
            ) {
                Live2dAvatarView(
                    api = api,
                    modelId = modelId,
                    modifier = Modifier.fillMaxSize(),
                    speakText = speakText,
                    mouthOpenness = mouthOpenness,
                    onEvent = { event ->
                        when (event) {
                            is Live2dEvent.Error -> avatarError = event.message
                            is Live2dEvent.Ready -> avatarError = ""
                            Live2dEvent.PageReady -> Unit
                        }
                    }
                )

                // Top bar overlays the stage.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, enabled = !recording) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = Color(0xFF1D1D1F))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(companion.displayName, color = Color(0xFF1D1D1F), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(companion.relation, color = Color(0xFF6E6E73), fontSize = 11.sp)
                    }
                    IconButton(onClick = onChangeAvatar, enabled = !recording) {
                        Icon(Icons.Rounded.Face, "更换形象", tint = Color(0xFF1D1D1F))
                    }
                }

                // Mandatory AI disclosure label (深度合成管理规定 第十七条).
                Surface(
                    color = Color.White.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 8.dp)
                ) {
                    Text(
                        "AI 生成形象",
                        color = Color(0xFF6B5310),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }

                if (avatarError.isNotBlank()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    ) {
                        Text(
                            "形象加载失败：$avatarError",
                            color = Color(0xFFB42318),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Chat area (lower ~45%).
            if (error.isNotBlank() || voiceError.isNotBlank()) {
                Surface(color = Color.White.copy(alpha = 0.82f)) {
                    Text(
                        error.ifBlank { voiceError }, color = Color(0xFFB42318), fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(0.85f),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (loading && messages.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF9A6B2F), modifier = Modifier.size(24.dp))
                        }
                    }
                }
                items(messages, key = { it.id }) { message ->
                    ImmersiveMessageRow(
                        message = message,
                        playing = voicePlayback.playingMessageId == message.id,
                        onPlayVoice = {
                            // The avatar's own reply voice and a replayed bubble must not overlap.
                            speech.stop()
                            voicePlayback.toggle(context, user.token, message) { voiceError = it }
                        }
                    )
                }
            }

            Surface(color = Color(0xFFF7F7F7).copy(alpha = 0.95f), shadowElevation = 4.dp) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (queuedCount > 0) {
                        Text(
                            "正在按顺序处理 $queuedCount 条消息",
                            color = Color(0xFF6E6E73), fontSize = 11.sp,
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
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    if (voiceMode) Icons.Rounded.Keyboard else Icons.Rounded.Mic,
                                    if (voiceMode) "切换到键盘" else "切换到语音",
                                    tint = Color(0xFF1D1D1F)
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
                                        // Stop the avatar's voice so the microphone does not pick it up.
                                        speech.stop()
                                        voicePlayback.stop()
                                        val started = voiceRecorder.start(companion.id)
                                        recording = started
                                        if (!started) voiceError = "无法开始录音，请检查麦克风权限"
                                        started
                                    }
                                },
                                onFinish = { cancel ->
                                    val wasRecording = recording
                                    recording = false
                                    cancelGesture = false
                                    val result = voiceRecorder.stop(cancel)
                                    if (result != null) onVoiceRecorded(result)
                                    else if (wasRecording && !cancel) voiceError = "录音太短，请至少说 1 秒"
                                },
                                onCancelChanged = { cancelGesture = it }
                            )
                        } else {
                            OutlinedTextField(
                                draft,
                                onDraftChange,
                                Modifier.weight(1f).height(52.dp),
                                singleLine = true,
                                placeholder = { Text("和 ${companion.displayName} 说点什么") }
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
        VoicePermissionDialog(
            onDismiss = { showVoicePermissionInfo = false },
            onAllow = {
                showVoicePermissionInfo = false
                if (saveVoiceMessageConsent(context)) {
                    voiceConsentAccepted = true
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    voiceError = "语音授权记录保存失败，请重试"
                }
            }
        )
    }
}

@Composable
private fun ImmersiveMessageRow(
    message: AiConversationMessage,
    playing: Boolean,
    onPlayVoice: () -> Unit
) {
    val mine = message.sender == "user"
    val voice = message.messageType == "voice"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (mine) Color(0xFF95EC69) else Color.White,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 1.dp,
            modifier = if (voice) Modifier.clickable(onClick = onPlayVoice) else Modifier
        ) {
            if (voice) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = if (playing) "播放中" else "播放语音",
                        tint = if (playing) Color(0xFF9A6B2F) else Color(0xFF1D1D1F),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        formatVoiceDuration(message.durationMs),
                        color = if (message.failed) Color(0xFFB42318) else Color(0xFF1D1D1F),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (message.pending) Text("排队中", color = Color(0xFF6E6E73), fontSize = 11.sp)
                    else if (message.failed) Text("发送失败", color = Color(0xFFB42318), fontSize = 11.sp)
                }
            } else {
                Text(
                    message.content,
                    color = if (message.failed) Color(0xFFB42318) else Color(0xFF1D1D1F),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }
}
