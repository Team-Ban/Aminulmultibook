package com.aminul.multibook.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aminul.multibook.data.database.AppDatabase
import com.aminul.multibook.data.model.AccountEntity
import com.aminul.multibook.data.profile.ProfileManager
import com.aminul.multibook.data.repository.AccountRepository
import com.aminul.multibook.data.util.DiagnosticLogger
import com.aminul.multibook.ui.theme.AccountColors
import com.aminul.multibook.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data object Home : Screen()
    data class Browser(val accountId: String) : Screen()
    data object Settings : Screen()
    data object DeveloperDashboard : Screen()
}

class MultiBookViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
    }

    private val repository: AccountRepository
    private val prefs = application.getSharedPreferences("multibook_prefs", Context.MODE_PRIVATE)

    val accounts: StateFlow<List<AccountEntity>>
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _activeAccountId = MutableStateFlow<String?>(null)
    val activeAccountId: StateFlow<String?> = _activeAccountId.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isDeveloperModeEnabled = MutableStateFlow(false)
    val isDeveloperModeEnabled: StateFlow<Boolean> = _isDeveloperModeEnabled.asStateFlow()

    private val _isDesktopMode = MutableStateFlow(false)
    val isDesktopMode: StateFlow<Boolean> = _isDesktopMode.asStateFlow()

    private val _zoomLevel = MutableStateFlow(100)
    val zoomLevel: StateFlow<Int> = _zoomLevel.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _isolationStatus = MutableStateFlow(ProfileManager.getIsolationDiagnostics(application))
    val isolationStatus: StateFlow<ProfileManager.IsolationStatus> = _isolationStatus.asStateFlow()

    // Browser State
    private val _webLoadProgress = MutableStateFlow(0)
    val webLoadProgress: StateFlow<Int> = _webLoadProgress.asStateFlow()

    private val _webIsLoading = MutableStateFlow(false)
    val webIsLoading: StateFlow<Boolean> = _webIsLoading.asStateFlow()

    private val _webCurrentUrl = MutableStateFlow("https://m.facebook.com/")
    val webCurrentUrl: StateFlow<String> = _webCurrentUrl.asStateFlow()

    private val _webPageTitle = MutableStateFlow("Facebook")
    val webPageTitle: StateFlow<String> = _webPageTitle.asStateFlow()

    private val _webError = MutableStateFlow<String?>(null)
    val webError: StateFlow<String?> = _webError.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = AccountRepository(db.accountDao())

        accounts = repository.accounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Restore theme
        val savedTheme = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        _themeMode.value = try {
            ThemeMode.valueOf(savedTheme)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        // Restore Developer Mode setting
        _isDeveloperModeEnabled.value = prefs.getBoolean("dev_mode_enabled", false)

        // Restore Desktop Mode setting
        _isDesktopMode.value = prefs.getBoolean("desktop_mode_default", false)

        DiagnosticLogger.info("MultiBookViewModel", "ViewModel initialized with theme: ${_themeMode.value.name}, DesktopMode: ${_isDesktopMode.value}")

        // Initialize with default account if database is completely empty
        viewModelScope.launch {
            if (repository.getAccountCount() == 0) {
                val acc1 = repository.addAccount("Personal Account", AccountColors[0])
                _activeAccountId.value = acc1.id
                DiagnosticLogger.info("MultiBookViewModel", "Initialized default account: ${acc1.name}")
            }
        }
    }

    fun finishSplash() {
        if (_currentScreen.value is Screen.Splash) {
            _currentScreen.value = Screen.Home
            DiagnosticLogger.debug("MultiBookViewModel", "Splash finished -> Navigating to Home")
        }
    }

    fun navigateToHome() {
        _isFullscreen.value = false
        _currentScreen.value = Screen.Home
    }

    fun navigateToBrowser(accountId: String) {
        _activeAccountId.value = accountId
        _currentScreen.value = Screen.Browser(accountId)
        DiagnosticLogger.info("MultiBookViewModel", "Opening browser for account ID: $accountId")
        viewModelScope.launch {
            repository.touchAccount(accountId)
        }
    }

    fun navigateToSettings() {
        _isFullscreen.value = false
        _currentScreen.value = Screen.Settings
    }

    fun navigateToDeveloperDashboard() {
        _currentScreen.value = Screen.DeveloperDashboard
        DiagnosticLogger.info("MultiBookViewModel", "Navigating to Developer Dashboard")
    }

    fun toggleDeveloperMode(enabled: Boolean) {
        _isDeveloperModeEnabled.value = enabled
        prefs.edit().putBoolean("dev_mode_enabled", enabled).apply()
        DiagnosticLogger.info("MultiBookViewModel", "Developer Mode toggled: $enabled")
    }

    fun toggleDesktopMode(explicit: Boolean? = null) {
        val next = explicit ?: !_isDesktopMode.value
        _isDesktopMode.value = next
        prefs.edit().putBoolean("desktop_mode_default", next).apply()
        DiagnosticLogger.info("MultiBookViewModel", "Desktop Mode toggled: $next")
    }

    fun setZoomLevel(level: Int) {
        val clamped = level.coerceIn(50, 200)
        _zoomLevel.value = clamped
        DiagnosticLogger.debug("MultiBookViewModel", "Zoom set to $clamped%")
    }

    fun zoomIn() {
        setZoomLevel(_zoomLevel.value + 15)
    }

    fun zoomOut() {
        setZoomLevel(_zoomLevel.value - 15)
    }

    fun resetZoom() {
        setZoomLevel(100)
    }

    fun toggleFullscreen(explicit: Boolean? = null) {
        _isFullscreen.value = explicit ?: !_isFullscreen.value
        DiagnosticLogger.debug("MultiBookViewModel", "Fullscreen toggled: ${_isFullscreen.value}")
    }

    fun addAccount(name: String, colorHex: String, openImmediately: Boolean = true) {
        viewModelScope.launch {
            val trimmed = name.trim().ifBlank {
                val count = repository.getAccountCount()
                "Account ${count + 1}"
            }
            val account = repository.addAccount(trimmed, colorHex)
            DiagnosticLogger.info("MultiBookViewModel", "Added account: ${account.name} (${account.id})")
            if (openImmediately) {
                navigateToBrowser(account.id)
            }
        }
    }

    fun renameAccount(accountId: String, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.updateName(accountId, newName)
                DiagnosticLogger.info("MultiBookViewModel", "Renamed account $accountId to: $newName")
            }
        }
    }

    fun removeAccount(accountId: String) {
        viewModelScope.launch {
            ProfileManager.clearAccountData(getApplication(), accountId)
            repository.deleteAccount(accountId)
            DiagnosticLogger.info("MultiBookViewModel", "Removed account $accountId and cleared its profile")
            if (_activeAccountId.value == accountId) {
                _activeAccountId.value = null
                _currentScreen.value = Screen.Home
            }
        }
    }

    fun clearAccountData(accountId: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            ProfileManager.clearAccountData(getApplication(), accountId) {
                onDone?.invoke()
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setWebProgress(progress: Int) {
        _webLoadProgress.value = progress
        _webIsLoading.value = progress in 1..99
    }

    fun setWebUrl(url: String) {
        _webCurrentUrl.value = url
    }

    fun setWebTitle(title: String) {
        _webPageTitle.value = title
    }

    fun setWebError(error: String?) {
        _webError.value = error
    }

    fun refreshIsolationDiagnostics() {
        _isolationStatus.value = ProfileManager.getIsolationDiagnostics(getApplication())
    }
}
