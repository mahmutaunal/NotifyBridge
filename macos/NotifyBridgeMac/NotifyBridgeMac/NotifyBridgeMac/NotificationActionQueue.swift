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

    /// Shared action queue instance.
    static let shared = NotificationActionQueue()

    /// Pending notification actions waiting to be delivered.
    private var commands: [NotificationActionCommand] = []

    private init() {}

    /// Adds a notification action to the delivery queue.
    func enqueue(_ command: NotificationActionCommand) {
        commands.append(command)
        print("Queued notification action:", command)
    }

    /// Returns all queued actions and clears the queue.
    func drain() -> [NotificationActionCommand] {
        let pending = commands
        commands.removeAll()
        return pending
    }
}
