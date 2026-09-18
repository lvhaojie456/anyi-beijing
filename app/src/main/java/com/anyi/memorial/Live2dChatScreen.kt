package com.anyi.memorial

import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Immersive chat: the companion's bound Live2D avatar fills the upper part of
 * the screen and a compact text chat sits below it.
 *
 * This composable owns no conversation state of its own. Messages, draft,
 * queue and the send action are the SAME objects that drive the normal
 * CompanionChat, so history and the per-companion FIFO worker are shared and
 * nothing is duplicated when the user toggles modes.
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = Color(0xFF1D1D1F))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(companion.displayName, color = Color(0xFF1D1D1F), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(companion.relation, color = Color(0xFF6E6E73), fontSize = 11.sp)
                    }
                    IconButton(onClick = onChangeAvatar) {
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
            if (error.isNotBlank()) {
                Surface(color = Color.White.copy(alpha = 0.82f)) {
                    Text(
                        error, color = Color(0xFFB42318), fontSize = 12.sp,
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
                    // Voice bubbles are shown as text here; playback stays in the normal chat.
                    ImmersiveMessageRow(message = message, user = user, companion = companion)
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
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

@Composable
private fun ImmersiveMessageRow(
    message: AiConversationMessage,
    user: AppUser,
    companion: AiCompanion
) {
    val mine = message.sender == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (mine) Color(0xFF95EC69) else Color.White,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 1.dp
        ) {
            Text(
                message.content.ifBlank { if (message.messageType == "voice") "[语音]" else "" },
                color = if (message.failed) Color(0xFFB42318) else Color(0xFF1D1D1F),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}
