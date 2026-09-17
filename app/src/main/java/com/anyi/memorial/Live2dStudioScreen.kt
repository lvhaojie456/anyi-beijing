package com.anyi.memorial

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyi.memorial.network.AnyiApiClient
import com.anyi.memorial.network.AnyiApiException
import com.anyi.memorial.network.UploadPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private data class Live2dJob(
    val id: String, val status: String, val stage: String, val progress: Int,
    val diagnosisCode: String? = null, val suggestion: String? = null, val summary: String? = null
) {
    val active get() = status == "queued" || status == "running"
}

private fun parseLive2dJob(item: org.json.JSONObject) = Live2dJob(
    item.getString("id"), item.getString("status"), item.getString("stage"), item.optInt("progress"),
    item.optString("diagnosisCode").takeIf { it.isNotEmpty() && !item.isNull("diagnosisCode") },
    item.optString("suggestion").takeIf { it.isNotEmpty() && !item.isNull("suggestion") },
    item.optString("summary").takeIf { it.isNotEmpty() && !item.isNull("summary") }
)

@Composable
internal fun Live2dStudioScreen(api: AnyiApiClient, companion: AiCompanion, onBack: () -> Unit, onActivated: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by rememberSaveable(companion.id) { mutableStateOf("") }
    var mode by rememberSaveable(companion.id) { mutableStateOf("prompt") }
    var requestId by rememberSaveable(companion.id) { mutableStateOf(UUID.randomUUID().toString()) }
    var file by remember(companion.id) { mutableStateOf<UploadPayload?>(null) }
    var jobs by remember(companion.id) { mutableStateOf(emptyList<Live2dJob>()) }
    var enabled by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var downloadId by rememberSaveable { mutableStateOf<String?>(null) }
    var previewId by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    suspend fun refreshJobs() {
        val response = withContext(Dispatchers.IO) { api.listLive2dJobs(companion.id) }
        jobs = List(response.length()) { index -> parseLive2dJob(response.getJSONObject(index)) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { context.readUploadPayload(uri) } }
                .onSuccess { payload ->
                    if (payload.bytes.size > 8 * 1024 * 1024) error = "图片不能超过 8 MB"
                    else { file = payload; requestId = UUID.randomUUID().toString(); error = "" }
                }.onFailure { error = "图片读取失败，请重新选择" }
        }
    }
    val saveProject = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val id = downloadId
        if (uri != null && id != null) scope.launch {
            busy = true
            runCatching { withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { api.downloadLive2dFile(id, "project.zip", it) }
                    ?: throw IllegalStateException("cannot_open_destination")
            } }.onFailure { error = "工程下载失败，请重试" }
            busy = false
            downloadId = null
        }
    }
    LaunchedEffect(companion.id, refresh) {
        while (isActive) {
            try {
                enabled = withContext(Dispatchers.IO) { api.live2dConfig().optBoolean("enabled") }
                refreshJobs()
            } catch (failure: Exception) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                error = "任务状态读取失败"
            }
            delay(8000)
        }
    }
    fun action(job: Live2dJob, command: String, hint: String? = null) {
        if (busy) return
        busy = true; error = ""
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { api.live2dJobAction(job.id, command, hint) } }
                .onSuccess {
                    if (command == "activate") onActivated("generated:${job.id}")
                    else refresh++
                }.onFailure { error = studioError(it) }
            busy = false
        }
    }
    BackHandler { if (previewId != null) previewId = null else onBack() }
    if (previewId != null) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF3F5F4)).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { previewId = null }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
                Text("动态形象预览", fontSize = 18.sp)
            }
            var previewError by remember(previewId) { mutableStateOf("") }
            Live2dAvatarView("generated:$previewId", Modifier.weight(1f).fillMaxWidth(), api = api,
                onEvent = { if (it is Live2dEvent.Error) previewError = "模型预览失败" })
            if (previewError.isNotEmpty()) Text(previewError, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            Text("AI 生成 · 待精修", fontSize = 12.sp, modifier = Modifier.padding(16.dp))
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF6F7F6)).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
                Text("创建动态形象", Modifier.weight(1f), fontSize = 19.sp)
                IconButton(onClick = { refresh++; error = "" }) { Icon(Icons.Rounded.Refresh, "刷新") }
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(companion.displayName, color = Color(0xFF557466), fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for ((id, title) in listOf("prompt" to "提示词", "image" to "图片")) {
                        FilterChip(selected = mode == id, onClick = { mode = id; requestId = UUID.randomUUID().toString() }, label = { Text(title) }, enabled = !busy)
                    }
                }
                if (mode == "prompt") OutlinedTextField(prompt, {
                    prompt = it.take(2000); requestId = UUID.randomUUID().toString()
                }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("人物描述") }, minLines = 3, maxLines = 6)
                else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !busy) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text(if (file == null) "选择图片" else "更换图片")
                        }
                        if (file != null) IconButton(onClick = { file = null; requestId = UUID.randomUUID().toString() }, enabled = !busy) { Icon(Icons.Rounded.Close, "移除图片") }
                    }
                    val image = remember(file) { file?.bytes?.let(::decodeStudioThumbnail) }
                    if (image != null) Image(image.asImageBitmap(), "输入图片", Modifier.fillMaxWidth().height(200.dp))
                }
                if (enabled == false) Text("生成服务未开启", color = MaterialTheme.colorScheme.error)
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Button(onClick = {
                    busy = true; error = ""
                    val sentPrompt = if (mode == "prompt") prompt else ""
                    val sentFile = if (mode == "image") file else null
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { api.createLive2dJob(companion.id, sentPrompt, sentFile, requestId) } }
                            .onSuccess { submitted ->
                                val created = parseLive2dJob(submitted)
                                jobs = listOf(created) + jobs.filterNot { it.id == created.id }
                                requestId = UUID.randomUUID().toString(); refresh++
                            }
                            .onFailure { error = studioError(it) }
                        busy = false
                    }
                }, enabled = enabled == true && !busy && jobs.none { it.active } &&
                    (if (mode == "prompt") prompt.isNotBlank() else file != null), modifier = Modifier.fillMaxWidth()) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("生成可精修初版")
                }
                Text("生成记录", fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
            }
        }
        items(jobs, key = { it.id }) { job ->
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stageTitle(job.status, job.stage), fontSize = 15.sp)
                if (job.active) {
                    LinearProgressIndicator(progress = { job.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${job.progress}%", fontSize = 12.sp)
                    TextButton(onClick = { action(job,"cancel") }, enabled = !busy) { Text("取消任务") }
                } else if (job.status == "succeeded") {
                    var bitmap by remember(job.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(job.id) {
                        runCatching { withContext(Dispatchers.IO) {
                            val bytes = api.readLive2dFile(job.id,"preview.png")
                            decodeStudioThumbnail(bytes)
                        } }.onSuccess { bitmap = it }
                    }
                    bitmap?.let { Image(it.asImageBitmap(),"生成形象",Modifier.fillMaxWidth().height(220.dp)) }
                    Text("AI 生成 · 待精修", fontSize = 12.sp, color = Color(0xFF65756D))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { previewId = job.id }) { Icon(Icons.Rounded.PlayArrow,"预览模型") }
                        IconButton(onClick = { downloadId = job.id; saveProject.launch("live2d-${job.id}.zip") }, enabled = !busy) { Icon(Icons.Rounded.Download,"下载精修工程") }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { action(job,"activate") }, enabled = !busy) { Text("使用此形象") }
                    }
                } else {
                    val failure = live2dFailureUi(job.diagnosisCode, job.suggestion, job.summary, hasSourceImage = mode == "image")
                    if (job.status == "failed") Text(failure.message, fontSize = 13.sp, color = Color(0xFF7A4B3A))
                    failure.primaryNote?.let { Text(it, fontSize = 12.sp, color = Color(0xFF65756D)) }
                    val canRetry = enabled == true && !busy && jobs.none { it.active }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (job.status == "failed" && failure.primaryLabel != null) {
                            Button(onClick = { action(job, "retry", failure.primaryHint) }, enabled = canRetry) { Text(failure.primaryLabel) }
                        }
                        TextButton(onClick = { action(job, "retry") }, enabled = canRetry) { Text(if (job.status == "failed") failure.retryLabel else "重新生成") }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

private fun stageTitle(status: String, stage: String): String = when (status) {
    "succeeded" -> "初版已生成"
    "failed" -> "生成失败"
    "cancelled" -> "已取消"
    else -> when (stage) {
        "queued" -> "等待制作"
        "generating" -> "生成立绘"
        "planning" -> "分析形象"
        "decomposing" -> "拆分图层"
        "expressions" -> "生成表情"
        "refining" -> "整理精修素材"
        "rigging" -> "制作动作"
        "verifying" -> "检查模型"
        "uploading" -> "保存工程"
        else -> "准备制作"
    }
}

private fun studioError(error: Throwable): String = when ((error as? AnyiApiException)?.message) {
    "live2d_not_configured" -> "生成服务未开启"
    "live2d_job_already_active" -> "这个对象已有生成任务"
    "live2d_daily_limit" -> "今天的生成任务已达上限"
    else -> "操作未完成，请刷新状态后重试"
}

private fun decodeStudioThumbnail(bytes: ByteArray): android.graphics.Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    var sample = 1
    while (maxOf(options.outWidth, options.outHeight) / sample > 1024) sample *= 2
    options.inJustDecodeBounds = false
    options.inSampleSize = sample
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
