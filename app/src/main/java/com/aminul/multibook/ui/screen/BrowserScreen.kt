package com.aminul.multibook.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aminul.multibook.data.model.AccountEntity
import com.aminul.multibook.data.profile.ProfileManager
import com.aminul.multibook.ui.components.FbStateDialog
import com.aminul.multibook.ui.theme.FacebookBlue
import com.aminul.multibook.ui.theme.RedDanger
import com.aminul.multibook.ui.viewmodel.MultiBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    account: AccountEntity,
    progress: Int,
    isLoading: Boolean,
    isDesktopMode: Boolean,
    zoomLevel: Int,
    isFullscreen: Boolean,
    onToggleDesktopMode: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onProgressChange: (Int) -> Unit,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var showFbStateDialog by remember { mutableStateOf(false) }
    var defaultMobileUa by remember { mutableStateOf<String?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showZoomMenu by remember { mutableStateOf(false) }

    // File upload picker support
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    // Hardware back navigation
    BackHandler {
        if (isFullscreen) {
            onToggleFullscreen()
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateHome()
        }
    }

    // React to Desktop Mode changes without losing session
    LaunchedEffect(isDesktopMode) {
        webViewInstance?.let { web ->
            val targetUa = if (isDesktopMode) {
                MultiBookViewModel.DESKTOP_USER_AGENT
            } else {
                defaultMobileUa ?: web.settings.userAgentString
            }
            web.settings.userAgentString = targetUa
            web.settings.useWideViewPort = true
            web.settings.loadWithOverviewMode = true

            // Adapt URL to desktop/mobile domain while keeping query parameters and sessions
            val current = web.url ?: ""
            if (isDesktopMode && current.contains("m.facebook.com")) {
                val desktopUrl = current.replace("m.facebook.com", "www.facebook.com")
                web.loadUrl(desktopUrl)
            } else if (!isDesktopMode && current.contains("www.facebook.com")) {
                val mobileUrl = current.replace("www.facebook.com", "m.facebook.com")
                web.loadUrl(mobileUrl)
            } else if (current.isNotEmpty()) {
                web.reload()
            }
        }
    }

    // React to Zoom changes
    LaunchedEffect(zoomLevel) {
        webViewInstance?.settings?.textZoom = zoomLevel
    }

    val avatarColor = remember(account.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(account.colorHex))
        } catch (_: Exception) {
            FacebookBlue
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!isFullscreen) {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(avatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = account.name.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isDesktopMode) "Desktop Mode • $zoomLevel%" else "Mobile Mode • $zoomLevel%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDesktopMode) FacebookBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateHome) {
                                Icon(Icons.Default.Home, contentDescription = "Home")
                            }
                        },
                        actions = {
                            // Back / Forward
                            IconButton(
                                onClick = { webViewInstance?.goBack() },
                                enabled = canGoBack
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            IconButton(
                                onClick = { webViewInstance?.goForward() },
                                enabled = canGoForward
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                            }

                            // Desktop Mode Toggle Button
                            IconButton(onClick = onToggleDesktopMode) {
                                Icon(
                                    imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                                    contentDescription = if (isDesktopMode) "Switch to Mobile Mode" else "Switch to Desktop Mode",
                                    tint = if (isDesktopMode) FacebookBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Zoom Controls Menu
                            Box {
                                IconButton(onClick = { showZoomMenu = true }) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom Controls")
                                }
                                DropdownMenu(
                                    expanded = showZoomMenu,
                                    onDismissRequest = { showZoomMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Zoom In (+15%)") },
                                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                        onClick = {
                                            onZoomIn()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Zoom Out (-15%)") },
                                        leadingIcon = { Icon(Icons.Default.Remove, contentDescription = null) },
                                        onClick = {
                                            onZoomOut()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Reset Zoom (100%)") },
                                        onClick = {
                                            onResetZoom()
                                            showZoomMenu = false
                                        }
                                    )
                                }
                            }

                            // c3c-fbstate Extension
                            IconButton(onClick = { showFbStateDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = "c3c-fbstate Extension",
                                    tint = FacebookBlue
                                )
                            }

                            // Fullscreen Mode Toggle
                            IconButton(onClick = onToggleFullscreen) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen Mode")
                            }

                            // Refresh
                            IconButton(onClick = {
                                hasError = false
                                webViewInstance?.reload()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = FacebookBlue,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            AndroidView(
                factory = { ctx: Context ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Store original mobile UA
                        if (defaultMobileUa == null) {
                            defaultMobileUa = settings.userAgentString
                        }

                        // Session isolation configuration
                        ProfileManager.applyProfileToWebView(this, account.id)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            loadsImagesAutomatically = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            textZoom = zoomLevel
                            allowFileAccess = false
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            userAgentString = if (isDesktopMode) MultiBookViewModel.DESKTOP_USER_AGENT else (defaultMobileUa ?: userAgentString)
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let { onUrlChange(it) }
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let { onUrlChange(it) }
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    errorMessage = error?.description?.toString() ?: "Network connection error"
                                }
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                hasError = true
                                errorMessage = "WebView renderer process stopped. Reloading session..."
                                view?.reload()
                                return true
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                onProgressChange(newProgress)
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                title?.let { onTitleChange(it) }
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallbackIn: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = filePathCallbackIn
                                filePickerLauncher.launch("*/*")
                                return true
                            }
                        }

                        val startUrl = if (isDesktopMode) "https://www.facebook.com/" else "https://m.facebook.com/"
                        loadUrl(startUrl)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating Controls when in Fullscreen Mode
            AnimatedVisibility(
                visible = isFullscreen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleDesktopMode,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                                contentDescription = "Mode",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Error Overlay
            if (hasError) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RedDanger,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Could Not Connect to Facebook",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Please check your internet connection and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    hasError = false
                                    webViewInstance?.reload()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                            ) {
                                Text("Retry")
                            }
                            Button(
                                onClick = onNavigateHome,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Back to Home")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFbStateDialog) {
        FbStateDialog(
            accountId = account.id,
            accountName = account.name,
            onDismiss = { showFbStateDialog = false },
            onCookiesInjected = {
                webViewInstance?.reload()
            }
        )
    }

    DisposableEffect(account.id) {
        onDispose {
            webViewInstance?.stopLoading()
        }
    }
}
