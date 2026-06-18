//
//  NotificationActionQueue.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 10.06.2026.
//

import Foundation
import Combine

/// Stores notification actions until they are fetched by the paired Android device.
final class NotificationActionQueue: ObservableObject {

    static let shared = NotificationActionQueue()

    private let queue = DispatchQueue(label: "com.alpware.notifybridge.notification-actions")
    private var commands: [NotificationActionCommand] = []

    private init() {}

    func enqueue(_ command: NotificationActionCommand) {
        queue.async {
            self.commands.append(command)
            print("Queued action:", command.type.rawValue, command.notificationKey ?? "nil")
        }
    }

    func drain() -> [NotificationActionCommand] {
        queue.sync {
            let pending = commands
            commands.removeAll()
            print("Draining actions:", pending.count)
            return pending
        }
    }
}
