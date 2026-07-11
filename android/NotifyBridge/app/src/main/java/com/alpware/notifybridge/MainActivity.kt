package com.alpware.notifybridge

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.alpware.notifybridge.core.BridgeStateStore
import com.alpware.notifybridge.core.MacConnectionStore
import com.alpware.notifybridge.core.PairedMacStore
import com.alpware.notifybridge.model.PairedMac
import java.util.UUID
import com.alpware.notifybridge.network.DiscoveryResult
import com.alpware.notifybridge.network.MacDiscoveryManager
import com.alpware.notifybridge.network.NotificationSender
import com.alpware.notifybridge.network.SendResult
import com.alpware.notifybridge.notification.NotifyBridgeNotificationListener
import com.alpware.notifybridge.pairing.PairingPayload
import com.alpware.notifybridge.service.BridgeServiceController
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.alpware.notifybridge.core.AppFilterStore
import com.alpware.notifybridge.core.AppLanguageManager
import com.alpware.notifybridge.core.LanguageStore
import com.alpware.notifybridge.core.LocaleContextWrapper
import com.alpware.notifybridge.core.PrivacyStore
import com.alpware.notifybridge.core.ThemeStore
import com.alpware.notifybridge.model.InstalledAppItem
import com.alpware.notifybridge.network.ConnectionHealthClient
import com.alpware.notifybridge.network.ConnectionHealthResult
import com.alpware.notifybridge.network.PairingClient
import com.alpware.notifybridge.network.UnpairClient
import com.alpware.notifybridge.ui.AppFilterScreen
import com.alpware.notifybridge.ui.AppLanguageMode
import com.alpware.notifybridge.ui.AppThemeMode
import com.alpware.notifybridge.ui.HomeScreen
import com.alpware.notifybridge.ui.QrScannerScreen
import com.alpware.notifybridge.ui.SettingsScreen
import com.alpware.notifybridge.ui.languageTag
import com.alpware.notifybridge.ui.theme.NotifyBridgeTheme

/**
 * Hosts the main Compose UI and coordinates permissions, pairing, and bridge state.
 */
class MainActivity : ComponentActivity() {

    // UI state values mirrored from permissions, preferences, and pairing status.
    private val hasNotificationAccessState = mutableStateOf(false)
    private val bridgeEnabledState = mutableStateOf(false)
    private val sendResultState = mutableStateOf<SendResult?>(null)
    private val discoveryResultState = mutableStateOf<DiscoveryResult?>(null)
    private val batteryOptimizationIgnoredState = mutableStateOf(false)
    private val postNotificationPermissionGrantedState = mutableStateOf(false)
    private val cameraPermissionGrantedState = mutableStateOf(false)
    private var shouldStartQrPairingAfterCameraPermission = false
    private var macDiscoveryManager: MacDiscoveryManager? = null
    private val currentScreenState = mutableStateOf(AppScreen.Home)
    private val installedAppsState = mutableStateOf<List<InstalledAppItem>>(emptyList())
    private val showNotificationContentState = mutableStateOf(true)
    private val sendAllAppsState = mutableStateOf(true)
    private val macOnlineState = mutableStateOf(false)
    private val isRefreshingConnectionState = mutableStateOf(false)
    private val themeModeState = mutableStateOf(AppThemeMode.SYSTEM)
    private val languageModeState = mutableStateOf(AppLanguageMode.SYSTEM)
    private val pairedMacsState = mutableStateOf<List<PairedMac>>(emptyList())
    private val selectedMacState = mutableStateOf<PairedMac?>(null)

    // Refreshes permission-dependent state after the notification permission dialog closes.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshStates()
        }

    // Refreshes camera permission state after the runtime permission dialog closes.
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            refreshStates()

            if (granted && shouldStartQrPairingAfterCameraPermission) {
                shouldStartQrPairingAfterCameraPermission = false
                currentScreenState.value = AppScreen.Scanner
            } else if (!granted) {
                shouldStartQrPairingAfterCameraPermission = false
            }
        }

    override fun attachBaseContext(newBase: Context) {
        val languageMode = LanguageStore.getLanguageMode(newBase)

        super.attachBaseContext(
            LocaleContextWrapper.wrap(
                context = newBase,
                languageTag = languageMode.languageTag()
            )
        )
    }

    /**
     * Initializes app state, permissions, discovery, and the main Compose content.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        languageModeState.value = LanguageStore.getLanguageMode(this)
        AppLanguageManager.apply(languageModeState.value)

        super.onCreate(savedInstanceState)

        requestPostNotificationPermissionIfNeeded()

        refreshStates()
        refreshPairedMacs()

        themeModeState.value = ThemeStore.getThemeMode(this)

        // Keep the latest Mac discovery result available for the UI.
        macDiscoveryManager = MacDiscoveryManager(this) { result ->
            runOnUiThread {
                discoveryResultState.value = result
            }
        }

        installSplashScreen()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )

        setContent {
            NotifyBridgeTheme(
                themeMode = themeModeState.value
            ) {
                BackHandler(enabled = currentScreenState.value != AppScreen.Home) {
                    currentScreenState.value = AppScreen.Home
                }

                // Switch between top-level Compose screens without a navigation framework.
                when (currentScreenState.value) {
                    AppScreen.Home -> {
                        HomeScreen(
                            hasNotificationAccess = hasNotificationAccessState.value,
                            bridgeEnabled = bridgeEnabledState.value,
                            isMacOnline = macOnlineState.value,
                            isRefreshingConnection = isRefreshingConnectionState.value,
                            macIp = selectedMacState.value?.host.orEmpty(),
                            macPort = selectedMacState.value?.port?.toString() ?: "8787",
                            macName = selectedMacState.value?.name.orEmpty(),
                            pairingToken = selectedMacState.value?.secret.orEmpty(),
                            pairedMacs = pairedMacsState.value,
                            selectedMacId = selectedMacState.value?.id,
                            onSelectMac = { id -> PairedMacStore.select(this, id); refreshPairedMacs(); checkMacHealth() },
                            sendResult = sendResultState.value,
                            isIgnoringBatteryOptimizations = batteryOptimizationIgnoredState.value,
                            onRequestBatteryOptimizationIgnore = {
                                requestIgnoreBatteryOptimizations()
                            },
                            onRefreshConnection = {
                                checkMacHealth()
                            },
                            onScanPairingQr = {
                                startQrPairing()
                            },
                            onBridgeEnabledChanged = { enabled ->
                                BridgeStateStore.setBridgeEnabled(this, enabled)
                                bridgeEnabledState.value = enabled

                                if (enabled) {
                                    requestPostNotificationPermissionIfNeeded()
                                    BridgeServiceController.start(this)
                                    checkMacHealth()
                                } else {
                                    BridgeServiceController.stop(this)
                                }
                            },
                            onSaveMacConnection = { ip, port, token ->
                                MacConnectionStore.setMacIp(this, ip)
                                MacConnectionStore.setMacPort(this, port)
                                MacConnectionStore.setPairingToken(this, token)
                                selectedMacState.value?.let { current ->
                                    PairedMacStore.upsert(this, current.copy(host = ip, port = port.toIntOrNull() ?: 8787, secret = token))
                                    refreshPairedMacs()
                                }
                            },
                            onSendTestNotification = {
                                NotificationSender.sendTest(this) { result ->
                                    runOnUiThread {
                                        sendResultState.value = result
                                    }
                                }
                            },
                            onOpenNotificationSettings = {
                                openNotificationAccessSettings()
                            },
                            showNotificationContent = showNotificationContentState.value,
                            onShowNotificationContentChanged = { enabled ->
                                PrivacyStore.setShowNotificationContent(this, enabled)
                                refreshStates()
                            },
                            onOpenSettings = {
                                currentScreenState.value = AppScreen.Settings
                            },
                            onOpenAppFilters = {
                                currentScreenState.value = AppScreen.AppFilters
                            },
                            selectedAppFilterCount = AppFilterStore.getSelectedPackages(this).size,
                            onResetPairing = {
                                UnpairClient.unpair(this) {
                                    runOnUiThread {
                                        clearPairingLocally()
                                    }
                                }
                            }
                        )
                    }

                    AppScreen.Settings -> {
                        SettingsScreen(
                            hasNotificationAccess = hasNotificationAccessState.value,
                            isIgnoringBatteryOptimizations = batteryOptimizationIgnoredState.value,
                            bridgeEnabled = bridgeEnabledState.value,
                            macIp = MacConnectionStore.getMacIp(this),
                            macPort = MacConnectionStore.getMacPort(this),
                            macName = MacConnectionStore.getMacName(this),
                            hasPairingSecret = MacConnectionStore.getPairingToken(this).isNotBlank(),
                            onBack = {
                                currentScreenState.value = AppScreen.Home
                            },
                            onOpenNotificationSettings = {
                                openNotificationAccessSettings()
                            },
                            onRequestBatteryOptimizationIgnore = {
                                requestIgnoreBatteryOptimizations()
                            },
                            isPostNotificationPermissionGranted = postNotificationPermissionGrantedState.value,
                            isCameraPermissionGranted = cameraPermissionGrantedState.value,
                            onRequestPostNotificationPermission = {
                                requestPostNotificationPermissionIfNeeded()
                            },
                            onRequestCameraPermission = {
                                requestCameraPermissionIfNeeded()
                            },
                            onOpenUrl = { url ->
                                openUrl(url)
                            },
                            currentThemeMode = themeModeState.value,
                            onThemeModeChanged = { mode ->
                                ThemeStore.setThemeMode(this, mode)
                                themeModeState.value = mode
                            },
                            currentLanguageMode = languageModeState.value,
                            onLanguageModeChanged = { mode ->
                                LanguageStore.setLanguageMode(this, mode)
                                languageModeState.value = mode
                            },
                            onRestartApp = {
                                recreate()
                            }
                        )
                    }

                    AppScreen.AppFilters -> {
                        AppFilterScreen(
                            apps = installedAppsState.value,
                            sendAllApps = sendAllAppsState.value,
                            onBack = {
                                currentScreenState.value = AppScreen.Home
                            },
                            onSendAllAppsChanged = { enabled ->
                                AppFilterStore.setSendAllApps(this, enabled)
                                if (enabled) AppFilterStore.clearSelectedApps(this)
                                refreshStates()
                            },
                            onAppFilterChanged = { packageName, enabled ->
                                AppFilterStore.setAppEnabled(this, packageName, enabled)
                                refreshStates()
                            }
                        )
                    }

                    AppScreen.Scanner -> {
                        QrScannerScreen(
                            onBack = {
                                currentScreenState.value = AppScreen.Home
                            },
                            onQrScanned = { contents ->
                                currentScreenState.value = AppScreen.Home
                                handlePairingQr(contents)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Re-checks external permission and settings changes when the app returns to foreground.
     */
    override fun onResume() {
        super.onResume()
        refreshStates()
        checkMacHealth()
    }

    /**
     * Refreshes permission and bridge state after returning from system settings.
     */
    private fun refreshStates() {
        hasNotificationAccessState.value = hasNotificationAccess()
        bridgeEnabledState.value = BridgeStateStore.isBridgeEnabled(this)
        batteryOptimizationIgnoredState.value = isIgnoringBatteryOptimizations()
        postNotificationPermissionGrantedState.value = isPostNotificationPermissionGranted()
        cameraPermissionGrantedState.value = isCameraPermissionGranted()
        installedAppsState.value = loadInstalledApps()
        showNotificationContentState.value = PrivacyStore.shouldShowNotificationContent(this)
        sendAllAppsState.value = AppFilterStore.shouldSendAllApps(this)
    }

    /**
     * Returns whether runtime notification permission is granted on supported Android versions.
     */
    private fun isPostNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns whether the camera permission required for QR pairing is granted.
     */
    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Opens Android settings where the user can enable notification listener access.
     */
    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /**
     * Checks whether the notification listener service is enabled in Android settings.
     */
    private fun hasNotificationAccess(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )

        if (enabledListeners.isNullOrBlank()) return false

        val expectedComponentName = ComponentName(
            this,
            NotifyBridgeNotificationListener::class.java
        ).flattenToString()

        return TextUtils.split(enabledListeners, ":")
            .any { it.equals(expectedComponentName, ignoreCase = true) }
    }

    /**
     * Opens the QR scanner after ensuring camera permission is available.
     */
    private fun startQrPairing() {
        if (!isCameraPermissionGranted()) {
            shouldStartQrPairingAfterCameraPermission = true
            requestCameraPermissionIfNeeded()
            return
        }

        currentScreenState.value = AppScreen.Scanner
    }

    /**
     * Handles QR pairing results produced by the camera scanner.
     */
    private fun handlePairingQr(contents: String) {
        runCatching {
            parsePairingQr(contents)
        }.onSuccess { payload ->
            sendResultState.value = SendResult.Loading

            PairingClient.pair(
                context = this,
                host = payload.host,
                port = payload.port,
                code = payload.code,
                fingerprint = payload.fingerprint
            ) { result ->
                runOnUiThread {
                    result.onSuccess { response ->
                        if (response.type == "notifybridge_pairing_response") {
                            MacConnectionStore.setMacIp(this, response.host)
                            MacConnectionStore.setMacPort(this, response.port.toString())
                            MacConnectionStore.setPairingToken(this, response.secret)
                            MacConnectionStore.setMacName(
                                this,
                                response.name.ifBlank { payload.name ?: response.host }
                            )
                            MacConnectionStore.setMacCertFingerprint(this, payload.fingerprint)
                            PairedMacStore.upsert(
                                this,
                                PairedMac(
                                    id = UUID.randomUUID().toString(),
                                    name = response.name.ifBlank { payload.name ?: response.host },
                                    host = response.host,
                                    port = response.port,
                                    secret = response.secret,
                                    fingerprint = payload.fingerprint
                                )
                            )
                            refreshPairedMacs()

                            BridgeStateStore.setBridgeEnabled(this, true)
                            bridgeEnabledState.value = true
                            requestPostNotificationPermissionIfNeeded()
                            BridgeServiceController.start(this)
                            checkMacHealth()
                            sendResultState.value = SendResult.Success
                        } else {
                            sendResultState.value = SendResult.Error(
                                getString(R.string.qr_pairing_error_invalid_response)
                            )
                        }
                    }.onFailure {
                        sendResultState.value = SendResult.Error(
                            it.message ?: getString(R.string.qr_pairing_error_pairing_failed)
                        )
                    }
                }
            }
        }.onFailure {
            sendResultState.value = SendResult.Error(getString(R.string.qr_pairing_error_read_failed))
        }
    }

    /**
     * Parses and validates a NotifyBridge pairing QR payload.
     */
    private fun parsePairingQr(contents: String): PairingPayload {
        val uri = contents.toUri()

        if (uri.scheme != "https" || uri.host != "www.alpwarestudio.com") {
            error(getString(R.string.qr_pairing_error_invalid_qr))
        }

        if (uri.path != "/notifybridge/pair") {
            error(getString(R.string.qr_pairing_error_invalid_qr))
        }

        val host = uri.getQueryParameter("host")
            ?: error(getString(R.string.qr_pairing_error_missing_host))
        val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 8787
        val code = uri.getQueryParameter("code")
            ?: error(getString(R.string.qr_pairing_error_missing_code))
        val name = uri.getQueryParameter("name")
        val fingerprint = uri.getQueryParameter("fingerprint")
            ?: error(getString(R.string.qr_pairing_error_missing_fingerprint))

        return PairingPayload(
            host = host,
            port = port,
            code = code,
            name = name,
            fingerprint = fingerprint
        )
    }

    /**
     * Requests runtime notification permission on Android 13 and newer devices.
     */
    private fun requestPostNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Returns whether Android battery optimizations are disabled for this app.
     */
    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    /**
     * Opens the system screen that allows the app to run more reliably in the background.
     */
    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        if (isIgnoringBatteryOptimizations()) return

        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:$packageName".toUri()
        }

        startActivity(intent)
    }

    /**
     * Requests camera permission when QR pairing cannot start yet.
     */
    private fun requestCameraPermissionIfNeeded() {
        if (!isCameraPermissionGranted()) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Opens an external URL using the user's default browser.
     */
    private fun openUrl(url: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                url.toUri()
            )
        )
    }

    /**
     * Loads launchable installed apps and marks those selected for notification forwarding.
     */
    @SuppressLint("QueryPermissionsNeeded")
    private fun loadInstalledApps(): List<InstalledAppItem> {
        val selectedPackages = AppFilterStore.getSelectedPackages(this)

        return packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { appInfo ->
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .filterNot { appInfo ->
                appInfo.packageName == packageName
            }
            .map { appInfo: ApplicationInfo ->
                InstalledAppItem(
                    packageName = appInfo.packageName,
                    appName = packageManager
                        .getApplicationLabel(appInfo)
                        .toString(),
                    isEnabled = appInfo.packageName in selectedPackages
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }


    private fun refreshPairedMacs() {
        pairedMacsState.value = PairedMacStore.getAll(this)
        selectedMacState.value = PairedMacStore.getSelected(this)
    }

    private fun checkMacHealth() {
        val hasPairing = PairedMacStore.getSelected(this)?.isValid == true

        if (!hasPairing) {
            macOnlineState.value = false
            isRefreshingConnectionState.value = false
            return
        }

        isRefreshingConnectionState.value = true

        ConnectionHealthClient.check(this) { result ->
            runOnUiThread {
                isRefreshingConnectionState.value = false

                when (result) {
                    ConnectionHealthResult.Online -> {
                        macOnlineState.value = true
                    }

                    ConnectionHealthResult.Offline -> {
                        macOnlineState.value = false
                    }

                    ConnectionHealthResult.PairingInvalid -> {
                        macOnlineState.value = false
                        clearPairingLocally()
                    }
                }
            }
        }
    }

    private fun clearPairingLocally() {
        selectedMacState.value?.let { PairedMacStore.remove(this, it.id) }
        refreshPairedMacs()

        val hasRemainingDevices = pairedMacsState.value.isNotEmpty()
        BridgeStateStore.setBridgeEnabled(this, hasRemainingDevices)
        bridgeEnabledState.value = hasRemainingDevices
        if (hasRemainingDevices) BridgeServiceController.start(this) else BridgeServiceController.stop(this)

        macOnlineState.value = false
        isRefreshingConnectionState.value = false
        sendResultState.value = null
        discoveryResultState.value = null
        if (hasRemainingDevices) checkMacHealth()
    }

    /**
     * Represents the currently visible top-level screen.
     */
    private enum class AppScreen {
        Home,
        Settings,
        AppFilters,
        Scanner
    }
}