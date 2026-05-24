# =========================================================
# NotifyBridge - ProGuard / R8 Rules
# =========================================================

# ---------------------------------------------------------
# App model classes used by Gson
# ---------------------------------------------------------
-keep class com.alpware.notifybridge.notification.NotificationPayload { *; }
-keep class com.alpware.notifybridge.network.EncryptedPayload { *; }
-keep class com.alpware.notifybridge.pairing.PairingPayload { *; }

# Gson generic metadata
-keepattributes Signature
-keepattributes *Annotation*

# ---------------------------------------------------------
# Android services
# ---------------------------------------------------------
-keep class com.alpware.notifybridge.notification.NotifyBridgeNotificationListener { *; }
-keep class com.alpware.notifybridge.tile.NotifyBridgeTileService { *; }
-keep class com.alpware.notifybridge.service.BridgeForegroundService { *; }

# ---------------------------------------------------------
# ZXing / JourneyApps QR Scanner
# ---------------------------------------------------------
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.barcodescanner.**

# ---------------------------------------------------------
# CameraX
# ---------------------------------------------------------
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ---------------------------------------------------------
# ML Kit Barcode Scanning / Google Play Services
# ---------------------------------------------------------
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.odml.** { *; }
-keep class com.google.firebase.components.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.odml.**
-dontwarn com.google.firebase.components.**

# ---------------------------------------------------------
# Gson
# ---------------------------------------------------------
-dontwarn com.google.gson.**

# Keep fields annotated with SerializedName, if used later.
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---------------------------------------------------------
# Kotlin
# ---------------------------------------------------------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ---------------------------------------------------------
# AndroidX / Compose
# ---------------------------------------------------------
-dontwarn androidx.compose.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.activity.**

# ---------------------------------------------------------
# Logging
# ---------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ---------------------------------------------------------
# Keep public app entry points
# ---------------------------------------------------------
-keep class com.alpware.notifybridge.MainActivity { *; }

# ---------------------------------------------------------
# Optional: Keep stores/controllers if reflection or debugging is needed
# ---------------------------------------------------------
-keep class com.alpware.notifybridge.core.** { *; }
-keep class com.alpware.notifybridge.network.CryptoManager { *; }
-keep class com.alpware.notifybridge.network.NotificationSender { *; }