# Pairing

NotifyBridge uses QR-based pairing between the Android app and the macOS menu bar app.

## Pairing Flow

1. Open the NotifyBridge macOS app.
2. Click the menu bar icon.
3. If no Android device is paired, a QR code is shown.
4. Open the Android app.
5. Tap **QR ile Eşleştir**.
6. Scan the QR code shown on the Mac.
7. The Android app stores:
   - Mac host/IP
   - Port
   - Pairing secret
   - Mac device name

After pairing, Android notifications can be securely sent to the Mac.

## Re-pairing

If you want to connect to a different Mac:

1. Open the Android app.
2. Tap **Mac’i Değiştir veya Yeni Eşleştir**.
3. Scan the new Mac QR code.

## Reset Pairing

Resetting pairing removes the saved connection data and secret.

After reset, the Android app must scan a new QR code before notifications can be sent again.
