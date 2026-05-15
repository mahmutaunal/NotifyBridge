//
//  ContentView.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import SwiftUI

/// Main desktop window used for local testing and QR-based pairing.
struct ContentView: View {

    @ObservedObject var server: LocalNotificationServer

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("app_name")
                    .font(.largeTitle)
                    .bold()

                Text(server.isRunning ? "server_status_active" : "server_status_inactive")
                    .font(.headline)

                Toggle("show_mac_notifications_toggle", isOn: $server.notificationsEnabled)
                    .toggleStyle(.switch)

                HStack {
                    Button("start_server_button") {
                        server.start()
                    }

                    Button("stop_server_button") {
                        server.stop()
                    }
                }

                Divider()

                Text("server_port_value")
                    .font(.subheadline)

                Text("last_notification_title")
                    .font(.headline)

                Text(server.lastPayloadText)
                    .font(.body)
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.gray.opacity(0.12))
                    .cornerRadius(12)

                Divider()

                Text("qr_pairing_title")
                    .font(.headline)

                HStack {
                    Spacer()

                    // Generates a pairing QR code that can be scanned by the Android app.
                    if let qrImage = QRCodeGenerator.generate(from: server.pairingQRCodeText) {
                        Image(nsImage: qrImage)
                            .interpolation(.none)
                            .resizable()
                            .frame(width: 220, height: 220)
                            .padding(16)
                            .background(Color.white)
                            .cornerRadius(16)
                    } else {
                        Text("qr_generation_failed_message")
                            .foregroundStyle(.red)
                    }

                    Spacer()
                }

                Text(String(localized: "pairing_host_value \(server.pairingHost)"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .textSelection(.enabled)

                Text("server_port_value")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(24)
        }
        .frame(width: 460, height: 620)
    }
}
