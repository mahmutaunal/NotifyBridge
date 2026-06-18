//
//  NotificationActionHandler.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 10.06.2026.
//

import Foundation
import UserNotifications

/// Handles notification actions triggered from macOS notification buttons.
final class NotificationActionHandler: NSObject, UNUserNotificationCenterDelegate {

    /// Shared notification action handler instance.
    static let shared = NotificationActionHandler()

    private override init() {}

    /// Converts macOS notification interactions into commands for the paired Android device.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        print("Received notification action:", response.actionIdentifier)
        
        let userInfo = response.notification.request.content.userInfo

        let notificationKey = userInfo["notificationKey"] as? String
        let packageName = userInfo["packageName"] as? String

        switch response.actionIdentifier {
        // Request dismissal of the original notification on Android.
        case "dismiss_on_phone":
            NotificationActionQueue.shared.enqueue(
                NotificationActionCommand(
                    id: UUID().uuidString,
                    type: .dismiss,
                    notificationKey: notificationKey,
                    packageName: packageName,
                    replyText: nil,
                    createdAt: ISO8601DateFormatter().string(from: Date())
                )
            )

        // Request opening the related Android application.
        case "open_on_phone":
            NotificationActionQueue.shared.enqueue(
                NotificationActionCommand(
                    id: UUID().uuidString,
                    type: .openOnPhone,
                    notificationKey: notificationKey,
                    packageName: packageName,
                    replyText: nil,
                    createdAt: ISO8601DateFormatter().string(from: Date())
                )
            )
            
        // Forward the user's reply text to the Android notification.
        case "reply_on_phone":
            guard let textResponse =
                response as? UNTextInputNotificationResponse
            else {
                return
            }

            NotificationActionQueue.shared.enqueue(
                NotificationActionCommand(
                    id: UUID().uuidString,
                    type: .reply,
                    notificationKey: notificationKey,
                    packageName: packageName,
                    replyText: textResponse.userText,
                    createdAt: ISO8601DateFormatter().string(from: Date())
                )
            )

        default:
            break
        }
    }
}
