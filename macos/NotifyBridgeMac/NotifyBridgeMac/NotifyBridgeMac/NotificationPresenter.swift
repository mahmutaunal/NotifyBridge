//
//  NotificationPresenter.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import Foundation
import UserNotifications

/// Handles displaying local macOS notifications received from the Android client.
final class NotificationPresenter {

    /// Requests permission required to display local notifications on macOS.
    func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            print("Notification permission granted:", granted)

            if let error {
                print("Notification permission error:", error.localizedDescription)
            }
        }
    }

    /// Presents a decrypted Android notification as a native macOS notification.
    func show(payload: NotificationPayload) {
        let content = UNMutableNotificationContent()
        let isContentHidden = payload.contentHidden ?? false

        content.title = if isContentHidden {
            payload.appName ?? String(localized: "default_android_notification_title")
        } else {
            payload.title?.isEmpty == false
                ? payload.title!
                : payload.appName ?? String(localized: "default_android_notification_title")
        }

        content.body = if isContentHidden {
            String(localized: "notification_content_hidden")
        } else {
            payload.text ?? ""
        }

        content.sound = .default

        if let attachment = makeIconAttachment(from: payload.appIconBase64) {
            content.attachments = [attachment]
        }

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )

        UNUserNotificationCenter.current().add(request) { error in
            if let error {
                print("Show notification error:", error.localizedDescription)
            }
        }
    }
    
    private func makeIconAttachment(from base64: String?) -> UNNotificationAttachment? {
        guard let base64,
              let data = Data(base64Encoded: base64) else {
            return nil
        }

        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("NotifyBridgeIcons", isDirectory: true)

        do {
            try FileManager.default.createDirectory(
                at: directory,
                withIntermediateDirectories: true
            )

            let fileURL = directory
                .appendingPathComponent(UUID().uuidString)
                .appendingPathExtension("png")

            try data.write(to: fileURL, options: .atomic)

            return try UNNotificationAttachment(
                identifier: "app-icon",
                url: fileURL,
                options: nil
            )

        } catch {
            print("Icon attachment error:", error.localizedDescription)
            return nil
        }
    }
}
