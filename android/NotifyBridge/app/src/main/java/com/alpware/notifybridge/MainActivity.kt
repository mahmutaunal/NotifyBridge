package com.alpware.notifybridge

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.alpware.notifybridge.core.BridgeStateStore
import com.alpware.notifybridge.core.MacConnectionStore
import com.alpware.notifybridge.network.DiscoveryResult
import com.alpware.notifybridge.network.MacDiscoveryManager
import com.alpware.notifybridge.network.NotificationSender
import com.alpware.notifybridge.network.SendResult
import com.alpware.notifybridge.notification.NotifyBridgeNotificationListener
import com.alpware.notifybridge.pairing.PairingPayload
import com.alpware.notifybridge.service.BridgeServiceController
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.alpware.notifybridge.ui.HomeScreen
import com.alpware.notifybridge.ui.SettingsScreen
import com.alpware.notifybridge.ui.theme.NotifyBridgeTheme

/**
 * Hosts the main Compose UI and coordinates permissions, pairing, and bridge state.
 */
class MainActivity : ComponentActivity() {

    private val hasNotificationAccessState = mutableStateOf(false)
    private val bridgeEnabledState = mutableStateOf(false)
    private val sendResultState = mutableStateOf<SendResult?>(null)
    private val discoveryResultState = mutableStateOf<DiscoveryResult?>(null)
    private val batteryOptimizationIgnoredState = mutableStateOf(false)
    private val postNotificationPermissionGrantedState = mutableStateOf(false)
    private val cameraPermissionGrantedState = mutableStateOf(false)
    private var macDiscoveryManager: MacDiscoveryManager? = null
    private val currentScreenState = mutableStateOf(AppScreen.Home)

    // Handles QR pairing results produced by the camera scanner.
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@registerForActivityResult

        runCatching {
            Gson().fromJson(contents, PairingPayload::class.java)
        }.onSuccess { payload ->
            if (payload.type == "notifybridge_pairing") {
                MacConnectionStore.setMacIp(this, payload.host)
                MacConnectionStore.setMacPort(this, payload.port.toString())
                MacConnectionStore.setPairingToken(this, payload.secret)
                MacConnectionStore.setMacName(this, payload.name?.takeIf { it.isNotBlank() } ?: payload.host)
                sendResultState.value = SendResult.Success
            } else {
                sendResultState.value = SendResult.Error(getString(R.string.qr_pairing_error_invalid_code))
            }
        }.onFailure {
            sendResultState.value = SendResult.Error(getString(R.string.qr_pairing_error_read_failed))
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshStates()
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshStates()
        }

    /**
     * Initializes app state, permissions, discovery, and the main Compose content.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPostNotificationPermissionIfNeeded()

        refreshStates()

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
            NotifyBridgeTheme {
                when (currentScreenState.value) {
                    AppScreen.Home -> {
                        HomeScreen(
                            hasNotificationAccess = hasNotificationAccessState.value,
                            bridgeEnabled = bridgeEnabledState.value,
                            macIp = MacConnectionStore.getMacIp(this),
                            macPort = MacConnectionStore.getMacPort(this),
                            macName = MacConnectionStore.getMacName(this),
                            pairingToken = MacConnectionStore.getPairingToken(this),
                            sendResult = sendResultState.value,
                            isIgnoringBatteryOptimizations = batteryOptimizationIgnoredState.value,
                            onRequestBatteryOptimizationIgnore = {
                                requestIgnoreBatteryOptimizations()
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
                                } else {
                                    BridgeServiceController.stop(this)
                                }
                            },
                            onSaveMacConnection = { ip, port, token ->
                                MacConnectionStore.setMacIp(this, ip)
                                MacConnectionStore.setMacPort(this, port)
                                MacConnectionStore.setPairingToken(this, token)
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
                            onOpenSettings = {
                                currentScreenState.value = AppScreen.Settings
                            },
                            onResetPairing = {
                                MacConnectionStore.setMacIp(this, "")
                                MacConnectionStore.setMacPort(this, "8787")
                                MacConnectionStore.setPairingToken(this, "")
                                MacConnectionStore.setMacName(this, "")
                                sendResultState.value = null
                                discoveryResultState.value = null
                            },
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
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStates()
    }

    /**
     * Refreshes permission and bridge state after returning from system settings.
     */
    private fun refreshStates() {
        hasNotificationAccessState.value = hasNotificationAccess()

        bridgeEnabledState.value =
            BridgeStateStore.isBridgeEnabled(this)

        batteryOptimizationIgnoredState.value =
            isIgnoringBatteryOptimizations()

        postNotificationPermissionGrantedState.value =
            isPostNotificationPermissionGranted()

        cameraPermissionGrantedState.value =
            isCameraPermissionGranted()
    }

    private fun isPostNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

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
            requestCameraPermissionIfNeeded()
            return
        }

        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(getString(R.string.qr_pairing_scanner_prompt))
            .setBeepEnabled(false)
            .setOrientationLocked(false)

        qrScannerLauncher.launch(options)
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
     * Represents the currently visible top-level screen.
     */
    private enum class AppScreen {
        Home,
        Settings
    }
}