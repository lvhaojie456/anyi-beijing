package com.anyi.memorial

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.LruCache
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anyi.memorial.network.AnyiApiClient
import com.anyi.memorial.network.AnyiApiException
import com.anyi.memorial.network.UploadPayload
import com.anyi.memorial.privacy.PrivacyConsentStore
import com.anyi.memorial.wechat.WechatAuthBridge
import com.anyi.memorial.wechat.WechatAuthResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Background = Color(0xFFF5F5F7)
private val Ink = Color(0xFF1D1D1F)
private val Muted = Color(0xFF6E6E73)
private val Line = Color(0xFFE1DCCF)
private val Green = Color(0xFF9A6B2F)
private val Leaf = Color(0xFFFFF4DB)
private val Amber = Color(0xFFE6B84A)
private val Blush = Color(0xFFFFF1E7)
private val Rose = Color(0xFFC86D4F)
private val Morning = Color(0xFFFFFAEF)
private val Night = Color(0xFF2B2418)
private val Paper = Color(0xFFFFFEFB)
private val HallWarmBackground = Color(0xFFFFF2DE)
private val HallWarmSurface = Color(0xFFFFFBF2)

private val AppCardShape = RoundedCornerShape(16.dp)
private val AppButtonShape = RoundedCornerShape(12.dp)
private val AppInputShape = RoundedCornerShape(12.dp)
private val AppTagShape = RoundedCornerShape(999.dp)

private const val PREFS_NAME = "anyi_memorial_app"
private const val KEY_USER_ID = "session_user_id"
private const val KEY_USER_NAME = "session_username"
private const val KEY_DISPLAY_NAME = "session_display_name"
private const val KEY_USER_GENDER = "session_gender"
private const val KEY_USER_ROLE = "session_role"
private const val KEY_USER_AVATAR = "session_avatar_url"
private const val KEY_AI_COMPANION_LIST_BACKGROUND = "session_ai_companion_list_background_url"
private const val KEY_LEGACY_AUTH_TOKEN = "session_token"
private const val KEY_AUTH_TOKEN_ENCRYPTED = "session_token_encrypted"
private const val SESSION_KEY_ALIAS = "anyi_session_key"
private const val PENDING_CRASH_REPORT_FILE = "pending_crash_report.json"

private const val DURIAN_OFFERING_FEATURE = "offering_durian"

private const val CLOUD_CACHE_DIR = "anyi_cloud_resources"
private const val CLOUD_CACHE_MAX_BYTES = 160L * 1024L * 1024L
private const val CLOUD_CACHE_TARGET_BYTES = 120L * 1024L * 1024L
private const val URI_IMAGE_MEMORY_CACHE_KB = 24 * 1024

private val uriImageMemoryCache = object : LruCache<String, Bitmap>(URI_IMAGE_MEMORY_CACHE_KB) {
    override fun sizeOf(key: String, value: Bitmap): Int {
        return (value.allocationByteCount / 1024).coerceAtLeast(1)
    }
}

data class AppUser(
    val id: String,
    val username: String,
    val displayName: String,
    val gender: String? = null,
    val role: String,
    val token: String,
    val avatarUrl: String? = null,
    val aiCompanionListBackgroundUrl: String? = null
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
    val createdAt: Long,
    val moderationStatus: String = "approved"
)

data class CommunityComment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarUrl: String?,
    val content: String,
    val createdAt: Long,
    val moderationStatus: String = "approved"
)

data class CommunityVolunteerPost(
    val id: String,
    val title: String,
    val body: String,
    val contact: String,
    val imageUrl: String? = null,
    val status: String = "open",
    val deadlineAt: Long? = null,
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
    val createdAt: Long,
    val updatedAt: Long
)

private data class CommunityRefreshResult(
    val posts: List<CommunityPost>,
    val commentsByPost: Map<String, List<CommunityComment>>,
    val volunteers: List<CommunityVolunteerPost>,
    val applications: List<CommunityVolunteerApplication>
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
    private var consentedServicesStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PrivacyConsentStore.isAccepted(applicationContext)) {
            startConsentedServices()
        }
        enableEdgeToEdge()
        setContent {
            AnyiTheme {
                PrivacyConsentRoot(
                    onConsentAccepted = ::startConsentedServices,
                    onConsentDeclined = ::finishAndRemoveTask
                )
            }
        }
    }

    private fun startConsentedServices() {
        if (consentedServicesStarted) return
        consentedServicesStarted = true
        installCrashReporterOnce(applicationContext)
        flushPendingCrashReportOnce(applicationContext)
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
private fun PrivacyConsentRoot(
    onConsentAccepted: () -> Unit,
    onConsentDeclined: () -> Unit
) {
    val context = LocalContext.current
    var accepted by rememberSaveable {
        mutableStateOf(PrivacyConsentStore.isAccepted(context))
    }

    if (accepted) {
        AnyiRoot()
    } else {
        PrivacyConsentScreen(
            onAccept = {
                if (PrivacyConsentStore.accept(context)) {
                    onConsentAccepted()
                    accepted = true
                    true
                } else {
                    false
                }
            },
            onDecline = onConsentDeclined
        )
    }
}

@Composable
private fun PrivacyConsentScreen(
    onAccept: () -> Boolean,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    var message by rememberSaveable { mutableStateOf("") }

    AppBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "欢迎使用安忆",
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "请在使用前阅读并同意用户协议和隐私政策。只有在您同意后，安忆才会连接云端，并启用账号登录、微信登录和必要的崩溃诊断。",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "您可以在隐私政策中了解个人信息的处理目的、使用方式、共享情况以及访问、更正、删除和注销账号的权利。",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { context.openUrl(legalUrl("terms")) }) {
                    Text("用户协议", color = Green, fontWeight = FontWeight.Bold)
                }
                Text("和", color = Muted, fontSize = 13.sp)
                TextButton(onClick = { context.openUrl(legalUrl("privacy")) }) {
                    Text("隐私政策", color = Green, fontWeight = FontWeight.Bold)
                }
            }
            if (message.isNotBlank()) {
                StatusMessage(message)
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(4.dp))
            }
            Button(
                onClick = {
                    if (!onAccept()) {
                        message = "同意记录保存失败，请重试"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = AppButtonShape,
                colors = primaryButtonColors(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("同意并继续", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth(),
                shape = AppButtonShape,
                border = BorderStroke(1.dp, Line),
                colors = quietOutlinedButtonColors(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("不同意并退出", color = Ink, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AnyiRoot() {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(readSession(context)) }

    DisposableEffect(context) {
        // API clients used by nested screens share this handler so any 401 returns
        // the app to the auth root instead of leaving a stale authenticated screen.
        AnyiApiClient.setUnauthorizedHandler {
            Handler(Looper.getMainLooper()).post {
                clearSession(context)
                currentUser = null
            }
        }
        onDispose { AnyiApiClient.setUnauthorizedHandler(null) }
    }

    LaunchedEffect(currentUser?.token) {
        val user = currentUser ?: return@LaunchedEffect
        val result = runCatching {
            withContext(Dispatchers.IO) {
                AnyiApiClient(tokenProvider = { user.token }).currentUser()
            }
        }
        result
            .onSuccess { response ->
                val refreshed = parseUserPayload(response.getJSONObject("user"), user.token)
                writeSession(context, refreshed)
                currentUser = refreshed
            }
            .onFailure { error ->
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
    var registrationName by rememberSaveable { mutableStateOf("") }
    var registrationGender by rememberSaveable { mutableStateOf("") }
    var registrationAvatarUri by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var serverMessage by rememberSaveable { mutableStateOf("正在检查云端服务...") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var wechatEnabled by rememberSaveable { mutableStateOf(false) }
    val registrationAvatar by rememberUriImage(registrationAvatarUri, maxDimensionPx = 512)
    val registrationAvatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.persistReadPermission(uri)
        registrationAvatarUri = uri.toString()
        message = ""
    }

    LaunchedEffect(Unit) {
        val healthResult = runCatching {
            withContext(Dispatchers.IO) { api.health() }
        }
        val configResult = runCatching {
            withContext(Dispatchers.IO) { api.appConfig() }
        }
        wechatEnabled = configResult.getOrNull()
            ?.optJSONObject("wechat")
            ?.optBoolean("enabled", false) == true && WechatAuthBridge.isConfigured()
        serverMessage = healthResult.fold(
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
                        .clip(RoundedCornerShape(22.dp))
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
                            .background(Leaf.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
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

                    if (wechatEnabled && !isRegister) {
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
                                                        parseSignedInUser(
                                                            api.loginWithWechat(
                                                                result.code,
                                                                acceptedTerms = true,
                                                                acceptedPrivacy = true
                                                            )
                                                        )
                                                    }
                                                }
                                                loading = false
                                                signedIn
                                                    .onSuccess { onSignedIn(it) }
                                                    .onFailure {
                                                        message = it.userFriendlyMessage(
                                                            "微信登录失败，请稍后再试"
                                                        )
                                                    }
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
                            shape = AppButtonShape,
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
                            Text(
                                if (loading) "正在连接微信..." else "微信一键登录",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                    }
                    Text(if (isRegister) "创建账号" else "使用账号密码登录", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))

                    if (isRegister) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Leaf)
                                    .border(1.dp, Line, CircleShape)
                                    .clickable(enabled = !loading) { registrationAvatarPicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                val avatarBitmap = registrationAvatar
                                if (avatarBitmap != null) {
                                    ComposeImage(
                                        bitmap = avatarBitmap,
                                        contentDescription = "注册头像",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Rounded.PhotoCamera, contentDescription = "选择头像", tint = Green)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("头像（必填）", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                TextButton(
                                    onClick = { registrationAvatarPicker.launch("image/*") },
                                    enabled = !loading,
                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (registrationAvatarUri.isBlank()) "选择头像" else "重新选择",
                                        color = Green,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = registrationName,
                            onValueChange = { registrationName = it.take(40) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = AppInputShape,
                            label = { Text("名称（必填）") },
                            leadingIcon = { Icon(Icons.Rounded.Face, contentDescription = null) },
                            colors = warmTextFieldColors()
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("性别（必填）", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Leaf.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            AuthModeButton("男", registrationGender == "男") {
                                registrationGender = "男"
                                message = ""
                            }
                            AuthModeButton("女", registrationGender == "女") {
                                registrationGender = "女"
                                message = ""
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = AppInputShape,
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
                        shape = AppInputShape,
                        label = { Text("密码") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = warmTextFieldColors()
                    )

                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        StatusMessage(message = message)
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            val trimmed = username.trim()
                            val trimmedRegistrationName = registrationName.trim()
                            when {
                                trimmed.isBlank() || password.isBlank() -> {
                                    message = "请输入账号和密码"
                                }
                                isRegister -> {
                                    if (trimmedRegistrationName.isBlank()) {
                                        message = "请输入名称"
                                        return@Button
                                    }
                                    if (registrationGender != "男" && registrationGender != "女") {
                                        message = "请选择性别"
                                        return@Button
                                    }
                                    if (registrationAvatarUri.isBlank()) {
                                        message = "请选择头像"
                                        return@Button
                                    }
                                    if (password.length < 8) {
                                        message = "密码至少需要 8 位"
                                        return@Button
                                    }
                                    loading = true
                                    scope.launch {
                                        val result = runCatching {
                                            withContext(Dispatchers.IO) {
                                                val avatarPayload = context.readUploadPayload(Uri.parse(registrationAvatarUri))
                                                parseSignedInUser(
                                                    api.register(
                                                        trimmed,
                                                        password,
                                                        trimmedRegistrationName,
                                                        registrationGender,
                                                        avatarPayload,
                                                        acceptedTerms = true,
                                                        acceptedPrivacy = true
                                                    )
                                                )
                                            }
                                        }
                                        loading = false
                                        result
                                            .onSuccess { user ->
                                                message = "账号已创建并同步到云端"
                                                onSignedIn(user)
                                            }
                                            .onFailure {
                                                message = it.userFriendlyMessage(
                                                    "注册失败，请检查网络或账号密码"
                                                )
                                            }
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
                                            .onFailure {
                                                message = it.userFriendlyMessage("登录失败，请确认账号密码")
                                            }
                                    }
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppButtonShape,
                        colors = primaryButtonColors(),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(if (loading) "正在连接云端..." else if (isRegister) "注册并进入" else "登录")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftTag("云端纪念")
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
        shape = AppInputShape,
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
    return listOf(
        "失败",
        "错误",
        "超时",
        "过期",
        "不正确",
        "请先",
        "请输入",
        "请选择",
        "不能",
        "未配置",
        "未通过",
        "没有"
    )
        .any { message.contains(it) }
}

@Composable
private fun SoftTag(text: String) {
    Surface(color = Color.White.copy(alpha = 0.78f), shape = AppTagShape, border = BorderStroke(1.dp, Color.White)) {
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
            .background(bg, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp)
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
    var companionChatOpen by rememberSaveable { mutableStateOf(false) }
    val tabStateHolder = rememberSaveableStateHolder()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!(selectedTab == ScreenTab.Companion && companionChatOpen)) {
                    AnyiBottomBar(selectedTab = selectedTab, onSelected = {
                        companionChatOpen = false
                        selectedTab = it
                    })
                }
            }
        ) { innerPadding ->
            val warmSurface = selectedTab == ScreenTab.Hall ||
                selectedTab == ScreenTab.Community ||
                selectedTab == ScreenTab.Profile
            AppBackdrop(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                backgroundColor = if (warmSurface) HallWarmBackground else Background,
                warmHall = warmSurface
            ) {
                val contentModifier = when (selectedTab) {
                    ScreenTab.Hall -> Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp)
                        .padding(horizontal = 18.dp)
                    ScreenTab.Companion -> Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                    ScreenTab.Community -> Modifier.fillMaxSize()
                    else -> Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 10.dp)
                        .padding(horizontal = 18.dp)
                }
                Column(
                    modifier = contentModifier
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        tabStateHolder.SaveableStateProvider(selectedTab.name) {
                            when (selectedTab) {
                                ScreenTab.Hall -> MemorialHallScreen(user)
                                ScreenTab.Companion -> AiCompanionScreen(
                                    user = user,
                                    onBack = { selectedTab = ScreenTab.Hall },
                                    onChatStateChange = { companionChatOpen = it },
                                    onUserChanged = onUserChanged
                                )
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
    val hallMode = selectedTab == ScreenTab.Hall ||
        selectedTab == ScreenTab.Community ||
        selectedTab == ScreenTab.Profile
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        color = if (hallMode) HallWarmSurface.copy(alpha = 0.96f) else Color.White.copy(alpha = 0.96f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, if (hallMode) Color(0xFFEAD7B8).copy(alpha = 0.85f) else Line.copy(alpha = 0.6f)),
        shadowElevation = 8.dp
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
                        .clip(RoundedCornerShape(22.dp))
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
private fun ProfileSettingsLink(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    contentColor: Color = Ink,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 46.dp)
                    .height(1.dp)
                    .background(Line.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun ProfileFeedback(message: String, isError: Boolean) {
    if (message.isBlank()) return
    val accent = if (isError) Color(0xFFB42318) else Green
    val background = if (isError) Color(0xFFFFF2EE) else Leaf.copy(alpha = 0.88f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = background,
        shape = AppInputShape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.Close else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                color = if (isError) Color(0xFF7A271A) else Green,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }
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
    var gender by rememberSaveable(user.id) { mutableStateOf(user.gender.orEmpty()) }
    var avatarUrl by rememberSaveable(user.id) { mutableStateOf(user.avatarUrl.orEmpty()) }
    var feedbackMessage by rememberSaveable(user.id) { mutableStateOf("") }
    var feedbackIsError by rememberSaveable(user.id) { mutableStateOf(false) }
    var showProfileEditor by rememberSaveable(user.id) { mutableStateOf(false) }
    var activeOperation by remember(user.id) { mutableStateOf("") }
    val avatar by rememberUriImage(avatarUrl, maxDimensionPx = 512)
    val isWorking = activeOperation.isNotBlank()

    fun saveProfile(nextAvatarUrl: String?) {
        val nextName = displayName.trim().ifBlank { user.username }
        if (gender != "男" && gender != "女") {
            feedbackIsError = true
            feedbackMessage = "请选择性别"
            return
        }
        feedbackMessage = ""
        activeOperation = "profile"
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val updated = api.updateCurrentUser(nextName, nextAvatarUrl, gender).getJSONObject("user")
                    parseUserPayload(updated, user.token)
                }
            }
            activeOperation = ""
            result
                .onSuccess { updated ->
                    displayName = updated.displayName
                    gender = updated.gender.orEmpty()
                    avatarUrl = updated.avatarUrl.orEmpty()
                    feedbackIsError = false
                    feedbackMessage = "个人资料已保存"
                    onUserChanged(updated)
                }
                .onFailure {
                    feedbackIsError = true
                    feedbackMessage = it.userFriendlyMessage("保存资料失败")
                }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.persistReadPermission(uri)
        feedbackMessage = ""
        activeOperation = "avatar"
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
                        uploadedUrl,
                        gender.takeIf { it == "男" || it == "女" }
                    ).getJSONObject("user")
                    parseUserPayload(updated, user.token)
                }
            }
            activeOperation = ""
            result
                .onSuccess { updated ->
                    displayName = updated.displayName
                    gender = updated.gender.orEmpty()
                    avatarUrl = updated.avatarUrl.orEmpty()
                    feedbackIsError = false
                    feedbackMessage = "头像已更新"
                    onUserChanged(updated)
                }
                .onFailure {
                    feedbackIsError = true
                    feedbackMessage = it.userFriendlyMessage("上传头像失败")
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "个人设置",
                color = Ink,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    displayName = user.displayName
                    gender = user.gender.orEmpty()
                    avatarUrl = user.avatarUrl.orEmpty()
                    feedbackMessage = ""
                    feedbackIsError = false
                    showProfileEditor = true
                },
                enabled = !isWorking
            ) {
                Icon(Icons.Rounded.Menu, contentDescription = "资料设置", tint = Ink)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.82f),
            shape = AppButtonShape,
            border = BorderStroke(1.dp, Line.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Night, Green, Amber.copy(alpha = 0.86f))))
                        .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarBitmap = avatar
                    if (avatarBitmap != null) {
                        ComposeImage(
                            bitmap = avatarBitmap,
                            contentDescription = "个人头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = "默认头像",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        displayName.ifBlank { user.username },
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "性别：${gender.ifBlank { "未设置" }}",
                        color = Muted,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(
                        "账号：${user.username}",
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("协议与隐私", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.9f),
                shape = AppButtonShape,
                border = BorderStroke(1.dp, Line.copy(alpha = 0.7f))
            ) {
                Column {
                    ProfileSettingsLink(
                        text = "用户协议",
                        icon = Icons.AutoMirrored.Rounded.Article,
                        onClick = { context.openUrl(legalUrl("terms")) }
                    )
                    ProfileSettingsLink(
                        text = "隐私政策",
                        icon = Icons.Rounded.Lock,
                        onClick = { context.openUrl(legalUrl("privacy")) }
                    )
                    ProfileSettingsLink(
                        text = "AI 服务说明",
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = { context.openUrl(legalUrl("ai-disclaimer")) }
                    )
                    ProfileSettingsLink(
                        text = "注销账号",
                        icon = Icons.Rounded.Delete,
                        onClick = { context.openUrl(legalUrl("account-deletion")) },
                        contentColor = Color(0xFFB42318),
                        showDivider = false
                    )
                }
            }
        }

        if (user.role == "admin") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("其他", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.72f),
                    shape = AppButtonShape,
                    border = BorderStroke(1.dp, Line.copy(alpha = 0.55f))
                ) {
                    ProfileSettingsLink(
                        text = "管理后台",
                        icon = Icons.Rounded.AdminPanelSettings,
                        onClick = { context.openUrl(adminUrl()) },
                        contentColor = Muted,
                        showDivider = false
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("登录状态", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            OutlinedButton(
                onClick = onLogout,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
                shape = AppButtonShape,
                border = BorderStroke(1.dp, Color(0xFFF04438).copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB42318)),
                contentPadding = PaddingValues(vertical = 13.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("退出登录", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showProfileEditor) {
        ProfileEditorDialog(
            username = user.username,
            displayName = displayName,
            onDisplayNameChange = {
                displayName = it.take(40)
                feedbackMessage = ""
            },
            gender = gender,
            onGenderChange = {
                gender = it
                feedbackMessage = ""
            },
            avatar = avatar,
            activeOperation = activeOperation,
            feedbackMessage = feedbackMessage,
            feedbackIsError = feedbackIsError,
            onChooseAvatar = { avatarPicker.launch("image/*") },
            onSave = { saveProfile(avatarUrl.takeIf { it.isNotBlank() }) },
            onDismiss = { if (!isWorking) showProfileEditor = false }
        )
    }
}

@Composable
private fun ProfileEditorDialog(
    username: String,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    avatar: ImageBitmap?,
    activeOperation: String,
    feedbackMessage: String,
    feedbackIsError: Boolean,
    onChooseAvatar: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val isWorking = activeOperation.isNotBlank()
    Dialog(onDismissRequest = { if (!isWorking) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Paper,
            shape = AppCardShape,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "资料设置",
                        color = Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, enabled = !isWorking) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Muted)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Night, Green, Amber.copy(alpha = 0.86f))))
                            .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                            .clickable(enabled = !isWorking, role = Role.Button, onClick = onChooseAvatar),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatar != null) {
                            ComposeImage(
                                bitmap = avatar,
                                contentDescription = "当前头像",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = "默认头像",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            displayName.ifBlank { username },
                            color = Ink,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("账号：$username", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(5.dp))
                        OutlinedButton(
                            onClick = onChooseAvatar,
                            enabled = !isWorking,
                            shape = AppButtonShape,
                            border = BorderStroke(1.dp, Line),
                            colors = quietOutlinedButtonColors(),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            if (activeOperation == "avatar") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    color = Green,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(if (activeOperation == "avatar") "正在上传" else "更换头像", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = onDisplayNameChange,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = AppInputShape,
                    label = { Text("昵称") },
                    leadingIcon = { Icon(Icons.Rounded.Face, contentDescription = null) },
                    colors = warmTextFieldColors()
                )

                Text("性别", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Leaf.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    AuthModeButton("男", gender == "男") { onGenderChange("男") }
                    AuthModeButton("女", gender == "女") { onGenderChange("女") }
                }

                Button(
                    onClick = onSave,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppButtonShape,
                    colors = primaryButtonColors(),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    if (activeOperation == "profile") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (activeOperation == "profile") "正在保存" else "保存个人资料", fontWeight = FontWeight.Bold)
                }

                ProfileFeedback(message = feedbackMessage, isError = feedbackIsError)
            }
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
    val portrait by rememberUriImage(portraitUri, maxDimensionPx = 768)

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
                        flowerText = flowerOfferingSummary(activeFlowers),
                        candleText = if (canLightCandle) {
                            "${activeCandles.size}/2"
                        } else {
                            "冷却 ${formatRemaining(nextCandleMillis)}"
                        },
                        incenseText = if (canLightIncense) "清香" else formatRemaining(nextIncenseMillis),
                        burnText = if (canOfferApple) "供苹果" else "苹果 ${formatRemaining(nextAppleMillis)}",
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
                                            response.optJSONArray("candleUntilList")
                                                ?: JSONArray().put(response.optLong("candleUntil"))
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
                Text("付费功能暂未开放。正式支付接入并通过验收后才会启用解锁。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        cloudMessage = "付费功能暂未开放"
                        showPayment = false
                    },
                    colors = primaryButtonColors()
                ) {
                    Text("知道了")
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
            canOfferApple = canOfferApple,
            nextAppleMillis = nextAppleMillis,
            onDismiss = { showBurnPicker = false },
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
            text = { Text("付费供品暂未开放。正式支付接入并通过验收后才会启用。") },
            confirmButton = {
                Button(
                    onClick = {
                        cloudMessage = "付费供品暂未开放"
                        showDurianPayment = false
                    },
                    colors = primaryButtonColors()
                ) {
                    Text("知道了")
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
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFFE6B0),
                            accent.copy(alpha = 0.72f),
                            Green.copy(alpha = 0.5f)
                        )
                    )
                )
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
            Text(
                title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                title = "上供",
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

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.17f)
                    .width(maxWidth * 0.34f)
                    .height(maxHeight * 0.34f)
            ) {
                val portraitBitmap = portrait
                if (portraitBitmap != null) {
                    ComposeImage(
                        bitmap = portraitBitmap,
                        contentDescription = "纪念照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MemorialMountainArtwork(modifier = Modifier.fillMaxSize())
                }
            }

            MemorialTablet(
                memorialName = memorialName,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.515f)
            )

            if (incenseUntil > now) {
                StageIncenseSticks(modifier = Modifier.matchParentSize())
            }

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
                    .offset(x = (-86).dp)
                    .padding(top = maxHeight * 0.658f)
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
private fun MemorialMountainArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color(0xFFFFF5E8))) {
        drawCircle(
            color = Color.White.copy(alpha = 0.86f),
            radius = size.width * 0.13f,
            center = Offset(size.width * 0.72f, size.height * 0.28f)
        )
        val rearMountain = Path().apply {
            moveTo(0f, size.height * 0.72f)
            cubicTo(
                size.width * 0.2f,
                size.height * 0.58f,
                size.width * 0.24f,
                size.height * 0.42f,
                size.width * 0.42f,
                size.height * 0.54f
            )
            cubicTo(
                size.width * 0.58f,
                size.height * 0.66f,
                size.width * 0.7f,
                size.height * 0.58f,
                size.width,
                size.height * 0.72f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(rearMountain, color = Color(0xFFEADBC7).copy(alpha = 0.75f))

        val frontMountain = Path().apply {
            moveTo(0f, size.height * 0.82f)
            cubicTo(
                size.width * 0.25f,
                size.height * 0.66f,
                size.width * 0.42f,
                size.height * 0.7f,
                size.width * 0.56f,
                size.height * 0.78f
            )
            cubicTo(
                size.width * 0.72f,
                size.height * 0.88f,
                size.width * 0.86f,
                size.height * 0.72f,
                size.width,
                size.height * 0.8f
            )
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
private fun MemorialTablet(memorialName: String, modifier: Modifier = Modifier) {
    val displayName = cleanMemorialDisplayName(memorialName).take(2)
    val tabletText = "${displayName}长存"
    Surface(
        modifier = modifier
            .width(64.dp)
            .height(116.dp),
        color = Color(0xFFF7DDB4).copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFFFF1D4).copy(alpha = 0.94f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 9.dp)
                .border(
                    1.dp,
                    Color(0xFFDAA764).copy(alpha = 0.58f),
                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 1.dp, bottomEnd = 1.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                tabletText.forEach { char ->
                    Text(
                        char.toString(),
                        color = Color(0xFF8B5E24),
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
            .width(112.dp)
            .height(82.dp)
    ) {
        val apples = fruitOfferings.filter { it.type == "apple" }.take(3)
        val hasDurian = fruitOfferings.any { it.type == "durian" }
        if (apples.isNotEmpty()) {
            val appleCount = apples.size
            val applePlateWidth = if (appleCount == 2) 96.dp else 108.dp
            val applePlateHeight = if (appleCount == 2) 70.dp else 78.dp
            ApplePlateOffering(
                appleCount = appleCount,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(applePlateWidth)
                    .height(applePlateHeight)
                    .offset(y = 4.dp)
            )
        }
        if (hasDurian) {
            FruitIcon(
                type = "durian",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(width = 42.dp, height = 38.dp)
            )
        }
    }
}

@Composable
private fun ApplePlateOffering(appleCount: Int, modifier: Modifier = Modifier) {
    val count = appleCount.coerceIn(1, 3)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        // Grounding contact shadow that adapts to the size
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.12f)
                .offset(y = 3.dp)
        ) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66000000), Color.Transparent),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width / 2
                )
            )
        }
        ComposeImage(
            painter = painterResource(id = appleOfferingImageRes(count)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            // Subtle ambient warm lighting filter to blend with the warm altar atmosphere
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                Color(0xFFFFFAF2).copy(alpha = 0.08f),
                androidx.compose.ui.graphics.BlendMode.SrcAtop
            )
        )
    }
}

private fun appleOfferingImageRes(count: Int): Int = when (count.coerceIn(1, 3)) {
    1 -> R.drawable.anyi_hall_apple_real
    2 -> R.drawable.anyi_hall_apples_two_plate
    else -> R.drawable.anyi_hall_apples_real
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
                                Text(
                                    choice.subtitle,
                                    color = Muted,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
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
    canOfferApple: Boolean,
    nextAppleMillis: Long,
    onDismiss: () -> Unit,
    onOfferApple: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = RoundedCornerShape(8.dp),
        icon = { Icon(Icons.Rounded.Redeem, contentDescription = null, tint = Green) },
        title = { Text("上供苹果") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("把苹果放上灵台，作为一份安静的心意。", color = Muted, fontSize = 12.sp)
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
                            ApplePlateOffering(
                                appleCount = 1,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(width = 70.dp, height = 50.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("上供苹果", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text(
                                if (canOfferApple) {
                                    "每次 1 个，10 分钟后消失"
                                } else {
                                    "冷却 ${formatRemaining(nextAppleMillis)}"
                                },
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
private fun HumanitiesCommunityScreen(user: AppUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(user.token) { AnyiApiClient(tokenProvider = { user.token }) }
    val isAdmin = user.role == "admin"
    var posts by remember { mutableStateOf(emptyList<CommunityPost>()) }
    var postComments by remember { mutableStateOf<Map<String, List<CommunityComment>>>(emptyMap()) }
    var volunteers by remember { mutableStateOf(emptyList<CommunityVolunteerPost>()) }
    var volunteerApplications by remember { mutableStateOf(emptyList<CommunityVolunteerApplication>()) }
    var volunteerIndex by remember { mutableStateOf(0) }
    var draft by rememberSaveable { mutableStateOf("") }
    var inlineCommentDraft by rememberSaveable { mutableStateOf("") }
    var inlineCommentPostId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedImageUris by remember { mutableStateOf(emptyList<String>()) }
    var loading by remember { mutableStateOf(false) }
    var posting by remember { mutableStateOf(false) }
    var commentLoadingPostIds by remember { mutableStateOf(emptySet<String>()) }
    var postingCommentPostId by remember { mutableStateOf<String?>(null) }
    var deletingPostId by remember { mutableStateOf<String?>(null) }
    var deletingCommentId by remember { mutableStateOf<String?>(null) }
    var volunteerPosting by remember { mutableStateOf(false) }
    var volunteerCoverUploading by remember { mutableStateOf(false) }
    var volunteerReviewChecking by remember { mutableStateOf(false) }
    var volunteerProjectsLoading by remember { mutableStateOf(false) }
    var volunteerApplying by remember { mutableStateOf(false) }
    var volunteerApplicationsLoading by remember { mutableStateOf(false) }
    var reviewingApplicationId by remember { mutableStateOf<String?>(null) }
    var cancellingApplicationId by remember { mutableStateOf<String?>(null) }
    var updatingVolunteerId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var showPublishDialog by remember { mutableStateOf(false) }
    var showVolunteer by remember { mutableStateOf(false) }
    var showVolunteerPublishDialog by rememberSaveable { mutableStateOf(false) }
    var selectedVolunteerDetail by remember { mutableStateOf<CommunityVolunteerPost?>(null) }
    var volunteerDraftTitle by rememberSaveable { mutableStateOf("") }
    var volunteerDraftBody by rememberSaveable { mutableStateOf("") }
    var volunteerDraftContact by rememberSaveable { mutableStateOf("") }
    var volunteerDraftDeadline by rememberSaveable { mutableStateOf("") }
    var volunteerCoverUri by rememberSaveable { mutableStateOf<String?>(null) }
    var volunteerCoverAssetId by rememberSaveable { mutableStateOf<String?>(null) }
    var volunteerCoverAssetUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var volunteerCoverReviewStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var volunteerCoverReviewReason by rememberSaveable { mutableStateOf<String?>(null) }
    var volunteerPublishRequested by rememberSaveable { mutableStateOf(false) }
    var volunteerPublishMessage by rememberSaveable { mutableStateOf("") }
    var uploadRequestId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var volunteerCreateRequestId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var applyingVolunteer by remember { mutableStateOf<CommunityVolunteerPost?>(null) }
    var reportingPost by remember { mutableStateOf<CommunityPost?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { context.persistReadPermission(it) }
        selectedImageUris = (selectedImageUris + uris.map { it.toString() }).distinct().take(9)
    }
    val volunteerCoverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.persistReadPermission(it)
            volunteerCoverUri = it.toString()
            volunteerCoverAssetId = null
            volunteerCoverAssetUrl = null
            volunteerCoverReviewStatus = null
            volunteerCoverReviewReason = null
            volunteerPublishRequested = false
            volunteerPublishMessage = ""
            uploadRequestId = UUID.randomUUID().toString()
        }
    }

    fun refreshCommunity() {
        scope.launch {
            loading = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val communityPosts = parseCommunityPosts(api.listCommunityPosts())
                    val commentsByPost = communityPosts.associate { post ->
                        val comments = runCatching {
                            parseCommunityComments(api.listCommunityPostComments(post.id))
                        }.getOrDefault(emptyList())
                        post.id to comments
                    }
                    val volunteerPosts = parseCommunityVolunteers(api.communityVolunteerInfo())
                    val applications = runCatching {
                        parseCommunityVolunteerApplications(
                            api.listCommunityVolunteerApplications("all")
                        )
                    }.getOrElse { error ->
                        if (isAdmin) throw error else emptyList()
                    }
                    CommunityRefreshResult(
                        posts = communityPosts,
                        commentsByPost = commentsByPost,
                        volunteers = volunteerPosts,
                        applications = applications
                    )
                }
            }
            loading = false
            result
                .onSuccess { resultData ->
                    posts = resultData.posts
                    postComments = resultData.commentsByPost
                    volunteers = resultData.volunteers
                    volunteerApplications = resultData.applications
                    volunteerIndex = volunteerIndex.coerceAtMost((resultData.volunteers.size - 1).coerceAtLeast(0))
                    message = ""
                }
                .onFailure { message = it.userFriendlyMessage("社区加载失败") }
        }
    }

    fun refreshVolunteerApplications() {
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

    fun refreshVolunteerProjects() {
        if (volunteerProjectsLoading) return
        scope.launch {
            volunteerProjectsLoading = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityVolunteers(api.communityVolunteerInfo())
                }
            }
            volunteerProjectsLoading = false
            result
                .onSuccess { loaded ->
                    volunteers = loaded
                    volunteerIndex = volunteerIndex.coerceAtMost((loaded.size - 1).coerceAtLeast(0))
                    selectedVolunteerDetail = selectedVolunteerDetail?.let { selected ->
                        loaded.firstOrNull { it.id == selected.id }
                    }
                }
                .onFailure { message = it.userFriendlyMessage("招募项目加载失败") }
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
                    parseCommunityPost(
                        api.createCommunityPost(
                            content,
                            imageUrls,
                            idempotencyKey = UUID.randomUUID().toString()
                        ).getJSONObject("post")
                    )
                }
            }
            posting = false
            result
                .onSuccess { post ->
                    draft = ""
                    selectedImageUris = emptyList()
                    showPublishDialog = false
                    posts = listOf(post) + posts.filterNot { it.id == post.id }
                    postComments = postComments + (post.id to emptyList())
                    message = if (post.moderationStatus == "pending") {
                        "已提交，审核通过后会对其他用户显示"
                    } else {
                        "已发布到人文社区"
                    }
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

    fun openInlineComment(post: CommunityPost) {
        inlineCommentPostId = post.id
        inlineCommentDraft = ""
        if (postComments.containsKey(post.id)) return
        scope.launch {
            commentLoadingPostIds = commentLoadingPostIds + post.id
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityComments(api.listCommunityPostComments(post.id))
                }
            }
            commentLoadingPostIds = commentLoadingPostIds - post.id
            result
                .onSuccess { loaded -> postComments = postComments + (post.id to loaded) }
                .onFailure { message = it.userFriendlyMessage("评论加载失败") }
        }
    }

    fun publishInlineComment(post: CommunityPost) {
        val content = inlineCommentDraft.trim()
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
        postComments = postComments + (post.id to ((postComments[post.id] ?: emptyList()) + optimisticComment))
        inlineCommentDraft = ""
        scope.launch {
            postingCommentPostId = post.id
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val response = api.createCommunityPostComment(post.id, content)
                    parseCommunityComment(response.getJSONObject("comment")) to
                        parseCommunityPost(response.getJSONObject("post"))
                }
            }
            postingCommentPostId = null
            result
                .onSuccess { (comment, updatedPost) ->
                    postComments = postComments + (post.id to ((postComments[post.id] ?: emptyList()).map {
                        if (it.id == tempCommentId) comment else it
                    }))
                    posts = posts.map { if (it.id == updatedPost.id) updatedPost else it }
                    inlineCommentPostId = null
                    message = ""
                }
                .onFailure {
                    postComments = postComments + (
                        post.id to (postComments[post.id] ?: emptyList()).filterNot { item ->
                            item.id == tempCommentId
                        }
                    )
                    inlineCommentDraft = content
                    message = it.userFriendlyMessage("评论发布失败")
                }
        }
    }

    fun deletePost(post: CommunityPost) {
        if (deletingPostId != null) return
        scope.launch {
            deletingPostId = post.id
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    api.deleteCommunityPost(post.id)
                }
            }
            deletingPostId = null
            result
                .onSuccess {
                    posts = posts.filterNot { it.id == post.id }
                    postComments = postComments - post.id
                    if (inlineCommentPostId == post.id) {
                        inlineCommentPostId = null
                        inlineCommentDraft = ""
                    }
                    message = "动态已删除"
                }
                .onFailure { message = it.userFriendlyMessage("删除动态失败") }
        }
    }

    fun deleteComment(post: CommunityPost, comment: CommunityComment) {
        if (deletingCommentId != null) return
        scope.launch {
            deletingCommentId = comment.id
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val response = api.deleteCommunityPostComment(post.id, comment.id)
                    parseCommunityPost(response.getJSONObject("post"))
                }
            }
            deletingCommentId = null
            result
                .onSuccess { updatedPost ->
                    posts = posts.map { if (it.id == updatedPost.id) updatedPost else it }
                    postComments = postComments + (post.id to postComments[post.id].orEmpty().filterNot { item ->
                        item.id == comment.id
                    })
                    message = "评论已删除"
                }
                .onFailure { message = it.userFriendlyMessage("删除评论失败") }
        }
    }

    fun reportPost(post: CommunityPost) {
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    api.reportCommunityContent("post", post.id, "用户举报：内容可能不当")
                }
            }
            reportingPost = null
            result
                .onSuccess { message = "举报已提交，管理员会尽快处理" }
                .onFailure { message = it.userFriendlyMessage("举报提交失败") }
        }
    }

    fun openVolunteerInfo() {
        showVolunteer = true
        refreshVolunteerApplications()
        refreshVolunteerProjects()
    }

    fun resetVolunteerPublishDraft() {
        volunteerDraftTitle = ""
        volunteerDraftBody = ""
        volunteerDraftContact = ""
        volunteerDraftDeadline = ""
        volunteerCoverUri = null
        volunteerCoverAssetId = null
        volunteerCoverAssetUrl = null
        volunteerCoverReviewStatus = null
        volunteerCoverReviewReason = null
        volunteerPublishRequested = false
        volunteerPublishMessage = ""
        uploadRequestId = UUID.randomUUID().toString()
        volunteerCreateRequestId = UUID.randomUUID().toString()
    }

    fun publishVolunteerWithCover(imageUrl: String?) {
        if (volunteerPosting) return
        val title = volunteerDraftTitle.trim()
        val body = volunteerDraftBody.trim()
        val contact = volunteerDraftContact.trim()
        if (title.isBlank() || body.isBlank()) {
            volunteerPublishMessage = "请填写义工招募标题和内容"
            return
        }
        val normalizedDeadline = volunteerDraftDeadline.trim().takeIf { it.isNotBlank() }?.let { value ->
            if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) {
                volunteerPublishMessage = "截止日期请按 YYYY-MM-DD 填写"
                return
            }
            "${value}T23:59:59+08:00"
        }
        scope.launch {
            volunteerPosting = true
            volunteerPublishMessage = if (imageUrl == null) "正在发布招募" else "封面审核已通过，正在发布招募"
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityVolunteer(
                        api.createCommunityVolunteer(
                            title,
                            body,
                            contact,
                            imageUrl,
                            normalizedDeadline,
                            volunteerCreateRequestId = volunteerCreateRequestId
                        )
                    )
                }
            }
            volunteerPosting = false
            result
                .onSuccess { item ->
                    volunteers = listOf(item) + volunteers.filterNot { it.id == item.id }
                    volunteerIndex = 0
                    showVolunteerPublishDialog = false
                    resetVolunteerPublishDraft()
                    message = "义工招募已发布"
                    refreshVolunteerProjects()
                }
                .onFailure {
                    volunteerPublishMessage = it.userFriendlyMessage("义工招募发布失败")
                }
        }
    }

    fun refreshVolunteerCoverReview() {
        val assetId = volunteerCoverAssetId ?: return
        val assetUrl = volunteerCoverAssetUrl ?: return
        if (volunteerReviewChecking || volunteerPosting || volunteerCoverUploading) return
        scope.launch {
            volunteerReviewChecking = true
            volunteerPublishMessage = "正在检查封面审核状态"
            val result = runCatching {
                withContext(Dispatchers.IO) { api.assetReviewStatus(assetId) }
            }
            volunteerReviewChecking = false
            result
                .onSuccess { review ->
                    val status = review.optString("status", "pending")
                    volunteerCoverReviewStatus = status
                    volunteerCoverReviewReason = review.optNullableString("reason")
                    when (status) {
                        "approved" -> {
                            volunteerPublishMessage = "封面审核已通过"
                            if (volunteerPublishRequested) {
                                publishVolunteerWithCover(assetUrl)
                            }
                        }
                        "rejected", "quarantined" -> {
                            volunteerPublishRequested = false
                            volunteerPublishMessage = "封面未通过审核，请重新选择图片"
                        }
                        else -> volunteerPublishMessage = "封面审核中，通过后点击刷新将自动发布"
                    }
                }
                .onFailure {
                    volunteerPublishMessage = it.userFriendlyMessage("封面审核状态获取失败")
                }
        }
    }

    fun submitVolunteerPublish() {
        if (volunteerPosting || volunteerCoverUploading || volunteerReviewChecking) return
        if (volunteerDraftTitle.trim().isBlank() || volunteerDraftBody.trim().isBlank()) {
            volunteerPublishMessage = "请填写义工招募标题和内容"
            return
        }
        val deadline = volunteerDraftDeadline.trim()
        if (deadline.isNotBlank() && !Regex("\\d{4}-\\d{2}-\\d{2}").matches(deadline)) {
            volunteerPublishMessage = "截止日期请按 YYYY-MM-DD 填写"
            return
        }
        val coverUri = volunteerCoverUri
        if (coverUri.isNullOrBlank()) {
            volunteerPublishRequested = true
            publishVolunteerWithCover(null)
            return
        }
        val uploadedUrl = volunteerCoverAssetUrl
        if (!uploadedUrl.isNullOrBlank()) {
            volunteerPublishRequested = true
            when (volunteerCoverReviewStatus) {
                "approved" -> publishVolunteerWithCover(uploadedUrl)
                "rejected", "quarantined" -> {
                    volunteerPublishRequested = false
                    volunteerPublishMessage = "封面未通过审核，请重新选择图片"
                }
                else -> refreshVolunteerCoverReview()
            }
            return
        }
        scope.launch {
            volunteerCoverUploading = true
            volunteerPublishRequested = true
            volunteerPublishMessage = "正在上传封面"
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val upload = context.readUploadPayload(Uri.parse(coverUri))
                    api.uploadAsset(
                        "community/volunteer",
                        upload.fileName,
                        upload.mimeType,
                        upload.bytes,
                        uploadRequestId = uploadRequestId
                    )
                        .getJSONObject("asset")
                }
            }
            volunteerCoverUploading = false
            result
                .onSuccess { asset ->
                    volunteerCoverAssetId = asset.optString("id").takeIf { it.isNotBlank() }
                    volunteerCoverAssetUrl = asset.optString("url").takeIf { it.isNotBlank() }
                    val status = asset.optString("reviewStatus", "pending")
                    volunteerCoverReviewStatus = status
                    volunteerCoverReviewReason = asset.optNullableString("reviewReason")
                    when (status) {
                        "approved" -> volunteerCoverAssetUrl?.let(::publishVolunteerWithCover)
                        "rejected", "quarantined" -> {
                            volunteerPublishRequested = false
                            volunteerPublishMessage = "封面未通过审核，请重新选择图片"
                        }
                        else -> volunteerPublishMessage = "封面审核中，通过后点击刷新将自动发布"
                    }
                }
                .onFailure {
                    volunteerPublishRequested = false
                    volunteerPublishMessage = it.userFriendlyMessage("封面上传失败")
                }
        }
    }

    fun submitVolunteerApplication(volunteer: CommunityVolunteerPost, name: String, phone: String, note: String) {
        if (volunteerApplying) return
        if (!volunteer.isOpenForApplications()) {
            message = "该招募已截止，暂时不能报名"
            return
        }
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
                .onSuccess { application ->
                    volunteerApplications = listOf(application) + volunteerApplications.filterNot {
                        it.id == application.id
                    }
                    applyingVolunteer = null
                    selectedVolunteerDetail = volunteer
                    showVolunteer = true
                    message = "报名已提交，当前状态：审核中"
                }
                .onFailure { message = it.userFriendlyMessage("义工报名提交失败") }
        }
    }

    fun reviewVolunteerApplication(application: CommunityVolunteerApplication, status: String) {
        if (reviewingApplicationId != null) return
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

    fun cancelVolunteerApplication(application: CommunityVolunteerApplication) {
        if (cancellingApplicationId != null) return
        scope.launch {
            cancellingApplicationId = application.id
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityVolunteerApplication(
                        api.cancelCommunityVolunteerApplication(application.id)
                    )
                }
            }
            cancellingApplicationId = null
            result
                .onSuccess { updated ->
                    volunteerApplications = volunteerApplications.map {
                        if (it.id == updated.id) updated else it
                    }
                    message = "报名已取消"
                }
                .onFailure { message = it.userFriendlyMessage("取消报名失败") }
        }
    }

    fun updateVolunteerStatus(volunteer: CommunityVolunteerPost) {
        if (!isAdmin || updatingVolunteerId != null) return
        val targetStatus = if (volunteer.isOpenForApplications()) "closed" else "open"
        scope.launch {
            updatingVolunteerId = volunteer.id
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    parseCommunityVolunteer(
                        api.updateCommunityVolunteerStatus(volunteer.id, targetStatus)
                    )
                }
            }
            updatingVolunteerId = null
            result
                .onSuccess { updated ->
                    volunteers = volunteers.map { if (it.id == updated.id) updated else it }
                    selectedVolunteerDetail = updated
                    message = if (updated.isOpenForApplications()) "招募已重新开放" else "招募已截止"
                }
                .onFailure { message = it.userFriendlyMessage("更新招募状态失败") }
        }
    }

    LaunchedEffect(showVolunteerPublishDialog, volunteerCoverAssetId) {
        if (
            showVolunteerPublishDialog &&
            volunteerPublishRequested &&
            volunteerCoverAssetId != null &&
            volunteerCoverReviewStatus == "pending"
        ) {
            refreshVolunteerCoverReview()
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

    val volunteerOverlayOpen = showVolunteer || applyingVolunteer != null
    Box(modifier = Modifier.fillMaxSize().background(HallWarmBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (volunteerOverlayOpen) Modifier.blur(14.dp) else Modifier)
        ) {
            CommunityFeedHeader(
                user = user,
                loading = loading,
                onRefresh = ::refreshCommunity,
                onPublish = { showPublishDialog = true }
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    CommunityMomentsCover(
                        user = user,
                        postCount = posts.size,
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
                    item {
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            StatusMessage(message = message)
                        }
                    }
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
                        comments = postComments[post.id].orEmpty(),
                        commentsLoading = post.id in commentLoadingPostIds,
                        commentDraft = if (inlineCommentPostId == post.id) inlineCommentDraft else "",
                        commentInputVisible = inlineCommentPostId == post.id,
                        commentPosting = postingCommentPostId == post.id,
                        canDeletePost = post.authorId == user.id || isAdmin,
                        canReportPost = post.authorId != user.id,
                        deletingPost = deletingPostId == post.id,
                        canDeleteComment = { comment ->
                            isAdmin || post.authorId == user.id || comment.authorId == user.id
                        },
                        deletingCommentId = deletingCommentId,
                        onLike = { likePost(post) },
                        onComment = { openInlineComment(post) },
                        onReportPost = { reportingPost = post },
                        onDeletePost = { deletePost(post) },
                        onDeleteComment = { comment -> deleteComment(post, comment) },
                        onCommentDraftChange = { inlineCommentDraft = it.take(300) },
                        onSubmitComment = { publishInlineComment(post) }
                    )
                }
            }
        }

        if (showVolunteer) {
            CommunityVolunteerDialog(
                volunteers = volunteers,
                selectedVolunteer = selectedVolunteerDetail,
                isAdmin = isAdmin,
                currentUserId = user.id,
                applications = volunteerApplications,
                projectsLoading = loading || volunteerProjectsLoading,
                applicationsLoading = volunteerApplicationsLoading,
                statusMessage = message,
                reviewingApplicationId = reviewingApplicationId,
                cancellingApplicationId = cancellingApplicationId,
                updatingVolunteerId = updatingVolunteerId,
                onSelectVolunteer = { selectedVolunteerDetail = it },
                onBackToList = { selectedVolunteerDetail = null },
                onApply = {
                    applyingVolunteer = it
                    showVolunteer = false
                    selectedVolunteerDetail = null
                },
                onOpenPublish = {
                    showVolunteerPublishDialog = true
                    volunteerPublishMessage = when (volunteerCoverReviewStatus) {
                        "pending" -> "封面审核中，通过后点击刷新将自动发布"
                        "approved" -> "封面审核已通过，可以发布"
                        "rejected", "quarantined" -> "封面未通过审核，请重新选择图片"
                        else -> ""
                    }
                },
                onReview = ::reviewVolunteerApplication,
                onCancelApplication = ::cancelVolunteerApplication,
                onUpdateVolunteerStatus = ::updateVolunteerStatus,
                onRefreshProjects = ::refreshVolunteerProjects,
                onRefreshApplications = ::refreshVolunteerApplications,
                onDismiss = {
                    showVolunteer = false
                    selectedVolunteerDetail = null
                }
            )
        }
    }

    if (showVolunteerPublishDialog) {
        CommunityVolunteerPublishDialog(
            title = volunteerDraftTitle,
            body = volunteerDraftBody,
            contact = volunteerDraftContact,
            deadline = volunteerDraftDeadline,
            coverUri = volunteerCoverUri,
            reviewStatus = volunteerCoverReviewStatus,
            reviewReason = volunteerCoverReviewReason,
            message = volunteerPublishMessage,
            uploading = volunteerCoverUploading,
            checkingReview = volunteerReviewChecking,
            publishing = volunteerPosting,
            onTitleChange = {
                volunteerDraftTitle = it.take(40)
                volunteerPublishMessage = ""
            },
            onBodyChange = {
                volunteerDraftBody = it.take(500)
                volunteerPublishMessage = ""
            },
            onContactChange = {
                volunteerDraftContact = it.take(160)
                volunteerPublishMessage = ""
            },
            onDeadlineChange = {
                volunteerDraftDeadline = it.take(10)
                volunteerPublishMessage = ""
            },
            onPickCover = { volunteerCoverPicker.launch(arrayOf("image/*")) },
            onClearCover = {
                volunteerCoverUri = null
                volunteerCoverAssetId = null
                volunteerCoverAssetUrl = null
                volunteerCoverReviewStatus = null
                volunteerCoverReviewReason = null
                volunteerPublishRequested = false
                volunteerPublishMessage = ""
                uploadRequestId = UUID.randomUUID().toString()
            },
            onSubmit = ::submitVolunteerPublish,
            onRefreshReview = ::refreshVolunteerCoverReview,
            onDismiss = { showVolunteerPublishDialog = false }
        )
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

    reportingPost?.let { post ->
        AlertDialog(
            onDismissRequest = { reportingPost = null },
            containerColor = Paper,
            shape = RoundedCornerShape(8.dp),
            icon = { Icon(Icons.Rounded.Flag, contentDescription = null, tint = Rose) },
            title = { Text("举报这条动态") },
            text = { Text("确认后将提交给管理员审核。") },
            confirmButton = {
                Button(
                    onClick = { reportPost(post) },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose)
                ) {
                    Text("提交举报")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportingPost = null }) {
                    Text("取消")
                }
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
}

@Composable
private fun CommunityFeedHeader(
    user: AppUser,
    loading: Boolean,
    onRefresh: () -> Unit,
    onPublish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Paper,
        border = BorderStroke(0.dp, Color.Transparent),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRefresh, enabled = !loading) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Green,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = "刷新", tint = Green)
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("人文社区", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onPublish) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = "发布动态", tint = Green)
                }
            }
        }
    }
}

@Composable
private fun CommunityMomentsCover(
    user: AppUser,
    postCount: Int,
    onPublish: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = Paper,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.5f)),
        shadowElevation = 0.5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
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
                                Color.Transparent,
                                Night.copy(alpha = 0.2f),
                                Night.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommunityAvatar(
                    name = user.displayName.ifBlank { user.username },
                    avatarUrl = user.avatarUrl,
                    size = 54.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user.displayName.ifBlank { user.username },
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "共记录了 $postCount 条动态",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onPublish,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = Ink
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Green
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("发布动态", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Leaf.copy(alpha = 0.8f),
                        Blush.copy(alpha = 0.9f)
                    )
                )
            )
            .border(BorderStroke(1.dp, Line.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = Rose.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Rose,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "暖心义工 · 互助招募",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = Green.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "招募中",
                            color = Green,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    item.title,
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CommunityInlineAction(
    icon: ImageVector,
    text: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val resolvedTint = if (enabled) tint else tint.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = resolvedTint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = resolvedTint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CommunityInteractionSummary(
    post: CommunityPost,
    comments: List<CommunityComment>,
    commentsLoading: Boolean,
    canDeleteComment: (CommunityComment) -> Boolean,
    deletingCommentId: String?,
    onDeleteComment: (CommunityComment) -> Unit,
    onComment: () -> Unit
) {
    if (post.likeCount <= 0 && comments.isEmpty() && !commentsLoading && post.commentCount <= 0) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HallWarmSurface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Line.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (post.likeCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Rose, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${post.likeCount} 人觉得有帮助",
                    color = Rose,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (comments.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Line.copy(alpha = 0.4f))
                )
            }
        }
        when {
            commentsLoading -> {
                Text("评论加载中...", color = Muted, fontSize = 12.sp)
            }
            comments.isNotEmpty() -> {
                comments.forEach { comment ->
                    val deleting = deletingCommentId == comment.id
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "${comment.authorName}：",
                            color = Green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            comment.content,
                            color = Ink,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (comment.moderationStatus != "approved") {
                            Text(
                                if (comment.moderationStatus == "pending") "审核中" else "已隐藏",
                                color = if (comment.moderationStatus == "pending") Amber else Rose,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (canDeleteComment(comment)) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (deleting) "删除中" else "删除",
                                color = if (deleting) Muted else Rose.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(enabled = !deleting) { onDeleteComment(comment) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            post.commentCount > 0 -> {
                Text(
                    "查看全部 ${post.commentCount} 条评论",
                    color = Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onComment)
                )
            }
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
    Dialog(onDismissRequest = { if (!posting) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            color = Paper,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Line.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(color = Leaf, shape = RoundedCornerShape(10.dp)) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("分享瞬间", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("发布动态或照片到社区", color = Muted, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !posting,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Muted)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = HallWarmSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Line.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommunityAvatar(
                            name = user.displayName.ifBlank { user.username },
                            avatarUrl = user.avatarUrl,
                            size = 36.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                user.displayName.ifBlank { user.username },
                                color = Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text("发布到人文社区", color = Muted, fontSize = 11.sp)
                        }
                        Surface(
                            color = Leaf,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "公开",
                                color = Green,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("记录近况、故事，或是寻求帮助...", fontSize = 14.sp) },
                    colors = warmTextFieldColors()
                )

                if (imageUris.isNotEmpty()) {
                    CommunityImageGrid(
                        imageUrls = imageUris,
                        removable = true,
                        onRemove = onRemoveImage,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    )
                }

                OutlinedButton(
                    onClick = onAddImages,
                    enabled = imageUris.size < 9 && !posting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Line),
                    colors = quietOutlinedButtonColors()
                ) {
                    Icon(
                        Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Green
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (imageUris.isEmpty()) "添加照片" else "继续添加 (${imageUris.size}/9)",
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${draft.length}/500", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss, enabled = !posting) {
                        Text("取消", color = Muted, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onPublish,
                        enabled = !posting && (draft.isNotBlank() || imageUris.isNotEmpty()),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green,
                            contentColor = Color.White,
                            disabledContainerColor = Line,
                            disabledContentColor = Muted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        if (posting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("发布", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityVolunteerDialog(
    volunteers: List<CommunityVolunteerPost>,
    selectedVolunteer: CommunityVolunteerPost?,
    isAdmin: Boolean,
    currentUserId: String,
    applications: List<CommunityVolunteerApplication>,
    projectsLoading: Boolean,
    applicationsLoading: Boolean,
    statusMessage: String,
    reviewingApplicationId: String?,
    cancellingApplicationId: String?,
    updatingVolunteerId: String?,
    onSelectVolunteer: (CommunityVolunteerPost) -> Unit,
    onBackToList: () -> Unit,
    onApply: (CommunityVolunteerPost) -> Unit,
    onOpenPublish: () -> Unit,
    onReview: (CommunityVolunteerApplication, String) -> Unit,
    onCancelApplication: (CommunityVolunteerApplication) -> Unit,
    onUpdateVolunteerStatus: (CommunityVolunteerPost) -> Unit,
    onRefreshProjects: () -> Unit,
    onRefreshApplications: () -> Unit,
    onDismiss: () -> Unit
) {
    var section by rememberSaveable { mutableStateOf("projects") }
    val myApplications = applications.filter { it.applicantId == currentUserId }
    val sortedApplications = applications.sortedWith(
        compareBy<CommunityVolunteerApplication> {
            when (it.status) {
                "pending" -> 0
                "approved" -> 1
                "rejected" -> 2
                else -> 3
            }
        }.thenByDescending { it.updatedAt }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            color = HallWarmBackground,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Line.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (selectedVolunteer != null) {
                        TextButton(
                            onClick = onBackToList,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("← 返回", color = Green, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "社区义工招募",
                            color = Ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (selectedVolunteer == null) "参与社区建设与互助服务" else "义工详情",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Ink)
                    }
                }

                if (selectedVolunteer == null) {
                    VolunteerSectionTabs(
                        selected = section,
                        isAdmin = isAdmin,
                        pendingCount = applications.count { it.status == "pending" },
                        onSelect = { section = it }
                    )

                    if (statusMessage.isNotBlank()) {
                        StatusMessage(message = statusMessage)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        when (section) {
                            "mine" -> {
                                if (applicationsLoading) {
                                    item { VolunteerLoadingState("正在加载我的报名") }
                                } else if (myApplications.isEmpty()) {
                                    item { VolunteerEmptyState("还没有报名记录", "可在“招募项目”中查看开放中的服务机会") }
                                } else {
                                    items(myApplications, key = { it.id }) { application ->
                                        CommunityVolunteerMyApplicationCard(
                                            application = application,
                                            cancelling = cancellingApplicationId == application.id,
                                            onOpenProject = {
                                                volunteers.firstOrNull { it.id == application.volunteerPostId }
                                                    ?.let(onSelectVolunteer)
                                            },
                                            onCancel = { onCancelApplication(application) }
                                        )
                                    }
                                }
                            }
                            "review" -> {
                                item {
                                    VolunteerReviewHeader(
                                        loading = applicationsLoading,
                                        onRefresh = onRefreshApplications
                                    )
                                }
                                if (applicationsLoading && sortedApplications.isEmpty()) {
                                    item { VolunteerLoadingState("正在加载报名审核") }
                                } else if (sortedApplications.isEmpty()) {
                                    item { VolunteerEmptyState("暂无报名申请", "收到新报名后会显示在这里") }
                                } else {
                                    items(sortedApplications, key = { it.id }) { application ->
                                        CommunityVolunteerApplicationReviewCard(
                                            application = application,
                                            busy = reviewingApplicationId == application.id,
                                            onApprove = { onReview(application, "approved") },
                                            onReject = { onReview(application, "rejected") }
                                        )
                                    }
                                }
                            }
                            else -> {
                                item {
                                    VolunteerProjectsHeader(
                                        isAdmin = isAdmin,
                                        loading = projectsLoading,
                                        onRefresh = onRefreshProjects,
                                        onPublish = onOpenPublish
                                    )
                                }
                                if (projectsLoading && volunteers.isEmpty()) {
                                    item { VolunteerLoadingState("正在加载招募项目") }
                                } else if (volunteers.isEmpty()) {
                                    item { VolunteerEmptyState("暂无招募项目", "新的社区服务机会发布后会显示在这里") }
                                } else {
                                    items(volunteers, key = { it.id }) { item ->
                                        val application = myApplications.firstOrNull {
                                            it.volunteerPostId == item.id
                                        }
                                        CommunityVolunteerImageCard(
                                            item = item,
                                            actionLabel = volunteerProjectActionLabel(item, application, isAdmin),
                                            onClick = { onSelectVolunteer(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val myApplication = myApplications.firstOrNull {
                        it.volunteerPostId == selectedVolunteer.id
                    }
                    CommunityVolunteerDetailPanel(
                        item = selectedVolunteer,
                        isAdmin = isAdmin,
                        application = myApplication,
                        updatingStatus = updatingVolunteerId == selectedVolunteer.id,
                        cancelling = myApplication?.id == cancellingApplicationId,
                        onApply = { onApply(selectedVolunteer) },
                        onCancelApplication = {
                            myApplication?.let(onCancelApplication)
                        },
                        onUpdateStatus = { onUpdateVolunteerStatus(selectedVolunteer) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VolunteerSectionTabs(
    selected: String,
    isAdmin: Boolean,
    pendingCount: Int,
    onSelect: (String) -> Unit
) {
    val sections = buildList {
        add("projects" to "招募项目")
        add("mine" to "我的报名")
        if (isAdmin) add("review" to if (pendingCount > 0) "报名审核 $pendingCount" else "报名审核")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Paper, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        sections.forEach { (key, label) ->
            val active = selected == key
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clickable { onSelect(key) },
                color = if (active) Green else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        color = if (active) Color.White else Muted,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun VolunteerProjectsHeader(
    isAdmin: Boolean,
    loading: Boolean,
    onRefresh: () -> Unit,
    onPublish: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("招募项目", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("查看开放项目与报名截止时间", color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.size(40.dp)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Green, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新招募项目", tint = Green)
            }
        }
        if (isAdmin) {
            Spacer(Modifier.width(6.dp))
            OutlinedButton(
                onClick = onPublish,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Green.copy(alpha = 0.45f)),
                colors = quietOutlinedButtonColors(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Green, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("发布招募", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VolunteerLoadingState(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Green, strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(label, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun VolunteerEmptyState(title: String, detail: String) {
    Surface(
        color = Paper,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(detail, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun VolunteerReviewHeader(loading: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("管理员审核", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("请核对联系方式与参与说明后再作决定", color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.size(36.dp)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Green, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新报名审核", tint = Green)
            }
        }
    }
}

private fun volunteerProjectActionLabel(
    item: CommunityVolunteerPost,
    application: CommunityVolunteerApplication?,
    isAdmin: Boolean
): String {
    if (isAdmin) return if (item.isOpenForApplications()) "管理" else "已截止"
    return when {
        !item.isOpenForApplications() -> "已截止"
        application?.status == "pending" -> "已报名 · 审核中"
        application?.status == "approved" -> "已通过"
        application?.status == "rejected" -> "重新报名"
        application?.status == "cancelled" -> "重新报名"
        else -> "查看详情"
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
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
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
                            Night.copy(alpha = 0.2f),
                            Night.copy(alpha = 0.65f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(end = 132.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                item.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    volunteerDeadlineText(item),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            color = if (item.isOpenForApplications()) Green else Muted,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                if (item.isOpenForApplications()) "开放报名" else "已截止",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            color = Paper,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp
        ) {
            Text(
                actionLabel,
                color = Green,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CommunityVolunteerDetailPanel(
    item: CommunityVolunteerPost,
    isAdmin: Boolean,
    application: CommunityVolunteerApplication?,
    updatingStatus: Boolean,
    cancelling: Boolean,
    onApply: () -> Unit,
    onCancelApplication: () -> Unit,
    onUpdateStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            CommunityVolunteerCoverImage(imageUrl = item.imageUrl, title = item.title)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Night.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            Text(
                item.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }

        Surface(
            color = Paper,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Line.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "项目介绍",
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item.body,
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (item.isOpenForApplications()) Green.copy(alpha = 0.12f) else Line,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            if (item.isOpenForApplications()) "开放报名" else "已截止",
                            color = if (item.isOpenForApplications()) Green else Muted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(volunteerDeadlineText(item), color = Muted, fontSize = 12.sp)
                }

                if (item.contact.isNotBlank()) {
                    Surface(
                        color = Leaf.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Line.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = Green,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "联系方式: ${item.contact}",
                                color = Green,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (isAdmin) {
                    OutlinedButton(
                        onClick = onUpdateStatus,
                        enabled = !updatingStatus,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (item.isOpenForApplications()) Rose else Green),
                        colors = quietOutlinedButtonColors(),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        if (updatingStatus) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Green, strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (item.isOpenForApplications()) "截止招募" else "重新开放招募",
                                color = if (item.isOpenForApplications()) Rose else Green,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    application?.let {
                        VolunteerApplicationStatusSummary(application = it)
                    }
                    val canApply = item.isOpenForApplications() &&
                        (application == null || application.status == "rejected" || application.status == "cancelled")
                    if (canApply) {
                        Button(
                            onClick = onApply,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(
                                if (application == null) "填写报名信息" else "重新报名",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else if (!item.isOpenForApplications() && application == null) {
                        Surface(color = Line.copy(alpha = 0.55f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "该项目已截止报名",
                                color = Muted,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                        }
                    }
                    if (application?.status == "pending" || application?.status == "approved") {
                        TextButton(
                            onClick = onCancelApplication,
                            enabled = !cancelling,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (cancelling) "正在取消..." else "取消报名",
                                color = Rose,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VolunteerApplicationStatusSummary(application: CommunityVolunteerApplication) {
    val color = volunteerApplicationStatusColor(application.status)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "报名状态：${volunteerApplicationStatusText(application.status)}",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                volunteerApplicationStatusDetail(application.status),
                color = Ink,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun CommunityVolunteerMyApplicationCard(
    application: CommunityVolunteerApplication,
    cancelling: Boolean,
    onOpenProject: () -> Unit,
    onCancel: () -> Unit
) {
    val color = volunteerApplicationStatusColor(application.status)
    Surface(
        color = Paper,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    application.volunteerTitle,
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        volunteerApplicationStatusText(application.status),
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                volunteerApplicationStatusDetail(application.status),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Text("报名人：${application.name} · ${application.phone}", color = Ink, fontSize = 12.sp)
            Text("提交时间：${formatTime(application.createdAt)}", color = Muted, fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenProject) {
                    Text("查看项目", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                if (application.status == "pending" || application.status == "approved") {
                    TextButton(onClick = onCancel, enabled = !cancelling) {
                        Text(
                            if (cancelling) "正在取消..." else "取消报名",
                            color = Rose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityVolunteerPublishDialog(
    title: String,
    body: String,
    contact: String,
    deadline: String,
    coverUri: String?,
    reviewStatus: String?,
    reviewReason: String?,
    message: String,
    uploading: Boolean,
    checkingReview: Boolean,
    publishing: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onDeadlineChange: (String) -> Unit,
    onPickCover: () -> Unit,
    onClearCover: () -> Unit,
    onSubmit: () -> Unit,
    onRefreshReview: () -> Unit,
    onDismiss: () -> Unit
) {
    val busy = uploading || checkingReview || publishing
    val reviewPending = reviewStatus == "pending"
    val reviewFailed = reviewStatus == "rejected" || reviewStatus == "quarantined"
    val formLocked = busy || reviewPending || reviewStatus == "approved"
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            color = Paper,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, Line.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("发布新招募", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text("填写项目资料并提交封面审核", color = Muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss, enabled = !busy) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭发布招募", tint = Ink)
                    }
                }

                if (coverUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        CommunityVolunteerCoverImage(imageUrl = coverUri, title = title)
                        IconButton(
                            onClick = onClearCover,
                            enabled = !busy,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(Night.copy(alpha = 0.62f), CircleShape)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "移除招募封面",
                                tint = Color.White
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onPickCover,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Line),
                    colors = quietOutlinedButtonColors()
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Green)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (coverUri == null) "选择卡片封面" else "更换卡片封面",
                        color = Ink,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (reviewStatus != null) {
                    val color = when {
                        reviewFailed -> Rose
                        reviewStatus == "approved" -> Green
                        else -> Amber
                    }
                    val label = when {
                        reviewFailed -> "封面未通过审核"
                        reviewStatus == "approved" -> "封面审核已通过"
                        else -> "封面审核中"
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = color.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    reviewFailed && !reviewReason.isNullOrBlank() -> "请更换封面后重新提交"
                                    reviewStatus == "approved" -> "正在使用同一张已审核封面发布"
                                    else -> "后台通过后点击刷新，系统会自动完成发布"
                                },
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    enabled = !formLocked,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("招募项目标题") },
                    colors = warmTextFieldColors()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    enabled = !formLocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("招募详情") },
                    colors = warmTextFieldColors()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = onContactChange,
                    enabled = !formLocked,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("联系方式") },
                    colors = warmTextFieldColors()
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = onDeadlineChange,
                    enabled = !formLocked,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("截止日期（可选）") },
                    supportingText = { Text("格式：YYYY-MM-DD") },
                    colors = warmTextFieldColors()
                )

                if (message.isNotBlank()) {
                    StatusMessage(
                        message = message,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                when {
                    reviewPending -> OutlinedButton(
                        onClick = onRefreshReview,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = quietOutlinedButtonColors()
                    ) {
                        if (checkingReview) {
                            CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Green, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Green)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (checkingReview) "正在检查" else "刷新审核状态", color = Green, fontWeight = FontWeight.Bold)
                    }
                    reviewFailed -> Button(
                        onClick = onPickCover,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = primaryButtonColors()
                    ) {
                        Text("重新选择封面", fontWeight = FontWeight.Bold)
                    }
                    else -> Button(
                        onClick = onSubmit,
                        enabled = !busy && title.isNotBlank() && body.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = primaryButtonColors()
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            when {
                                uploading -> "正在上传封面"
                                checkingReview -> "正在检查审核"
                                publishing -> "正在发布"
                                else -> "确认发布招募"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityVolunteerCoverImage(imageUrl: String?, title: String) {
    val bitmap by rememberUriImage(imageUrl, maxDimensionPx = 1024)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.LocalFlorist,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    "等待上传义工照片",
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
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

private fun CommunityVolunteerPost.isOpenForApplications(now: Long = System.currentTimeMillis()): Boolean {
    return status == "open" && (deadlineAt == null || deadlineAt > now)
}

private fun volunteerDeadlineText(item: CommunityVolunteerPost): String {
    val deadline = item.deadlineAt
    if (deadline == null || deadline <= 0L) {
        return if (item.isOpenForApplications()) "长期招募" else "招募已截止"
    }
    val formatted = SimpleDateFormat("MM月dd日", Locale.CHINA).format(Date(deadline))
    return if (item.isOpenForApplications()) "截止 $formatted" else "已于 $formatted 截止"
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
    Dialog(onDismissRequest = { if (!applying) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            color = Paper,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Line.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(color = Leaf, shape = RoundedCornerShape(10.dp)) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("义工报名", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("请填写联系信息以便领队与您确认", color = Muted, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !applying,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Muted)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = HallWarmSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Line.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("报名项目", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(volunteer.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("您的姓名") },
                    colors = warmTextFieldColors()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("联系电话") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = warmTextFieldColors()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("备注说明（例如可参与时间、相关特长等）") },
                    colors = warmTextFieldColors()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss, enabled = !applying) {
                        Text("取消", color = Muted, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { onSubmit(name, phone, note) },
                        enabled = !applying && name.isNotBlank() && phone.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green,
                            contentColor = Color.White,
                            disabledContainerColor = Line,
                            disabledContentColor = Muted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        if (applying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("确认报名", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
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
        color = Paper,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CommunityAvatar(
                    name = application.applicantName.ifBlank { application.name },
                    avatarUrl = application.applicantAvatarUrl,
                    size = 36.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        application.name,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        application.volunteerTitle,
                        color = Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        volunteerApplicationStatusText(application.status),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Line.copy(alpha = 0.3f))
            )

            Text(
                "联系方式: ${application.phone}",
                color = Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            if (application.note.isNotBlank()) {
                Text(
                    application.note,
                    color = Ink,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            if (application.createdAt > 0L) {
                Text(
                    "申请时间: " + formatTime(application.createdAt),
                    color = Muted,
                    fontSize = 11.sp
                )
            }

            if (application.status == "pending") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Line),
                        colors = quietOutlinedButtonColors()
                    ) {
                        Text("拒绝", fontSize = 13.sp)
                    }
                    Button(
                        onClick = onApprove,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green,
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (busy) "处理中" else "通过", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        "cancelled" -> "已取消"
        else -> "审核中"
    }
}

private fun volunteerApplicationStatusDetail(status: String): String {
    return when (status) {
        "approved" -> "审核已通过，请留意管理员后续联系。"
        "rejected" -> "本次报名未通过；招募开放时可补充信息后重新报名。"
        "cancelled" -> "你已取消本次报名；招募开放时仍可重新报名。"
        else -> "已报名，管理员正在审核，请勿重复提交。"
    }
}

private fun volunteerApplicationStatusColor(status: String): Color {
    return when (status) {
        "approved" -> Green
        "rejected" -> Color(0xFFB42318)
        "cancelled" -> Muted
        else -> Rose
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    comments: List<CommunityComment>,
    commentsLoading: Boolean,
    commentDraft: String,
    commentInputVisible: Boolean,
    commentPosting: Boolean,
    canDeletePost: Boolean,
    canReportPost: Boolean,
    deletingPost: Boolean,
    canDeleteComment: (CommunityComment) -> Boolean,
    deletingCommentId: String?,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onReportPost: () -> Unit,
    onDeletePost: () -> Unit,
    onDeleteComment: (CommunityComment) -> Unit,
    onCommentDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = Paper,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.5f)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommunityAvatar(name = post.authorName, avatarUrl = post.authorAvatarUrl, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.authorName,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatTime(post.createdAt),
                        color = Muted,
                        fontSize = 11.sp
                    )
                    if (post.moderationStatus != "approved") {
                        Text(
                            if (post.moderationStatus == "pending") "审核中" else "已隐藏",
                            color = if (post.moderationStatus == "pending") Amber else Rose,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (canReportPost) {
                    IconButton(
                        onClick = onReportPost,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = "举报",
                            tint = Muted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                if (canDeletePost) {
                    IconButton(
                        onClick = onDeletePost,
                        enabled = !deletingPost,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "删除",
                            tint = Rose.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (post.content.isNotBlank()) {
                Text(
                    post.content,
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }

            if (post.imageUrls.isNotEmpty()) {
                CommunityImageGrid(
                    imageUrls = post.imageUrls,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                CommunityInlineAction(
                    icon = if (post.likedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    text = if (post.likeCount > 0) "${post.likeCount}" else "觉得有帮助",
                    tint = if (post.likedByMe) Rose else Green,
                    onClick = onLike
                )
                Spacer(Modifier.width(16.dp))
                CommunityInlineAction(
                    icon = Icons.Rounded.ChatBubble,
                    text = if (post.commentCount > 0) "${post.commentCount}" else "评论",
                    tint = Green,
                    onClick = onComment
                )
            }

            CommunityInteractionSummary(
                post = post,
                comments = comments,
                commentsLoading = commentsLoading,
                canDeleteComment = canDeleteComment,
                deletingCommentId = deletingCommentId,
                onDeleteComment = onDeleteComment,
                onComment = onComment
            )

            if (commentInputVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HallWarmSurface, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Line.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = onCommentDraftChange,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("写评论...", fontSize = 13.sp) },
                        colors = warmTextFieldColors()
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onSubmitComment,
                        enabled = !commentPosting && commentDraft.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green,
                            contentColor = Color.White,
                            disabledContainerColor = Line,
                            disabledContentColor = Muted
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (commentPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("发送", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
    var previewImageUrl by rememberSaveable { mutableStateOf<String?>(null) }
    if (imageUrls.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (imageUrls.size == 1) {
            val url = imageUrls.first()
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1.33f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7F5F0))
            ) {
                CommunityImageTileContent(
                    imageUrl = url,
                    removable = removable,
                    onRemove = { onRemove(url) },
                    onPreview = { previewImageUrl = url }
                )
            }
        } else {
            val rows = imageUrls.chunked(3)
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { url ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF7F5F0))
                        ) {
                            CommunityImageTileContent(
                                imageUrl = url,
                                removable = removable,
                                onRemove = { onRemove(url) },
                                onPreview = { previewImageUrl = url }
                            )
                        }
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    previewImageUrl?.let { imageUrl ->
        ImagePreviewDialog(
            imageUrl = imageUrl,
            title = "社区图片预览",
            onDismiss = { previewImageUrl = null }
        )
    }
}

@Composable
private fun CommunityImageTileContent(
    imageUrl: String,
    removable: Boolean,
    onRemove: () -> Unit,
    onPreview: () -> Unit
) {
    val bitmap by rememberUriImage(imageUrl, maxDimensionPx = 768)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = bitmap != null,
                role = Role.Button,
                onClick = onPreview
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            ComposeImage(
                bitmap = bitmap!!,
                contentDescription = "查看图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Rounded.Image, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
        }
        if (removable) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                color = Night.copy(alpha = 0.6f),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewDialog(
    imageUrl: String,
    title: String,
    onDismiss: () -> Unit
) {
    val bitmap by rememberUriImage(imageUrl, maxDimensionPx = 2048)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            if (bitmap != null) {
                ComposeImage(
                    bitmap = bitmap!!,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 54.dp)
                )
            } else {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )
            }
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 18.dp, top = 12.dp)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 8.dp, top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "关闭图片预览",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
internal fun AiCompanionAvatar(name: String, avatarUrl: String?, size: Dp) {
    CommunityAvatar(name = name, avatarUrl = avatarUrl, size = size)
}

@Composable
private fun CommunityAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp
) {
    val initial = name.trim().firstOrNull()?.toString() ?: "人"
    val avatarShape = CircleShape
    Box(
        modifier = modifier
            .size(size)
            .clip(avatarShape)
            .background(Brush.linearGradient(listOf(Color.White, Leaf, Blush)))
            .border(1.dp, Line.copy(alpha = 0.5f), avatarShape),
        contentAlignment = Alignment.Center
    ) {
        val avatarState = rememberUriImage(avatarUrl, maxDimensionPx = 256)
        val avatar = avatarState.value
        if (avatar != null) {
            ComposeImage(
                bitmap = avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(initial, color = Green, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4).sp)
        }
    }
}

@Composable
private fun EmptyCommunityFeed() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        color = Paper,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = Leaf,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Article,
                        contentDescription = null,
                        tint = Green,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text("还没有社区动态", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "发第一条动态，开启温暖的人文社区交流吧。",
                color = Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
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
        shape = AppCardShape,
        border = BorderStroke(1.dp, Line.copy(alpha = 0.6f)),
        shadowElevation = 3.dp
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
internal fun rememberUriImage(uriString: String?, maxDimensionPx: Int = 1024) = LocalContext.current.let { context ->
    produceState<ImageBitmap?>(initialValue = null, uriString, maxDimensionPx) {
        value = null
        if (uriString.isNullOrBlank()) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val normalized = absoluteAssetUrl(uriString)
                val assetPath = isAuthenticatedAssetResource(normalized)
                val requestUrl = if (assetPath) normalized.imageVariantUrl(maxDimensionPx) else normalized
                val prefs = context.appPrefs()
                val userId = prefs.getString(KEY_USER_ID, null).orEmpty()
                val cacheIdentity = if (assetPath) "$userId|$requestUrl" else requestUrl
                val memoryKey = "$cacheIdentity|decode=$maxDimensionPx"
                uriImageMemoryCache.get(memoryKey)?.let { return@runCatching it.asImageBitmap() }

                val bitmap = when {
                    requestUrl.startsWith("http://") || requestUrl.startsWith("https://") -> {
                        if (!BuildConfig.DEBUG && requestUrl.startsWith("http://")) {
                            null
                        } else {
                            val token = SessionTokenVault.read(context)
                                ?.takeIf { assetPath && it.isNotBlank() }
                            val cached = context.loadCachedCloudResource(
                                url = requestUrl,
                                authorizationToken = token,
                                cacheIdentity = cacheIdentity
                            )
                            if (cached != null) {
                                decodeSampledBitmap(cached, maxDimensionPx)
                            } else {
                                val connection = (URL(requestUrl).openConnection() as? HttpURLConnection)
                                    ?: return@runCatching null
                                try {
                                    connection.connectTimeout = 12_000
                                    connection.readTimeout = 20_000
                                    connection.requestMethod = "GET"
                                    token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
                                    connection.connect()
                                    if (connection.responseCode !in 200..299) {
                                        null
                                    } else {
                                        connection.inputStream.use { input -> BitmapFactory.decodeStream(input) }
                                    }
                                } finally {
                                    connection.disconnect()
                                }
                            }
                        }
                    }
                    else -> context.decodeSampledBitmap(Uri.parse(normalized), maxDimensionPx)
                }

                bitmap?.also { uriImageMemoryCache.put(memoryKey, it) }?.asImageBitmap()
            }.getOrNull()
        }
    }
}

private fun String.imageVariantUrl(maxDimensionPx: Int): String {
    return Uri.parse(this).buildUpon()
        .appendQueryParameter("max", maxDimensionPx.toString())
        .appendQueryParameter("format", "webp")
        .build()
        .toString()
}

private fun Context.decodeSampledBitmap(uri: Uri, maxDimensionPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = bitmapSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

private fun decodeSampledBitmap(file: File, maxDimensionPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = bitmapSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeFile(file.absolutePath, options)
}

private fun bitmapSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
    val target = maxDimensionPx.coerceAtLeast(128)
    var sampleSize = 1
    while (maxOf(width, height) / (sampleSize * 2) >= target) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun Context.loadCachedCloudResource(
    url: String,
    authorizationToken: String? = null,
    cacheIdentity: String = url
): File? {
    if (!isCacheableCloudResource(url) && !isAuthenticatedAssetResource(url)) return null
    val cacheDir = File(this.cacheDir, CLOUD_CACHE_DIR).apply { mkdirs() }
    val cacheFile = File(cacheDir, cloudCacheFileName(url, cacheIdentity))
    if (cacheFile.isFile && cacheFile.length() > 0L) {
        cacheFile.setLastModified(System.currentTimeMillis())
        return cacheFile
    }

    val tmpFile = File(cacheDir, "${cacheFile.name}.${UUID.randomUUID()}.tmp")
    val connection = (URL(url).openConnection() as? HttpURLConnection) ?: return null
    return try {
        if (!BuildConfig.DEBUG && !url.startsWith("https://")) {
            return null
        }
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept-Encoding", "identity")
        authorizationToken?.takeIf { it.isNotBlank() }
            ?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            null
        } else {
            connection.inputStream.use { input ->
                tmpFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (tmpFile.length() <= 0L) {
                tmpFile.delete()
                null
            } else {
                if (!tmpFile.renameTo(cacheFile)) {
                    tmpFile.copyTo(cacheFile, overwrite = true)
                    tmpFile.delete()
                }
                pruneCloudResourceCache()
                cacheFile
            }
        }
    } catch (_: Exception) {
        tmpFile.delete()
        cacheFile.takeIf { it.isFile && it.length() > 0L }
    } finally {
        connection.disconnect()
    }
}

private fun Context.pruneCloudResourceCache() {
    val dir = File(cacheDir, CLOUD_CACHE_DIR)
    val files = dir.listFiles()?.filter { it.isFile } ?: return
    var totalBytes = files.sumOf { it.length() }
    if (totalBytes <= CLOUD_CACHE_MAX_BYTES) return
    files.sortedBy { it.lastModified() }.forEach { file ->
        if (totalBytes <= CLOUD_CACHE_TARGET_BYTES) return@forEach
        val size = file.length()
        if (file.delete()) {
            totalBytes -= size
        }
    }
}

private fun isCacheableCloudResource(url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase(Locale.US)
    if (scheme != "https" && scheme != "http") return false
    val apiHost = runCatching { Uri.parse(BuildConfig.API_BASE_URL).host }.getOrNull()
    if (uri.host != apiHost && uri.host != "api.anyibj.cn") return false
    val path = uri.path.orEmpty().lowercase(Locale.US)
    val inStaticArea = path.startsWith("/assets/") ||
        path.startsWith("/vtuber/") ||
        path.startsWith("/live2d-models/") ||
        path.startsWith("/downloads/")
    if (!inStaticArea || path.endsWith("/") || path.endsWith(".html") || path.endsWith(".htm")) {
        return false
    }
    return listOf(
        ".png",
        ".jpg",
        ".jpeg",
        ".webp",
        ".gif",
        ".svg",
        ".css",
        ".js",
        ".mjs",
        ".json",
        ".moc3",
        ".wasm",
        ".atlas",
        ".skel",
        ".wav",
        ".mp3",
        ".ogg"
    ).any { path.endsWith(it) }
}

private fun cloudCacheFileName(url: String, cacheIdentity: String = url): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(cacheIdentity.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
    val path = Uri.parse(url).path.orEmpty()
    val extension = path.substringAfterLast('/', "")
        .substringAfterLast('.', "")
        .takeIf { it.length in 1..12 && it.all { char -> char.isLetterOrDigit() } }
        ?.let { ".$it" }
        ?: ".bin"
    return "$digest$extension"
}

private fun Context.appPrefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

/** Keeps the bearer token out of plain SharedPreferences using the platform keystore. */
private object SessionTokenVault {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12

    fun read(context: Context): String? {
        val prefs = context.appPrefs()
        val encrypted = prefs.getString(KEY_AUTH_TOKEN_ENCRYPTED, null)
        if (!encrypted.isNullOrBlank()) {
            decrypt(encrypted)?.let { return it }
        }

        // Migrate sessions written by older builds once, then remove the legacy value.
        val legacy = prefs.getString(KEY_LEGACY_AUTH_TOKEN, null)
        if (!legacy.isNullOrBlank() && write(context, legacy)) {
            prefs.edit().remove(KEY_LEGACY_AUTH_TOKEN).apply()
            return legacy
        }
        return null
    }

    fun write(context: Context, token: String): Boolean {
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encrypted.size)
            iv.copyInto(combined)
            encrypted.copyInto(combined, iv.size)
            context.appPrefs().edit()
                .putString(KEY_AUTH_TOKEN_ENCRYPTED, Base64.encodeToString(combined, Base64.NO_WRAP))
                .remove(KEY_LEGACY_AUTH_TOKEN)
                .apply()
        }.isSuccess
    }

    fun clear(context: Context) {
        context.appPrefs().edit()
            .remove(KEY_AUTH_TOKEN_ENCRYPTED)
            .remove(KEY_LEGACY_AUTH_TOKEN)
            .apply()
    }

    private fun decrypt(encoded: String): String? {
        return runCatching {
            val combined = Base64.decode(encoded, Base64.DEFAULT)
            require(combined.size > IV_BYTES)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(128, combined.copyOfRange(0, IV_BYTES))
            )
            String(cipher.doFinal(combined.copyOfRange(IV_BYTES, combined.size)), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun key(): java.security.Key {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(SESSION_KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    SESSION_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generator.generateKey()
        }
        return (keyStore.getKey(SESSION_KEY_ALIAS, null) as javax.crypto.SecretKey)
    }
}

@Volatile
private var crashReporterInstalled = false

@Volatile
private var pendingCrashFlushStarted = false

private fun installCrashReporterOnce(context: Context) {
    synchronized(MainActivity::class.java) {
        if (crashReporterInstalled) return
        installCrashReporter(context)
        crashReporterInstalled = true
    }
}

private fun flushPendingCrashReportOnce(context: Context) {
    synchronized(MainActivity::class.java) {
        if (pendingCrashFlushStarted) return
        pendingCrashFlushStarted = true
    }
    flushPendingCrashReport(context)
}

private fun installCrashReporter(context: Context) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching {
            val stackTrace = StringWriter().also { writer ->
                throwable.printStackTrace(PrintWriter(writer))
            }.toString()
            val report = JSONObject()
                .put("platform", "android")
                .put("appVersion", BuildConfig.VERSION_NAME)
                .put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
                .put("osVersion", "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
                .put("errorType", throwable::class.java.name)
                .put("message", throwable.message)
                .put("stackTrace", stackTrace)
            val pending = File(context.filesDir, PENDING_CRASH_REPORT_FILE)
            val temporary = File(context.filesDir, "$PENDING_CRASH_REPORT_FILE.tmp")
            temporary.writeText(report.toString(), Charsets.UTF_8)
            if (!temporary.renameTo(pending)) {
                temporary.delete()
            }
        }

        if (previous != null) {
            previous.uncaughtException(thread, throwable)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }
}

private fun flushPendingCrashReport(context: Context) {
    val pending = File(context.filesDir, PENDING_CRASH_REPORT_FILE)
    if (!pending.exists()) return
    Thread {
        val reportResult = runCatching {
            JSONObject(pending.readText(Charsets.UTF_8))
        }
        if (reportResult.isFailure) {
            pending.delete()
            return@Thread
        }
        val report = reportResult.getOrThrow()
        val result = runCatching {
            AnyiApiClient(tokenProvider = { SessionTokenVault.read(context) }).reportCrash(
                platform = report.optString("platform", "android"),
                appVersion = report.optString("appVersion", BuildConfig.VERSION_NAME),
                deviceModel = report.optString("deviceModel"),
                osVersion = report.optString("osVersion"),
                errorType = report.optString("errorType", "unknown"),
                message = report.optString("message").takeIf { it.isNotBlank() },
                stackTrace = report.optString("stackTrace").takeIf { it.isNotBlank() }
            )
        }
        if (result.isSuccess) {
            pending.delete()
        }
    }.apply {
        isDaemon = true
        start()
    }
}

private fun readSession(context: Context): AppUser? {
    val prefs = context.appPrefs()
    val token = SessionTokenVault.read(context) ?: return null
    val id = prefs.getString(KEY_USER_ID, null) ?: return null
    val username = prefs.getString(KEY_USER_NAME, null) ?: return null
    val displayName = prefs.getString(KEY_DISPLAY_NAME, null) ?: username
    val gender = prefs.getString(KEY_USER_GENDER, null).takeIf { it == "男" || it == "女" }
    val role = prefs.getString(KEY_USER_ROLE, null) ?: "user"
    val avatarUrl = prefs.getString(KEY_USER_AVATAR, null)
    val aiCompanionListBackgroundUrl = prefs.getString(KEY_AI_COMPANION_LIST_BACKGROUND, null)
    return AppUser(
        id = id,
        username = username,
        displayName = displayName,
        gender = gender,
        role = role,
        token = token,
        avatarUrl = avatarUrl,
        aiCompanionListBackgroundUrl = aiCompanionListBackgroundUrl
    )
}

private fun writeSession(context: Context, user: AppUser) {
    val editor = context.appPrefs().edit()
        .putString(KEY_USER_ID, user.id)
        .putString(KEY_USER_NAME, user.username)
        .putString(KEY_DISPLAY_NAME, user.displayName)
        .putString(KEY_USER_ROLE, user.role)
    if (user.gender == "男" || user.gender == "女") {
        editor.putString(KEY_USER_GENDER, user.gender)
    } else {
        editor.remove(KEY_USER_GENDER)
    }
    if (user.avatarUrl.isNullOrBlank()) {
        editor.remove(KEY_USER_AVATAR)
    } else {
        editor.putString(KEY_USER_AVATAR, user.avatarUrl)
    }
    if (user.aiCompanionListBackgroundUrl.isNullOrBlank()) {
        editor.remove(KEY_AI_COMPANION_LIST_BACKGROUND)
    } else {
        editor.putString(KEY_AI_COMPANION_LIST_BACKGROUND, user.aiCompanionListBackgroundUrl)
    }
    editor.apply()
    // Keep the token out of plaintext storage even if the keystore is temporarily
    // unavailable; the in-memory session remains usable until the process exits.
    SessionTokenVault.write(context, user.token)
}

private fun clearSession(context: Context) {
    context.appPrefs().edit()
        .remove(KEY_USER_ID)
        .remove(KEY_USER_NAME)
        .remove(KEY_DISPLAY_NAME)
        .remove(KEY_USER_GENDER)
        .remove(KEY_USER_ROLE)
        .remove(KEY_USER_AVATAR)
        .remove(KEY_AI_COMPANION_LIST_BACKGROUND)
        .apply()
    SessionTokenVault.clear(context)
}

internal fun Context.persistReadPermission(uri: Uri) {
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
                    val type = if (
                        rawType == "wreath" ||
                        rawType == "chrysanthemum" ||
                        rawType == "lily"
                    ) {
                        rawType
                    } else {
                        "wreath"
                    }
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
            ?: parseTimeMillis(item.optString("created_at")),
        moderationStatus = item.optString("moderationStatus", item.optString("status", "approved"))
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
            ?: parseTimeMillis(item.optString("created_at")),
        moderationStatus = item.optString("moderationStatus", item.optString("status", "approved"))
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
    val deadlineValue = item.optNullableString("deadlineAt")
        ?: item.optNullableString("deadline_at")
    return CommunityVolunteerPost(
        id = item.optString("id", "volunteer-${item.optString("title").hashCode()}"),
        title = item.optString("title", "招募社区义工").ifBlank { "招募社区义工" },
        body = item.optString("body"),
        contact = item.optString("contact")
            .takeIf { it.isNotBlank() && it != "null" }
            ?: "请在人文社区留言报名。",
        imageUrl = item.optString("imageUrl", item.optString("image_url")).takeIf { it.isNotBlank() && it != "null" },
        status = item.optString("status", "open").ifBlank { "open" },
        deadlineAt = item.optLong("deadlineAt").takeIf { it > 0L }
            ?: parseOptionalTimeMillis(deadlineValue),
        createdAt = item.optLong("createdAt").takeIf { it > 0L }
            ?: parseTimeMillis(item.optString("createdAt", item.optString("created_at")))
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
            ?: parseTimeMillis(item.optString("createdAt", item.optString("created_at"))),
        updatedAt = item.optLong("updatedAt").takeIf { it > 0L }
            ?: parseTimeMillis(item.optString("updatedAt", item.optString("updated_at")))
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
        gender = user.optString("gender").takeIf { it == "男" || it == "女" },
        role = user.optString("role", "user"),
        token = token,
        avatarUrl = user.optNullableString("avatarUrl"),
        aiCompanionListBackgroundUrl = user.optNullableString("aiCompanionListBackgroundUrl")
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
        code.contains("invalid_credentials") -> "账号或密码不正确"
        isAuthExpired() -> "登录状态已过期，请重新登录"
        code.contains("username_exists") -> "账号已存在，请直接登录"
        code.contains("weak_password") -> "密码至少需要 8 位"
        code.contains("registration_profile_required") -> "注册需要填写名称、性别并选择头像"
        code.contains("displayName_required") -> "请输入名称"
        code.contains("gender_invalid") -> "性别只能选择男或女"
        code.contains("avatar_required") -> "请选择头像"
        code.contains("unsupported_avatar_type") -> "头像仅支持 JPG、PNG 或 WebP"
        code.contains("avatar_image_size_invalid") -> "头像文件过大，请选择较小的图片"
        code.contains("terms_approval_required") -> "请先阅读并同意用户协议和隐私政策"
        code.contains("wechat_login_not_configured") -> "微信登录还没配置 AppID 和 AppSecret"
        code.contains("wechat_code_invalid") -> "微信授权已失效，请重新点微信登录"
        code.contains("wechat_userinfo_failed") -> "微信资料获取失败，请稍后重试"
        code.contains("ai_memory_sensitive_not_saved") -> "这类敏感信息不会被保存为 AI 记忆"
        code.contains("ai_memory_not_found") -> "这条记忆已经不存在"
        code.contains("ai_memory_id_invalid") -> "记忆编号无效"
        code.contains("ai_memory_type_invalid") -> "记忆类型无效"
        code.contains("community_post_delete_forbidden") -> "只能删除自己发布的动态"
        code.contains("community_comment_delete_forbidden") -> "只能删除自己动态下的评论"
        code.contains("community_content_rejected") -> "内容未通过审核，请修改后再发布"
        code.contains("community_image_not_owned") -> "社区图片必须来自你自己上传的素材"
        code.contains("community_media_not_approved") -> "图片还在审核中，请稍后再试"
        code.contains("community_report_exists") -> "你已经举报过这条内容"
        code.contains("community_volunteer_closed") -> "该招募已截止，暂时不能报名"
        code.contains("volunteer_application_already_pending") -> "你已报名，管理员正在审核，请勿重复提交"
        code.contains("volunteer_application_already_approved") -> "你的报名已通过，无需重复提交"
        code.contains("volunteer_application_not_pending") -> "该报名状态已变化，请刷新审核列表"
        code.contains("volunteer_application_cancel_forbidden") -> "只能取消自己的义工报名"
        code.contains("invalid_volunteer_deadline") ||
            code.contains("volunteer_deadline_must_be_future") -> "请填写未来的有效截止日期"
        code.contains("account_blocked") -> "账号暂时被限制使用"
        code.contains("account_banned") -> "账号已被封禁"
        code.contains("admin_account_deletion_forbidden") -> "管理员账号不能在 App 内注销"
        code.contains("flower_limit_reached") -> "当前已有 2 个花篮，冷却结束后再献花"
        code.contains("candle_limit_reached") -> "当前已有 2 根蜡烛，任一根燃尽后可继续点蜡烛"
        code.contains("durian_offering_requires_payment") -> "榴莲是付费供品，请先完成解锁"
        code.contains("apple_offering_limit_reached") -> "当前已有 3 个苹果，10 分钟后可继续放苹果"
        code.contains("durian_offering_limit_reached") -> "当前已有 1 个榴莲，10 分钟后可继续放榴莲"
        code.contains("invalid_fruit_type") -> "暂不支持这种供品"
        code.contains("payment_not_configured") -> "付费功能暂未开放，请勿重复支付"
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

internal fun Context.readUploadPayload(uri: Uri): UploadPayload {
    val declaredMimeType = contentResolver.getType(uri)
    val rawName = queryDisplayName(uri)
        ?: uri.lastPathSegment?.substringAfterLast('/')
        ?: "upload-${System.currentTimeMillis()}"
    // A few document providers omit ContentResolver.getType(). Use the file
    // name as a first-pass limit, then refine it from the actual bytes below.
    val preliminaryMimeType = inferUploadMimeType(declaredMimeType, rawName, byteArrayOf())
    val maxBytes = if (preliminaryMimeType.startsWith("audio/")) 50L * 1024L * 1024L else 20L * 1024L * 1024L
    val declaredSize = queryFileSize(uri)
    if (declaredSize != null && (declaredSize <= 0L || declaredSize > maxBytes)) {
        throw IllegalArgumentException("file_size_invalid")
    }
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
    val mimeType = inferUploadMimeType(declaredMimeType, fileName, bytes)
    val finalMaxBytes = if (mimeType.startsWith("audio/")) 50L * 1024L * 1024L else 20L * 1024L * 1024L
    if (bytes.size.toLong() > finalMaxBytes) {
        throw IllegalArgumentException("file_size_invalid")
    }
    return UploadPayload(fileName = fileName, mimeType = mimeType, bytes = bytes)
}

/**
 * Normalizes MIME values from Android document providers. Some providers
 * return null or application/octet-stream even for a real image; in that case
 * use a safe signature/extension fallback so the API can validate the upload.
 */
internal fun inferUploadMimeType(
    declaredMimeType: String?,
    fileName: String,
    bytes: ByteArray
): String {
    val normalizedDeclared = declaredMimeType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.US)
    val declared = normalizedDeclared
        ?.let {
            when (it) {
                "image/jpg" -> "image/jpeg"
                "image/x-png" -> "image/png"
                else -> it
            }
        }
        ?.takeIf {
            it.isNotBlank() &&
                it != "application/octet-stream" &&
                it != "*/*" &&
                !it.endsWith("/*")
        }
    if (declared != null) return declared

    val signature = when {
        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(
            byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
        ) -> "image/png"
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
        bytes.size >= 12 && bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(82, 73, 70, 70)) &&
            bytes.copyOfRange(8, 12).contentEquals(byteArrayOf(87, 69, 66, 80)) -> "image/webp"
        bytes.size >= 3 && bytes.copyOfRange(0, 3).contentEquals(byteArrayOf(73, 68, 51)) -> "audio/mpeg"
        bytes.size >= 12 && bytes.copyOfRange(4, 8).contentEquals(byteArrayOf(102, 116, 121, 112)) -> "audio/mp4"
        bytes.size >= 12 && bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(82, 73, 70, 70)) &&
            bytes.copyOfRange(8, 12).contentEquals(byteArrayOf(87, 65, 86, 69)) -> "audio/wav"
        else -> null
    }
    if (signature != null) return signature

    return when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "wav" -> "audio/wav"
        "txt" -> "text/plain"
        else -> declared
            ?: "application/octet-stream"
    }
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
        value.startsWith("http://api.anyibj.cn/") -> value.replace("http://api.anyibj.cn", apiBase)
        else -> value
    }
}

private fun isAuthenticatedAssetResource(url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase(Locale.US)
    if (scheme != "https" && scheme != "http") return false
    val apiHost = runCatching { Uri.parse(BuildConfig.API_BASE_URL).host }.getOrNull()
    if (uri.host != apiHost && uri.host != "api.anyibj.cn") return false
    return uri.encodedPath.orEmpty().startsWith("/assets/") || uri.path.orEmpty().startsWith("/assets/")
}

private fun parseOptionalTimeMillis(raw: String?): Long? {
    val normalized = raw?.trim()?.takeIf { it.isNotBlank() && it != "null" } ?: return null
    normalized.toLongOrNull()?.takeIf { it > 0L }?.let { return it }
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(normalized)?.time
    }.getOrNull()
}

private fun parseTimeMillis(raw: String): Long {
    return parseOptionalTimeMillis(raw) ?: System.currentTimeMillis()
}

private fun flowerChoices() = listOf(
    FlowerChoice(
        type = "wreath",
        name = "白菊花束",
        subtitle = "白菊花束",
        imageResId = R.drawable.anyi_hall_flower_original_bouquet
    )
)

private fun flowerDisplayName(type: String) = flowerChoices()
    .firstOrNull { it.type == type }
    ?.name
    ?: "花束"

private fun flowerOfferingSummary(flowers: List<FlowerOffering>): String {
    val names = flowers
        .map { flowerDisplayName(it.type) }
        .distinct()
    return names.joinToString("、").ifBlank { flowerDisplayName("wreath") }
}

private fun defaultCommunityVolunteerPosts() = listOf(
    CommunityVolunteerPost(
        id = "default-story",
        title = "故事整理义工",
        body =
            "协助家属整理纪念故事、照片说明和人生片段，让重要记忆被温柔地保存下来。",
        contact = "在人文社区留言“故事义工”，安忆团队会联系你。",
        createdAt = parseTimeMillis("2026-06-05T00:00:00.000Z")
    ),
    CommunityVolunteerPost(
        id = "default-companion",
        title = "陪伴倾听义工",
        body =
            "为需要倾诉的人提供耐心、克制、尊重边界的陪伴，帮他们把想念慢慢说出来。",
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

private fun formatRemaining(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatTime(millis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(millis))
}
