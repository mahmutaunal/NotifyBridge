//
//  NotificationActionCommand.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 10.06.2026.
//

import Foundation

/// Represents a notification action queued by the Mac app for execution on Android.
struct NotificationActionCommand: Codable, Identifiable {
    let id: String
    let type: NotificationActionType
    let notificationKey: String?
    let packageName: String?
    let replyText: String?
    let createdAt: String
}

/// Supported notification actions that can be executed on the Android device.
enum NotificationActionType: String, Codable {
    case dismiss
    case openOnPhone
    case reply
}
