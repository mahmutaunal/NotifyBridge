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
    @ObservedObject private var pairedDevices = PairedAndroidDeviceStore.shared

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

                Text("Paired Android Devices")
                    .font(.headline)

                if pairedDevices.devices.isEmpty {
                    Text("No Android device has been paired yet.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(pairedDevices.devices) { device in
                        HStack(spacing: 12) {
                            Image(systemName: device.isOnline ? "iphone.radiowaves.left.and.right" : "iphone.slash")
                                .foregroundStyle(device.isOnline ? .green : .secondary)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(device.name).fontWeight(.semibold)
                                Text(device.lastAddress.isEmpty ? "Paired" : device.lastAddress)
                                    .font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Toggle("", isOn: Binding(
                                get: { device.isEnabled },
                                set: { pairedDevices.setEnabled(id: device.id, enabled: $0) }
                            ))
                            .labelsHidden()
                            Button(role: .destructive) {
                                pairedDevices.remove(id: device.id)
                            } label: {
                                Image(systemName: "trash")
                            }
                            .buttonStyle(.borderless)
                        }
                        .padding(10)
                        .background(Color.gray.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }

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
        .frame(width: 500, height: 760)
    }
}
