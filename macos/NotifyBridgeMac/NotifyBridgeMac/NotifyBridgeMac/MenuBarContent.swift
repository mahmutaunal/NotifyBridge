//
//  MenuBarContent.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import SwiftUI
import AppKit
import Combine

struct MenuBarContent: View {

    @ObservedObject var server: LocalNotificationServer
    @StateObject private var launchAtLoginManager = LaunchAtLoginManager()

    var body: some View {
        Group {
            if server.pairingCompleted {
                VStack(spacing: 0) {
                    ConnectedHeader(server: server)

                    // Shows local notification and startup controls.
                    ControlSection(
                        server: server,
                        launchAtLoginManager: launchAtLoginManager
                    )

                    PairedDeviceSection(server: server)
                    FooterSection(server: server)
                }
                .padding(16)
                .fixedSize(horizontal: false, vertical: true)
            } else {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        PairingHeader()
                        PairingQRCodeSection(server: server)

                        // Shows local notification and startup controls.
                        ControlSection(
                            server: server,
                            launchAtLoginManager: launchAtLoginManager
                        )

                        FooterSection(server: server)
                    }
                    .padding(16)
                }
                .frame(height: 690)
            }
        }
        .frame(width: 430)
        .background(Color(nsColor: .windowBackgroundColor))
    }
}

private struct ConnectedHeader: View {

    @ObservedObject var server: LocalNotificationServer

    var body: some View {
        CardContainer {
            HStack(spacing: 18) {
                DeviceIcon(isConnected: server.isClientOnline)

                VStack(alignment: .leading, spacing: 8) {
                    StatusBadge(
                        text: server.isClientOnline
                            ? String(localized: "connection_status_connected")
                            : String(localized: "connection_status_offline"),
                        color: server.isClientOnline ? .green : .orange
                    )

                    Text(
                        server.isClientOnline
                            ? String(localized: "connected_description")
                            : String(localized: "offline_description")
                    )
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)

                    Label(
                        server.isClientOnline
                            ? String(localized: "local_network_active")
                            : String(localized: "waiting_for_connection"),
                        systemImage: server.isClientOnline ? "wifi" : "wifi.slash"
                    )
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(server.isClientOnline ? .green : .orange)
                }

                Spacer()
            }
        }
    }
}

private struct PairingHeader: View {

    var body: some View {
        CardContainer {
            HStack(spacing: 18) {
                DeviceIcon(isConnected: false)

                VStack(alignment: .leading, spacing: 8) {
                    StatusBadge(
                        text: String(localized: "Eşleştirmeye hazır"),
                        color: .blue
                    )

                    Text(String(localized: "Android bağlantısı bekleniyor"))
                        .font(.system(size: 22, weight: .bold))

                    Text(String(localized: "Android cihazınızla eşleştirin ve bildirimleri Mac’inizde görün."))
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)

                    Label(String(localized: "Yerel ağ üzerinden bekleniyor"), systemImage: "wifi")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(.blue)
                }

                Spacer()
            }
        }
    }
}

private struct PairingQRCodeSection: View {

    @ObservedObject var server: LocalNotificationServer
    
    private let pairingCodeTimer =
        Timer.publish(every: 30, on: .main, in: .common)
            .autoconnect()

    var body: some View {
        CardContainer {
            VStack(spacing: 14) {
                HStack(spacing: 22) {
                    ConnectionGlyph(systemImage: "laptopcomputer")
                    DottedLine()
                    ConnectionGlyph(systemImage: "shield.checkered")
                    DottedLine()
                    ConnectionGlyph(systemImage: "iphone")
                }

                Text(String(localized: "Android cihazınızla eşleştirin"))
                    .font(.system(size: 20, weight: .bold))
                    .lineLimit(1)

                Text(String(localized: "Telefonunuzdaki NotifyBridge uygulamasında “QR ile Eşleştir” seçeneğini açın ve kodu tarayın."))
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 300)

                if let qrImage = QRCodeGenerator.generate(from: server.pairingQRCodeText) {
                    Image(nsImage: qrImage)
                        .interpolation(.none)
                        .resizable()
                        .frame(width: 170, height: 170)
                        .padding(14)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                } else {
                    Text(String(localized: "QR kod oluşturulamadı."))
                        .foregroundStyle(.red)
                }

                Label(String(localized: "Uçtan uca şifreli ve güvenli bağlantı"), systemImage: "lock")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .onReceive(pairingCodeTimer) { _ in
                server.refreshPairingCodeIfNeeded()
            }
        }
    }
}

/// Displays user controls for notification delivery and launch-at-login behavior.
private struct ControlSection: View {

    @ObservedObject var server: LocalNotificationServer
    @ObservedObject var launchAtLoginManager: LaunchAtLoginManager

    var body: some View {
        CardContainer(padding: 0) {
            VStack(spacing: 0) {
                // Enables or disables displaying forwarded Android notifications on macOS.
                ToggleRow(
                    icon: "bell",
                    title: String(localized: "Bildirimleri Göster"),
                    subtitle: String(localized: "Android bildirimlerini Mac’te göster."),
                    isOn: $server.notificationsEnabled
                )

                Divider()

                // Keeps the app available after restart by managing launch-at-login state.
                ToggleRow(
                    icon: "power",
                    title: String(localized: "Mac Açıldığında Başlat"),
                    subtitle: String(localized: "Uygulama Mac açıldığında otomatik başlar."),
                    isOn: Binding(
                        get: { launchAtLoginManager.isEnabled },
                        set: { launchAtLoginManager.setEnabled($0) }
                    )
                )
            }
        }
    }
}

private struct PairedDeviceSection: View {

    @ObservedObject var server: LocalNotificationServer
    @State private var showQr = false

    var body: some View {
        CardContainer {
            VStack(alignment: .leading, spacing: 16) {
                Text(String(localized: "Eşleşmiş Cihaz"))
                    .font(.headline)
                    .foregroundStyle(.secondary)

                HStack(spacing: 14) {
                    CircleIcon(systemImage: "iphone")

                    VStack(alignment: .leading, spacing: 4) {
                        Text(server.lastClientName.isEmpty ? server.lastClientAddress : server.lastClientName)
                            .font(.system(size: 24, weight: .bold))
                            .lineLimit(1)

                        Text(server.isClientOnline
                             ? String(localized: "device_online_now")
                             : lastActiveText)
                            .font(.system(size: 13))
                            .foregroundStyle(server.isClientOnline ? .green : .secondary)
                    }

                    Spacer()

                    Button(String(localized: "Yeniden Eşleştir")) {
                        showQr.toggle()
                    }

                    Menu {
                        Button(String(localized: "Eşleştirmeyi Sıfırla"), role: .destructive) {
                            server.resetPairing()
                        }
                    } label: {
                        Image(systemName: "ellipsis")
                            .frame(width: 34, height: 28)
                    }
                }

                if showQr && !server.isClientOnline {
                    Divider()

                    PairingQRCodeSection(server: server)
                }
            }
        }
        .onChange(of: server.lastClientHeartbeatDate) { _, _ in
            if server.isClientOnline {
                showQr = false
            }
        }
        .onChange(of: server.lastClientName) { _, _ in
            if server.isClientOnline {
                showQr = false
            }
        }
        .onChange(of: server.pairingCompleted) { _, isPaired in
            if !isPaired {
                showQr = false
            }
        }
    }

    private var lastActiveText: String {
        if let lastConnectionText = server.lastConnectionText {
            return String(localized: "Son aktif: \(lastConnectionText)")
        }

        return String(localized: "Son aktif: Bilinmiyor")
    }
}

private struct FooterSection: View {

    @ObservedObject var server: LocalNotificationServer

    var body: some View {
        HStack(spacing: 10) {
            Label(
                server.isClientOnline
                    ? String(localized: "encryption_active")
                    : String(localized: "device_offline_footer"),
                systemImage: server.isClientOnline ? "shield.checkered" : "wifi.slash"
            )
            .foregroundStyle(server.isClientOnline ? .green : .orange)
            .lineLimit(1)

            Spacer(minLength: 8)

            Button(String(localized: "Sunucuyu Yeniden Başlat")) {
                server.stop()
                
                DispatchQueue.main.asyncAfter(
                    deadline: .now() + 0.3
                ) {
                    server.start()
                }
            }

            Divider()
                .frame(height: 18)

            Button(String(localized: "Çıkış")) {
                NSApp.terminate(nil)
            }
        }
        .font(.system(size: 12))
        .padding(.top, 10)
        .padding(.horizontal, 2)
    }
}

private struct CardContainer<Content: View>: View {

    var padding: CGFloat = 16
    @ViewBuilder let content: Content

    var body: some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity)
            .background(Color.secondary.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(Color.white.opacity(0.08))
            )
            .padding(.bottom, 12)
    }
}

private struct DeviceIcon: View {

    let isConnected: Bool

    private var accentColor: Color {
        isConnected ? .green : .orange
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(accentColor.opacity(isConnected ? 1.0 : 0.75), lineWidth: 3)
                .frame(width: 82, height: 82)

            Image(systemName: "laptopcomputer")
                .font(.system(size: 32, weight: .medium))
                .foregroundStyle(isConnected ? accentColor : .secondary)
                .frame(width: 82, height: 82, alignment: .center)

            Circle()
                .fill(accentColor)
                .frame(width: 30, height: 30)
                .overlay {
                    Image(systemName: isConnected ? "checkmark" : "wifi.slash")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(.white)
                }
                .offset(x: 28, y: 28)
        }
        .frame(width: 92, height: 92)
    }
}

private struct CircleIcon: View {

    let systemImage: String

    var body: some View {
        Circle()
            .fill(Color.secondary.opacity(0.10))
            .frame(width: 46, height: 46)
            .overlay {
                Image(systemName: systemImage)
                    .font(.system(size: 19))
                    .foregroundStyle(.secondary)
            }
    }
}

private struct ToggleRow: View {

    let icon: String
    let title: String
    let subtitle: String
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color.blue.opacity(0.14))
                .frame(width: 44, height: 44)
                .overlay {
                    Image(systemName: icon)
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(.blue)
                }

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))

                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Toggle("", isOn: $isOn)
                .toggleStyle(.switch)
                .labelsHidden()
        }
        .padding(16)
    }
}

private struct StatusBadge: View {

    let text: String
    let color: Color

    var body: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(color)
                .frame(width: 10, height: 10)

            Text(text)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(color)
        }
    }
}

private struct ConnectionGlyph: View {

    let systemImage: String

    var body: some View {
        RoundedRectangle(cornerRadius: 14)
            .fill(Color.blue.opacity(0.12))
            .frame(width: 58, height: 58)
            .overlay {
                Image(systemName: systemImage)
                    .font(.system(size: 24, weight: .medium))
                    .foregroundStyle(.blue)
            }
    }
}

private struct DottedLine: View {

    var body: some View {
        HStack(spacing: 5) {
            ForEach(0..<5, id: \.self) { _ in
                Circle()
                    .fill(Color.blue.opacity(0.75))
                    .frame(width: 4, height: 4)
            }
        }
    }
}
