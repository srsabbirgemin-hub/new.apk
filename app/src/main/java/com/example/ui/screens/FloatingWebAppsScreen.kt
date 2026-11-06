package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

enum class QuickSocialApp(
    val title: String,
    val url: String,
    val brandColor: Color,
    val isDesktopByDefault: Boolean
) {
    YOUTUBE("YouTube", "https://m.youtube.com", Color(0xFFFF0000), false),
    FACEBOOK("Facebook", "https://m.facebook.com", Color(0xFF1877F2), false),
    TELEGRAM("Telegram", "https://web.telegram.org/k/", Color(0xFF24A1DE), false),
    WHATSAPP("WhatsApp", "https://web.whatsapp.com", Color(0xFF25D366), true),
    TIKTOK("TikTok", "https://www.tiktok.com", Color(0xFFFE2C55), false),
    INSTAGRAM("Instagram", "https://www.instagram.com", Color(0xFFE4405F), false),
    TWITTER("X (Twitter)", "https://x.com", Color(0xFF1DA1F2), false),
    WIKIPEDIA("Wikipedia", "https://bn.wikipedia.org", Color(0xFF636466), false),
    GOOGLE("Google", "https://www.google.com", Color(0xFF4285F4), false)
}

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FloatingWebAppsScreen(
    modifier: Modifier = Modifier,
    initialApp: QuickSocialApp = QuickSocialApp.YOUTUBE
) {
    val context = LocalContext.current
    var currentApp by remember { mutableStateOf(initialApp) }
    var currentUrl by remember { mutableStateOf(initialApp.url) }
    var inputUrl by remember { mutableStateOf(initialApp.url) }
    var isDesktopMode by remember { mutableStateOf(initialApp.isDesktopByDefault) }
    var pageTitle by remember { mutableStateOf(initialApp.title) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    if (customVideoView != null) {
        BackHandler {
            customViewCallback?.onCustomViewHidden()
            customVideoView = null
            customViewCallback = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { customVideoView!! },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = {
                    customViewCallback?.onCustomViewHidden()
                    customVideoView = null
                    customViewCallback = null
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Fullscreen Video",
                    tint = Color.White
                )
            }
        }
        return
    }

    // Intercept back button to navigate web history
    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Quick App Switcher Header (Horizontal row with YouTube, Facebook, Telegram, WhatsApp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickSocialApp.values().forEach { app ->
                val isSelected = currentApp == app
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        currentApp = app
                        isDesktopMode = app.isDesktopByDefault
                        currentUrl = app.url
                        inputUrl = app.url
                        webViewRef?.let { wv ->
                            wv.settings.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else null
                            wv.loadUrl(app.url)
                        }
                    },
                    label = {
                        Text(
                            text = app.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(app.brandColor)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = app.brandColor.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        // Browser Navigation Bar (Back, Forward, URL/Search field, Reload, Desktop Toggle)
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { webViewRef?.goBack() },
                    enabled = canGoBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { webViewRef?.goForward() },
                    enabled = canGoForward,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (isLoading) webViewRef?.stopLoading() else webViewRef?.reload()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = if (isLoading) "Stop" else "Reload",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // URL Input Field
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    var target = inputUrl.trim()
                                    if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                        target = if (target.contains(".") && !target.contains(" ")) {
                                            "https://$target"
                                        } else {
                                            "https://www.google.com/search?q=" + android.net.Uri.encode(target)
                                        }
                                    }
                                    inputUrl = target
                                    currentUrl = target
                                    webViewRef?.loadUrl(target)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Zoom In Button
                IconButton(
                    onClick = { webViewRef?.zoomIn() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Zoom Out Button
                IconButton(
                    onClick = { webViewRef?.zoomOut() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Desktop / Mobile Mode Toggle
                IconButton(
                    onClick = {
                        isDesktopMode = !isDesktopMode
                        webViewRef?.let { wv ->
                            wv.settings.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else null
                            wv.reload()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                        contentDescription = if (isDesktopMode) "Desktop View Active" else "Mobile View Active",
                        tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Web Loading Progress Bar
        AnimatedVisibility(visible = isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = currentApp.brandColor
            )
        }

        // Hardware-Accelerated Lag-Free WebView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // Hardware acceleration for zero lag & 60fps rendering
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            loadsImagesAutomatically = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            allowContentAccess = true
                            userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else null

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                        }

                        // Enable cookies
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    currentUrl = it
                                    inputUrl = it
                                }
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let {
                                    currentUrl = it
                                    inputUrl = it
                                }
                                pageTitle = view?.title ?: currentApp.title
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val targetUri = request?.url ?: return false
                                val scheme = targetUri.scheme ?: ""
                                if (scheme == "http" || scheme == "https") {
                                    return false // Load internally in floating window
                                }
                                return try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, targetUri)
                                    context.startActivity(intent)
                                    true
                                } catch (_: Exception) {
                                    true
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                title?.let { pageTitle = it }
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customVideoView = view
                                customViewCallback = callback
                            }

                            override fun onHideCustomView() {
                                customViewCallback?.onCustomViewHidden()
                                customVideoView = null
                                customViewCallback = null
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }
                        }

                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                update = { wv ->
                    webViewRef = wv
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
