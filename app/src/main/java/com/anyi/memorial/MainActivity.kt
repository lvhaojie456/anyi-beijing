package com.anyi.memorial

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anyi.memorial.network.AnyiApiClient
import com.anyi.memorial.network.AnyiApiException
import com.anyi.memorial.network.UploadPayload
import com.anyi.memorial.wechat.WechatAuthBridge
import com.anyi.memorial.wechat.WechatAuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.net.URL

private val Background = Color(0xFFF5F5F7)
private val Ink = Color(0xFF1D1D1F)
private val Muted = Color(0xFF6E6E73)
private val Line = Color(0xFFE1DCCF)
private val Green = Color(0xFF9A6B2F)
private val Leaf = Color(0xFFFFF4DB)
private val Amber = Color(0xFFE6B84A)
private val Blush = Color(0xFFFFF1E7)
private val BlueMist = Color(0xFFFFF8EA)
private val Rose = Color(0xFFC86D4F)
private val Lavender = Color(0xFFFFF4DC)
private val Morning = Color(0xFFFFFAEF)
private val Night = Color(0xFF2B2418)
private val Paper = Color(0xFFFFFEFB)
private val HallWarmBackground = Color(0xFFFFF2DE)
private val HallWarmSurface = Color(0xFFFFFBF2)

private const val PREFS_NAME = "anyi_memorial_app"
private const val KEY_USER_ID = "session_user_id"
private const val KEY_USER_NAME = "session_username"
private const val KEY_DISPLAY_NAME = "session_display_name"
private const val KEY_USER_ROLE = "session_role"
private const val KEY_USER_AVATAR = "session_avatar_url"
private const val KEY_AUTH_TOKEN = "session_token"
private const val KEY_AI_CHAT = "ai_chat_messages"
private const val DURIAN_OFFERING_FEATURE = "offering_durian"
private const val OFFERING_DURATION_MS = 10L * 60L * 1000L

data class AppUser(
    val id: String,
    val username: String,
    val displayName: String,
    val role: String,
    val token: String,
    val avatarUrl: String? = null
)

data class FruitOffering(
    val type: String,
    val until: Long
)

data class FlowerOffering(
    val type: String,
    val until: Long
)

data class FlowerChoice(
    val type: String,
    val name: String,
    val subtitle: String,
    val imageResId: Int
)

data class CommunityPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarUrl: String?,
    val content: String,
    val imageUrls: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val likedByMe: Boolean,
    val createdAt: Long
)

data class CommunityComment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarUrl: String?,
    val content: String,
    val createdAt: Long
)

data class CommunityVolunteerPost(
    val id: String,
    val title: String,
    val body: String,
    val contact: String,
    val imageUrl: String? = null,
    val createdAt: Long
)

data class CommunityVolunteerApplication(
    val id: String,
    val volunteerPostId: String,
    val volunteerTitle: String,
    val applicantId: String,
    val applicantName: String,
    val applicantUsername: String,
    val applicantAvatarUrl: String?,
    val name: String,
    val phone: String,
    val note: String,
    val status: String,
    val createdAt: Long
)

data class ChatMessage(
    val id: String,
    val sender: String,
    val content: String,
    val createdAt: Long
)

data class AvatarMotionBox(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

data class AvatarMotion(
    val status: String,
    val source: String,
    val confidence: Float,
    val face: AvatarMotionBox,
    val mouth: AvatarMotionBox
)

data class AiCompanion(
    val id: String,
    val displayName: String,
    val gender: String,
    val relation: String,
    val avatarUrl: String?,
    val smileAvatarUrl: String?,
    val avatarMotion: AvatarMotion?,
    val paidUnlocked: Boolean,
    val photoCount: Int,
    val voiceCount: Int,
    val momentCount: Int,
    val generated: Boolean,
    val isDefault: Boolean,
    val updatedAt: Long
)

enum class ScreenTab(
    val title: String,
    val icon: ImageVector
) {
    Hall("云端纪念馆", Icons.Rounded.Cloud),
    Companion("AI陪伴", Icons.Rounded.ChatBubble),
    Community("人文社区", Icons.AutoMirrored.Rounded.Article),
    Profile("个人设置", Icons.Rounded.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashReporter(applicationContext)
        enableEdgeToEdge()
        setContent {
            AnyiTheme {
                AnyiRoot()
            }
        }
    }
}

@Composable
private fun AnyiTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Green,
        secondary = Amber,
        tertiary = Amber,
        background = Background,
        surface = Paper,
        surfaceVariant = Color(0xFFF8F4EA),
        outline = Line,
        outlineVariant = Line.copy(alpha = 0.7f),
        onPrimary = Color.White,
        onSecondary = Ink,
        onBackground = Ink,
        onSurface = Ink
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun AnyiRoot() {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(readSession(context)) }

    LaunchedEffect(currentUser?.token) {
        val user = currentUser ?: return@LaunchedEffect
        val result = runCatching {
            withContext(Dispatchers.IO) {
                AnyiApiClient(tokenProvider = { user.token }).currentUser()
            }
        }
        result.onFailure { error ->
            if (error.isAuthExpired()) {
                clearSession(context)
                currentUser = null
            }
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onSignedIn = { user ->
                writeSession(context, user)
                currentUser = user
            }
        )
    } else {
        MainScaffold(
            user = currentUser!!,
            onUserChanged = { user ->
                writeSession(context, user)
                currentUser = user
            },
            onLogout = {
                clearSession(context)
                currentUser = null
            }
        )
    }
}

@Composable
private fun AuthScreen(onSignedIn: (AppUser) -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val api = remember { AnyiApiClient() }
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var serverMessage by rememberSaveable { mutableStateOf("正在检查云端服务...") }
    var loading by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = runCatching {
            withContext(Dispatchers.IO) { api.health() }
        }
        serverMessage = result.fold(
            onSuccess = { "云端服务正常，可以登录或注册" },
            onFailure = { it.userFriendlyMessage("云端服务暂时不可用") }
        )
    }

    AppBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ComposeImage(
                painter = painterResource(id = R.drawable.anyi_launch_clouds),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                alpha = 0.38f
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Night, Green, Amber.copy(alpha = 0.9f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("安忆", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("把想念安放在云端", fontSize = 14.sp, color = Muted)
                Spacer(Modifier.height(24.dp))

                Panel {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Leaf.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        AuthModeButton("登录", !isRegister) {
                            isRegister = false
                            message = ""
                        }
                        AuthModeButton("注册", isRegister) {
                            isRegister = true
                            message = ""
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val currentActivity = activity
                            if (currentActivity == null) {
                                message = "当前页面暂时不能唤起微信登录"
                                return@Button
                            }
                            loading = true
                            WechatAuthBridge.startLogin(currentActivity) { result ->
                                when (result) {
                                    is WechatAuthResult.Success -> {
                                        scope.launch {
                                            val signedIn = runCatching {
                                                withContext(Dispatchers.IO) {
                                                    parseSignedInUser(api.loginWithWechat(result.code))
                                                }
                                            }
                                            loading = false
                                            signedIn
                                                .onSuccess { onSignedIn(it) }
                                                .onFailure { message = it.userFriendlyMessage("微信登录失败，请稍后再试") }
                                        }
                                    }
                                    is WechatAuthResult.Failure -> {
                                        loading = false
                                        message = result.message
                                    }
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1AAD19),
                            contentColor = Color.White,
                            disabledContainerColor = Line.copy(alpha = 0.72f),
                            disabledContentColor = Muted
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubble,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (loading) "正在连接微信..." else "微信一键登录", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("或使用账号密码登录", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        label = { Text("账号") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        colors = warmTextFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        label = { Text("密码") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = warmTextFieldColors()
                    )

                    if (isRegister) {
                        Spacer(Modifier.height(12.dp))
                        AssistChip(
                            onClick = { username = "admin" },
                            label = { Text("注册 admin 后可登录 Web 管理后台") },
                            leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) }
                        )
                    }

                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        StatusMessage(message = message)
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            val trimmed = username.trim()
                            when {
                                trimmed.isBlank() || password.isBlank() -> {
                                    message = "请输入账号和密码"
                                }
                                isRegister -> {
                                    if (password.length < 8) {
                                        message = "密码至少需要 8 位"
                                        return@Button
                                    }
                                    loading = true
                                    scope.launch {
                                        val result = runCatching {
                                            withContext(Dispatchers.IO) {
                                                parseSignedInUser(api.register(trimmed, password, trimmed))
                                            }
                                        }
                                        loading = false
                                        result
                                            .onSuccess { user ->
                                                message = if (user.role == "admin") "管理员账号已创建，请使用 Web 后台管理订单" else "账号已创建并同步到云端"
                                                onSignedIn(user)
                                            }
                                            .onFailure { message = it.userFriendlyMessage("注册失败，请检查网络或账号密码") }
                                    }
                                }
                                else -> {
                                    loading = true
                                    scope.launch {
                                        val result = runCatching {
                                            withContext(Dispatchers.IO) {
                                                parseSignedInUser(api.login(trimmed, password))
                                            }
                                        }
                                        loading = false
                                        result
                                            .onSuccess { onSignedIn(it) }
                                            .onFailure { message = it.userFriendlyMessage("登录失败，请确认账号密码") }
                                    }
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = primaryButtonColors(),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(if (loading) "正在连接云端..." else if (isRegister) "注册并进入" else "登录")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftTag("云端纪念")
                    SoftTag("数字陪伴")
                    SoftTag("人文社区")
                }
                Spacer(Modifier.height(10.dp))
                StatusMessage(message = serverMessage)
            }
        }
    }
}

@Composable
private fun StatusMessage(message: String, modifier: Modifier = Modifier) {
    if (message.isBlank()) return
    val error = messageLooksLikeProblem(message)
    val accent = if (error) Color(0xFFB42318) else Green
    val background = if (error) Color(0xFFFFF2EE) else Leaf.copy(alpha = 0.88f)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (error) Icons.Rounded.FavoriteBorder else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                message,
                color = if (error) Color(0xFF7A271A) else Green,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun messageLooksLikeProblem(message: String): Boolean {
    return listOf("失败", "错误", "超时", "过期", "不正确", "请先", "不能", "未配置", "未通过", "没有")
        .any { message.contains(it) }
}

@Composable
private fun SoftTag(text: String) {
    Surface(color = Color.White.copy(alpha = 0.78f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White)) {
        Text(
            text = text,
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun warmFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.White,
    labelColor = Muted,
    iconColor = Muted,
    selectedContainerColor = Leaf,
    selectedLabelColor = Green,
    selectedLeadingIconColor = Green,
    selectedTrailingIconColor = Green
)

@Composable
private fun warmTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green,
    unfocusedBorderColor = Line,
    focusedLabelColor = Green,
    unfocusedLabelColor = Muted,
    cursorColor = Green,
    focusedLeadingIconColor = Green,
    unfocusedLeadingIconColor = Muted,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

@Composable
private fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Ink,
    contentColor = Color.White,
    disabledContainerColor = Line.copy(alpha = 0.72f),
    disabledContentColor = Muted
)

@Composable
private fun quietOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    containerColor = Color.White.copy(alpha = 0.82f),
    contentColor = Ink,
    disabledContentColor = Muted
)

@Composable
private fun RowScope.AuthModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color.White else Color.Transparent
    val fg = if (selected) Ink else Muted
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .background(bg, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MainScaffold(
    user: AppUser,
    onUserChanged: (AppUser) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(ScreenTab.Hall) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnyiBottomBar(selectedTab = selectedTab, onSelected = { selectedTab = it })
        }
) { innerPadding ->
        val warmSurface = selectedTab == ScreenTab.Hall || selectedTab == ScreenTab.Community
        AppBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            backgroundColor = if (warmSurface) HallWarmBackground else Background,
            warmHall = warmSurface
        ) {
            val contentModifier = Modifier
                .fillMaxSize()
                .then(
                    if (selectedTab == ScreenTab.Hall) {
                        Modifier.padding(top = 18.dp)
                    } else {
                        Modifier
                            .statusBarsPadding()
                            .padding(top = 10.dp)
                    }
                )
                .padding(horizontal = 18.dp)
            Column(
                modifier = contentModifier
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        ScreenTab.Hall -> MemorialHallScreen(user)
                        ScreenTab.Companion -> AiCompanionScreen(user)
                        ScreenTab.Community -> HumanitiesCommunityScreen(user = user)
                        ScreenTab.Profile -> ProfileSettingsScreen(
                            user = user,
                            onUserChanged = onUserChanged,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBackdrop(
    modifier: Modifier = Modifier,
    imageResId: Int? = null,
    imageAlpha: Float = 0f,
    backgroundColor: Color = Background,
    warmHall: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.background(backgroundColor)
    ) {
        if (warmHall) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFE7C7),
                                Color(0xFFFFF5E5),
                                Color(0xFFFFEFD8)
                            )
                        )
                    )
            )
        }
        if (imageResId != null) {
            ComposeImage(
                painter = painterResource(id = imageResId),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = imageAlpha
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Background.copy(alpha = 0.2f))
            )
        }
        content()
    }
}

@Composable
private fun AnyiBottomBar(selectedTab: ScreenTab, onSelected: (ScreenTab) -> Unit) {
    val hallMode = selectedTab == ScreenTab.Hall || selectedTab == ScreenTab.Community
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (hallMode) HallWarmSurface.copy(alpha = 0.98f) else Color.White.copy(alpha = 0.98f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (hallMode) Color(0xFFEAD7B8) else Line.copy(alpha = 0.82f)),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScreenTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                selected && hallMode -> Color(0xFFFFE9BE)
                                selected -> Leaf
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (selected) Green else Muted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            tab.title,
                            color = if (selected) Green else Muted,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTopBar(user: AppUser, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(user.token) { AnyiApiClient(tokenProvider = { user.token }) }
    var showLegal by remember { mutableStateOf(false) }
    var legalMessage by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.65f)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(Night, Green))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Amber, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("安忆", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.username, color = Muted, fontSize = 12.sp)
                    if (user.role == "admin") {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = Leaf, shape = RoundedCornerShape(8.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Green,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("管理员", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (user.role == "admin") {
                TextButton(onClick = { context.openUrl(adminUrl()) }) {
                    Text("后台", color = Green, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = { showLegal = true }) {
                Text("协议", color = Green, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "退出登录", tint = Muted)
            }
        }
    }

    if (showLegal) {
        AlertDialog(
            onDismissRequest = { showLegal = false },
            containerColor = Paper,
            shape = RoundedCornerShape(8.dp),
            title = { Text("协议与账号") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("投诉电话：+8619310425540", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("客服邮箱：544908186@qq.com", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    LegalLinkButton("隐私政策") { context.openUrl(legalUrl("privacy")) }
                    LegalLinkButton("用户协议") { context.openUrl(legalUrl("terms")) }
                    LegalLinkButton("AI 免责声明") { context.openUrl(legalUrl("ai-disclaimer")) }
                    LegalLinkButton("账号注销页面") { context.openUrl(legalUrl("account-deletion")) }
                    if (legalMessage.isNotBlank()) {
                        Text(legalMessage, color = Muted, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = runCatching {
                                withContext(Dispatchers.IO) { api.deleteAccount() }
                            }
                            result
                                .onSuccess {
                                    legalMessage = "账号已注销"
                                    showLegal = false
                                    onLogout()
                                }
                                .onFailure { legalMessage = it.userFriendlyMessage("注销失败") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB42318))
                ) {
                    Text("注销当前账号")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLegal = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun LegalLinkButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line),
        colors = quietOutlinedButtonColors()
    ) {
        Text(text)
    }
}

@Composable
private fun ProfileSettingsScreen(
    user: AppUser,
    onUserChanged: (AppUser) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(user.token) { AnyiApiClient(tokenProvider = { user.token }) }
    var displayName by rememberSaveable(user.id) { mutableStateOf(user.displayName) }
    var avatarUrl by rememberSaveable(user.id) { mutableStateOf(user.avatarUrl.orEmpty()) }
    var message by rememberSaveable(user.id) { mutableStateOf("") }
    var loading by rememberSaveable(user.id) { mutableStateOf(false) }
    val avatar by rememberUriImage(avatarUrl)

    fun saveProfile(nextAvatarUrl: String?) {
        val nextName = displayName.trim().ifBlank { user.username }
        loading = true
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val updated = api.updateCurrentUser(nextName, nextAvatarUrl).getJSONObject("user")
                    parseUserPayload(updated, user.token)
                }
            }
            loading = false
            result
                .onSuccess { updated ->
                    displayName = updated.displayName
                    avatarUrl = updated.avatarUrl.orEmpty()
                    message = "个人资料已保存"
                    onUserChanged(updated)
                }
                .onFailure { message = it.userFriendlyMessage("保存资料失败") }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.persistReadPermission(uri)
        loading = true
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val payload = context.readUploadPayload(uri)
                    val asset = api.uploadAsset(
                        scope = "profiles",
                        fileName = payload.fileName,
                        mimeType = payload.mimeType,
                        bytes = payload.bytes
                    ).getJSONObject("asset")
                    val uploadedUrl = asset.getString("url")
                    val updated = api.updateCurrentUser(
                        displayName.trim().ifBlank { user.username },
                        uploadedUrl
                    ).getJSONObject("user")
                    parseUserPayload(updated, user.token)
                }
            }
            loading = false
            result
                .onSuccess { updated ->
                    displayName = updated.displayName
                    avatarUrl = updated.avatarUrl.orEmpty()
                    message = "头像已更新"
                    onUserChanged(updated)
                }
                .onFailure { message = it.userFriendlyMessage("上传头像失败") }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenIntro(
            title = "个人设置",
            subtitle = "头像、昵称和账号状态",
            icon = Icons.Rounded.Person,
            accent = Green
        )

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Night, Green, Amber.copy(alpha = 0.86f))))
                        .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarBitmap = avatar
                    if (avatarBitmap != null) {
                        ComposeImage(
                            bitmap = avatarBitmap,
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName.ifBlank { user.username }, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text("账号：${user.username}", color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (user.role == "admin") {
                        Spacer(Modifier.height(6.dp))
                        Surface(color = Leaf, shape = RoundedCornerShape(8.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = Green, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("管理员", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { avatarPicker.launch("image/*") },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Line),
                colors = quietOutlinedButtonColors()
            ) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("更换头像", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                label = { Text("昵称") },
                leadingIcon = { Icon(Icons.Rounded.Face, contentDescription = null) },
                colors = warmTextFieldColors()
            )

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { saveProfile(avatarUrl.takeIf { it.isNotBlank() }) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = primaryButtonColors(),
                contentPadding = PaddingValues(vertical = 13.dp)
            ) {
                Text(if (loading) "正在保存..." else "保存个人资料", fontWeight = FontWeight.Bold)
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                StatusMessage(message = message)
            }
        }

        if (user.role == "admin") {
            Panel {
                SectionTitle("管理后台", "订单验收、用户沟通和审核都在 Web 后台处理")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { context.openUrl(adminUrl()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line),
                    colors = quietOutlinedButtonColors()
                ) {
                    Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = Ink)
                    Spacer(Modifier.width(8.dp))
                    Text("打开 Web 管理后台", color = Ink, fontWeight = FontWeight.Bold)
                }
            }
        }

        Panel {
            SectionTitle("协议与客服", "隐私政策、用户协议和账号注销入口")
            Spacer(Modifier.height(10.dp))
            Text("投诉电话：+8619310425540", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("客服邮箱：544908186@qq.com", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LegalLinkButton("隐私政策") { context.openUrl(legalUrl("privacy")) }
            Spacer(Modifier.height(8.dp))
            LegalLinkButton("用户协议") { context.openUrl(legalUrl("terms")) }
            Spacer(Modifier.height(8.dp))
            LegalLinkButton("AI 免责声明") { context.openUrl(legalUrl("ai-disclaimer")) }
            Spacer(Modifier.height(8.dp))
            LegalLinkButton("账号注销页面") { context.openUrl(legalUrl("account-deletion")) }
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFF04438).copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB42318)),
            contentPadding = PaddingValues(vertical = 13.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("退出账号", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MemorialHallScreen(user: AppUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(user.token) { AnyiApiClient(tokenProvider = { user.token }) }
    var memorialId by remember { mutableStateOf<String?>(null) }
    var portraitUri by remember { mutableStateOf<String?>(null) }
    var memorialName by remember { mutableStateOf("") }
    var flowerUntilList by remember { mutableStateOf(emptyList<Long>()) }
    var flowerOfferings by remember { mutableStateOf(emptyList<FlowerOffering>()) }
    var candleUntil by remember { mutableStateOf(0L) }
    var candleUntilList by remember { mutableStateOf(emptyList<Long>()) }
    var incenseUntil by remember { mutableStateOf(0L) }
    var fruitOfferings by remember { mutableStateOf(emptyList<FruitOffering>()) }
    var paidUnlocked by remember { mutableStateOf(false) }
    var durianUnlocked by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }
    var showDurianPayment by remember { mutableStateOf(false) }
    var showFlowerPicker by remember { mutableStateOf(false) }
    var showBurnPicker by remember { mutableStateOf(false) }
    var burnPaperAnimationKey by remember { mutableStateOf(0) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showHallMenu by rememberSaveable { mutableStateOf(false) }
    var cloudMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val portrait by rememberUriImage(portraitUri)

    fun applyMemorial(json: JSONObject) {
        memorialId = json.optString("id").takeIf { it.isNotBlank() }
        memorialName = json.optString("name")
        portraitUri = json.optString("imageUrl").takeIf { it.isNotBlank() && it != "null" }
        flowerUntilList = parseLongArray(json.optJSONArray("flowerUntil"))
        flowerOfferings = parseMemorialFlowers(json.optJSONArray("flowerOfferings"), flowerUntilList)
        candleUntil = json.optLong("candleUntil", 0L)
        candleUntilList = parseLongArray(json.optJSONArray("candleUntilList")).ifEmpty {
            listOf(candleUntil).filter { it > System.currentTimeMillis() }
        }
        incenseUntil = json.optLong("incenseUntil", 0L)
        fruitOfferings = parseMemorialFruits(json.optJSONArray("fruitOfferings"))
    }

    fun refreshHall() {
        scope.launch {
            loading = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val memorials = api.listMemorials()
                    val unlock = api.featureUnlocked("hall_more")
                    val durianUnlock = api.featureUnlocked(DURIAN_OFFERING_FEATURE)
                    val first = if (memorials.length() > 0) memorials.getJSONObject(0) else null
                    Triple(first, unlock, durianUnlock)
                }
            }
            loading = false
            result
                .onSuccess { (memorial, unlocked, durianUnlock) ->
                    if (memorial != null) applyMemorial(memorial)
                    paidUnlocked = unlocked
                    durianUnlocked = durianUnlock
                    cloudMessage = "已连接云端纪念馆"
                }
                .onFailure { cloudMessage = it.userFriendlyMessage("云端纪念馆加载失败") }
        }
    }

    fun offerFlower(type: String) {
        val id = memorialId
        if (id == null) {
            cloudMessage = "请先保存纪念资料"
            return
        }
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.offerFlower(id, type) }
            }
            result
                .onSuccess {
                    flowerUntilList = parseLongArray(it.optJSONArray("flowerUntil"))
                    flowerOfferings = parseMemorialFlowers(it.optJSONArray("flowerOfferings"), flowerUntilList)
                    cloudMessage = "献花已同步到云端"
                    showFlowerPicker = false
                }
                .onFailure { cloudMessage = it.userFriendlyMessage("献花失败") }
        }
    }

    fun offerFruit(type: String, unlockDurian: Boolean = false) {
        val id = memorialId
        if (id == null) {
            cloudMessage = "请先保存纪念资料"
            return
        }
        scope.launch {
            loading = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    if (unlockDurian) api.unlockFeature(DURIAN_OFFERING_FEATURE)
                    api.offerFruit(id, type).getJSONArray("fruitOfferings")
                }
            }
            loading = false
            result
                .onSuccess {
                    fruitOfferings = parseMemorialFruits(it)
                    if (type == "durian") durianUnlocked = true
                    cloudMessage = if (type == "apple") "苹果已放到灵台" else "榴莲供品已放到灵台"
                }
                .onFailure { cloudMessage = it.userFriendlyMessage("供果失败") }
        }
    }

    fun lightIncense() {
        val id = memorialId
        if (id == null) {
            cloudMessage = "请先保存纪念资料"
            return
        }
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.lightIncense(id).optLong("incenseUntil") }
            }
            result
                .onSuccess {
                    incenseUntil = it
                    cloudMessage = "清香已上到灵台"
                }
                .onFailure { cloudMessage = it.userFriendlyMessage("上香失败") }
        }
    }

    fun saveMemorial(nextImageUrl: String? = portraitUri) {
        val cleanName = memorialName.trim()
        if (cleanName.isBlank()) {
            cloudMessage = "请先填写纪念名字"
            return
        }
        scope.launch {
            loading = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val response = if (memorialId == null) {
                        api.createMemorial(cleanName, nextImageUrl)
                    } else {
                        api.updateMemorial(memorialId!!, cleanName, nextImageUrl)
                    }
                    response.getJSONObject("memorial")
                }
            }
            loading = false
            result
                .onSuccess {
                    applyMemorial(it)
                    cloudMessage = "纪念资料已保存到云端"
                }
                .onFailure { cloudMessage = it.userFriendlyMessage("保存失败，请稍后再试") }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            context.persistReadPermission(uri)
            portraitUri = uri.toString()
            scope.launch {
                loading = true
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val upload = context.readUploadPayload(uri)
                        api.uploadAsset("memorials", upload.fileName, upload.mimeType, upload.bytes)
                            .getJSONObject("asset")
                            .getString("url")
                    }
                }
                loading = false
                result
                    .onSuccess { url ->
                        portraitUri = url
                        if (memorialName.isBlank()) memorialName = "我的纪念"
                        saveMemorial(url)
                    }
                    .onFailure { cloudMessage = it.userFriendlyMessage("图片上传失败") }
            }
        }
    }

    LaunchedEffect(user.token) {
        refreshHall()
    }

    LaunchedEffect(candleUntilList, flowerOfferings, fruitOfferings, incenseUntil) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val activeCandles = candleUntilList.filter { it > now }.sorted().take(2)
    val nextCandleMillis = (activeCandles.minOrNull()?.minus(now) ?: 0L).coerceAtLeast(0L)
    val activeFlowers = flowerOfferings.filter { it.until > now }.sortedBy { it.until }.take(2)
    val canOfferFlower = activeFlowers.size < 2
    val canLightCandle = activeCandles.size < 2
    val canLightIncense = incenseUntil <= now
    val nextIncenseMillis = (incenseUntil - now).coerceAtLeast(0L)
    val activeFruitOfferings = fruitOfferings.filter { it.until > now }
    val activeApples = activeFruitOfferings.filter { it.type == "apple" }.sortedBy { it.until }.take(3)
    val activeDurians = activeFruitOfferings.filter { it.type == "durian" }.sortedBy { it.until }.take(1)
    val canOfferApple = activeApples.size < 3
    val canOfferDurian = activeDurians.isEmpty()
    val nextAppleMillis = (activeApples.minOfOrNull { it.until - now } ?: 0L).coerceAtLeast(0L)
    val nextDurianMillis = (activeDurians.minOfOrNull { it.until - now } ?: 0L).coerceAtLeast(0L)
    val visibleCloudMessage = cloudMessage.takeUnless { it == "已连接云端纪念馆" }.orEmpty()

    if (showEditor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenIntro(
                title = "纪念资料",
                subtitle = "更改照片、名字与云端资料",
                icon = Icons.Rounded.Favorite,
                accent = Amber
            )
            OutlinedButton(
                onClick = { showEditor = false },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Line),
                colors = quietOutlinedButtonColors()
            ) {
                Text("返回灵台")
            }
            Panel(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("纪念材料", "上传图像并填写名字，灵台会同步展示")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = memorialName,
                    onValueChange = { memorialName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("纪念的人或宠物名字") },
                    leadingIcon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
                    colors = warmTextFieldColors()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line),
                    colors = quietOutlinedButtonColors()
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (loading) "正在上传..." else "上传纪念图像")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { saveMemorial() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("保存到云端")
                }
                if (cloudMessage.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    StatusMessage(message = cloudMessage)
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val messageSpace = if (visibleCloudMessage.isNotBlank()) 42.dp else 0.dp
                val stageHeight = (maxHeight - 204.dp - messageSpace).coerceIn(420.dp, 560.dp)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    MinimalHallHeader(
                        memorialName = memorialName,
                        onMenuClick = { showHallMenu = true }
                    )
                    Spacer(Modifier.height(6.dp))
                    MemorialStage(
                        memorialName = memorialName,
                        portrait = portrait,
                        activeCandles = activeCandles,
                        incenseUntil = incenseUntil,
                        now = now,
                        activeFlowers = activeFlowers,
                        fruitOfferings = activeFruitOfferings,
                        burnPaperAnimationKey = burnPaperAnimationKey,
                        stageHeight = stageHeight
                    )

                    if (visibleCloudMessage.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        StatusMessage(message = visibleCloudMessage)
                        Spacer(Modifier.height(6.dp))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }

                    HallActionStrip(
                        flowerText = if (canOfferFlower) "${activeFlowers.size}/2" else "冷却 ${formatRemaining(activeFlowers.first().until - now)}",
                        candleText = if (canLightCandle) "${activeCandles.size}/2" else "冷却 ${formatRemaining(nextCandleMillis)}",
                        incenseText = if (canLightIncense) "清香" else formatRemaining(nextIncenseMillis),
                        burnText = if (canOfferApple) "烧纸/苹果" else "苹果 ${formatRemaining(nextAppleMillis)}",
                        canOfferFlower = canOfferFlower,
                        canLightCandle = canLightCandle,
                        canLightIncense = canLightIncense,
                        onFlower = {
                            if (memorialId == null) {
                                cloudMessage = "请先保存纪念资料"
                            } else {
                                showFlowerPicker = true
                            }
                        },
                        onCandle = {
                            val id = memorialId
                            if (id == null) {
                                cloudMessage = "请先保存纪念资料"
                            } else {
                                scope.launch {
                                    val result = runCatching {
                                        withContext(Dispatchers.IO) {
                                            val response = api.lightCandle(id)
                                            response.optJSONArray("candleUntilList") ?: JSONArray().put(response.optLong("candleUntil"))
                                        }
                                    }
                                    result
                                        .onSuccess {
                                            candleUntilList = parseLongArray(it)
                                            candleUntil = candleUntilList.firstOrNull() ?: 0L
                                            cloudMessage = "蜡烛已同步到云端"
                                        }
                                        .onFailure { cloudMessage = it.userFriendlyMessage("点蜡烛失败") }
                                }
                            }
                        },
                        onIncense = ::lightIncense,
                        onBurn = {
                            if (memorialId == null) {
                                cloudMessage = "请先保存纪念资料"
                            } else {
                                showBurnPicker = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showPayment) {
        AlertDialog(
            onDismissRequest = { showPayment = false },
            containerColor = Paper,
            shape = RoundedCornerShape(8.dp),
            icon = { Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = Amber) },
            title = { Text("解锁云端纪念馆") },
            text = {
                Text("解锁后开放纪念相册、更多供品、专属布景等入口。当前先记录到云端，真实支付可继续接入支付回调。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = runCatching {
                                withContext(Dispatchers.IO) { api.unlockFeature("hall_more") }
                            }
                            result
                                .onSuccess {
                                    paidUnlocked = true
                                    cloudMessage = "解锁状态已同步到云端"
                                }
                                .onFailure { cloudMessage = it.userFriendlyMessage("解锁失败") }
                            showPayment = false
                        }
                    },
                    colors = primaryButtonColors()
                ) {
                    Text("云端解锁")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayment = false }) {
                    Text("稍后")
                }
            }
        )
    }

    if (showHallMenu) {
        AlertDialog(
            onDismissRequest = { showHallMenu = false },
            containerColor = Paper,
            shape = RoundedCornerShape(8.dp),
            icon = { Icon(Icons.Rounded.Menu, contentDescription = null, tint = Green) },
            title = { Text("纪念馆菜单") },
            text = {
                Text("在这里修改纪念照片和名字。", color = Muted)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHallMenu = false
                        showEditor = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = primaryButtonColors()
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("更改照片和名字")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHallMenu = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showFlowerPicker) {
        FlowerPickerDialog(
            activeCount = activeFlowers.size,
            onDismiss = { showFlowerPicker = false },
            onPick = { type -> offerFlower(type) }
        )
    }

    if (showBurnPicker) {
        BurnOfferingDialog(
            activeApples = activeApples.size,
            canOfferApple = canOfferApple,
            nextAppleMillis = nextAppleMillis,
            onDismiss = { showBurnPicker = false },
            onBurnPaper = {
                burnPaperAnimationKey += 1
                cloudMessage = "心意已化作纸火"
                showBurnPicker = false
            },
            onOfferApple = {
                offerFruit("apple")
                showBurnPicker = false
            }
        )
    }

    if (showDurianPayment) {
        AlertDialog(
            onDismissRequest = { showDurianPayment = false },
            containerColor = Paper,
            shape = RoundedCornerShape(8.dp),
            icon = { Icon(Icons.Rounded.Redeem, contentDescription = null, tint = Amber) },
            title = { Text("供奉榴莲") },
            text = { Text("榴莲属于付费供品。当前先用云端解锁记录模拟支付，接入真实支付后会改为支付成功回调再摆放。") },
            confirmButton = {
                Button(
                    onClick = {
                        offerFruit("durian", unlockDurian = true)
                        showDurianPayment = false
                    },
                    colors = primaryButtonColors()
                ) {
                    Text("付费供奉")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDurianPayment = false }) {
                    Text("稍后")
                }
            }
        )
    }
}

@Composable
private fun ScreenIntro(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    imageResId: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFE6B0), accent.copy(alpha = 0.72f), Green.copy(alpha = 0.5f))))
                .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageResId != null) {
                ComposeImage(
                    painter = painterResource(id = imageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(
            color = Leaf.copy(alpha = 0.86f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Line.copy(alpha = 0.6f))
        ) {
            Text(
                "安忆",
                color = Green,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun IntroBadge(text: String) {
    Surface(color = Color.White.copy(alpha = 0.68f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line.copy(alpha = 0.48f))) {
        Text(
            text,
            color = Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun MinimalHallHeader(memorialName: String, onMenuClick: () -> Unit) {
    val displayName = cleanMemorialDisplayName(memorialName)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "云端纪念馆",
                color = Color(0xFF6F4A21),
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "$displayName · 让思念温暖永恒",
                color = Color(0xFF9B7A58),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = "修改照片和名字",
                tint = Color(0xFF8B5E24),
                modifier = Modifier.size(27.dp)
            )
        }
    }
}

@Composable
private fun MinimalHallAction(
    modifier: Modifier,
    title: String,
    detail: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(94.dp)
            .clickable(enabled = enabled, onClick = onClick),
        color = Paper.copy(alpha = if (enabled) 0.92f else 0.66f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (enabled) Line else Line.copy(alpha = 0.5f)),
        shadowElevation = if (enabled) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = accent.copy(alpha = if (enabled) 0.15f else 0.08f),
                shape = CircleShape
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) accent else Muted,
                    modifier = Modifier.padding(7.dp).size(18.dp)
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Spacer(Modifier.height(1.dp))
            Text(detail, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HallActionStrip(
    flowerText: String,
    candleText: String,
    incenseText: String,
    burnText: String,
    canOfferFlower: Boolean,
    canLightCandle: Boolean,
    canLightIncense: Boolean,
    onFlower: () -> Unit,
    onCandle: () -> Unit,
    onIncense: () -> Unit,
    onBurn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HallRitualAction(
                modifier = Modifier.weight(1f),
                title = "上香",
                detail = incenseText,
                icon = Icons.Rounded.AutoAwesome,
                accent = Green,
                enabled = canLightIncense,
                onClick = onIncense
            )
            HallRitualAction(
                modifier = Modifier.weight(1f),
                title = "献花",
                detail = flowerText,
                icon = if (canOfferFlower) Icons.Rounded.LocalFlorist else Icons.Rounded.CheckCircle,
                accent = Rose,
                enabled = canOfferFlower,
                onClick = onFlower
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HallRitualAction(
                modifier = Modifier.weight(1f),
                title = "点烛",
                detail = candleText,
                icon = Icons.Rounded.Whatshot,
                accent = Amber,
                enabled = canLightCandle,
                onClick = onCandle
            )
            HallRitualAction(
                modifier = Modifier.weight(1f),
                title = "烧供",
                detail = burnText,
                icon = Icons.Rounded.Redeem,
                accent = Green,
                enabled = true,
                onClick = onBurn
            )
        }
    }
}

@Composable
private fun HallRitualAction(
    modifier: Modifier,
    title: String,
    detail: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tint = if (enabled) accent else Muted
    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) Color(0xFFFFFBF3) else Color(0xFFF3E8D5).copy(alpha = 0.72f),
        shape = RoundedCornerShape(36.dp),
        border = BorderStroke(1.dp, Color(0xFFEBDCC6)),
        shadowElevation = if (enabled) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center) {
                Text(
                    title,
                    color = Color(0xFF7A5225),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    detail,
                    color = if (enabled) Color(0xFF9A7650) else Muted,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MinimalFruitOfferingPanel(
    activeApples: Int,
    activeDurians: Int,
    canOfferApple: Boolean,
    canOfferDurian: Boolean,
    durianUnlocked: Boolean,
    nextAppleMillis: Long,
    nextDurianMillis: Long,
    onOfferApple: () -> Unit,
    onOfferDurian: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Paper.copy(alpha = 0.78f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.82f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("供果", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text("苹果免费，榴莲付费", color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FruitOfferingButton(
                    modifier = Modifier.weight(1f),
                    type = "apple",
                    title = "苹果 $activeApples/3",
                    subtitle = if (canOfferApple) "放上灵台" else "冷却 ${formatRemaining(nextAppleMillis)}",
                    enabled = canOfferApple,
                    paid = false,
                    onClick = onOfferApple
                )
                FruitOfferingButton(
                    modifier = Modifier.weight(1f),
                    type = "durian",
                    title = "榴莲 $activeDurians/1",
                    subtitle = when {
                        !canOfferDurian -> "冷却 ${formatRemaining(nextDurianMillis)}"
                        durianUnlocked -> "已解锁"
                        else -> "付费供品"
                    },
                    enabled = canOfferDurian,
                    paid = true,
                    onClick = onOfferDurian
                )
            }
        }
    }
}

@Composable
private fun MemorialStage(
    memorialName: String,
    portrait: ImageBitmap?,
    activeCandles: List<Long>,
    incenseUntil: Long,
    now: Long,
    activeFlowers: List<FlowerOffering>,
    fruitOfferings: List<FruitOffering>,
    burnPaperAnimationKey: Int,
    stageHeight: Dp = 378.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(stageHeight)
            .clip(RoundedCornerShape(30.dp))
            .border(1.dp, Color.White.copy(alpha = 0.82f), RoundedCornerShape(30.dp))
    ) {
        ComposeImage(
            painter = painterResource(id = R.drawable.anyi_hall_memorial_bg_empty),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )

        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            StageOriginalFlowers(
                activeCount = activeFlowers.size,
                modifier = Modifier.matchParentSize()
            )

            StageForegroundOverlay(modifier = Modifier.matchParentSize())

            if (incenseUntil > now) {
                StageIncenseSticks(modifier = Modifier.matchParentSize())
            }

            if (portrait != null) {
                ComposeImage(
                    bitmap = portrait,
                    contentDescription = "纪念照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = maxHeight * 0.19f)
                        .width(maxWidth * 0.34f)
                        .height(maxHeight * 0.34f)
                )
            }

            MemorialTablet(
                memorialName = memorialName,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.49f)
            )

            CandleFlames(
                activeCandles = activeCandles,
                modifier = Modifier.matchParentSize()
            )

            PaperBurningAnimation(
                animationKey = burnPaperAnimationKey,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = (-70).dp)
                    .padding(top = maxHeight * 0.69f)
            )

            FruitOfferingsOnAltar(
                fruitOfferings = fruitOfferings,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.72f)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.815f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "思念从未远去",
                    color = Color(0xFF8D7154),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Text(
                    "爱在云端，永恒陪伴",
                    color = Color(0xFF8D7154),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(1.dp)
                            .background(Color(0xFFD8B987))
                    )
                    Icon(
                        Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = Color(0xFFD2A35D),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(13.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(1.dp)
                            .background(Color(0xFFD8B987))
                    )
                }
            }
        }
    }
}

@Composable
private fun MemorialPortraitFrame(portrait: ImageBitmap?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(width = 124.dp, height = 168.dp),
        color = Color(0xFFFFF9EE),
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE8D5B8)),
        shadowElevation = 5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .background(Color(0xFFFFFCF5))
                .border(1.dp, Color(0xFFEBDCC6)),
            contentAlignment = Alignment.Center
        ) {
            if (portrait != null) {
                ComposeImage(
                    bitmap = portrait,
                    contentDescription = "纪念照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MemorialMountainArtwork(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun MemorialMountainArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color(0xFFFFF5E8))) {
        drawCircle(
            color = Color.White.copy(alpha = 0.86f),
            radius = size.width * 0.13f,
            center = Offset(size.width * 0.72f, size.height * 0.28f)
        )
        val rearMountain = Path().apply {
            moveTo(0f, size.height * 0.72f)
            cubicTo(size.width * 0.2f, size.height * 0.58f, size.width * 0.24f, size.height * 0.42f, size.width * 0.42f, size.height * 0.54f)
            cubicTo(size.width * 0.58f, size.height * 0.66f, size.width * 0.7f, size.height * 0.58f, size.width, size.height * 0.72f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(rearMountain, color = Color(0xFFEADBC7).copy(alpha = 0.75f))

        val frontMountain = Path().apply {
            moveTo(0f, size.height * 0.82f)
            cubicTo(size.width * 0.25f, size.height * 0.66f, size.width * 0.42f, size.height * 0.7f, size.width * 0.56f, size.height * 0.78f)
            cubicTo(size.width * 0.72f, size.height * 0.88f, size.width * 0.86f, size.height * 0.72f, size.width, size.height * 0.8f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(frontMountain, color = Color(0xFFDCCBB7).copy(alpha = 0.82f))
    }
}

@Composable
private fun StageOriginalFlowers(activeCount: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        if (activeCount >= 1) {
            StageOriginalOverlay(
                imageResId = R.drawable.anyi_hall_stage_flower_original_left,
                modifier = Modifier.matchParentSize()
            )
        }
        if (activeCount >= 2) {
            StageOriginalOverlay(
                imageResId = R.drawable.anyi_hall_stage_flower_original_right,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun StageOriginalOverlay(imageResId: Int, modifier: Modifier = Modifier) {
    ComposeImage(
        painter = painterResource(id = imageResId),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}

@Composable
private fun StageForegroundOverlay(modifier: Modifier = Modifier) {
    ComposeImage(
        painter = painterResource(id = R.drawable.anyi_hall_stage_foreground_empty),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}

@Composable
private fun HallSideFlowers(activeFlowers: List<FlowerOffering>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        HallFlowerBouquet(
            flower = activeFlowers.getOrNull(0),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-28).dp, y = 12.dp)
                .size(width = 126.dp, height = 126.dp)
        )
        HallFlowerBouquet(
            flower = activeFlowers.getOrNull(1),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 28.dp, y = 12.dp)
                .size(width = 126.dp, height = 126.dp)
        )
    }
}

@Composable
private fun HallFlowerBouquet(flower: FlowerOffering?, modifier: Modifier = Modifier) {
    if (flower != null) {
        MemorialFlowerImage(type = flower.type, modifier = modifier)
    } else {
        ComposeImage(
            painter = painterResource(id = R.drawable.anyi_hall_flower_lily),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}

@Composable
private fun MemorialTablet(memorialName: String, modifier: Modifier = Modifier) {
    val displayName = cleanMemorialDisplayName(memorialName).take(2)
    val tabletText = "${displayName}长存"
    Column(
        modifier = modifier
            .width(46.dp)
            .height(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        tabletText.forEach { char ->
            Text(
                char.toString(),
                color = Color(0xFF8B5E24),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AltarPlatform(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .width(296.dp)
            .height(84.dp)
    ) {
        drawOval(
            color = Color(0xFF6B4D2B).copy(alpha = 0.14f),
            topLeft = Offset(size.width * 0.11f, size.height * 0.8f),
            size = Size(size.width * 0.78f, size.height * 0.12f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFE6BF87), Color(0xFFC98B49))
            ),
            topLeft = Offset(size.width * 0.08f, size.height * 0.34f),
            size = Size(size.width * 0.84f, size.height * 0.28f),
            cornerRadius = CornerRadius(size.height * 0.08f, size.height * 0.08f)
        )
        drawRoundRect(
            color = Color(0xFFF8E1B7).copy(alpha = 0.72f),
            topLeft = Offset(size.width * 0.1f, size.height * 0.33f),
            size = Size(size.width * 0.8f, size.height * 0.06f),
            cornerRadius = CornerRadius(size.height * 0.04f, size.height * 0.04f)
        )
        drawRoundRect(
            color = Color(0xFFB77A3A),
            topLeft = Offset(size.width * 0.18f, size.height * 0.59f),
            size = Size(size.width * 0.045f, size.height * 0.27f),
            cornerRadius = CornerRadius(size.width * 0.02f, size.width * 0.02f)
        )
        drawRoundRect(
            color = Color(0xFFB77A3A),
            topLeft = Offset(size.width * 0.775f, size.height * 0.59f),
            size = Size(size.width * 0.045f, size.height * 0.27f),
            cornerRadius = CornerRadius(size.width * 0.02f, size.width * 0.02f)
        )
    }
}

@Composable
private fun IncenseOnAltar(active: Boolean, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .width(72.dp)
            .height(62.dp)
    ) {
        val bowlTop = size.height * 0.66f
        drawOval(
            color = Color(0xFF5A3920).copy(alpha = 0.18f),
            topLeft = Offset(size.width * 0.2f, size.height * 0.86f),
            size = Size(size.width * 0.6f, size.height * 0.08f)
        )
        drawOval(
            brush = Brush.verticalGradient(listOf(Color(0xFFB68759), Color(0xFF714321))),
            topLeft = Offset(size.width * 0.18f, bowlTop),
            size = Size(size.width * 0.64f, size.height * 0.25f)
        )
        drawOval(
            color = Color(0xFFE0B783),
            topLeft = Offset(size.width * 0.18f, bowlTop - size.height * 0.035f),
            size = Size(size.width * 0.64f, size.height * 0.12f)
        )
        drawOval(
            color = Color(0xFF775132),
            topLeft = Offset(size.width * 0.25f, bowlTop),
            size = Size(size.width * 0.5f, size.height * 0.06f)
        )

        val stickTop = size.height * 0.27f
        val stickHeight = size.height * 0.42f
        listOf(0.4f, 0.5f, 0.6f).forEachIndexed { index, xFraction ->
            val lean = (index - 1) * size.width * 0.035f
            drawRoundRect(
                color = if (active) Color(0xFF8C4B22) else Color(0xFFBDA281),
                topLeft = Offset(size.width * xFraction + lean, stickTop),
                size = Size(size.width * 0.024f, stickHeight),
                cornerRadius = CornerRadius(size.width * 0.012f, size.width * 0.012f)
            )
            if (active) {
                drawCircle(
                    color = Color(0xFFFFCF7A),
                    radius = size.width * 0.018f,
                    center = Offset(size.width * xFraction + lean + size.width * 0.012f, stickTop)
                )
            }
        }

        if (active) {
            listOf(0.42f, 0.53f).forEachIndexed { index, xFraction ->
                val smoke = Path().apply {
                    moveTo(size.width * xFraction, size.height * 0.28f)
                    cubicTo(
                        size.width * (xFraction - 0.12f),
                        size.height * 0.18f,
                        size.width * (xFraction + 0.15f),
                        size.height * 0.12f,
                        size.width * (xFraction + if (index == 0) 0.01f else -0.04f),
                        size.height * 0.02f
                    )
                }
                drawPath(smoke, color = Color.White.copy(alpha = 0.42f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.018f))
            }
        }
    }
}

@Composable
private fun PaperBurningAnimation(animationKey: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }

    LaunchedEffect(animationKey) {
        if (animationKey > 0) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2600, easing = FastOutSlowInEasing)
            )
        }
    }

    if (animationKey > 0 && progress.value < 0.998f) {
        Canvas(
            modifier = modifier
                .width(78.dp)
                .height(102.dp)
        ) {
            val burn = progress.value
            val remaining = (1f - burn).coerceIn(0f, 1f)
            val center = size.width * 0.5f
            val ashAlpha = burn.coerceAtMost(0.82f)
            val paperBottom = size.height * 0.82f
            val paperTop = size.height * (0.32f + burn * 0.28f)
            val paperHeight = (paperBottom - paperTop).coerceAtLeast(size.height * 0.04f)
            val paperWidth = size.width * (0.62f - burn * 0.28f).coerceAtLeast(0.18f)
            val charLine = paperTop + paperHeight * (0.15f + burn * 0.42f)

            drawOval(
                color = Color(0xFF4E3521).copy(alpha = 0.18f + ashAlpha * 0.18f),
                topLeft = Offset(size.width * 0.18f, size.height * 0.84f),
                size = Size(size.width * 0.64f, size.height * 0.08f)
            )

            listOf(-0.08f, 0.0f, 0.08f).forEachIndexed { index, shift ->
                val localWidth = paperWidth * (1f - index * 0.07f)
                val localTop = paperTop + size.height * index * 0.035f
                val left = center - localWidth / 2f + size.width * shift
                val paper = Path().apply {
                    moveTo(left + localWidth * 0.08f, localTop)
                    lineTo(left + localWidth * 0.92f, localTop + size.height * 0.02f)
                    lineTo(left + localWidth * (0.82f - burn * 0.22f), localTop + paperHeight)
                    lineTo(left + localWidth * (0.16f + burn * 0.12f), localTop + paperHeight * 0.96f)
                    close()
                }
                drawPath(
                    paper,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFE8A3).copy(alpha = 0.94f * remaining),
                            Color(0xFFE2B150).copy(alpha = 0.86f * remaining)
                        )
                    )
                )
                drawRoundRect(
                    color = Color(0xFF7A4A20).copy(alpha = 0.34f * remaining),
                    topLeft = Offset(left + localWidth * 0.31f, localTop + paperHeight * 0.22f),
                    size = Size(localWidth * 0.38f, size.height * 0.018f),
                    cornerRadius = CornerRadius(size.width * 0.01f, size.width * 0.01f)
                )
            }

            val charredEdge = Path().apply {
                moveTo(center - paperWidth * 0.34f, charLine)
                cubicTo(
                    center - paperWidth * 0.16f,
                    charLine - size.height * 0.08f,
                    center + paperWidth * 0.08f,
                    charLine + size.height * 0.05f,
                    center + paperWidth * 0.34f,
                    charLine - size.height * 0.03f
                )
                lineTo(center + paperWidth * 0.38f, paperBottom)
                lineTo(center - paperWidth * 0.38f, paperBottom)
                close()
            }
            drawPath(charredEdge, color = Color(0xFF3B271B).copy(alpha = 0.34f + burn * 0.36f))

            listOf(
                Triple(center - size.width * 0.16f, size.width * 0.24f, 0.85f),
                Triple(center, size.width * 0.32f, 1.08f),
                Triple(center + size.width * 0.15f, size.width * 0.22f, 0.78f)
            ).forEachIndexed { index, (flameCenter, flameWidth, flameScale) ->
                val fireBase = paperBottom - size.height * (0.02f + index * 0.01f)
                val flameHeight = size.height * (0.27f + (0.18f * remaining)) * flameScale
                val flame = Path().apply {
                    moveTo(flameCenter - flameWidth * 0.48f, fireBase)
                    cubicTo(
                        flameCenter - flameWidth * 0.42f,
                        fireBase - flameHeight * 0.42f,
                        flameCenter - flameWidth * 0.1f,
                        fireBase - flameHeight * 0.72f,
                        flameCenter,
                        fireBase - flameHeight
                    )
                    cubicTo(
                        flameCenter + flameWidth * 0.18f,
                        fireBase - flameHeight * 0.7f,
                        flameCenter + flameWidth * 0.46f,
                        fireBase - flameHeight * 0.36f,
                        flameCenter + flameWidth * 0.42f,
                        fireBase
                    )
                    close()
                }
                drawPath(
                    flame,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFF5B8).copy(alpha = 0.92f),
                            Color(0xFFFF9D39).copy(alpha = 0.9f),
                            Color(0xFFD94D24).copy(alpha = 0.78f)
                        )
                    )
                )
            }

            listOf(0.28f, 0.45f, 0.63f).forEachIndexed { index, xFraction ->
                val rise = burn * size.height * (0.36f + index * 0.08f)
                drawCircle(
                    color = Color(0xFF5D4230).copy(alpha = 0.18f * remaining),
                    radius = size.width * (0.022f + index * 0.004f),
                    center = Offset(size.width * xFraction, size.height * 0.76f - rise)
                )
            }

            val smoke = Path().apply {
                moveTo(center + size.width * 0.04f, paperTop - size.height * 0.02f)
                cubicTo(
                    center - size.width * 0.24f,
                    paperTop - size.height * (0.16f + burn * 0.08f),
                    center + size.width * 0.24f,
                    paperTop - size.height * (0.34f + burn * 0.12f),
                    center - size.width * 0.05f,
                    paperTop - size.height * (0.54f + burn * 0.16f)
                )
            }
            drawPath(
                smoke,
                color = Color.White.copy(alpha = 0.3f * remaining),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.035f)
            )
        }
    }
}

@Composable
private fun FruitOfferingsOnAltar(fruitOfferings: List<FruitOffering>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(128.dp)
            .height(62.dp)
    ) {
        val apples = fruitOfferings.filter { it.type == "apple" }.take(3)
        val hasDurian = fruitOfferings.any { it.type == "durian" }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 1.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy((-7).dp)
        ) {
            apples.forEachIndexed { index, _ ->
                FruitIcon(
                    type = "apple",
                    modifier = Modifier
                        .size(28.dp)
                        .offset(y = if (index == 1) (-4).dp else 0.dp)
                )
            }
            if (hasDurian) {
                FruitIcon(
                    type = "durian",
                    modifier = Modifier.size(width = 42.dp, height = 38.dp)
                )
            }
        }
    }
}

@Composable
private fun FruitIcon(type: String, modifier: Modifier = Modifier) {
    ComposeImage(
        painter = painterResource(
            id = if (type == "durian") R.drawable.anyi_hall_durian_real else R.drawable.anyi_hall_apple_real
        ),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun FruitOfferingButton(
    modifier: Modifier,
    type: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    paid: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(76.dp)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) Color.White else Leaf.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (enabled) Line else Amber.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FruitIcon(
                type = type,
                modifier = Modifier.size(if (type == "durian") 48.dp else 42.dp)
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = if (paid && enabled) Rose else Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!enabled) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
            } else if (paid) {
                Surface(color = Amber.copy(alpha = 0.22f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "付费",
                        color = Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowerPickerDialog(activeCount: Int, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = RoundedCornerShape(8.dp),
        icon = { Icon(Icons.Rounded.LocalFlorist, contentDescription = null, tint = Rose) },
        title = { Text("选择献花") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("当前已献 $activeCount/2，花会在 10 分钟后消失。", color = Muted, fontSize = 12.sp)
                flowerChoices().forEach { choice ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(choice.type) },
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Line.copy(alpha = 0.72f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemorialFlowerImage(
                                type = choice.type,
                                modifier = Modifier.size(width = 56.dp, height = 60.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(choice.name, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                Text(choice.subtitle, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun BurnOfferingDialog(
    activeApples: Int,
    canOfferApple: Boolean,
    nextAppleMillis: Long,
    onDismiss: () -> Unit,
    onBurnPaper: () -> Unit,
    onOfferApple: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = RoundedCornerShape(8.dp),
        icon = { Icon(Icons.Rounded.Redeem, contentDescription = null, tint = Green) },
        title = { Text("烧供") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("选择一份心意，纸火会在灵台前化开。", color = Muted, fontSize = 12.sp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBurnPaper),
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line.copy(alpha = 0.72f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Amber.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                            Icon(
                                Icons.Rounded.Whatshot,
                                contentDescription = null,
                                tint = Rose,
                                modifier = Modifier.padding(13.dp).size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("烧纸", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("播放纸火焚化动画", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canOfferApple, onClick = onOfferApple),
                    color = if (canOfferApple) Color.White else Leaf.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line.copy(alpha = 0.72f)),
                    shadowElevation = if (canOfferApple) 1.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color.White.copy(alpha = 0.86f), shape = RoundedCornerShape(8.dp)) {
                            FruitIcon(type = "apple", modifier = Modifier.padding(4.dp).size(44.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("供苹果 $activeApples/3", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text(
                                if (canOfferApple) "免费放上灵台，10 分钟后消失" else "冷却 ${formatRemaining(nextAppleMillis)}",
                                color = Muted,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun StandingFlowers(flowers: List<FlowerOffering>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(206.dp)
    ) {
        flowers.getOrNull(0)?.let { flower ->
            MemorialFlowerImage(
                type = flower.type,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-20).dp, y = 5.dp)
                    .size(width = 104.dp, height = 188.dp)
            )
        }
        flowers.getOrNull(1)?.let { flower ->
            MemorialFlowerImage(
                type = flower.type,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 5.dp)
                    .size(width = 104.dp, height = 188.dp)
            )
        }
    }
}

@Composable
private fun MemorialFlowerImage(type: String, modifier: Modifier = Modifier) {
    ComposeImage(
        painter = painterResource(id = flowerImageRes(type)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun CandleFlames(activeCandles: List<Long>, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        activeCandles.getOrNull(0)?.let {
            StageOriginalOverlay(
                imageResId = R.drawable.anyi_hall_stage_candle_flame_original_left,
                modifier = Modifier.matchParentSize()
            )
        }
        activeCandles.getOrNull(1)?.let {
            StageOriginalOverlay(
                imageResId = R.drawable.anyi_hall_stage_candle_flame_original_right,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun StageIncenseSticks(modifier: Modifier = Modifier) {
    StageOriginalOverlay(
        imageResId = R.drawable.anyi_hall_stage_incense_original,
        modifier = modifier
    )
}

@Composable
private fun CandlePair(activeCandles: List<Long>, now: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(318.dp)
            .height(86.dp)
    ) {
        activeCandles.getOrNull(0)?.let { until ->
            CandleSlot(
                untilMillis = until,
                now = now,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 24.dp)
            )
        }
        activeCandles.getOrNull(1)?.let { until ->
            CandleSlot(
                untilMillis = until,
                now = now,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-24).dp)
            )
        }
    }
}

@Composable
private fun CandleSlot(untilMillis: Long, now: Long, modifier: Modifier = Modifier) {
    val remaining = (untilMillis - now).coerceAtLeast(0L)
    val waxFraction = (remaining.toFloat() / OFFERING_DURATION_MS.toFloat()).coerceIn(0.18f, 1f)
    Canvas(modifier = modifier.size(width = 54.dp, height = 82.dp)) {
        val holderTop = size.height * 0.78f
        drawOval(
            color = Color(0xFF8A6A45).copy(alpha = 0.36f),
            topLeft = Offset(size.width * 0.12f, holderTop),
            size = Size(size.width * 0.76f, size.height * 0.16f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFFFF6DD), Color(0xFFE9B85E))),
            topLeft = Offset(size.width * 0.17f, holderTop - size.height * 0.04f),
            size = Size(size.width * 0.66f, size.height * 0.16f),
            cornerRadius = CornerRadius(size.width * 0.16f, size.width * 0.16f)
        )

        val maxWaxHeight = size.height * 0.36f
        val waxHeight = maxWaxHeight * waxFraction
        val waxTop = holderTop - waxHeight
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFFFFF7DF), Color(0xFFFFE4A8), Color(0xFFFFF9E9))
            ),
            topLeft = Offset(size.width * 0.18f, waxTop),
            size = Size(size.width * 0.64f, waxHeight),
            cornerRadius = CornerRadius(size.width * 0.14f, size.width * 0.14f)
        )
        drawOval(
            color = Color(0xFFFFFAEB),
            topLeft = Offset(size.width * 0.18f, waxTop - size.height * 0.032f),
            size = Size(size.width * 0.64f, size.height * 0.072f)
        )
        drawRoundRect(
            color = Color(0xFF6A4A27),
            topLeft = Offset(size.width * 0.485f, waxTop - size.height * 0.07f),
            size = Size(size.width * 0.03f, size.height * 0.08f),
            cornerRadius = CornerRadius(size.width * 0.02f, size.width * 0.02f)
        )
        drawOval(
            brush = Brush.verticalGradient(listOf(Color(0xFFFFF1A6), Color(0xFFFFA33D), Color(0xFFE95A30))),
            topLeft = Offset(size.width * 0.39f, waxTop - size.height * 0.22f),
            size = Size(size.width * 0.22f, size.height * 0.2f)
        )
    }
}

@Composable
private fun MemorialCandle(lit: Boolean, modifier: Modifier = Modifier) {
    ComposeImage(
        painter = painterResource(id = R.drawable.anyi_hall_altar_candle),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        alpha = if (lit) 1f else 0.86f,
        modifier = modifier.size(width = 32.dp, height = 82.dp)
    )
}

@Composable
private fun OfferingButton(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    accent: Color,
    imageResId: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(96.dp),
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.5f)),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            if (imageResId != null) {
                ComposeImage(
                    painter = painterResource(id = imageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = if (enabled) 0.92f else 0.48f,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.copy(alpha = if (enabled) 0.22f else 0.12f),
                                    Amber.copy(alpha = 0.16f)
                                )
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.38f),
                                Color.White.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                color = Color.White.copy(alpha = 0.84f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(icon, contentDescription = null, tint = if (enabled) accent else Muted, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        title,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (enabled) Ink else Muted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniFeature(text: String, icon: ImageVector, modifier: Modifier, imageResId: Int? = null) {
    Surface(
        modifier = modifier.height(118.dp),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.58f)),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageResId != null) {
                ComposeImage(
                    painter = painterResource(id = imageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Brush.linearGradient(listOf(Leaf, Color.White)))
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.02f),
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                color = Color.White.copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text,
                    color = Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun GradientActionButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        color = Paper.copy(alpha = 0.9f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = Leaf, shape = CircleShape) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.padding(7.dp).size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(text, color = Ink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("查看", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AiCompanionScreen(user: AppUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(user.token) { AnyiApiClient(tokenProvider = { user.token }) }
    val companions = remember { mutableStateListOf<AiCompanion>() }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var chatInput by rememberSaveable { mutableStateOf("") }
    var cloudMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sendingChat by remember { mutableStateOf(false) }
    var uploadingAvatar by rememberSaveable { mutableStateOf(false) }
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    val selected = companions.firstOrNull { it.id == selectedId }
    val selectedAvatarUrl = selected?.smileAvatarUrl ?: selected?.avatarUrl
    val selectedHasSmileAvatar = !selected?.smileAvatarUrl.isNullOrBlank()
    val selectedAvatar by rememberUriImage(selectedAvatarUrl)
    val selectedMotion = selected?.avatarMotion
    var createName by rememberSaveable { mutableStateOf("") }
    var createGender by rememberSaveable { mutableStateOf("女性") }
    var createRelation by rememberSaveable { mutableStateOf("母亲") }

    fun upsertCompanion(companion: AiCompanion) {
        val index = companions.indexOfFirst { it.id == companion.id }
        if (index >= 0) {
            companions[index] = companion
        } else {
            companions.add(0, companion)
        }
    }

    fun loadMessages(companionId: String) {
        scope.launch {
            loading = true
            val result = runCatching {
                withContext(Dispatchers.IO) { parseChatMessages(api.listAiMessages(companionId)) }
            }
            loading = false
            result
                .onSuccess { messages ->
                    chatMessages.clear()
                    chatMessages.addAll(messages)
                }
                .onFailure { cloudMessage = it.userFriendlyMessage("AI 陪伴加载失败") }
        }
    }

    fun refreshCompanions() {
        scope.launch {
            loading = true
            val result = runCatching {
                withContext(Dispatchers.IO) { parseAiCompanions(api.listAiCompanions()) }
            }
            loading = false
            result
                .onSuccess { rows ->
                    companions.clear()
                    companions.addAll(rows)
                    selectedId?.let { id ->
                        if (rows.any { it.id == id }) {
                            loadMessages(id)
                        } else {
                            selectedId = null
                            chatMessages.clear()
                        }
                    }
                }
                .onFailure { cloudMessage = it.userFriendlyMessage("AI 陪伴加载失败") }
        }
    }

    LaunchedEffect(user.token) {
        refreshCompanions()
    }

    fun sendChatMessage() {
        val companion = selected ?: return
        val content = chatInput.trim()
        if (content.isBlank() || sendingChat) return
        chatInput = ""
        cloudMessage = ""
        scope.launch {
            sendingChat = true
            val result = runCatching {
                withContext(Dispatchers.IO) { parseChatMessages(api.sendAiMessage(companion.id, content)) }
            }
            sendingChat = false
            result
                .onSuccess { messages ->
                    val existingIds = chatMessages.map { it.id }.toSet()
                    val newMessages = messages.filter { it.id.isBlank() || it.id !in existingIds }
                    chatMessages.addAll(newMessages)
                }
                .onFailure {
                    chatInput = content
                    cloudMessage = it.userFriendlyMessage("消息发送失败")
                }
        }
    }

    val companionAvatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val companionId = selectedId
        if (uri == null || companionId == null || uploadingAvatar) {
            return@rememberLauncherForActivityResult
        }
        context.persistReadPermission(uri)
        cloudMessage = ""
        uploadingAvatar = true
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val payload = context.readUploadPayload(uri)
                    val response = api.uploadAiCompanionAsset(
                        companionId = companionId,
                        kind = "avatar",
                        fileName = payload.fileName,
                        mimeType = payload.mimeType,
                        bytes = payload.bytes
                    )
                    parseAiCompanion(response.getJSONObject("companion"))
                }
            }
            uploadingAvatar = false
            result
                .onSuccess { updated ->
                    upsertCompanion(updated)
                    selectedId = updated.id
                    cloudMessage = when {
                        !updated.smileAvatarUrl.isNullOrBlank() ->
                            "头像已识别，已生成真实微笑头像，说话嘴型也已匹配"
                        (updated.avatarMotion?.confidence ?: 0f) > 0.2f ->
                            "头像已识别，说话嘴型已匹配；真实微笑生成失败，请检查图片模型接口"
                        else ->
                            "头像已更新，未识别到清晰人脸，已使用默认动作"
                    }
                }
                .onFailure { cloudMessage = it.userFriendlyMessage("上传头像失败") }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建陪伴人物", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        singleLine = true,
                        label = { Text("人物名称") },
                        shape = RoundedCornerShape(8.dp),
                        colors = warmTextFieldColors()
                    )
                    ChipRow(
                        title = "身份",
                        options = listOf("母亲", "父亲", "祖辈", "伴侣", "朋友", "宠物"),
                        selected = createRelation,
                        onSelect = { createRelation = it }
                    )
                    ChipRow(
                        title = "性别",
                        options = listOf("女性", "男性", "不限定"),
                        selected = createGender,
                        onSelect = { createGender = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            loading = true
                            val result = runCatching {
                                withContext(Dispatchers.IO) {
                                    parseAiCompanion(
                                        api.createAiCompanion(
                                            createName.ifBlank { createRelation },
                                            createGender,
                                            createRelation
                                        )
                                    )
                                }
                            }
                            loading = false
                            result
                                .onSuccess {
                                    upsertCompanion(it)
                                    selectedId = it.id
                                    showCreateDialog = false
                                    createName = ""
                                    chatMessages.clear()
                                    cloudMessage = "已创建陪伴人物"
                                }
                                .onFailure { cloudMessage = it.userFriendlyMessage("创建失败") }
                        }
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = Paper
        )
    }

    if (selected == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showCreateDialog = true },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = primaryButtonColors(),
                contentPadding = PaddingValues(vertical = 15.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("新建陪伴人物", fontWeight = FontWeight.Bold)
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (companions.isEmpty()) {
                    item {
                        Panel {
                            Text(
                                if (loading) "正在同步陪伴人物..." else "还没有陪伴人物",
                                color = Muted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    items(companions, key = { it.id }) { companion ->
                        AiCompanionListRow(companion = companion) {
                            selectedId = companion.id
                            chatMessages.clear()
                            loadMessages(companion.id)
                        }
                    }
                }
            }
            if (cloudMessage.isNotBlank()) {
                StatusMessage(message = cloudMessage)
            }
        }
        return
    }

    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        selectedId = null
                        chatMessages.clear()
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line),
                    colors = quietOutlinedButtonColors()
                ) {
                    Text("返回")
                }
                Spacer(Modifier.width(10.dp))
                AnimatedCompanionAvatar(
                    avatar = selectedAvatar,
                    size = 40.dp,
                    motion = selectedMotion,
                    speaking = sendingChat,
                    smiling = !selectedHasSmileAvatar
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(selected.displayName, color = Ink, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${selected.relation} · ${selected.gender}", color = Muted, fontSize = 12.sp)
                }
                IconButton(
                    onClick = { companionAvatarPicker.launch(arrayOf("image/*")) },
                    enabled = !uploadingAvatar && !loading
                ) {
                    if (uploadingAvatar) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Green,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = "上传头像", tint = Green)
                    }
                }
            }
            ChatWindow(
                modifier = Modifier.weight(1f),
                avatar = selectedAvatar,
                avatarMotion = selectedMotion,
                smiling = !selectedHasSmileAvatar,
                relation = selected.displayName,
                messages = chatMessages,
                input = chatInput,
                onInputChange = { chatInput = it },
                onSend = ::sendChatMessage,
                isSending = sendingChat
            )
            if (cloudMessage.isNotBlank()) {
                StatusMessage(message = cloudMessage)
            }
        }
}

@Composable
private fun AiCompanionListRow(companion: AiCompanion, onClick: () -> Unit) {
    val avatar by rememberUriImage(companion.smileAvatarUrl ?: companion.avatarUrl)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Paper,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompanionAvatar(avatar = avatar, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        companion.displayName,
                        color = Ink,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (companion.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        SoftTag("默认")
                    }
                }
                Text(
                    "${companion.relation} · ${companion.gender}",
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("聊天", color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ChipRow(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) },
                colors = warmFilterChipColors()
            )
        }
    }
}

@Composable
private fun ChatWindow(
    modifier: Modifier = Modifier,
    avatar: ImageBitmap?,
    avatarMotion: AvatarMotion?,
    smiling: Boolean,
    relation: String,
    messages: List<ChatMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean
) {
    val messageListState = rememberLazyListState()
    val lastAssistantMessage = messages.lastOrNull { it.sender != "user" }
    var recentlySpeaking by remember { mutableStateOf(false) }

    LaunchedEffect(lastAssistantMessage?.speechUtteranceId()) {
        if (lastAssistantMessage == null) {
            return@LaunchedEffect
        }
        recentlySpeaking = true
        delay(2600)
        recentlySpeaking = false
    }
    val avatarSpeaking = isSending || recentlySpeaking

    LaunchedEffect(messages.size, messages.lastOrNull()?.id) {
        if (messages.isNotEmpty()) {
            messageListState.animateScrollToItem(messages.lastIndex)
        }
    }

    Panel(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedCompanionAvatar(
                    avatar = avatar,
                    size = 72.dp,
                    motion = avatarMotion,
                    speaking = avatarSpeaking,
                    smiling = smiling
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(relation, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (avatarSpeaking) "正在回应" else "在线陪伴",
                        color = Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle("聊天窗口", "消息保存到云端，AI 回复由服务端生成")
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Paper.copy(alpha = 0.82f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Line)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = messageListState,
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CompanionAvatar(avatar = avatar, size = 36.dp)
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "我在这里。你可以先和我说一句想说的话。",
                                    color = Ink,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = messages,
                        key = { message -> "${message.id}-${message.createdAt}-${message.sender}" }
                    ) { message ->
                        ChatBubble(message = message, avatar = avatar, relation = relation)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                enabled = !isSending,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                label = { Text("输入消息") },
                colors = warmTextFieldColors()
            )
            Button(
                onClick = onSend,
                enabled = input.isNotBlank() && !isSending,
                shape = RoundedCornerShape(8.dp),
                colors = primaryButtonColors(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, avatar: ImageBitmap?, relation: String) {
    val isUser = message.sender == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            CompanionAvatar(avatar = avatar, size = 32.dp)
            Spacer(Modifier.width(7.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(0.78f),
            color = if (isUser) Night else Color.White,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Text(
                    text = if (isUser) "我" else relation,
                    color = if (isUser) Color.White.copy(alpha = 0.72f) else Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message.content,
                    color = if (isUser) Color.White else Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun AnimatedCompanionAvatar(
    avatar: ImageBitmap?,
    size: Dp,
    motion: AvatarMotion?,
    speaking: Boolean,
    smiling: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "companionAvatar")
    val breath by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val sway by transition.animateFloat(
        initialValue = -0.8f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )
    val mouth by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth"
    )
    val smilePhase by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smile"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = breath
                scaleY = breath
                rotationZ = sway
            }
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(Blush, BlueMist))),
        contentAlignment = Alignment.Center
    ) {
        if (avatar != null) {
            ComposeImage(
                bitmap = avatar,
                contentDescription = "闄即澶村儚",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ComposeImage(
                painter = painterResource(id = R.drawable.anyi_ai_avatar),
                contentDescription = "闄即澶村儚",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size
            val mouthBox = motion?.mouth ?: AvatarMotionBox(0.5f, 0.68f, 0.18f, 0.06f)
            val mouthWidth = (canvasSize.width * mouthBox.w).coerceAtLeast(8.dp.toPx())
            val baseMouthHeight = (canvasSize.height * mouthBox.h).coerceAtLeast(4.dp.toPx())
            val mouthHeight = if (speaking) baseMouthHeight * (0.8f + mouth * 1.25f) else baseMouthHeight * (0.42f + smilePhase * 0.18f)
            val mouthCenter = Offset(canvasSize.width * mouthBox.x, canvasSize.height * mouthBox.y)
            val mouthTopLeft = Offset(mouthCenter.x - mouthWidth / 2f, mouthCenter.y - mouthHeight / 2f)
            if (avatar != null) {
                if (speaking) {
                    drawRoundRect(
                        color = Night.copy(alpha = 0.68f),
                        topLeft = mouthTopLeft,
                        size = Size(mouthWidth, mouthHeight),
                        cornerRadius = CornerRadius(mouthHeight, mouthHeight)
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.58f),
                        startAngle = 18f,
                        sweepAngle = 144f,
                        useCenter = false,
                        topLeft = Offset(mouthTopLeft.x + mouthWidth * 0.12f, mouthTopLeft.y + mouthHeight * 0.16f),
                        size = Size(mouthWidth * 0.76f, mouthHeight * 0.82f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                } else if (smiling) {
                    drawArc(
                        color = Night.copy(alpha = 0.72f),
                        startAngle = 18f,
                        sweepAngle = 144f,
                        useCenter = false,
                        topLeft = Offset(mouthTopLeft.x, mouthTopLeft.y - mouthHeight * 0.15f),
                        size = Size(mouthWidth, mouthHeight * 2.4f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            } else {
                drawCircle(
                    color = Rose.copy(alpha = 0.16f),
                    radius = canvasSize.minDimension * 0.065f,
                    center = Offset(canvasSize.width * 0.32f, canvasSize.height * 0.53f)
                )
                drawCircle(
                    color = Rose.copy(alpha = 0.16f),
                    radius = canvasSize.minDimension * 0.065f,
                    center = Offset(canvasSize.width * 0.68f, canvasSize.height * 0.53f)
                )
                if (speaking) {
                    drawRoundRect(
                        color = Night.copy(alpha = 0.82f),
                        topLeft = mouthTopLeft,
                        size = Size(mouthWidth, mouthHeight),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                } else if (smiling) {
                    drawArc(
                        color = Night.copy(alpha = 0.62f),
                        startAngle = 18f,
                        sweepAngle = 142f,
                        useCenter = false,
                        topLeft = Offset(mouthTopLeft.x, mouthTopLeft.y),
                        size = Size(mouthWidth, mouthHeight * 2.4f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                val eyeHeight = if (smilePhase > 0.7f) 4.dp.toPx() else 6.dp.toPx()
                drawRoundRect(
                    color = Night.copy(alpha = 0.82f),
                    topLeft = Offset(canvasSize.width * 0.42f, canvasSize.height * 0.31f),
                    size = Size(8.dp.toPx(), eyeHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = Night.copy(alpha = 0.82f),
                    topLeft = Offset(canvasSize.width * 0.58f, canvasSize.height * 0.31f),
                    size = Size(8.dp.toPx(), eyeHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun CompanionAvatar(avatar: ImageBitmap?, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(Blush, BlueMist))),
        contentAlignment = Alignment.Center
    ) {
        if (avatar != null) {
            ComposeImage(
                bitmap = avatar,
                contentDescription = "陪伴头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ComposeImage(
                painter = painterResource(id = R.drawable.anyi_ai_avatar),
                contentDescription = "陪伴头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HumanitiesCommunityScreen(user: AppUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(user.token) { AnyiApiClient(tokenProvider = { user.token }) }
    val isAdmin = user.role == "admin"
    var posts by remember { mutableStateOf(emptyList<CommunityPost>()) }
    var volunteers by remember { mutableStateOf(defaultCommunityVolunteerPosts()) }
    var volunteerApplications by remember { mutableStateOf(emptyList<CommunityVolunteerApplication>()) }
    var volunteerIndex by remember { mutableStateOf(0) }
    var draft by rememberSaveable { mutableStateOf("") }
    var commentDraft by rememberSaveable { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf(emptyList<String>()) }
    var selectedCommentPost by remember { mutableStateOf<CommunityPost?>(null) }
    var comments by remember { mutableStateOf(emptyList<CommunityComment>()) }
    val commentListState = rememberLazyListState()
    var loading by remember { mutableStateOf(false) }
    var posting by remember { mutableStateOf(false) }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentPosting by remember { mutableStateOf(false) }
    var volunteerPosting by remember { mutableStateOf(false) }
    var volunteerApplying by remember { mutableStateOf(false) }
    var volunteerApplicationsLoading by remember { mutableStateOf(false) }
    var reviewingApplicationId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var showPublishDialog by remember { mutableStateOf(false) }
    var showVolunteer by remember { mutableStateOf(false) }
    var selectedVolunteerDetail by remember { mutableStateOf<CommunityVolunteerPost?>(null) }
    var volunteerCoverUri by remember { mutableStateOf<String?>(null) }
    var applyingVolunteer by remember { mutableStateOf<CommunityVolunteerPost?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { context.persistReadPermission(it) }
        selectedImageUris = (selectedImageUris + uris.map { it.toString() }).distinct().take(9)
    }
    val volunteerCoverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.persistReadPermission(it)
            volunteerCoverUri = it.toString()
        }
    }

    fun refreshCommunity() {
        scope.launch {
            loading = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val communityPosts = parseCommunityPosts(api.listCommunityPosts())
                    val volunteerPosts = parseCommunityVolunteers(api.communityVolunteerInfo())
                    val applications = if (isAdmin) {
                        parseCommunityVolunteerApplications(api.listCommunityVolunteerApplications("all"))
                    } else {
                        emptyList()
                    }
                    Triple(communityPosts, volunteerPosts.ifEmpty { defaultCommunityVolunteerPosts() }, applications)
                }
            }
            loading = false
            result
                .onSuccess { (communityPosts, volunteerPosts, applications) ->
                    posts = communityPosts
                    volunteers = volunteerPosts
                    volunteerApplications = applications
                    volunteerIndex = volunteerIndex.coerceAtMost((volunteerPosts.size - 1).coerceAtLeast(0))
                    message = ""
                }
                .onFailure { message = it.userFriendlyMessage("社区加载失败") }
        }
    }

    fun refreshVolunteerApplications() {
        if (!isAdmin) return
        scope.launch {
            volunteerApplicationsLoading = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityVolunteerApplications(api.listCommunityVolunteerApplications("all"))
                }
            }
            volunteerApplicationsLoading = false
            result
                .onSuccess { volunteerApplications = it }
                .onFailure { message = it.userFriendlyMessage("报名审核列表加载失败") }
        }
    }

    fun publishPost() {
        val content = draft.trim()
        if (content.isBlank()) {
            if (selectedImageUris.isEmpty()) {
                message = "写点内容或选择照片再发布"
                return
            }
        }
        val pendingImageUris = selectedImageUris
        if (content.isBlank() && pendingImageUris.isEmpty()) {
            message = "写点内容或选择照片再发布"
            return
        }
        scope.launch {
            posting = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val imageUrls = pendingImageUris.map { uriString ->
                        val upload = context.readUploadPayload(Uri.parse(uriString))
                        api.uploadAsset("community/posts", upload.fileName, upload.mimeType, upload.bytes)
                            .getJSONObject("asset")
                            .getString("url")
                    }
                    parseCommunityPost(api.createCommunityPost(content, imageUrls).getJSONObject("post"))
                }
            }
            posting = false
            result
                .onSuccess { post ->
                    draft = ""
                    selectedImageUris = emptyList()
                    showPublishDialog = false
                    posts = listOf(post) + posts.filterNot { it.id == post.id }
                    message = "已发布到人文社区"
                }
                .onFailure { message = it.userFriendlyMessage("发布失败") }
        }
    }

    fun likePost(post: CommunityPost) {
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityPost(api.likeCommunityPost(post.id).getJSONObject("post"))
                }
            }
            result
                .onSuccess { updated ->
                    posts = posts.map { if (it.id == updated.id) updated else it }
                }
                .onFailure { message = it.userFriendlyMessage("点赞失败") }
        }
    }

    fun loadComments(post: CommunityPost) {
        selectedCommentPost = post
        commentDraft = ""
        scope.launch {
            commentsLoading = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityComments(api.listCommunityPostComments(post.id))
                }
            }
            commentsLoading = false
            result
                .onSuccess { comments = it }
                .onFailure { message = it.userFriendlyMessage("评论加载失败") }
        }
    }

    fun publishComment() {
        val post = selectedCommentPost ?: return
        val content = commentDraft.trim()
        if (content.isBlank()) {
            message = "请先写评论内容"
            return
        }
        val now = System.currentTimeMillis()
        val tempCommentId = "local-$now"
        val optimisticComment = CommunityComment(
            id = tempCommentId,
            postId = post.id,
            authorId = user.id,
            authorName = user.displayName.ifBlank { user.username },
            authorUsername = user.username,
            authorAvatarUrl = user.avatarUrl,
            content = content,
            createdAt = now
        )
        comments = comments + optimisticComment
        commentDraft = ""
        scope.launch {
            commentPosting = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val response = api.createCommunityPostComment(post.id, content)
                    parseCommunityComment(response.getJSONObject("comment")) to
                        parseCommunityPost(response.getJSONObject("post"))
                }
            }
            commentPosting = false
            result
                .onSuccess { (comment, updatedPost) ->
                    comments = comments.map { if (it.id == tempCommentId) comment else it }
                    selectedCommentPost = updatedPost
                    posts = posts.map { if (it.id == updatedPost.id) updatedPost else it }
                    message = ""
                }
                .onFailure {
                    comments = comments.filterNot { item -> item.id == tempCommentId }
                    commentDraft = content
                    message = it.userFriendlyMessage("评论发布失败")
                }
        }
    }

    fun openVolunteerInfo() {
        showVolunteer = true
        refreshVolunteerApplications()
    }

    fun publishVolunteer(title: String, body: String, contact: String, coverUri: String?) {
        if (title.trim().isBlank() || body.trim().isBlank()) {
            message = "请填写义工标题和内容"
            return
        }
        val pendingCoverUri = coverUri?.takeIf { it.isNotBlank() }
        scope.launch {
            volunteerPosting = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val imageUrl = pendingCoverUri?.let { uriString ->
                        val upload = context.readUploadPayload(Uri.parse(uriString))
                        api.uploadAsset("community/volunteer", upload.fileName, upload.mimeType, upload.bytes)
                            .getJSONObject("asset")
                            .getString("url")
                    }
                    parseCommunityVolunteer(api.createCommunityVolunteer(title, body, contact, imageUrl))
                }
            }
            volunteerPosting = false
            result
                .onSuccess { item ->
                    volunteers = listOf(item) + volunteers.filterNot { it.id == item.id }
                    volunteerIndex = 0
                    volunteerCoverUri = null
                    message = "义工招募已发布"
                }
                .onFailure { message = it.userFriendlyMessage("义工招募发布失败") }
        }
    }

    fun submitVolunteerApplication(volunteer: CommunityVolunteerPost, name: String, phone: String, note: String) {
        if (name.trim().isBlank() || phone.trim().isBlank()) {
            message = "请填写姓名和联系方式"
            return
        }
        scope.launch {
            volunteerApplying = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityVolunteerApplication(
                        api.applyForCommunityVolunteer(volunteer.id, name, phone, note)
                    )
                }
            }
            volunteerApplying = false
            result
                .onSuccess {
                    applyingVolunteer = null
                    message = "报名已提交，等待管理员审核"
                }
                .onFailure { message = it.userFriendlyMessage("义工报名提交失败") }
        }
    }

    fun reviewVolunteerApplication(application: CommunityVolunteerApplication, status: String) {
        scope.launch {
            reviewingApplicationId = application.id
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityVolunteerApplication(
                        api.reviewCommunityVolunteerApplication(application.id, status)
                    )
                }
            }
            reviewingApplicationId = null
            result
                .onSuccess { updated ->
                    volunteerApplications = volunteerApplications.map {
                        if (it.id == updated.id) updated else it
                    }
                    message = if (status == "approved") "报名已通过" else "报名已拒绝"
                }
                .onFailure { message = it.userFriendlyMessage("报名审核失败") }
        }
    }

    LaunchedEffect(user.token) {
        refreshCommunity()
    }

    LaunchedEffect(volunteers.size) {
        while (volunteers.size > 1) {
            delay(3200)
            volunteerIndex = (volunteerIndex + 1) % volunteers.size
        }
    }

    LaunchedEffect(selectedCommentPost?.id, comments.size) {
        if (selectedCommentPost != null && comments.isNotEmpty()) {
            commentListState.scrollToItem(comments.lastIndex)
        }
    }

    val volunteerOverlayOpen = showVolunteer || applyingVolunteer != null
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (volunteerOverlayOpen) Modifier.blur(14.dp) else Modifier)
        ) {
            ComposeImage(
                painter = painterResource(id = R.drawable.anyi_hall_page_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                HallWarmBackground.copy(alpha = 0.72f),
                                Morning.copy(alpha = 0.90f),
                                Background.copy(alpha = 0.96f)
                            )
                        )
                    )
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                item {
                    CommunityFeedHeader(
                        user = user,
                        loading = loading,
                        onRefresh = ::refreshCommunity,
                        onPublish = { showPublishDialog = true }
                    )
                }
                item {
                    CommunityVolunteerCarousel(
                        volunteers = volunteers,
                        index = volunteerIndex,
                        onClick = ::openVolunteerInfo
                    )
                }
                if (message.isNotBlank()) {
                    item { StatusMessage(message = message) }
                }
                if (loading && posts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green)
                        }
                    }
                }
                if (!loading && posts.isEmpty()) {
                    item { EmptyCommunityFeed() }
                }
                items(
                    items = posts,
                    key = { post -> post.id }
                ) { post ->
                    CommunityPostCard(
                        post = post,
                        onLike = { likePost(post) },
                        onComment = { loadComments(post) }
                    )
                }
            }
        }

        if (showVolunteer) {
            CommunityVolunteerDialog(
                volunteers = volunteers,
                selectedVolunteer = selectedVolunteerDetail,
                isAdmin = isAdmin,
                applications = volunteerApplications,
                applicationsLoading = volunteerApplicationsLoading,
                posting = volunteerPosting,
                reviewingApplicationId = reviewingApplicationId,
                coverUri = volunteerCoverUri,
                onPickCover = { volunteerCoverPicker.launch(arrayOf("image/*")) },
                onClearCover = { volunteerCoverUri = null },
                onSelectVolunteer = { selectedVolunteerDetail = it },
                onBackToList = { selectedVolunteerDetail = null },
                onApply = {
                    applyingVolunteer = it
                    showVolunteer = false
                    selectedVolunteerDetail = null
                },
                onPublish = ::publishVolunteer,
                onReview = ::reviewVolunteerApplication,
                onRefreshApplications = ::refreshVolunteerApplications,
                onDismiss = {
                    showVolunteer = false
                    selectedVolunteerDetail = null
                }
            )
        }
    }

    if (showPublishDialog) {
        PublishCommunityPostDialog(
            user = user,
            draft = draft,
            imageUris = selectedImageUris,
            posting = posting,
            onDraftChange = { draft = it.take(500) },
            onAddImages = { photoPicker.launch(arrayOf("image/*")) },
            onRemoveImage = { uri -> selectedImageUris = selectedImageUris.filterNot { it == uri } },
            onPublish = ::publishPost,
            onDismiss = {
                if (!posting) showPublishDialog = false
            }
        )
    }

    applyingVolunteer?.let { volunteer ->
        CommunityVolunteerApplicationDialog(
            volunteer = volunteer,
            applying = volunteerApplying,
            onSubmit = { name, phone, note -> submitVolunteerApplication(volunteer, name, phone, note) },
            onDismiss = {
                if (!volunteerApplying) applyingVolunteer = null
            }
        )
    }

    selectedCommentPost?.let { post ->
        CommunityCommentsDialog(
            post = post,
            comments = comments,
            draft = commentDraft,
            loading = commentsLoading,
            posting = commentPosting,
            listState = commentListState,
            onDraftChange = { commentDraft = it.take(300) },
            onPublish = ::publishComment,
            onDismiss = {
                if (!commentPosting) {
                    selectedCommentPost = null
                    comments = emptyList()
                    commentDraft = ""
                }
            }
        )
    }
}

@Composable
private fun CommunityFeedHeader(
    user: AppUser,
    loading: Boolean,
    onRefresh: () -> Unit,
    onPublish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("人文社区", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("社区动态", color = Muted, fontSize = 13.sp)
        }
        IconButton(onClick = onRefresh, enabled = !loading) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = "刷新", tint = Green)
        }
        Surface(
            color = Ink,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 3.dp
        ) {
            IconButton(onClick = onPublish) {
                Icon(Icons.Rounded.Add, contentDescription = "发布动态", tint = Color.White)
            }
        }
    }
}

@Composable
private fun CommunityVolunteerCarousel(
    volunteers: List<CommunityVolunteerPost>,
    index: Int,
    onClick: () -> Unit
) {
    val item = volunteers.getOrNull(index % volunteers.size.coerceAtLeast(1))
        ?: defaultCommunityVolunteerPosts().first()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Paper.copy(alpha = 0.86f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.70f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.55f),
                            Leaf.copy(alpha = 0.72f),
                            Blush.copy(alpha = 0.50f)
                        )
                    )
                )
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Line.copy(alpha = 0.42f))
            ) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Rose, modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("义工招募", color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Text("${index % volunteers.size.coerceAtLeast(1) + 1}/${volunteers.size.coerceAtLeast(1)}", color = Muted, fontSize = 10.sp)
                }
                Text(item.title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.body, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text("全部", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PublishCommunityPostDialog(
    user: AppUser,
    draft: String,
    imageUris: List<String>,
    posting: Boolean,
    onDraftChange: (String) -> Unit,
    onAddImages: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onPublish: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = RoundedCornerShape(8.dp),
        icon = { Icon(Icons.Rounded.Add, contentDescription = null, tint = Green) },
        title = { Text("发布动态") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CommunityAvatar(name = user.displayName.ifBlank { user.username }, avatarUrl = user.avatarUrl, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(user.displayName.ifBlank { user.username }, color = Ink, fontWeight = FontWeight.ExtraBold)
                        Text("文字、照片或两者都可以", color = Muted, fontSize = 12.sp)
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("写点想分享的内容，也可以只发照片") },
                    colors = warmTextFieldColors()
                )
                OutlinedButton(
                    onClick = onAddImages,
                    enabled = imageUris.size < 9 && !posting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line),
                    colors = quietOutlinedButtonColors()
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageUris.isEmpty()) "添加照片" else "继续添加 ${imageUris.size}/9")
                }
                if (imageUris.isNotEmpty()) {
                    CommunityImageGrid(
                        imageUrls = imageUris,
                        removable = true,
                        onRemove = onRemoveImage
                    )
                }
                Text("${draft.length}/500", color = Muted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onPublish,
                enabled = !posting && (draft.isNotBlank() || imageUris.isNotEmpty()),
                colors = primaryButtonColors(),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (posting) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("发布")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !posting) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CommunityVolunteerDialog(
    volunteers: List<CommunityVolunteerPost>,
    selectedVolunteer: CommunityVolunteerPost?,
    isAdmin: Boolean,
    applications: List<CommunityVolunteerApplication>,
    applicationsLoading: Boolean,
    posting: Boolean,
    reviewingApplicationId: String?,
    coverUri: String?,
    onPickCover: () -> Unit,
    onClearCover: () -> Unit,
    onSelectVolunteer: (CommunityVolunteerPost) -> Unit,
    onBackToList: () -> Unit,
    onApply: (CommunityVolunteerPost) -> Unit,
    onPublish: (String, String, String, String?) -> Unit,
    onReview: (CommunityVolunteerApplication, String) -> Unit,
    onRefreshApplications: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    val sortedApplications = applications.sortedWith(
        compareBy<CommunityVolunteerApplication> {
            when (it.status) {
                "pending" -> 0
                "approved" -> 1
                else -> 2
            }
        }.thenByDescending { it.createdAt }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night.copy(alpha = 0.28f))
            .padding(horizontal = 13.dp, vertical = 18.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clickable(onClick = {}),
            color = Background.copy(alpha = 0.96f),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedVolunteer != null) {
                        TextButton(onClick = onBackToList) {
                            Text("返回", color = Green, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("义工招募", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (selectedVolunteer == null) "点击卡片查看详情与报名入口" else "项目详情与报名入口",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        "×",
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (selectedVolunteer == null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Hi，选择一个想参与的义工项目", color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Surface(
                                color = Color(0xFF5EA0EE),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(
                                    "详情页内提交报名",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 11.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(volunteers, key = { it.id }) { item ->
                            CommunityVolunteerImageCard(
                                item = item,
                                actionLabel = if (isAdmin) "详情" else "Join",
                                onClick = { onSelectVolunteer(item) }
                            )
                        }
                        if (isAdmin) {
                            item {
                                CommunityVolunteerAdminPanel(
                                    title = title,
                                    body = body,
                                    contact = contact,
                                    coverUri = coverUri,
                                    posting = posting,
                                    applications = sortedApplications,
                                    applicationsLoading = applicationsLoading,
                                    reviewingApplicationId = reviewingApplicationId,
                                    onTitleChange = { title = it.take(40) },
                                    onBodyChange = { body = it.take(500) },
                                    onContactChange = { contact = it.take(160) },
                                    onPickCover = onPickCover,
                                    onClearCover = onClearCover,
                                    onPublish = { onPublish(title, body, contact, coverUri) },
                                    onRefreshApplications = onRefreshApplications,
                                    onReview = onReview
                                )
                            }
                        }
                    }
                } else {
                    CommunityVolunteerDetailPanel(
                        item = selectedVolunteer,
                        isAdmin = isAdmin,
                        onApply = { onApply(selectedVolunteer) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityVolunteerImageCard(
    item: CommunityVolunteerPost,
    actionLabel: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        CommunityVolunteerCoverImage(imageUrl = item.imageUrl, title = item.title)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp, end = 116.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(item.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("领队：安忆社区", color = Color.White.copy(alpha = 0.90f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(formatVolunteerDate(item.createdAt), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            color = Color.White,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 4.dp
        ) {
            Text(
                actionLabel,
                color = Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun CommunityVolunteerDetailPanel(
    item: CommunityVolunteerPost,
    isAdmin: Boolean,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            CommunityVolunteerCoverImage(imageUrl = item.imageUrl, title = item.title)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f))))
            )
            Text(
                item.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            )
        }
        Surface(
            color = Color.White.copy(alpha = 0.94f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Line.copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("详细介绍", "了解项目内容后再报名")
                Text(item.body, color = Ink, fontSize = 14.sp, lineHeight = 22.sp)
                if (item.contact.isNotBlank()) {
                    Surface(
                        color = Leaf.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            item.contact,
                            color = Green,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (!isAdmin) {
                    Button(
                        onClick = onApply,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5EA0EE), contentColor = Color.White)
                    ) {
                        Text("我要报名", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityVolunteerAdminPanel(
    title: String,
    body: String,
    contact: String,
    coverUri: String?,
    posting: Boolean,
    applications: List<CommunityVolunteerApplication>,
    applicationsLoading: Boolean,
    reviewingApplicationId: String?,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onPickCover: () -> Unit,
    onClearCover: () -> Unit,
    onPublish: () -> Unit,
    onRefreshApplications: () -> Unit,
    onReview: (CommunityVolunteerApplication, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("发布招募", "管理员添加照片后会显示在义工卡片上")
        Surface(
            color = Color.White.copy(alpha = 0.92f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Line.copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (coverUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(138.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        CommunityVolunteerCoverImage(imageUrl = coverUri, title = title)
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            color = Ink.copy(alpha = 0.68f),
                            shape = CircleShape
                        ) {
                            Text(
                                "×",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable(onClick = onClearCover)
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onPickCover,
                    enabled = !posting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Line),
                    colors = quietOutlinedButtonColors()
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (coverUri == null) "添加卡片照片" else "更换卡片照片")
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("标题") },
                    colors = warmTextFieldColors()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    modifier = Modifier.fillMaxWidth().height(104.dp),
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("招募内容") },
                    colors = warmTextFieldColors()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = onContactChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("联系方式") },
                    colors = warmTextFieldColors()
                )
                Button(
                    onClick = onPublish,
                    enabled = !posting && title.isNotBlank() && body.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = primaryButtonColors()
                ) {
                    if (posting) {
                        CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("发布义工信息")
                    }
                }
            }
        }

        SectionTitle("报名审核", "管理员审核后再联系报名用户")
        OutlinedButton(
            onClick = onRefreshApplications,
            enabled = !applicationsLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Line),
            colors = quietOutlinedButtonColors()
        ) {
            if (applicationsLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Green)
            } else {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(if (applicationsLoading) "正在刷新" else "刷新报名列表")
        }
        if (!applicationsLoading && applications.isEmpty()) {
            Surface(
                color = Color.White.copy(alpha = 0.78f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Line.copy(alpha = 0.50f))
            ) {
                Text("暂时还没有报名申请", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }
        applications.forEach { application ->
            CommunityVolunteerApplicationReviewCard(
                application = application,
                busy = reviewingApplicationId == application.id,
                onApprove = { onReview(application, "approved") },
                onReject = { onReview(application, "rejected") }
            )
        }
    }
}

@Composable
private fun CommunityVolunteerCoverImage(imageUrl: String?, title: String) {
    val bitmap by rememberUriImage(imageUrl)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(communityVolunteerFallbackBrush(title)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            ComposeImage(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.LocalFlorist, contentDescription = null, tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(34.dp))
                Text("等待管理员添加照片", color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun communityVolunteerFallbackBrush(title: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFF83C5BE), Color(0xFF006D77), Color(0xFF2B2418)),
        listOf(Color(0xFFFFB703), Color(0xFFFB8500), Color(0xFF7B341E)),
        listOf(Color(0xFF90CAF9), Color(0xFF5EA0EE), Color(0xFF1D3557)),
        listOf(Color(0xFFE9C46A), Color(0xFFF4A261), Color(0xFF7F5539)),
        listOf(Color(0xFFCDB4DB), Color(0xFFFFAFCC), Color(0xFF6D597A))
    )
    val palette = palettes[kotlin.math.abs(title.hashCode()) % palettes.size]
    return Brush.linearGradient(palette)
}

private fun formatVolunteerDate(createdAt: Long): String {
    if (createdAt <= 0L) return "长期招募"
    return "发布 " + SimpleDateFormat("MM.dd", Locale.CHINA).format(Date(createdAt))
}

@Composable
private fun CommunityVolunteerApplicationDialog(
    volunteer: CommunityVolunteerPost,
    applying: Boolean,
    onSubmit: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = RoundedCornerShape(8.dp),
        icon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Green) },
        title = { Text("义工报名") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = Leaf.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line.copy(alpha = 0.44f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(volunteer.title, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(volunteer.body, color = Muted, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("姓名") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    colors = warmTextFieldColors()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("联系方式") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = warmTextFieldColors()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("报名说明") },
                    placeholder = { Text("可以写可参与时间、擅长事项或想补充的话") },
                    colors = warmTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, phone, note) },
                enabled = !applying && name.isNotBlank() && phone.isNotBlank(),
                colors = primaryButtonColors(),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (applying) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("提交报名")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !applying) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CommunityVolunteerApplicationReviewCard(
    application: CommunityVolunteerApplication,
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val statusColor = volunteerApplicationStatusColor(application.status)
    Surface(
        color = Color.White.copy(alpha = 0.82f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.30f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommunityAvatar(
                    name = application.applicantName.ifBlank { application.name },
                    avatarUrl = application.applicantAvatarUrl,
                    size = 34.dp
                )
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(application.name, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text(application.volunteerTitle, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f))
                ) {
                    Text(
                        volunteerApplicationStatusText(application.status),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
            Text("联系方式：${application.phone}", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (application.note.isNotBlank()) {
                Text(application.note, color = Ink, fontSize = 13.sp, lineHeight = 19.sp)
            }
            if (application.createdAt > 0L) {
                Text(formatTime(application.createdAt), color = Muted, fontSize = 11.sp)
            }
            if (application.status == "pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Line),
                        colors = quietOutlinedButtonColors()
                    ) {
                        Text("拒绝")
                    }
                    Button(
                        onClick = onApprove,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = primaryButtonColors()
                    ) {
                        Text(if (busy) "处理中" else "通过")
                    }
                }
            }
        }
    }
}

private fun volunteerApplicationStatusText(status: String): String {
    return when (status) {
        "approved" -> "已通过"
        "rejected" -> "已拒绝"
        else -> "待审核"
    }
}

private fun volunteerApplicationStatusColor(status: String): Color {
    return when (status) {
        "approved" -> Green
        "rejected" -> Color(0xFFB42318)
        else -> Rose
    }
}

@Composable
private fun CommunityHero(
    user: AppUser,
    posts: List<CommunityPost>,
    loading: Boolean,
    onVolunteer: () -> Unit,
    onRefresh: () -> Unit
) {
    val totalLikes = posts.sumOf { it.likeCount }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Paper.copy(alpha = 0.74f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.62f)),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.height(178.dp)) {
            ComposeImage(
                painter = painterResource(id = R.drawable.anyi_hall_page_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Night.copy(alpha = 0.44f),
                                Green.copy(alpha = 0.30f),
                                Amber.copy(alpha = 0.18f),
                                Paper.copy(alpha = 0.16f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Article,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(9.dp).size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("人文社区", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text(
                            "把近况、故事和互助放在同一个温暖的地方",
                            color = Color.White.copy(alpha = 0.86f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !loading,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.54f)
                        )
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (loading) "同步" else "刷新", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    CommunityStatPill("动态", posts.size.toString(), Icons.AutoMirrored.Rounded.Article, Modifier.weight(1f))
                    CommunityStatPill("点赞", totalLikes.toString(), Icons.Rounded.Favorite, Modifier.weight(1f))
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.58f))
                    ) {
                        Row(
                            modifier = Modifier.clickable(onClick = onVolunteer).padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Rose, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("义工", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("招募", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityStatPill(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.78f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.58f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Green, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(value, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CommunityComposer(
    user: AppUser,
    draft: String,
    posting: Boolean,
    onDraftChange: (String) -> Unit,
    onPublish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Paper.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommunityAvatar(
                    name = user.displayName.ifBlank { user.username },
                    avatarUrl = user.avatarUrl,
                    size = 42.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.displayName.ifBlank { user.username }, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text("把这一刻留给社区", color = Muted, fontSize = 12.sp)
                }
                Surface(color = Leaf.copy(alpha = 0.82f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line.copy(alpha = 0.42f))) {
                    Text("公开", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { onDraftChange(it.take(500)) },
                modifier = Modifier.fillMaxWidth().height(116.dp),
                shape = RoundedCornerShape(8.dp),
                placeholder = { Text("这一刻，想和大家聊点什么？") },
                colors = warmTextFieldColors()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommunityTopicChip("近况")
                    CommunityTopicChip("故事")
                    CommunityTopicChip("互助")
                }
                Spacer(Modifier.weight(1f))
                Text("${draft.length}/500", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onPublish,
                    enabled = draft.isNotBlank() && !posting,
                    shape = RoundedCornerShape(8.dp),
                    colors = primaryButtonColors()
                ) {
                    if (posting) {
                        CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("发布")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityTopicChip(text: String) {
    Surface(
        color = Blush.copy(alpha = 0.74f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.36f))
    ) {
        Text(
            text,
            color = Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun VolunteerRecruitmentEntry(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.86f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.70f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .background(Brush.linearGradient(listOf(Leaf.copy(alpha = 0.72f), Blush.copy(alpha = 0.48f), Color.White.copy(alpha = 0.32f))))
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = Paper.copy(alpha = 0.92f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line.copy(alpha = 0.46f))) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Rose, modifier = Modifier.padding(8.dp).size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("招募社区义工", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text("陪伴、整理故事、线下互助", color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Green.copy(alpha = 0.32f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                colors = quietOutlinedButtonColors()
            ) {
                Text("查看", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CommunityPostCard(post: CommunityPost, onLike: () -> Unit, onComment: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Paper.copy(alpha = 0.94f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.70f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(contentAlignment = Alignment.TopCenter) {
                    Canvas(modifier = Modifier.width(42.dp).height(54.dp)) {
                        drawLine(
                            color = Line.copy(alpha = 0.72f),
                            start = Offset(size.width / 2f, 32.dp.toPx()),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    CommunityAvatar(name = post.authorName, avatarUrl = post.authorAvatarUrl, size = 42.dp)
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.authorName, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text("@${post.authorUsername}", color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(color = Leaf.copy(alpha = 0.72f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line.copy(alpha = 0.36f))) {
                    Text(
                        formatTime(post.createdAt),
                        color = Green,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
            if (post.content.isNotBlank()) {
                Text(
                    post.content,
                    color = Ink,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(start = 53.dp)
                )
            }
            if (post.imageUrls.isNotEmpty()) {
                CommunityImageGrid(
                    imageUrls = post.imageUrls,
                    modifier = Modifier.padding(start = 53.dp)
                )
            }
            Row(modifier = Modifier.padding(start = 50.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onLike,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(
                        if (post.likedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (post.likedByMe) Rose else Muted,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("${post.likeCount}", color = if (post.likedByMe) Rose else Muted, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.54f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line.copy(alpha = 0.30f)),
                    modifier = Modifier.clickable(onClick = onComment)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ChatBubble, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("${post.commentCount}", color = Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityCommentsDialog(
    post: CommunityPost,
    comments: List<CommunityComment>,
    draft: String,
    loading: Boolean,
    posting: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onDraftChange: (String) -> Unit,
    onPublish: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = RoundedCornerShape(8.dp),
        icon = { Icon(Icons.Rounded.ChatBubble, contentDescription = null, tint = Green) },
        title = { Text("留言评论") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Leaf.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line.copy(alpha = 0.42f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CommunityAvatar(name = post.authorName, avatarUrl = post.authorAvatarUrl, size = 30.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(post.authorName, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        if (post.content.isNotBlank()) {
                            Text(post.content, color = Muted, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        } else if (post.imageUrls.isNotEmpty()) {
                            Text("图片动态", color = Muted, fontSize = 12.sp)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Green)
                            }
                        }
                    } else if (comments.isEmpty()) {
                        item {
                            Surface(
                                color = Color.White.copy(alpha = 0.76f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Line.copy(alpha = 0.38f))
                            ) {
                                Text(
                                    "还没有评论",
                                    color = Muted,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(
                            items = comments,
                            key = { it.id }
                        ) { comment ->
                            CommunityCommentItem(comment = comment)
                        }
                    }
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth().height(92.dp),
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("写一条友善的评论") },
                    colors = warmTextFieldColors()
                )
                Text("${draft.length}/300", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
            }
        },
        confirmButton = {
            Button(
                onClick = onPublish,
                enabled = !posting && draft.isNotBlank(),
                colors = primaryButtonColors(),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (posting) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("发送")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !posting) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun CommunityCommentItem(comment: CommunityComment) {
    Surface(
        color = Color.White.copy(alpha = 0.82f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.34f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            CommunityAvatar(name = comment.authorName, avatarUrl = comment.authorAvatarUrl, size = 32.dp)
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.authorName, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    Spacer(Modifier.width(7.dp))
                    Text(formatTime(comment.createdAt), color = Muted, fontSize = 10.sp)
                }
                Text(comment.content, color = Ink, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun CommunityImageGrid(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    removable: Boolean = false,
    onRemove: (String) -> Unit = {}
) {
    if (imageUrls.isEmpty()) return
    val rows = imageUrls.chunked(3)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { url ->
                    CommunityImageTile(
                        imageUrl = url,
                        removable = removable,
                        onRemove = { onRemove(url) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CommunityImageTile(
    imageUrl: String,
    removable: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap by rememberUriImage(imageUrl)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Leaf.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            ComposeImage(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Rounded.Image, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
        }
        if (removable) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                color = Ink.copy(alpha = 0.72f),
                shape = CircleShape
            ) {
                Text(
                    "×",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clickable(onClick = onRemove)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CommunityAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp
) {
    val initial = name.trim().firstOrNull()?.toString() ?: "人"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color.White, Leaf, Blush)))
            .border(1.dp, Color.White.copy(alpha = 0.78f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val avatarState = rememberUriImage(avatarUrl)
        val avatar = avatarState.value
        if (avatar != null) {
            ComposeImage(
                bitmap = avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(initial, color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun EmptyCommunityFeed() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Paper.copy(alpha = 0.86f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.68f)),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.56f), Leaf.copy(alpha = 0.52f))))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Surface(color = Leaf.copy(alpha = 0.82f), shape = CircleShape, border = BorderStroke(1.dp, Line.copy(alpha = 0.36f))) {
                    Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = Green, modifier = Modifier.padding(12.dp).size(28.dp))
                }
                Text("还没有社区动态", color = Ink, fontWeight = FontWeight.ExtraBold)
                Text("发第一条，让大家看到你的分享。", color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.72f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun rememberUriImage(uriString: String?) = LocalContext.current.let { context ->
    produceState<ImageBitmap?>(initialValue = null, uriString) {
        value = null
        if (uriString.isNullOrBlank()) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val normalized = absoluteAssetUrl(uriString)
                if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                    URL(normalized).openStream().use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                } else {
                    context.contentResolver.openInputStream(Uri.parse(normalized)).use { input ->
                        if (input == null) null else BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }
            }.getOrNull()
        }
    }
}

private fun Context.appPrefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

private fun installCrashReporter(context: Context) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching {
            val stackTrace = StringWriter().also { writer ->
                throwable.printStackTrace(PrintWriter(writer))
            }.toString()
            val token = context.appPrefs().getString(KEY_AUTH_TOKEN, null)
            val reporter = Thread {
                runCatching {
                    AnyiApiClient(tokenProvider = { token }).reportCrash(
                        platform = "android",
                        appVersion = BuildConfig.VERSION_NAME,
                        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                        osVersion = "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}",
                        errorType = throwable::class.java.name,
                        message = throwable.message,
                        stackTrace = stackTrace
                    )
                }
            }
            reporter.start()
            reporter.join(1800)
        }

        if (previous != null) {
            previous.uncaughtException(thread, throwable)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }
}

private fun readSession(context: Context): AppUser? {
    val prefs = context.appPrefs()
    val token = prefs.getString(KEY_AUTH_TOKEN, null) ?: return null
    val id = prefs.getString(KEY_USER_ID, null) ?: return null
    val username = prefs.getString(KEY_USER_NAME, null) ?: return null
    val displayName = prefs.getString(KEY_DISPLAY_NAME, null) ?: username
    val role = prefs.getString(KEY_USER_ROLE, null) ?: "user"
    val avatarUrl = prefs.getString(KEY_USER_AVATAR, null)
    return AppUser(
        id = id,
        username = username,
        displayName = displayName,
        role = role,
        token = token,
        avatarUrl = avatarUrl
    )
}

private fun writeSession(context: Context, user: AppUser) {
    val editor = context.appPrefs().edit()
        .putString(KEY_USER_ID, user.id)
        .putString(KEY_USER_NAME, user.username)
        .putString(KEY_DISPLAY_NAME, user.displayName)
        .putString(KEY_USER_ROLE, user.role)
        .putString(KEY_AUTH_TOKEN, user.token)
    if (user.avatarUrl.isNullOrBlank()) {
        editor.remove(KEY_USER_AVATAR)
    } else {
        editor.putString(KEY_USER_AVATAR, user.avatarUrl)
    }
    editor.apply()
}

private fun clearSession(context: Context) {
    context.appPrefs().edit()
        .remove(KEY_USER_ID)
        .remove(KEY_USER_NAME)
        .remove(KEY_DISPLAY_NAME)
        .remove(KEY_USER_ROLE)
        .remove(KEY_USER_AVATAR)
        .remove(KEY_AUTH_TOKEN)
        .apply()
}

private fun registerUser(context: Context, username: String, password: String): AppUser {
    val role = if (username.equals("admin", ignoreCase = true)) "admin" else "user"
    context.appPrefs().edit()
        .putString("user:$username:password", password)
        .putString("user:$username:role", role)
        .apply()
    return AppUser(id = username, username = username, displayName = username, role = role, token = "")
}

private fun loginUser(context: Context, username: String, password: String): AppUser? {
    val prefs = context.appPrefs()
    val storedPassword = prefs.getString("user:$username:password", null) ?: return null
    if (storedPassword != password) return null
    val role = prefs.getString("user:$username:role", null) ?: "user"
    return AppUser(id = username, username = username, displayName = username, role = role, token = "")
}

private fun Context.persistReadPermission(uri: Uri) {
    runCatching {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun parseStringArray(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
}

private fun parseMemorialFlowers(array: JSONArray?, legacyUntil: List<Long> = emptyList()): List<FlowerOffering> {
    val parsed = if (array == null) {
        emptyList()
    } else {
        List(array.length()) { index ->
            val item = array.opt(index)
            when (item) {
                is JSONObject -> {
                    val rawType = item.optString("type", "wreath")
                    val type = if (rawType == "wreath" || rawType == "chrysanthemum" || rawType == "lily") rawType else "wreath"
                    val until = item.optLong("until", 0L)
                    if (until > 0L) FlowerOffering(type, until) else null
                }
                is Number -> FlowerOffering("wreath", item.toLong())
                else -> null
            }
        }.filterNotNull()
    }
    return parsed.ifEmpty { legacyUntil.map { FlowerOffering("wreath", it) } }
}

private fun parseMemorialFruits(array: JSONArray?): List<FruitOffering> {
    if (array == null) return emptyList()
    val fallbackUntil = System.currentTimeMillis() + 10L * 60L * 1000L
    return List(array.length()) { index ->
        val item = array.opt(index)
        when (item) {
            is JSONObject -> {
                val type = item.optString("type")
                val until = item.optLong("until", 0L)
                if ((type == "apple" || type == "durian") && until > 0L) FruitOffering(type, until) else null
            }
            is String -> {
                if (item == "apple" || item == "durian") FruitOffering(item, fallbackUntil) else null
            }
            else -> null
        }
    }.filterNotNull()
}

private fun parseLongArray(array: JSONArray?): List<Long> {
    if (array == null) return emptyList()
    return List(array.length()) { index -> array.optLong(index) }.filter { it > 0L }
}

private fun parseCommunityPosts(array: JSONArray?): List<CommunityPost> {
    if (array == null) return emptyList()
    return List(array.length()) { index ->
        parseCommunityPost(array.optJSONObject(index) ?: JSONObject())
    }.filter { it.id.isNotBlank() && (it.content.isNotBlank() || it.imageUrls.isNotEmpty()) }
}

private fun parseCommunityPost(item: JSONObject): CommunityPost {
    val authorName = item.optString(
        "authorName",
        item.optString("display_name", item.optString("username", "社区用户"))
    ).ifBlank { "社区用户" }
    val authorUsername = item.optString("authorUsername", item.optString("username", "user"))
        .ifBlank { "user" }
    val avatarUrl = item.optString("authorAvatarUrl", item.optString("avatar_url"))
        .takeIf { it.isNotBlank() && it != "null" }
    return CommunityPost(
        id = item.optString("id"),
        authorId = item.optString("authorId", item.optString("user_id")),
        authorName = authorName,
        authorUsername = authorUsername,
        authorAvatarUrl = avatarUrl,
        content = item.optString("content"),
        imageUrls = parseStringArray(item.optJSONArray("imageUrls")),
        likeCount = item.optInt("likeCount", item.optInt("like_count", 0)),
        commentCount = item.optInt("commentCount", item.optInt("comment_count", 0)),
        likedByMe = item.optBoolean("likedByMe", item.optInt("liked_by_me", 0) == 1),
        createdAt = item.optLong("createdAt").takeIf { it > 0L }
            ?: parseTimeMillis(item.optString("created_at"))
    )
}

private fun parseCommunityComments(array: JSONArray?): List<CommunityComment> {
    if (array == null) return emptyList()
    return List(array.length()) { index ->
        parseCommunityComment(array.optJSONObject(index) ?: JSONObject())
    }.filter { it.id.isNotBlank() && it.content.isNotBlank() }
}

private fun parseCommunityComment(item: JSONObject): CommunityComment {
    val authorName = item.optString(
        "authorName",
        item.optString("display_name", item.optString("username", "社区用户"))
    ).ifBlank { "社区用户" }
    val authorUsername = item.optString("authorUsername", item.optString("username", "user"))
        .ifBlank { "user" }
    val avatarUrl = item.optString("authorAvatarUrl", item.optString("avatar_url"))
        .takeIf { it.isNotBlank() && it != "null" }
    return CommunityComment(
        id = item.optString("id"),
        postId = item.optString("postId", item.optString("post_id")),
        authorId = item.optString("authorId", item.optString("user_id")),
        authorName = authorName,
        authorUsername = authorUsername,
        authorAvatarUrl = avatarUrl,
        content = item.optString("content"),
        createdAt = item.optLong("createdAt").takeIf { it > 0L }
            ?: parseTimeMillis(item.optString("created_at"))
    )
}

private fun parseCommunityVolunteers(response: JSONObject): List<CommunityVolunteerPost> {
    val array = response.optJSONArray("volunteers")
    if (array != null) {
        return List(array.length()) { index ->
            parseCommunityVolunteer(array.optJSONObject(index) ?: JSONObject())
        }.filter { it.title.isNotBlank() && it.body.isNotBlank() }
    }
    val single = response.optJSONObject("volunteer")
    return if (single != null) listOf(parseCommunityVolunteer(single)) else emptyList()
}

private fun parseCommunityVolunteer(item: JSONObject): CommunityVolunteerPost {
    return CommunityVolunteerPost(
        id = item.optString("id", "volunteer-${item.optString("title").hashCode()}"),
        title = item.optString("title", "招募社区义工").ifBlank { "招募社区义工" },
        body = item.optString("body"),
        contact = item.optString("contact").takeIf { it.isNotBlank() && it != "null" } ?: "请在人文社区留言报名。",
        imageUrl = item.optString("imageUrl", item.optString("image_url")).takeIf { it.isNotBlank() && it != "null" },
        createdAt = item.optLong("createdAt").takeIf { it > 0L }
            ?: parseTimeMillis(item.optString("created_at"))
    )
}

private fun parseCommunityVolunteerApplications(array: JSONArray?): List<CommunityVolunteerApplication> {
    if (array == null) return emptyList()
    return List(array.length()) { index ->
        parseCommunityVolunteerApplication(array.optJSONObject(index) ?: JSONObject())
    }.filter { it.id.isNotBlank() }
}

private fun parseCommunityVolunteerApplication(item: JSONObject): CommunityVolunteerApplication {
    return CommunityVolunteerApplication(
        id = item.optString("id"),
        volunteerPostId = item.optString("volunteerPostId", item.optString("volunteer_post_id")),
        volunteerTitle = item.optString("volunteerTitle", item.optString("volunteer_title")),
        applicantId = item.optString("applicantId", item.optString("user_id")),
        applicantName = item.optString("applicantName", item.optString("display_name")),
        applicantUsername = item.optString("applicantUsername", item.optString("username")),
        applicantAvatarUrl = item.optNullableString("applicantAvatarUrl"),
        name = item.optString("name"),
        phone = item.optString("phone"),
        note = item.optString("note"),
        status = item.optString("status", "pending"),
        createdAt = item.optLong("createdAt").takeIf { it > 0L }
            ?: parseTimeMillis(item.optString("createdAt", item.optString("created_at")))
    )
}

private fun parseChatMessages(array: JSONArray?): List<ChatMessage> {
    if (array == null) return emptyList()
    return List(array.length()) { index ->
        val item = array.optJSONObject(index) ?: JSONObject()
        ChatMessage(
            id = item.optString("id"),
            sender = item.optString("sender"),
            content = item.optString("content"),
            createdAt = item.optLong("createdAt").takeIf { it > 0L }
                ?: parseTimeMillis(item.optString("created_at"))
        )
    }.filter { it.content.isNotBlank() }
}

private fun ChatMessage.speechUtteranceId(): String {
    val stablePart = id.ifBlank { "$createdAt-${content.hashCode()}" }
    return "anyi-ai-$stablePart"
}

private fun parseAiCompanions(array: JSONArray?): List<AiCompanion> {
    if (array == null) return emptyList()
    return List(array.length()) { index ->
        parseAiCompanion(array.optJSONObject(index) ?: JSONObject())
    }.filter { it.id.isNotBlank() }
}

private fun parseAiCompanion(item: JSONObject): AiCompanion {
    return AiCompanion(
        id = item.optString("id"),
        displayName = item.optString("displayName", item.optString("relation", "陪伴人物")).ifBlank { "陪伴人物" },
        gender = item.optString("gender", "不限定").ifBlank { "不限定" },
        relation = item.optString("relation", "亲人").ifBlank { "亲人" },
        avatarUrl = item.optString("avatarUrl").takeIf { it.isNotBlank() && it != "null" },
        smileAvatarUrl = item.optString("smileAvatarUrl").takeIf { it.isNotBlank() && it != "null" },
        avatarMotion = parseAvatarMotion(item.optJSONObject("avatarMotion")),
        paidUnlocked = item.optBoolean("paidUnlocked", false),
        photoCount = item.optInt("photoCount", 0),
        voiceCount = item.optInt("voiceCount", 0),
        momentCount = item.optInt("momentCount", 0),
        generated = item.optBoolean("generated", false),
        isDefault = item.optBoolean("isDefault", false),
        updatedAt = item.optLong("updatedAt").takeIf { it > 0L }
            ?: parseTimeMillis(item.optString("updated_at"))
    )
}

private fun parseAvatarMotion(item: JSONObject?): AvatarMotion? {
    if (item == null) return null
    val face = parseAvatarMotionBox(
        item.optJSONObject("face"),
        AvatarMotionBox(x = 0.5f, y = 0.46f, w = 0.5f, h = 0.58f)
    )
    val mouth = parseAvatarMotionBox(
        item.optJSONObject("mouth"),
        AvatarMotionBox(x = face.x, y = (face.y + face.h * 0.34f).coerceAtMost(0.88f), w = face.w * 0.34f, h = face.h * 0.1f)
    )
    return AvatarMotion(
        status = item.optString("status", "fallback"),
        source = item.optString("source", "fallback"),
        confidence = item.optDouble("confidence", 0.0).toFloat().coerceIn(0f, 1f),
        face = face,
        mouth = mouth
    )
}

private fun parseAvatarMotionBox(item: JSONObject?, fallback: AvatarMotionBox): AvatarMotionBox {
    val w = item?.optDouble("w", fallback.w.toDouble())?.toFloat()?.coerceIn(0.05f, 1f) ?: fallback.w
    val h = item?.optDouble("h", fallback.h.toDouble())?.toFloat()?.coerceIn(0.03f, 1f) ?: fallback.h
    return AvatarMotionBox(
        x = (item?.optDouble("x", fallback.x.toDouble())?.toFloat() ?: fallback.x).coerceIn(w / 2f, 1f - w / 2f),
        y = (item?.optDouble("y", fallback.y.toDouble())?.toFloat() ?: fallback.y).coerceIn(h / 2f, 1f - h / 2f),
        w = w,
        h = h
    )
}

private fun parseSignedInUser(response: JSONObject): AppUser {
    return parseUserPayload(response.getJSONObject("user"), response.getString("token"))
}

private fun parseUserPayload(user: JSONObject, token: String): AppUser {
    return AppUser(
        id = user.getString("id"),
        username = user.getString("username"),
        displayName = user.optString("displayName", user.getString("username")),
        role = user.optString("role", "user"),
        token = token,
        avatarUrl = user.optNullableString("avatarUrl")
    )
}

private fun JSONObject.optNullableString(key: String): String? {
    if (isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() && it != "null" }
}

private fun Throwable.userFriendlyMessage(fallback: String): String {
    val code = message.orEmpty()
    return when {
        code.contains("failed to connect", ignoreCase = true) ||
            code.contains("timed out", ignoreCase = true) ||
            code.contains("timeout", ignoreCase = true) ||
            code.contains("unable to resolve host", ignoreCase = true) ||
        code.contains("network is unreachable", ignoreCase = true) ->
            "云端连接超时：请检查服务器、网络或 API 地址后重新打包"
        code.contains("request_failed") -> "云端请求失败：服务器可能未更新接口或返回了异常"
        isAuthExpired() -> "登录状态已过期，请重新登录"
        code.contains("username_exists") -> "账号已存在，请直接登录"
        code.contains("weak_password") -> "密码至少需要 8 位"
        code.contains("invalid_credentials") -> "账号或密码不正确"
        code.contains("wechat_login_not_configured") -> "微信登录还没配置 AppID 和 AppSecret"
        code.contains("wechat_code_invalid") -> "微信授权已失效，请重新点微信登录"
        code.contains("wechat_userinfo_failed") -> "微信资料获取失败，请稍后重试"
        code.contains("flower_limit_reached") -> "当前已有 2 个花篮，冷却结束后再献花"
        code.contains("candle_limit_reached") -> "当前已有 2 根蜡烛，任一根燃尽后可继续点蜡烛"
        code.contains("durian_offering_requires_payment") -> "榴莲是付费供品，请先完成解锁"
        code.contains("apple_offering_limit_reached") -> "当前已有 3 个苹果，10 分钟后可继续放苹果"
        code.contains("durian_offering_limit_reached") -> "当前已有 1 个榴莲，10 分钟后可继续放榴莲"
        code.contains("invalid_fruit_type") -> "暂不支持这种供品"
        code.contains("payment_webhook_not_configured") -> "支付回调还未配置"
        code.contains("unsupported_file_type") -> "暂不支持这种文件类型，请更换图片或音频"
        code.contains("file_size_invalid") -> "文件过大或为空，图片最多 20MB，音频最多 50MB"
        code.contains("file_required") -> "没有读取到文件，请重新选择"
        code.contains("upload_content_rejected") -> "上传内容未通过审核，请更换文件"
        code.contains("rate_limit_exceeded") -> "操作太频繁，请稍后再试"
        code.isNotBlank() -> "$fallback：$code"
        else -> fallback
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Throwable.isAuthExpired(): Boolean {
    if (this is AnyiApiException) {
        return statusCode == 401 || code == "invalid_token" || code == "missing_token"
    }
    val text = message.orEmpty()
    return text.contains("invalid_token", ignoreCase = true) ||
        text.contains("missing_token", ignoreCase = true)
}

private fun Context.readUploadPayload(uri: Uri): UploadPayload {
    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
    val maxBytes = if (mimeType.startsWith("audio/")) 50L * 1024L * 1024L else 20L * 1024L * 1024L
    val declaredSize = queryFileSize(uri)
    if (declaredSize != null && (declaredSize <= 0L || declaredSize > maxBytes)) {
        throw IllegalArgumentException("file_size_invalid")
    }
    val rawName = queryDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "upload-${System.currentTimeMillis()}"
    val fileName = rawName
        .replace("\r", "_")
        .replace("\n", "_")
        .replace("\"", "_")
        .take(120)
        .ifBlank { "upload-${System.currentTimeMillis()}" }
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytesLimited(maxBytes) }
        ?: throw IllegalArgumentException("file_required")
    if (bytes.isEmpty()) {
        throw IllegalArgumentException("file_size_invalid")
    }
    return UploadPayload(fileName = fileName, mimeType = mimeType, bytes = bytes)
}

private fun InputStream.readBytesLimited(maxBytes: Long): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read.toLong()
        if (total > maxBytes) {
            throw IllegalArgumentException("file_size_invalid")
        }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun Context.queryFileSize(uri: Uri): Long? {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
        }
    }.getOrNull()
}

private fun Context.queryDisplayName(uri: Uri): String? {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
}

private fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
        .onFailure { Toast.makeText(this, "没有找到可打开链接的应用", Toast.LENGTH_SHORT).show() }
}

private fun legalUrl(path: String): String {
    return "${BuildConfig.API_BASE_URL.trimEnd('/')}/legal/$path"
}

private fun adminUrl(): String {
    return "${BuildConfig.API_BASE_URL.trimEnd('/')}/admin"
}

private fun absoluteAssetUrl(value: String): String {
    val apiBase = BuildConfig.API_BASE_URL.trimEnd('/')
    return when {
        value.startsWith("/") -> "$apiBase$value"
        value.startsWith("http://101.42.1.45/assets/") -> value.replace("http://101.42.1.45", apiBase)
        value.startsWith("https://101.42.1.45/assets/") -> value.replace("https://101.42.1.45", apiBase)
        value.startsWith("http://api.anyibj.cn/assets/") -> value.replace("http://api.anyibj.cn", apiBase)
        else -> value
    }
}

private fun parseTimeMillis(raw: String): Long {
    if (raw.isBlank()) return System.currentTimeMillis()
    raw.toLongOrNull()?.let { return it }
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(raw)?.time ?: System.currentTimeMillis()
    }.getOrElse { System.currentTimeMillis() }
}

private fun loadChatMessages(context: Context): List<ChatMessage> {
    val raw = context.appPrefs().getString(KEY_AI_CHAT, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ChatMessage(
                id = item.optString("id"),
                sender = item.optString("sender"),
                content = item.optString("content"),
                createdAt = item.optLong("createdAt")
            )
        }
    }.getOrElse { emptyList() }
}

private fun saveChatMessages(context: Context, messages: List<ChatMessage>) {
    val array = JSONArray()
    messages.forEach { message ->
        array.put(
            JSONObject()
                .put("id", message.id)
                .put("sender", message.sender)
                .put("content", message.content)
                .put("createdAt", message.createdAt)
        )
    }
    context.appPrefs().edit().putString(KEY_AI_CHAT, array.toString()).apply()
}

private fun companionReply(relation: String, content: String): String {
    val prefix = when (relation) {
        "宠物" -> "我好像听见你在叫我。"
        "朋友" -> "我在，慢慢说。"
        "伴侣" -> "我一直在认真听你说。"
        else -> "我在这里陪着你。"
    }
    val echo = content.take(18)
    return "$prefix 你刚才说「$echo」，这句话我会记在心里。"
}

private fun flowerChoices() = listOf(
    FlowerChoice(
        type = "wreath",
        name = "原背景花束",
        subtitle = "献花后按原背景花束放回灵台",
        imageResId = R.drawable.anyi_hall_flower_original_bouquet
    )
)

private fun defaultCommunityVolunteerPosts() = listOf(
    CommunityVolunteerPost(
        id = "default-story",
        title = "故事整理义工",
        body = "协助家属整理纪念故事、照片说明和人生片段，让重要记忆被温柔地保存下来。",
        contact = "在人文社区留言“故事义工”，安忆团队会联系你。",
        createdAt = parseTimeMillis("2026-06-05T00:00:00.000Z")
    ),
    CommunityVolunteerPost(
        id = "default-companion",
        title = "陪伴倾听义工",
        body = "为需要倾诉的人提供耐心、克制、尊重边界的陪伴，帮他们把想念慢慢说出来。",
        contact = "在人文社区留言“陪伴义工”报名。",
        createdAt = parseTimeMillis("2026-06-05T00:00:00.000Z")
    ),
    CommunityVolunteerPost(
        id = "default-offline",
        title = "线下互助义工",
        body = "参与纪念活动协助、物资整理和线下互助，让社区里的善意真正落到日常里。",
        contact = "在人文社区留言“线下义工”报名。",
        createdAt = parseTimeMillis("2026-06-05T00:00:00.000Z")
    )
)

private fun flowerImageRes(type: String) = flowerChoices()
    .firstOrNull { it.type == type }
    ?.imageResId
    ?: R.drawable.anyi_hall_flower_original_bouquet

private fun cleanMemorialDisplayName(raw: String, fallback: String = "安忆"): String {
    val cleaned = raw.trim()
        .filterNot { it == '?' || it == '？' || it == '\uFFFD' }
        .trim()
    return cleaned.ifBlank { fallback }
}

private fun loadLongList(raw: String?): List<Long> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        List(array.length()) { index -> array.optLong(index) }.filter { it > 0L }
    }.getOrElse { emptyList() }
}

private fun encodeLongList(values: List<Long>): String {
    val array = JSONArray()
    values.forEach { array.put(it) }
    return array.toString()
}

private fun formatRemaining(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatTime(millis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(millis))
}
