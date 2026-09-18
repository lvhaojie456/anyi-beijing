package com.anyi.memorial

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject
import com.anyi.memorial.network.AnyiApiClient
import java.io.ByteArrayInputStream

internal fun generatedLive2dJobId(modelId: String?): String? = modelId
    ?.takeIf { it.matches(Regex("generated:[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")) }
    ?.removePrefix("generated:")

internal fun supportedLive2dModel(modelId: String?): Boolean =
    Live2dCatalog.find(modelId) != null || generatedLive2dJobId(modelId) != null

/**
 * Catalogue of Live2D avatars bundled in the APK under assets/live2d/models/<id>/.
 * Ids must match the backend allow-list in backend/src/index.ts (live2dModelIds)
 * and MODEL_FILES in assets/live2d/index.html.
 */
internal data class Live2dModel(val id: String, val title: String, val subtitle: String)

internal object Live2dCatalog {
    val models: List<Live2dModel> = listOf(
        Live2dModel("kei", "Kei", "白发中性 · 沉静"),
        Live2dModel("izumi", "Izumi", "素色长裙 · 手绘风"),
        Live2dModel("haru", "Haru", "职业装 · 前台接待"),
        Live2dModel("hiyori", "Hiyori", "桃瀬日和 · 校服少女"),
        Live2dModel("tororo", "Tororo", "白猫 · 宠物陪伴"),
        Live2dModel("hijiki", "Hijiki", "黑猫 · 宠物陪伴")
    )

    fun find(id: String?): Live2dModel? = id?.let { key -> models.firstOrNull { it.id == key } }
}

/** Events the JS side raises through the AndroidBridge JavascriptInterface. */
internal sealed interface Live2dEvent {
    data object PageReady : Live2dEvent
    data class Ready(val modelId: String) : Live2dEvent
    data class Error(val message: String) : Live2dEvent
}

/**
 * A Compose-hosted WebView that renders bundled or owner-scoped generated models.
 *
 * The HTML and runtime are bundled. Generated model data is fetched by the
 * native authenticated API client and exposed only on a job-scoped virtual
 * same-origin path; the page never receives credentials or file access.
 *
 * `speakText` is a "signal": every time it changes to a non-null value the
 * avatar plays a talk animation sized to the text length. Callers should bump
 * it per AI reply and reset it to null afterwards.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun Live2dAvatarView(
    modelId: String,
    modifier: Modifier = Modifier,
    speakText: String? = null,
    mouthOpenness: Float? = null,
    api: AnyiApiClient? = null,
    onEvent: (Live2dEvent) -> Unit = {}
) {
    val latestOnEvent by rememberUpdatedState(onEvent)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var loadedModel by remember { mutableStateOf<String?>(null) }

    key(modelId) { AndroidView(
        modifier = modifier,
        factory = { context ->
            pageReady = false
            loadedModel = null
            createLive2dWebView(context, generatedLive2dJobId(modelId), api) { event ->
                when (event) {
                    Live2dEvent.PageReady -> pageReady = true
                    is Live2dEvent.Ready -> loadedModel = event.modelId
                    is Live2dEvent.Error -> Unit
                }
                latestOnEvent(event)
            }.also { webView = it }
        },
        onRelease = { view ->
            view.stopLoading()
            view.removeJavascriptInterface("AndroidBridge")
            view.loadUrl("about:blank")
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
            if (webView === view) webView = null
        }
    ) }

    // Load (or switch) the model once the page has booted.
    LaunchedEffect(pageReady, modelId) {
        val view = webView ?: return@LaunchedEffect
        if (!pageReady) return@LaunchedEffect
        if (loadedModel == modelId) return@LaunchedEffect
        val jobId = generatedLive2dJobId(modelId)
        val modelUrl = jobId?.let { "$LIVE2D_ORIGIN/generated-live2d/$it/runtime/model.model3.json" }
        view.evaluateJavascript("window.anyiLive2d && window.anyiLive2d.load(${JSONObject.quote(modelId)}, ${modelUrl?.let(JSONObject::quote) ?: "null"})", null)
    }

    // Real audio drives the mouth while a synthesized reply is playing; the text
    // animation stays as the fallback when there is no audio (or TTS is off).
    LaunchedEffect(mouthOpenness) {
        val view = webView ?: return@LaunchedEffect
        if (loadedModel == null || mouthOpenness == null) return@LaunchedEffect
        view.evaluateJavascript("window.anyiLive2d && window.anyiLive2d.setLipSync(" + mouthOpenness + ")", null)
    }

    // Trigger talk animation on each new reply.
    LaunchedEffect(speakText, loadedModel) {
        val view = webView ?: return@LaunchedEffect
        if (loadedModel == null) return@LaunchedEffect
        val text = speakText
        if (text.isNullOrBlank()) {
            view.evaluateJavascript("window.anyiLive2d && window.anyiLive2d.idle()", null)
        } else {
            view.evaluateJavascript("window.anyiLive2d && window.anyiLive2d.speak(${JSONObject.quote(text)})", null)
        }
    }

}

private const val LIVE2D_ORIGIN = "https://appassets.androidplatform.net"
private const val LIVE2D_PATH = "/assets/live2d/"

@SuppressLint("SetJavaScriptEnabled")
private fun createLive2dWebView(context: Context, jobId: String?, api: AnyiApiClient?, emit: (Live2dEvent) -> Unit): WebView {
    val assetLoader = WebViewAssetLoader.Builder()
        .setDomain("appassets.androidplatform.net")
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(AndroidColor.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.mediaPlaybackRequiresUserGesture = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = WebView.OVER_SCROLL_NEVER

        addJavascriptInterface(object {
            @JavascriptInterface fun onPageReady(@Suppress("UNUSED_PARAMETER") arg: String) {
                post { emit(Live2dEvent.PageReady) }
            }
            @JavascriptInterface fun onReady(modelId: String) {
                post { emit(Live2dEvent.Ready(modelId)) }
            }
            @JavascriptInterface fun onError(message: String) {
                post { emit(Live2dEvent.Error(message)) }
            }
        }, "AndroidBridge")

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url
                fun denied() = WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                if (url.scheme != "https" || url.host != "appassets.androidplatform.net" || url.port !in listOf(-1,443) || request.method != "GET") return denied()
                if (url.path?.startsWith(LIVE2D_PATH) == true) return assetLoader.shouldInterceptRequest(url) ?: denied()
                val prefix = "/generated-live2d/$jobId/"
                val path = url.path.orEmpty()
                if (jobId == null || api == null || !path.startsWith(prefix) || url.query != null) return denied()
                val name = path.removePrefix(prefix)
                if (!name.startsWith("runtime/")) return denied()
                return try {
                    val bytes = api.readLive2dFile(jobId, name)
                    val mime = when {
                        name.endsWith(".json") -> "application/json"
                        name.endsWith(".png") -> "image/png"
                        else -> "application/octet-stream"
                    }
                    WebResourceResponse(mime, null, 200, "OK", mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(bytes))
                } catch (_: Exception) { denied() }
            }

            // Keep the page sandboxed to bundled assets: refuse any navigation
            // away from the asset origin.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return request.url.toString() != "$LIVE2D_ORIGIN${LIVE2D_PATH}index.html"
            }
        }
        loadUrl("$LIVE2D_ORIGIN${LIVE2D_PATH}index.html")
    }
}
