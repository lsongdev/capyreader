package com.capyreader.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebView.VisualStateCallback
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.ResourcesPathHandler
import coil.executeBlocking
import coil.imageLoader
import coil.request.ImageRequest
import com.capyreader.app.preferences.AppPreferences
import com.capyreader.app.common.Media
import com.capyreader.app.common.WebViewInterface
import com.capyreader.app.common.openLink
import com.capyreader.app.ui.articles.detail.articleTemplateColors
import com.capyreader.app.ui.articles.detail.byline
import com.jocmp.capy.Article
import com.jocmp.capy.articles.ArticleRenderer
import com.jocmp.capy.common.windowOrigin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    state: WebViewState,
    onDispose: (WebView) -> Unit,
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { state.webView },
        onRelease = {
            onDispose(it)
        }
    )
}

class AccompanistWebViewClient(
    private val assetLoader: WebViewAssetLoader,
) : WebViewClient(),
    KoinComponent {
    lateinit var state: WebViewState
        internal set

    private val appPreferences by inject<AppPreferences>()

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        view.postVisualStateCallback(0, object : VisualStateCallback() {
            override fun onComplete(requestId: Long) {
                view.visibility = View.VISIBLE
            }
        })
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val asset = assetLoader.shouldInterceptRequest(request.url)

        if (asset != null) {
            val headers = asset.responseHeaders ?: mutableMapOf()
            headers["Access-Control-Allow-Origin"] = "*"
            asset.responseHeaders = headers

            return asset
        }

        return try {
            val imageRequest = ImageRequest.Builder(view.context)
                .data(request.url)
                .build()
            val bitmap =
                view.context.imageLoader.executeBlocking(imageRequest).drawable?.toBitmap()

            if (bitmap != null) {
                return WebResourceResponse(
                    "image/jpg",
                    "UTF-8",
                    jpegStream(bitmap)
                )
            }

            null
        } catch (exception: Exception) {
            null
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url

        if (view != null && url != null) {
            view.context.openLink(url, appPreferences)
        }

        return true
    }
}

@Stable
class WebViewState(
    private val renderer: ArticleRenderer,
    private val colors: Map<String, String>,
    private val scope: CoroutineScope,
    internal val webView: WebView,
) {
    private var htmlId: String? = null

    init {
        loadEmpty()
    }

    fun loadHtml(article: Article, showImages: Boolean) {
        val id = article.id

        if (htmlId == null || id != htmlId) {
            webView.visibility = View.INVISIBLE
        }

        htmlId = id

        scope.launch {
            withContext(Dispatchers.IO) {
                val html = renderer.render(
                    article,
                    hideImages = !showImages,
                    byline = article.byline(context = webView.context),
                    colors = colors
                )

                withContext(Dispatchers.Main) {
                    webView.loadDataWithBaseURL(
                        windowOrigin(article.url),
                        html,
                        null,
                        "UTF-8",
                        null,
                    )
                }
            }
        }
    }

    fun reset() {
        htmlId = null
        loadEmpty()
    }

    private fun loadEmpty() = webView.loadUrl("about:blank")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun rememberWebViewState(
    renderer: ArticleRenderer = koinInject(),
    onNavigateToMedia: (media: Media) -> Unit,
): WebViewState {
    val colors = articleTemplateColors()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val client = remember {
        AccompanistWebViewClient(
            assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", AssetsPathHandler(context))
                .addPathHandler("/res/", ResourcesPathHandler(context))
                .build(),
        )
    }

    return remember {
        val webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
            }
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            addJavascriptInterface(
                WebViewInterface(
                    navigateToMedia = { onNavigateToMedia(it) }
                ),
                WebViewInterface.INTERFACE_NAME
            )

            setBackgroundColor(context.getColor(android.R.color.transparent))

            webViewClient = client
        }

        WebViewState(renderer, colors, scope, webView).also {
            client.state = it
        }
    }
}

private fun jpegStream(
    bitmap: Bitmap,
): InputStream {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    val bitmapData = byteArrayOutputStream.toByteArray()
    return ByteArrayInputStream(bitmapData)
}
