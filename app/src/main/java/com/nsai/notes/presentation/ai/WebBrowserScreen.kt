package com.nsai.notes.presentation.ai

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nsai.notes.domain.model.SearchEngine
import androidx.compose.ui.viewinterop.AndroidView
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebBrowserDialog(
    initialUrl: String = "",
    searchEngine: String = "GOOGLE",
    searchEngineCustomUrl: String = "",
    bookmarksTitles: List<String> = emptyList(),
    bookmarkUrls: List<String> = emptyList(),
    searchHistory: List<String> = emptyList(),
    onSearchEngineChange: (String) -> Unit = {},
    onSearchEngineCustomUrlChange: (String) -> Unit = {},
    onAddBookmark: (title: String, url: String) -> Unit = { _, _ -> },
    onRemoveBookmark: (url: String) -> Unit = {},
    onAddSearchHistory: (query: String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val tokens = LocalAnimationConfig.current
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var inputUrl by remember { mutableStateOf(initialUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("") }
    var desktopMode by remember { mutableStateOf(false) }
    var showHome by remember { mutableStateOf(true) }
    var swipeOffset by remember { mutableStateOf(0f) }
    var swipeTarget by remember { mutableStateOf(0f) }
    var edgeArmed by remember { mutableStateOf(false) }
    var navigatingBack by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }

    val animatedOffset by animateFloatAsState(
        targetValue = if (edgeArmed || swipeOffset > 20f) swipeTarget.coerceAtLeast(0f) else -100f,
        animationSpec = spring(dampingRatio = tokens.springDamping, stiffness = tokens.springStiffness),
        label = "swipeIndicator"
    )

    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val minDimension = minOf(config.screenWidthDp, config.screenHeightDp)
    val deviceCategory = when {
        minDimension >= 840 -> "tablet_large"
        minDimension >= 600 -> "tablet"
        else -> "phone"
    }
    LaunchedEffect(Unit) { if (deviceCategory == "tablet_large") desktopMode = true }
    val isWideDevice = deviceCategory != "phone"

    // Home page state
    var homeQuery by remember { mutableStateOf("") }
    var homeEngine by remember { mutableStateOf(SearchEngine.fromName(searchEngine)) }
    var homeCustomUrl by remember { mutableStateOf(searchEngineCustomUrl) }
    val homeHistory = remember { mutableStateListOf<String>() }

    // Sync persisted history into local list on first load
    LaunchedEffect(searchHistory) {
        homeHistory.clear()
        homeHistory.addAll(searchHistory)
    }

    fun buildSearchUrl(query: String, eng: SearchEngine, custom: String): String {
        if (query.isBlank()) return eng.homepage()
        if (query.startsWith("http://") || query.startsWith("https://")) return query
        if (query.contains('.') && !query.contains(' ')) return "https://$query"
        return eng.buildUrl(query, custom)
    }

    fun navigateTo(url: String) {
        currentUrl = url; inputUrl = url; showHome = false
        webView?.loadUrl(url)
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.clearCache(true)
            webView?.clearHistory()
            webView?.destroy()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Toolbar Row 1: Navigation + URL ──
            val displayUrl = when {
                showHome || currentUrl.isBlank() -> "首页"
                else -> currentUrl.removePrefix("https://").removePrefix("http://")
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 10.dp)
                    .then(if (isWideDevice) Modifier.padding(start = 40.dp, end = 40.dp) else Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { webView?.goBack() }, Modifier.size(36.dp), enabled = canGoBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "后退", Modifier.size(22.dp),
                        tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                }
                IconButton(onClick = { webView?.goForward() }, Modifier.size(36.dp), enabled = canGoForward) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "前进", Modifier.size(22.dp),
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                }
                OutlinedTextField(
                    value = if (showHome) homeQuery else inputUrl,
                    onValueChange = { if (showHome) homeQuery = it else inputUrl = it },
                    modifier = Modifier.weight(1f).height(52.dp),
                    singleLine = true, textStyle = MaterialTheme.typography.bodySmall,
                    placeholder = { Text(if (showHome) "搜索或输入网址..." else displayUrl, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = {
                    val q = if (showHome) homeQuery else inputUrl.ifBlank { displayUrl }
                    val target = if (showHome) buildSearchUrl(q, homeEngine, homeCustomUrl) else resolveUrl(q, homeEngine, homeCustomUrl)
                    if (showHome && q.isNotBlank()) {
                        onAddSearchHistory(q)
                        if (!homeHistory.contains(q)) { homeHistory.add(0, q); if (homeHistory.size > 8) homeHistory.removeLast() }
                    }
                    navigateTo(target)
                }, Modifier.size(36.dp)) {
                    Icon(Icons.Default.Search, "前往", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { webView?.reload() }, Modifier.size(36.dp)) {
                    Icon(Icons.Default.Refresh, "刷新", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, Modifier.size(36.dp)) {
                    Icon(Icons.Default.AutoAwesome, "返回AI", Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            // ── Toolbar Row 2: Actions ──
            if (!showHome) Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                    .then(if (isWideDevice) Modifier.padding(start = 40.dp, end = 40.dp) else Modifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showHome = true; homeQuery = "" }, Modifier.size(30.dp)) {
                        Icon(Icons.Default.Home, "主页", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val isBookmarked = bookmarkUrls.any { it == currentUrl }
                    IconButton(onClick = {
                        if (isBookmarked) onRemoveBookmark(currentUrl)
                        else onAddBookmark(pageTitle.ifBlank { currentUrl }, currentUrl)
                    }, Modifier.size(30.dp)) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "移除书签" else "添加书签",
                            Modifier.size(18.dp),
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(pageTitle.ifBlank { "" }, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
                    IconButton(onClick = {
                        desktopMode = !desktopMode; webView?.let { applyViewMode(it, desktopMode) }
                    }, Modifier.size(30.dp)) {
                        Icon(if (desktopMode) Icons.Default.Tablet else Icons.Default.PhoneAndroid,
                            "切换", Modifier.size(18.dp),
                            tint = if (desktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss, Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, "关闭", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (isLoading) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // ── Content: Home Page or WebView ──
            if (showHome) {
                BrowserHomePage(
                    query = homeQuery,
                    onQueryChange = { homeQuery = it },
                    engine = homeEngine,
                    onEngineChange = {
                        homeEngine = it
                        onSearchEngineChange(it.name)
                    },
                    customUrl = homeCustomUrl,
                    onCustomUrlChange = {
                        homeCustomUrl = it
                        onSearchEngineCustomUrlChange(it)
                    },
                    history = homeHistory,
                    onClearHistory = {
                        homeHistory.clear()
                        onClearSearchHistory()
                    },
                    bookmarksTitles = bookmarksTitles,
                    bookmarkUrls = bookmarkUrls,
                    onDeleteBookmark = onRemoveBookmark,
                    onSearch = { q, eng -> navigateTo(buildSearchUrl(q, eng, homeCustomUrl)) },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webView = this

                            var touchStartX = 0f
                            var tracking = false
                            setOnTouchListener { _, event ->
                                when (event.action) {
                                    android.view.MotionEvent.ACTION_DOWN -> {
                                        touchStartX = event.x
                                        tracking = touchStartX < 40f && canGoBack
                                        swipeOffset = 0f; swipeTarget = 0f; edgeArmed = false
                                    }
                                    android.view.MotionEvent.ACTION_MOVE -> {
                                        if (tracking) {
                                            val dx = event.x - touchStartX
                                            val dy = kotlin.math.abs(event.y - touchStartX)
                                            if (dy > dx + 20f) { tracking = false; swipeOffset = 0f; swipeTarget = 0f }
                                            else if (dx > 10f) {
                                                swipeOffset = dx.coerceAtMost(swipeThresholdPx * 2f)
                                                swipeTarget = swipeOffset; edgeArmed = true
                                                return@setOnTouchListener true
                                            }
                                        }
                                    }
                                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                        if (tracking && swipeOffset > swipeThresholdPx) {
                                            edgeArmed = false; navigatingBack = true
                                            postDelayed({ goBack(); navigatingBack = false }, 250)
                                        }
                                        tracking = false; swipeOffset = 0f; swipeTarget = 0f
                                    }
                                }
                                false
                            }

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.setSupportZoom(true)
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.allowFileAccess = false
                            // Reduce renderer memory pressure on heavy pages like Zhihu
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            // Replace WebView UA with Chrome mobile UA — Baidu and many Chinese
                            // sites detect and block WebView user agents, returning empty pages.
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; en-US) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.179 Mobile Safari/537.36"

                            // Inject anti-detection JS: Bilibili and other Chinese sites check
                            // navigator.webdriver, window.chrome, etc. to detect WebView
                            val antiDetectJs = """
                                (function(){
                                    delete navigator.__proto__.webdriver;
                                    Object.defineProperty(navigator, 'webdriver', {get:function(){return false}});
                                    window.chrome = window.chrome || {runtime:{},loadTimes:function(){},csi:function(){}};
                                    ['plugins','mimeTypes'].forEach(function(p){
                                        Object.defineProperty(navigator, p, {get:function(){return {length:1}}})
                                    });
                                })();
                            """.trimIndent()
                            // Baidu mobile uses window.open() for search result links
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            applyViewMode(this, desktopMode)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: ""
                                    val scheme = request?.url?.scheme ?: ""
                                    // Block localhost redirects (Baidu ad tracking) and custom
                                    // app schemes like baiduboxapp://, intent:// which the
                                    // WebView cannot handle — they cause ERR_UNKNOWN_URL_SCHEME.
                                    if (url.contains("127.0.0.1") || url.contains("localhost")) {
                                        return true
                                    }
                                    if (scheme != "http" && scheme != "https" && scheme != "javascript" && scheme != "data") {
                                        return true
                                    }
                                    return false
                                }
                                override fun onReceivedError(
                                    view: WebView?, request: android.webkit.WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    // Do NOT retry — retrying the same failing URL creates infinite loops.
                                    // The WebView's built-in error page will show the error to the user.
                                }
                                override fun onReceivedSslError(
                                    view: WebView?, handler: android.webkit.SslErrorHandler?,
                                    error: android.net.http.SslError?
                                ) {
                                    handler?.cancel()
                                }
                                override fun onPageStarted(view: WebView?, u: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    u?.let { currentUrl = it; inputUrl = it }
                                    // Inject anti-WebView-detection JS before page content loads
                                    view?.evaluateJavascript(antiDetectJs, null)
                                }
                                override fun onPageFinished(view: WebView?, u: String?) {
                                    isLoading = false
                                    canGoBack = view?.canGoBack() ?: false
                                    canGoForward = view?.canGoForward() ?: false
                                    pageTitle = view?.title ?: ""
                                }
                                // Renderer process crashed — show toast and return to home
                                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                    if (detail?.didCrash() == true) {
                                        android.widget.Toast.makeText(
                                            context, "页面崩溃，已返回主页", android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        webView?.destroy()
                                        webView = null
                                        showHome = true
                                    }
                                    return true
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) { pageTitle = title ?: "" }
                                // Create a lightweight dummy WebView for window.open() calls
                                // (Baidu, Zhihu, etc.). Reusing the parent WebView causes JS
                                // state corruption and renderer crashes on sites like Zhihu.
                                override fun onCreateWindow(
                                    view: WebView?, isDialog: Boolean, isUserGesture: Boolean,
                                    resultMsg: android.os.Message?
                                ): Boolean {
                                    if (resultMsg == null) return false
                                    val ctx = view?.context ?: context
                                    val dummy = WebView(ctx)
                                    val parent = view ?: webView ?: return false
                                    dummy.webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(v: WebView?, req: android.webkit.WebResourceRequest?): Boolean {
                                            req?.url?.toString()?.let { parent.loadUrl(it) }
                                            return true
                                        }
                                    }
                                    (resultMsg.obj as? WebView.WebViewTransport)?.webView = dummy
                                    resultMsg.sendToTarget()
                                    return true
                                }
                            }
                            // Load the correct initial URL: if the user just searched,
                            // navigateTo has set currentUrl to the search URL — use that
                            // instead of initialUrl (which is always the homepage).
                            val launchUrl = when {
                                currentUrl.isNotBlank() && currentUrl != initialUrl -> currentUrl
                                initialUrl.isNotBlank() -> initialUrl
                                else -> SearchEngine.fromName(searchEngine).homepage()
                            }
                            loadUrl(launchUrl)
                        }
                    },
                    update = {},
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .then(if (isWideDevice) Modifier.padding(horizontal = 40.dp) else Modifier)
                )
            }
        }

        // Edge swipe indicator
        if (canGoBack && edgeArmed) {
            val progress = (swipeOffset / swipeThresholdPx).coerceIn(0.12f, 1f)
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
                    .offset { IntOffset(animatedOffset.toInt() - 48, 0) }.size(40.dp).clip(CircleShape)
                    .background(if (swipeOffset > swipeThresholdPx * 0.8f) MaterialTheme.colorScheme.primary.copy(alpha = progress)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", Modifier.size(22.dp),
                    tint = if (swipeOffset > swipeThresholdPx * 0.8f) Color.White
                    else MaterialTheme.colorScheme.primary.copy(alpha = progress))
            }
        }

        AnimatedVisibility(
            visible = navigatingBack,
            enter = fadeIn(tween(tokens.fastDuration)) + scaleIn(initialScale = 1.4f, animationSpec = spring(dampingRatio = tokens.springDamping)),
            exit = fadeOut(tween(tokens.fastDuration)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", Modifier.size(32.dp), tint = Color.White)
            }
        }
    }
}

// ── Compose Home Page ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowserHomePage(
    query: String,
    onQueryChange: (String) -> Unit,
    engine: SearchEngine,
    onEngineChange: (SearchEngine) -> Unit,
    customUrl: String,
    onCustomUrlChange: (String) -> Unit,
    history: List<String>,
    onClearHistory: () -> Unit,
    bookmarksTitles: List<String>,
    bookmarkUrls: List<String>,
    onDeleteBookmark: (String) -> Unit,
    onSearch: (String, SearchEngine) -> Unit,
    modifier: Modifier = Modifier
) {
    val engines = SearchEngine.entries.toList()
    val shortcuts = listOf(
        Triple("GitHub", "https://github.com", Color(0xFF24292e)),
        Triple("Wikipedia", "https://wikipedia.org", Color(0xFF444444)),
        Triple("知乎", "https://zhihu.com", Color(0xFF0066FF)),
        Triple("B站", "https://bilibili.com", Color(0xFFFB7299)),
        Triple("翻译", "https://translate.google.com", Color(0xFF4285F4))
    )

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        // Logo
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
            )), contentAlignment = Alignment.Center) {
            Text("N", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text("NSAI 浏览器", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("安全 · 快速 · 智能", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

        Spacer(Modifier.height(32.dp))

        // Search box
        OutlinedTextField(
            value = query, onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索或输入网址...") },
            singleLine = true, shape = RoundedCornerShape(20.dp),
            trailingIcon = {
                IconButton(onClick = { if (query.isNotBlank()) onSearch(query, engine) }) {
                    Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        )

        Spacer(Modifier.height(16.dp))

        // Engine chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            engines.forEach { eng ->
                FilterChip(
                    selected = engine == eng,
                    onClick = { onEngineChange(eng) },
                    label = { Text(eng.displayName, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Custom URL
        if (engine == SearchEngine.CUSTOM) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = customUrl, onValueChange = onCustomUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索 URL 模板（{query}=搜索词）") },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(36.dp))

        // Shortcuts
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            shortcuts.forEach { (name, url, color) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).clickable { onSearch(url, engine) }
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(color),
                        contentAlignment = Alignment.Center) {
                        Text(name.take(1), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
        }


        // Bookmarks
        if (bookmarkUrls.isNotEmpty()) {
            Spacer(Modifier.height(36.dp))
            Text("书签", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val count = minOf(bookmarkUrls.size, bookmarksTitles.size)
                for (i in 0 until count.coerceAtMost(10)) {
                    val title = bookmarksTitles[i]
                    val url = bookmarkUrls[i]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            onClick = { onSearch(url, engine) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                title.ifBlank { url },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onDeleteBookmark(url) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, "删除书签", Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                        }
                    }
                }
            }
        }

        // History
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(36.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("最近搜索", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                TextButton(onClick = onClearHistory) { Text("清空") }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                history.take(6).forEach { h ->
                    Card(
                        onClick = { onSearch(h, engine) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(h, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

private enum class DeviceCategory { PHONE, TABLET, LARGE_TABLET }

private fun applyViewMode(webView: WebView, desktopMode: Boolean) {
    val settings = webView.settings
    if (desktopMode) {
        settings.useWideViewPort = true; settings.loadWithOverviewMode = true; settings.textZoom = 100
        val ua = settings.userAgentString
        settings.userAgentString = ua.replace("; wv", "; X11; Linux x86_64")
            .replace("Mobile ", "").replace("Android", "X11; Linux x86_64")
    } else {
        settings.userAgentString = null; settings.useWideViewPort = false
        settings.loadWithOverviewMode = true; settings.textZoom = 100
    }
}

private fun resolveUrl(input: String, searchEngine: SearchEngine = SearchEngine.BAIDU, customUrl: String = ""): String {
    if (input.isBlank()) return searchEngine.homepage()
    return when {
        input.startsWith("http://") || input.startsWith("https://") -> input
        input.contains('.') && !input.contains(' ') ->
            if (!input.startsWith("http")) "https://$input" else input
        else -> searchEngine.buildUrl(input, customUrl)
    }
}
