//
//  NotificationPayload.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import Foundation

/// Represents a decrypted notification received from the paired Android device.
struct NotificationPayload: Codable {
    let packageName: String
    let appName: String?
    let title: String?
    let text: String?
    let postTime: Int64
    let contentHidden: Bool?
    let appIconBase64: String?
    let deviceName: String?
}
