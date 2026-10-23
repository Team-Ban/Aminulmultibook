package com.aminul.multibook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aminul.multibook.ui.screen.BrowserScreen
import com.aminul.multibook.ui.screen.DeveloperDashboardScreen
import com.aminul.multibook.ui.screen.HomeScreen
import com.aminul.multibook.ui.screen.SettingsScreen
import com.aminul.multibook.ui.screen.SplashScreen
import com.aminul.multibook.ui.theme.MultiBookTheme
import com.aminul.multibook.ui.viewmodel.MultiBookViewModel
import com.aminul.multibook.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: MultiBookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val accounts by viewModel.accounts.collectAsStateWithLifecycle()
            val activeAccountId by viewModel.activeAccountId.collectAsStateWithLifecycle()
            val isolationStatus by viewModel.isolationStatus.collectAsStateWithLifecycle()
            val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled.collectAsStateWithLifecycle()
            val isDesktopMode by viewModel.isDesktopMode.collectAsStateWithLifecycle()
            val zoomLevel by viewModel.zoomLevel.collectAsStateWithLifecycle()
            val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
            val webProgress by viewModel.webLoadProgress.collectAsStateWithLifecycle()
            val webIsLoading by viewModel.webIsLoading.collectAsStateWithLifecycle()

            MultiBookTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            is Screen.Splash -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        viewModel.finishSplash()
                                    }
                                )
                            }

                            is Screen.Home -> {
                                HomeScreen(
                                    accounts = accounts,
                                    activeAccountId = activeAccountId,
                                    isolationStatus = isolationStatus,
                                    themeMode = themeMode,
                                    onOpenAccount = { accId ->
                                        viewModel.navigateToBrowser(accId)
                                    },
                                    onAddAccount = { name, colorHex ->
                                        viewModel.addAccount(name, colorHex)
                                    },
                                    onRenameAccount = { accId, newName ->
                                        viewModel.renameAccount(accId, newName)
                                    },
                                    onRemoveAccount = { accId ->
                                        viewModel.removeAccount(accId)
                                    },
                                    onClearAccountData = { accId ->
                                        viewModel.clearAccountData(accId)
                                    },
                                    onToggleTheme = {
                                        val next = when (themeMode) {
                                            com.aminul.multibook.ui.theme.ThemeMode.SYSTEM -> com.aminul.multibook.ui.theme.ThemeMode.LIGHT
                                            com.aminul.multibook.ui.theme.ThemeMode.LIGHT -> com.aminul.multibook.ui.theme.ThemeMode.DARK
                                            com.aminul.multibook.ui.theme.ThemeMode.DARK -> com.aminul.multibook.ui.theme.ThemeMode.SYSTEM
                                        }
                                        viewModel.setThemeMode(next)
                                    },
                                    onNavigateToSettings = {
                                        viewModel.navigateToSettings()
                                    }
                                )
                            }

                            is Screen.Browser -> {
                                val currentAccount = accounts.firstOrNull { it.id == screen.accountId }
                                if (currentAccount != null) {
                                    BrowserScreen(
                                        account = currentAccount,
                                        progress = webProgress,
                                        isLoading = webIsLoading,
                                        isDesktopMode = isDesktopMode,
                                        zoomLevel = zoomLevel,
                                        isFullscreen = isFullscreen,
                                        onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                                        onZoomIn = { viewModel.zoomIn() },
                                        onZoomOut = { viewModel.zoomOut() },
                                        onResetZoom = { viewModel.resetZoom() },
                                        onToggleFullscreen = { viewModel.toggleFullscreen() },
                                        onProgressChange = { p -> viewModel.setWebProgress(p) },
                                        onUrlChange = { url -> viewModel.setWebUrl(url) },
                                        onTitleChange = { title -> viewModel.setWebTitle(title) },
                                        onNavigateHome = { viewModel.navigateToHome() }
                                    )
                                } else {
                                    viewModel.navigateToHome()
                                }
                            }

                            is Screen.Settings -> {
                                SettingsScreen(
                                    themeMode = themeMode,
                                    isolationStatus = isolationStatus,
                                    isDeveloperModeEnabled = isDeveloperModeEnabled,
                                    isDesktopMode = isDesktopMode,
                                    onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
                                    onToggleDeveloperMode = { enabled -> viewModel.toggleDeveloperMode(enabled) },
                                    onToggleDesktopMode = { enabled -> viewModel.toggleDesktopMode(enabled) },
                                    onNavigateToDeveloperDashboard = { viewModel.navigateToDeveloperDashboard() },
                                    onNavigateBack = { viewModel.navigateToHome() }
                                )
                            }

                            is Screen.DeveloperDashboard -> {
                                DeveloperDashboardScreen(
                                    onNavigateBack = { viewModel.navigateToSettings() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
