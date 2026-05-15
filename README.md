# NotifyBridge

NotifyBridge securely forwards Android notifications to your Mac over your local network.

It is designed as a privacy-first, local-first, open-source utility. Notifications are not sent to any cloud service.

## Features

- Android notification forwarding to macOS
- Native Android app built with Kotlin and Jetpack Compose
- Native macOS menu bar app built with SwiftUI
- QR-based pairing
- AES-256-GCM encrypted payloads
- HMAC-SHA256 request signing
- Quick Settings tile on Android
- Foreground service for stable background operation
- Launch at login support on macOS
- Local network only, no cloud dependency

## Project Structure

```text
NotifyBridge/
├─ android/
│  └─ NotifyBridge/
├─ macos/
│  └─ NotifyBridgeMac/
├─ docs/
│  ├─ PAIRING.md
│  ├─ SECURITY.md
│  └─ TROUBLESHOOTING.md
├─ README.md
├─ LICENSE
└─ .gitignore
```

## How It Works

1. The Mac app runs a local listener on port `8787`.
2. The Mac app shows a QR code containing pairing data.
3. The Android app scans the QR code and stores the Mac connection details.
4. Android notifications are encrypted and signed before being sent.
5. The Mac validates, decrypts, and displays the notification natively.

## Privacy

NotifyBridge does not use a server, cloud sync, analytics, or third-party backend.

All notification transfer happens directly between your Android device and Mac over your local network.

## Current Status

`v0.1.0`

This is an early public version intended for testing and feedback.

## License

MIT License.
