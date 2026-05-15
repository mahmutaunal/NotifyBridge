//
//  NotifyBridgeMacApp.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import SwiftUI
import AppKit

/// Entry point of the macOS menu bar application.
@main
struct NotifyBridgeMacApp: App {

    @StateObject private var server: LocalNotificationServer

    init() {
        let localServer = LocalNotificationServer()

        // Start listening immediately when the app launches.
        localServer.start()
        _server = StateObject(wrappedValue: localServer)
    }

    var body: some Scene {
        MenuBarExtra("app_name", systemImage: "bell.badge") {
            MenuBarContent(server: server)
        }
        .menuBarExtraStyle(.window)
    }
}
