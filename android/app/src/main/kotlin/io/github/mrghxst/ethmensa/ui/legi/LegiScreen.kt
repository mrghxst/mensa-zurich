// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.legi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.ui.components.EmptyState
import io.github.mrghxst.ethmensa.ui.components.GlassIconButton
import io.github.mrghxst.ethmensa.ui.components.Icons
import io.github.mrghxst.ethmensa.ui.settings.openUrl
import io.github.mrghxst.ethmensa.ui.theme.Eth

private const val LEGI_URL = "https://eduapp.ethz.ch/eth-card"
private const val LEGI_HOST = "eduapp.ethz.ch"

/** Hosts that stay inside the WebView: the card itself and the ETH login (SWITCH edu-ID, Microsoft Entra). */
private val IN_APP_HOSTS = listOf("ethz.ch", "switch.ch", "eduid.ch", "microsoftonline.com", "microsoft.com", "live.com", "msauth.net", "msftauth.net")

/** The card app on eduapp.ethz.ch. Its Keycloak login lives on the same host under /keycloak/. */
internal fun isCardPage(url: String?): Boolean {
    val uri = url.orEmpty().toUri()
    return uri.host == LEGI_HOST && uri.path?.startsWith("/keycloak/") != true
}

internal fun isInAppHost(host: String?): Boolean {
    val h = host?.lowercase() ?: return false
    return IN_APP_HOSTS.any { h == it || h.endsWith(".$it") }
}

/**
 * The ETH Legi card page (LegiView.swift): injected CSS in the theme colors, no pinch zoom,
 * full brightness and screen kept on while visible. The user signs in with their own ETH account.
 */
@Composable
fun LegiScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = Eth.colors
    val css = remember(colors) { legiCss(background = colors.background, foreground = colors.textPrimary) }
    val currentCss by rememberUpdatedState(css)
    val currentBackground by rememberUpdatedState(colors.background.toArgb())
    var loaded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    // A crashed or killed renderer leaves the WebView unusable: a new generation builds a fresh one.
    var generation by remember { mutableIntStateOf(0) }

    val webView = remember(generation) {
        createLegiWebView(
            context = context,
            css = { currentCss },
            cardBackground = { currentBackground },
            onPageFinished = { loaded = true },
            onMainFrameError = { failed = true },
            onHistoryChanged = { canGoBack = it },
            onRendererGone = {
                loaded = false
                failed = true
                generation++
            },
        )
    }
    DisposableEffect(webView) {
        webView.loadUrl(LEGI_URL)
        onDispose { webView.destroy() }
    }
    // Re-style when the theme changes while the page is open.
    LaunchedEffect(css) {
        if (loaded && isCardPage(webView.url)) {
            webView.setBackgroundColor(currentBackground)
            webView.evaluateJavascript(injectCssScript(css), null)
        }
    }

    FullBrightness()
    // Back steps through the WebView's history first; at its start, Navigation 3 closes the card.
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = canGoBack,
        onBackCompleted = { webView.goBack() },
    )

    val contentAlpha by animateFloatAsState(if (loaded && !failed) 1f else 0f, label = "legi-alpha")
    Column(Modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.legi_card),
                style = Eth.type.screenTitle,
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center).semantics { heading() },
            )
            GlassIconButton(Icons.close, stringResource(R.string.close), onClose, Modifier.align(Alignment.CenterEnd))
        }
        Box(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha },
            )
            when {
                failed -> EmptyState(
                    icon = Icons.wifiOff,
                    title = stringResource(R.string.internet_connection_required),
                    message = stringResource(R.string.internet_connection_required_message),
                    modifier = Modifier.align(Alignment.Center),
                    action = stringResource(R.string.retry),
                    onAction = {
                        failed = false
                        loaded = false
                        webView.loadUrl(LEGI_URL)
                    },
                )
                !loaded -> LoadingIndicator(Modifier.align(Alignment.Center), color = colors.accent)
            }
        }
    }
}

/** Screen brightness 1.0 and keep-screen-on while resumed, restored on pause and when the screen leaves. */
@Composable
private fun FullBrightness() {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val window = activity.window
    // The activity's lifecycle: a Navigation 3 entry's own lifecycle doesn't always reach RESUMED.
    val lifecycleOwner: LifecycleOwner = activity
    DisposableEffect(window, lifecycleOwner) {
        var previous: Float? = null
        fun apply() {
            if (previous != null) return
            val attrs = window.attributes
            previous = attrs.screenBrightness
            window.attributes = attrs.apply { screenBrightness = 1f }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        fun restore() {
            val old = previous ?: return
            window.attributes = window.attributes.apply { screenBrightness = old }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            previous = null
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> apply()
                Lifecycle.Event.ON_PAUSE -> {
                    restore()
                    CookieManager.getInstance().flush()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            restore()
            CookieManager.getInstance().flush()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createLegiWebView(
    context: Context,
    css: () -> String,
    cardBackground: () -> Int,
    onPageFinished: () -> Unit,
    onMainFrameError: () -> Unit,
    onHistoryChanged: (canGoBack: Boolean) -> Unit,
    onRendererGone: () -> Unit,
): WebView = WebView(context).apply webView@{
    // White like a browser: the ETH login pages have no background of their own.
    setBackgroundColor(android.graphics.Color.WHITE)
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
        setSupportMultipleWindows(false)
        allowFileAccess = false
        allowContentAccess = false
    }
    // Our CSS sets the colors; algorithmic darkening could invert the barcode and make it unscannable.
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
    }
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(this@webView, true)
    }
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            return when (uri.scheme?.lowercase()) {
                "https" -> if (isInAppHost(uri.host)) false else true.also { openUrl(view.context, uri.toString()) }
                "mailto", "tel" -> true.also { runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } }
                // http, intent: and everything else is blocked.
                else -> true
            }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            onHistoryChanged(view.canGoBack())
        }

        override fun onPageFinished(view: WebView, url: String?) {
            // Only the card page gets our colors: the ETH login pages keep their own styling.
            if (isCardPage(url)) {
                view.setBackgroundColor(cardBackground())
                view.evaluateJavascript(injectCssScript(css()), null)
            } else {
                view.setBackgroundColor(android.graphics.Color.WHITE)
            }
            onPageFinished()
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            onHistoryChanged(view.canGoBack())
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) onMainFrameError()
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            onRendererGone()
            return true
        }
    }
}

private fun Color.hex(): String = String.format("#%06x", toArgb() and 0xFFFFFF)

/** The CSS from LegiView.swift, with our background and text colors. */
internal fun legiCss(background: Color, foreground: Color): String {
    val bg = background.hex()
    val fg = foreground.hex()
    return listOf(
        ".modal {visibility: hidden !important}",
        ".header {visibility: hidden !important}",
        ".sidebar {visibility: hidden !important}",
        "#eth-card-display {background: white;}",
        ".barcode-wrap {height: 70px !important}",
        ".barcode-wrap {padding-top: 20px !important}",
        "#main-content {margin: 0 !important}",
        "#main-content {background: $bg !important}",
        "#app {background: $bg}",
        ".layout {background: $bg}",
        ".time-display.date-time {color: $fg !important}",
        ".bottom-text-wrap {color: $fg !important}",
    ).joinToString("")
}

/** Adds or replaces our style element. The CSS has no quotes or backslashes. */
private fun injectCssScript(css: String): String =
    "(function(){var s=document.getElementById('ethmensa-style');" +
        "if(!s){s=document.createElement('style');s.id='ethmensa-style';(document.head||document.documentElement).appendChild(s);}" +
        "s.textContent='$css';})();"
