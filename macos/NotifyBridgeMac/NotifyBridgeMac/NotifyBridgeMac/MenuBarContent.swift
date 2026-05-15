//
//  MenuBarContent.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import SwiftUI

/// Main menu bar popover that controls the local server, pairing, and app preferences.
struct MenuBarContent: View {

    @ObservedObject var server: LocalNotificationServer
    @StateObject private var launchAtLoginManager = LaunchAtLoginManager()

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HeaderSection(server: server)

            Divider()

            Toggle("show_mac_notifications_toggle", isOn: $server.notificationsEnabled)
                .toggleStyle(.switch)
            
            Toggle(
                "launch_at_login_toggle",
                isOn: Binding(
                    get: {
                        launchAtLoginManager.isEnabled
                    },
                    set: { value in
                        launchAtLoginManager.setEnabled(value)
                    }
                )
            )
            .toggleStyle(.switch)

            Divider()

            PairingSection(server: server)

            Divider()

            ActionSection(server: server)
        }
        .padding(20)
        .frame(width: 360)
    }
}

/// Shows the current server status at the top of the menu bar popover.
private struct HeaderSection: View {

    @ObservedObject var server: LocalNotificationServer

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("app_name")
                .font(.title2)
                .fontWeight(.bold)

            Text(server.isRunning ? "menu_server_active" : "menu_server_inactive")
                .font(.subheadline)
                .foregroundStyle(server.isRunning ? .green : .secondary)
        }
    }
}

/// Displays pairing status and the QR code used by the Android app to connect securely.
private struct PairingSection: View {

    @ObservedObject var server: LocalNotificationServer
    @State private var showQr: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(spacing: 6) {
                Text("pairing_section_title")
                    .font(.headline)

                Text(pairingDescription)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                if server.pairingCompleted {
                    Text("pairing_status_paired")
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(.green.opacity(0.15))
                        .foregroundStyle(.green)
                        .clipShape(Capsule())
                }
            }
            .frame(maxWidth: .infinity)

            if server.pairingCompleted {
                VStack(alignment: .leading, spacing: 8) {
                    Text("paired_android_device_title")
                        .font(.subheadline)
                        .fontWeight(.semibold)

                    if !server.lastClientAddress.isEmpty {
                        InfoRow(title: String(localized: "device_label"), value: server.lastClientAddress)
                    }

                    if let lastConnectionText = server.lastConnectionText {
                        InfoRow(title: String(localized: "last_connection_label"), value: lastConnectionText)
                    }

                    InfoRow(title: String(localized: "security_label"), value: String(localized: "security_method_value"))
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.secondary.opacity(0.10))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            if !server.pairingCompleted || showQr {
                qrView
            }

            if server.pairingCompleted {
                HStack(spacing: 12) {
                    Spacer()

                    Button(showQr ? String(localized: "hide_qr_button") : String(localized: "pair_again_button")) {
                        showQr.toggle()
                    }

                    Button("reset_pairing_button") {
                        server.resetPairing()
                        showQr = true
                    }
                    .foregroundStyle(.red)

                    Spacer()
                }
            }
        }
    }

    private var qrView: some View {
        VStack(spacing: 10) {
            // The QR code contains the local host, port, and pairing token.
            if let qrImage = QRCodeGenerator.generate(from: server.pairingQRCodeText) {
                Image(nsImage: qrImage)
                    .interpolation(.none)
                    .resizable()
                    .frame(width: 220, height: 220)
                    .padding(14)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
            } else {
                Text("qr_generation_failed_message")
                    .foregroundStyle(.red)
            }

            Text("qr_pairing_instruction")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Text(server.pairingHost)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity)
    }

    private var pairingDescription: String {
        if server.pairingCompleted {
            return String(localized: "pairing_description_paired")
        } else {
            return String(localized: "pairing_description_unpaired")
        }
    }
}

/// Compact key-value row used for paired device details.
private struct InfoRow: View {
    let title: String
    let value: String

    var body: some View {
        HStack {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)

            Spacer()

            Text(value)
                .font(.caption)
                .fontWeight(.medium)
                .lineLimit(1)
                .truncationMode(.middle)
        }
    }
}

/// Contains primary app actions such as starting the server and quitting the app.
private struct ActionSection: View {

    @ObservedObject var server: LocalNotificationServer

    var body: some View {
        VStack(spacing: 10) {
            Button {
                if server.isRunning {
                    server.stop()
                } else {
                    server.start()
                }
            } label: {
                Text(server.isRunning ? "stop_server_button" : "start_server_button")
                    .frame(maxWidth: .infinity)
            }

            Button {
                NSApp.terminate(nil)
            } label: {
                Text("quit_button")
                    .frame(maxWidth: .infinity)
            }
        }
    }
}
