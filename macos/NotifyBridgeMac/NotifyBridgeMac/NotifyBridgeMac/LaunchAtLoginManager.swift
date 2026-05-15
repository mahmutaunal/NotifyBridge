//
//  LaunchAtLoginManager.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import Foundation
import ServiceManagement
import Combine

/// Manages automatic app launch registration using macOS login items.
@MainActor
final class LaunchAtLoginManager: ObservableObject {

    @Published var isEnabled: Bool = false

    init() {
        refresh()
    }

    /// Reloads the current launch-at-login state from the system.
    func refresh() {
        isEnabled = SMAppService.mainApp.status == .enabled
    }

    /// Registers or unregisters the app as a login item.
    func setEnabled(_ enabled: Bool) {
        do {
            if enabled {
                try SMAppService.mainApp.register()
            } else {
                try SMAppService.mainApp.unregister()
            }

            refresh()

        } catch {
            print("Launch at login error:", error)
        }
    }
}
