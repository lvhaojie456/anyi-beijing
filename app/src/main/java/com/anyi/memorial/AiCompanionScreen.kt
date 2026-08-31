package com.anyi.memorial

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
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anyi.memorial.network.AnyiApiClient
import com.anyi.memorial.network.UploadPayload
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val CompanionBackground = Color(0xFFF5F5F7)
private val CompanionInk = Color(0xFF1D1D1F)
private val CompanionMuted = Color(0xFF6E6E73)
private val CompanionLine = Color(0xFFE1DCCF)
private val CompanionGreen = Color(0xFF9A6B2F)
private val CompanionPaper = Color(0xFFFFFEFB)
private val CompanionButtonShape = RoundedCornerShape(12.dp)

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

private data class AiConversationMessage(
    val id: String,
    val sender: String,
    val content: String,
    val createdAt: Long
)

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
    var selected by remember { mutableStateOf<AiCompanion?>(null) }
    var studioTarget by remember { mutableStateOf<AiCompanion?>(null) }
    var messages by remember { mutableStateOf(emptyList<AiConversationMessage>()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var editorTarget by remember { mutableStateOf<AiCompanion?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var listBackgroundSaving by remember { mutableStateOf(false) }
    var chatBackgroundSaving by remember { mutableStateOf(false) }

    fun replaceCompanion(updated: AiCompanion) {
        companions = (companions.filterNot { it.id == updated.id } + updated)
            .sortedByDescending { it.updatedAt }
        if (selected?.id == updated.id) selected = updated
        if (studioTarget?.id == updated.id) studioTarget = updated
    }

    fun refreshCompanions() {
        loading = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { api.listAiCompanions() } }
                .onSuccess { companions = parseCompanions(it); error = "" }
                .onFailure { error = it.companionError("陪伴对象加载失败") }
            loading = false
        }
    }

    LaunchedEffect(user.token) { refreshCompanions() }

    fun openChat(companion: AiCompanion) {
        selected = companion
        onChatStateChange(true)
        messages = emptyList()
        error = ""
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { api.listAiCompanionMessages(companion.id) } }
                .onSuccess { messages = parseMessages(it) }
                .onFailure { error = it.companionError("聊天记录加载失败") }
        }
    }

    fun sendMessage() {
        val companion = selected ?: return
        val content = draft.trim()
        if (content.isBlank() || sending) return
        val local = AiConversationMessage(UUID.randomUUID().toString(), "user", content, System.currentTimeMillis())
        messages = messages + local
        draft = ""
        sending = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { api.sendAiCompanionMessage(companion.id, content) } }
                .onSuccess { response ->
                    val returned = parseMessages(response.optJSONArray("messages"))
                    if (returned.isNotEmpty()) {
                        messages = messages.dropLast(1) + returned
                        val latest = returned.last()
                        replaceCompanion(companion.copy(latestMessage = latest.content, latestMessageAt = latest.createdAt))
                    }
                }
                .onFailure {
                    messages = messages + AiConversationMessage(
                        UUID.randomUUID().toString(), "ai", "网络有点慢，请稍后再试。", System.currentTimeMillis()
                    )
                }
            sending = false
        }
    }

    fun updateListBackground(backgroundUrl: String?) {
        if (listBackgroundSaving) return
        listBackgroundSaving = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.updateAiCompanionListBackground(backgroundUrl) }
            }.onSuccess {
                onUserChanged(user.copy(aiCompanionListBackgroundUrl = backgroundUrl))
                error = ""
            }.onFailure { error = it.companionError("列表背景更新失败") }
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
                error = ""
            }.onFailure { error = it.companionError("聊天背景更新失败") }
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
                error = ""
            }.onFailure { error = it.companionError("列表背景上传失败") }
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
                error = ""
            }.onFailure { error = it.companionError("聊天背景上传失败") }
            chatBackgroundSaving = false
        }
    }

    BackHandler(enabled = studioTarget != null || selected != null) {
        when {
            studioTarget != null -> studioTarget = null
            selected != null -> {
                selected = null
                error = ""
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
                loading = loading,
                error = error,
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
                messages = messages,
                draft = draft,
                sending = sending,
                error = error,
                onBack = { selected = null; error = ""; onChatStateChange(false) },
                onDraftChange = { draft = it.take(500) },
                onSend = ::sendMessage,
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
                if (selected?.id == deletedId) {
                    selected = null
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
    user: AppUser, companion: AiCompanion, messages: List<AiConversationMessage>, draft: String,
    sending: Boolean, error: String, onBack: () -> Unit, onDraftChange: (String) -> Unit,
    onSend: () -> Unit, onEdit: () -> Unit, onStudio: () -> Unit,
    backgroundSaving: Boolean, onChooseBackground: () -> Unit, onClearBackground: () -> Unit
) {
    val listState = rememberLazyListState()
    var showMore by remember { mutableStateOf(false) }
    val background by rememberUriImage(companion.chatBackgroundUrl, maxDimensionPx = 2048)
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }
    Box(Modifier.fillMaxSize().background(Color(0xFFEDEDED))) {
        BackgroundLayer(background, overlayAlpha = 0.34f)
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Surface(color = Color(0xFFF7F7F7).copy(alpha = if (background == null) 1f else 0.9f), shadowElevation = 1.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = CompanionInk) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(companion.displayName, color = CompanionInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(companion.relation, color = CompanionMuted, fontSize = 10.sp)
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
            if (error.isNotBlank()) {
                Surface(color = Color.White.copy(alpha = 0.82f)) {
                    Text(error, color = Color(0xFFB42318), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(), state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    WeChatMessageRow(message = message, user = user, companion = companion)
                }
            }
            Surface(color = Color(0xFFF7F7F7).copy(alpha = if (background == null) 1f else 0.92f), shadowElevation = 4.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        enabled = draft.isNotBlank() && !sending,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF07C160)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (sending) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Rounded.Send, "发送")
                    }
                }
            }
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
    companion: AiCompanion
) {
    val mine = message.sender == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!mine) {
            AiCompanionAvatar(companion.displayName, companion.avatarUrl, 40.dp)
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 280.dp)
                .width(IntrinsicSize.Max),
            color = if (mine) Color(0xFF95EC69) else Color.White,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                message.content,
                color = CompanionInk,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
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
    val scope = rememberCoroutineScope()
    var name by remember(companion?.id) { mutableStateOf(companion?.displayName.orEmpty()) }
    var relation by remember(companion?.id) { mutableStateOf(companion?.relation.orEmpty()) }
    var manualMemory by remember(companion?.id) { mutableStateOf("") }
    var memories by remember(companion?.id) { mutableStateOf(emptyList<AiCompanionMemory>()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingCreatedCompanion by remember(companion?.id) { mutableStateOf<AiCompanion?>(null) }

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
                OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("姓名或名称（必填）") }, singleLine = true, colors = companionTextFieldColors())
                OutlinedTextField(relation, { relation = it.take(30) }, Modifier.fillMaxWidth(), label = { Text("对方是我的（必填）") }, placeholder = { Text("例如：儿子、朋友、宠物或旧物") }, singleLine = true, colors = companionTextFieldColors())
                OutlinedTextField(manualMemory, { manualMemory = it.take(240) }, Modifier.fillMaxWidth(), label = { Text(if (companion == null) "初始记忆（可选）" else "手动记忆（可选）") }, enabled = !saving, minLines = 2, maxLines = 4, colors = companionTextFieldColors())
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
                                    val saved = parseCompanion(response.optJSONObject("companion") ?: response)
                                    attemptSaved = saved
                                    if (companion == null && manualMemory.isNotBlank()) api.createAiCompanionMemory(saved.id, manualMemory.trim())
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

private fun parseMessages(array: JSONArray?): List<AiConversationMessage> {
    if (array == null) return emptyList()
    return List(array.length()) { index ->
        val item = array.optJSONObject(index) ?: JSONObject()
        AiConversationMessage(
            item.optString("id").ifBlank { UUID.randomUUID().toString() },
            if (item.optString("sender") == "user") "user" else "ai",
            item.optString("content"),
            item.optLong("createdAt").takeIf { it > 0L } ?: System.currentTimeMillis()
        )
    }.filter { it.content.isNotBlank() }
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
        detail.contains("companion_not_found") -> "该陪伴对象已不存在"
        detail.contains("relation_direction_required") -> "请填写单向关系，例如儿子、爸爸、哥哥或妹妹"
        detail.contains("rate_limit") -> "操作太频繁，请稍后再试"
        detail.isBlank() -> fallback
        else -> "$fallback：$detail"
    }
}
