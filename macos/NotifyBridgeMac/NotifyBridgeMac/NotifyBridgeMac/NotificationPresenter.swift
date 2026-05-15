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
        content.title = payload.title?.isEmpty == false
            ? payload.title!
            : payload.appName ?? String(localized: "default_android_notification_title")

        content.body = payload.text ?? ""
        content.sound = .default

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
}
