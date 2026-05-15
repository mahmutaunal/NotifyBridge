# Troubleshooting

## Android notifications are not appearing on Mac

Check the following:

1. Android and Mac are on the same local network.
2. NotifyBridge is enabled on Android.
3. Notification Access permission is granted.
4. The Android app is paired with the Mac.
5. The macOS app is running.
6. macOS notifications are allowed for NotifyBridge.

## QR pairing works but notifications do not arrive

Try:

1. Tap **Bağlantıyı Test Et** on Android.
2. Check that the Mac app is active.
3. Make sure no firewall is blocking port `8787`.
4. Re-pair using a new QR code.

## Android background service stops

Some Android manufacturers restrict background services.

Recommended actions:

1. Exclude NotifyBridge from battery optimization.
2. Keep the foreground service enabled.
3. Enable the Quick Settings tile when needed.

## macOS app does not start automatically

Open the menu bar app and enable:

```text
Mac açıldığında otomatik başlat
```

Then check:

```text
System Settings → General → Login Items
```

## Invalid signature or unauthorized errors

This usually means Android and Mac have different pairing secrets.

Fix:

1. Reset pairing on Android.
2. Reset pairing on Mac.
3. Scan the QR code again.
