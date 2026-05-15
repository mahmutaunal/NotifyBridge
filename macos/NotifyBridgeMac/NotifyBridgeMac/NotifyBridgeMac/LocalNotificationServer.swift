//
//  LocalNotificationServer.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import Foundation
import Network
import Combine
import CryptoKit

/// Runs the local TCP server that receives encrypted notifications from paired Android devices.
final class LocalNotificationServer: ObservableObject {

    @Published var isRunning: Bool = false
    @Published var lastPayloadText: String = String(localized: "last_notification_empty_message")
    @Published var notificationsEnabled: Bool = UserDefaults.standard.bool(forKey: "notificationsEnabled") {
        didSet {
            UserDefaults.standard.set(notificationsEnabled, forKey: "notificationsEnabled")
        }
    }
    @Published var pairingToken: String = UserDefaults.standard.string(forKey: "pairingToken") ?? "" {
        didSet {
            UserDefaults.standard.set(pairingToken, forKey: "pairingToken")
        }
    }
    @Published var pairingCompleted: Bool = UserDefaults.standard.bool(forKey: "pairingCompleted")
    @Published var lastClientAddress: String = UserDefaults.standard.string(forKey: "lastClientAddress") ?? ""
    @Published var lastConnectionDate: Date? = UserDefaults.standard.object(forKey: "lastConnectionDate") as? Date

    /// Human-readable relative time for the most recent Android client connection.
    var lastConnectionText: String? {
        guard let lastConnectionDate else { return nil }

        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full

        return formatter.localizedString(for: lastConnectionDate, relativeTo: Date())
    }
    
    /// Local network address advertised to the Android app during pairing.
    var pairingHost: String {
        LocalIPAddress.getWiFiAddress()
    }
    
    /// Encoded pairing payload shown as a QR code in the Mac app.
    var pairingQRCodeText: String {
        let payload = PairingPayload(
            type: "notifybridge_pairing",
            host: pairingHost,
            port: 8787,
            secret: pairingToken,
            name: Host.current().localizedName ?? String(localized: "default_mac_name")
        )

        let data = try? JSONEncoder().encode(payload)
        return String(data: data ?? Data(), encoding: .utf8) ?? ""
    }

    private let port: NWEndpoint.Port = 8787
    private var listener: NWListener?
    private let presenter = NotificationPresenter()
    private let bonjourPublisher = BonjourPublisher()
    
    init() {
        if UserDefaults.standard.object(forKey: "notificationsEnabled") == nil {
            UserDefaults.standard.set(true, forKey: "notificationsEnabled")
            notificationsEnabled = true
        }
        if UserDefaults.standard.string(forKey: "pairingToken") == nil {
            let token = UUID().uuidString
            UserDefaults.standard.set(token, forKey: "pairingToken")
            pairingToken = token
        }
    }

    /// Starts listening for notification requests and advertises the service via Bonjour.
    func start() {
        guard listener == nil else { return }

        do {
            let listener = try NWListener(using: .tcp, on: port)
            self.listener = listener

            listener.newConnectionHandler = { [weak self] connection in
                self?.handle(connection)
            }

            listener.stateUpdateHandler = { [weak self] state in
                DispatchQueue.main.async {
                    switch state {
                    case .ready:
                        self?.isRunning = true
                        self?.bonjourPublisher.start()
                        print("NotifyBridge server running on port 8787")

                    case .failed(let error):
                        self?.isRunning = false
                        print("Server failed:", error.localizedDescription)

                    case .cancelled:
                        self?.isRunning = false
                        print("Server cancelled")

                    default:
                        break
                    }
                }
            }

            listener.start(queue: .global(qos: .userInitiated))
            presenter.requestPermission()

        } catch {
            print("Failed to start server:", error.localizedDescription)
        }
    }

    /// Stops the local server and removes the Bonjour advertisement.
    func stop() {
        bonjourPublisher.stop()
        listener?.cancel()
        listener = nil

        DispatchQueue.main.async {
            self.isRunning = false
        }
    }

    /// Reads the raw HTTP-like request from a newly accepted client connection.
    private func handle(_ connection: NWConnection) {
        connection.start(queue: .global(qos: .userInitiated))

        connection.receive(minimumIncompleteLength: 1, maximumLength: 64 * 1024) { [weak self] data, _, _, error in
            if let error {
                print("Receive error:", error.localizedDescription)
                connection.cancel()
                return
            }

            guard let data, !data.isEmpty else {
                connection.cancel()
                return
            }

            self?.handleRequestData(data, connection: connection)
        }
    }

    /// Validates, decrypts, and presents a notification request from the Android app.
    private func handleRequestData(_ data: Data, connection: NWConnection) {
        guard let requestText = String(data: data, encoding: .utf8) else {
            sendBadRequest(connection)
            return
        }

        guard requestText.hasPrefix("POST /notify") else {
            sendNotFound(connection)
            return
        }

        guard let bodyRange = requestText.range(of: "\r\n\r\n") else {
            sendBadRequest(connection)
            return
        }

        let body = String(requestText[bodyRange.upperBound...])
        
        guard validateSignature(requestText: requestText, body: body) else {
            sendUnauthorized(connection)
            return
        }

        guard let bodyData = body.data(using: .utf8) else {
            sendBadRequest(connection)
            return
        }

        do {
            let encryptedPayload = try JSONDecoder().decode(
                EncryptedPayload.self,
                from: bodyData
            )

            let decryptedJson = try CryptoManager.decryptAesGcm(
                secret: pairingToken,
                encryptedPayload: encryptedPayload
            )

            guard let decryptedData = decryptedJson.data(using: .utf8) else {
                sendBadRequest(connection)
                return
            }

            let payload = try JSONDecoder().decode(
                NotificationPayload.self,
                from: decryptedData
            )
            
            DispatchQueue.main.async {
                self.lastPayloadText = """
                \(payload.appName ?? payload.packageName)
                \(payload.title ?? "")
                \(payload.text ?? "")
                """
            }

            print("Received notification:", payload)

            if notificationsEnabled {
                markPairingCompleted(connection: connection)
                presenter.show(payload: payload)
            } else {
                print("Notification received but Mac notifications are disabled.")
            }

            sendOk(connection)

        } catch {
            print("JSON decode error:", error.localizedDescription)
            print("Body:", body)
            sendBadRequest(connection)
        }
    }
    
    /// Persists pairing state after the first valid notification request is received.
    private func markPairingCompleted(connection: NWConnection) {
        let address: String

        switch connection.endpoint {
        case .hostPort(let host, _):
            address = "\(host)"
        default:
            address = String(localized: "unknown_client_device")
        }

        DispatchQueue.main.async {
            self.pairingCompleted = true
            self.lastClientAddress = address
            self.lastConnectionDate = Date()

            UserDefaults.standard.set(true, forKey: "pairingCompleted")
            UserDefaults.standard.set(address, forKey: "lastClientAddress")
            UserDefaults.standard.set(Date(), forKey: "lastConnectionDate")
        }
    }

    private func sendOk(_ connection: NWConnection) {
        let response = """
        HTTP/1.1 200 OK\r
        Content-Type: text/plain\r
        Content-Length: 2\r
        \r
        OK
        """

        send(response, connection: connection)
    }

    private func sendBadRequest(_ connection: NWConnection) {
        let response = """
        HTTP/1.1 400 Bad Request\r
        Content-Type: text/plain\r
        Content-Length: 11\r
        \r
        Bad Request
        """

        send(response, connection: connection)
    }

    private func sendNotFound(_ connection: NWConnection) {
        let response = """
        HTTP/1.1 404 Not Found\r
        Content-Type: text/plain\r
        Content-Length: 9\r
        \r
        Not Found
        """

        send(response, connection: connection)
    }

    private func send(_ response: String, connection: NWConnection) {
        connection.send(
            content: response.data(using: .utf8),
            completion: .contentProcessed { _ in
                connection.cancel()
            }
        )
    }
    
    private func sendUnauthorized(_ connection: NWConnection) {
        let response = """
        HTTP/1.1 401 Unauthorized\r
        Content-Type: text/plain\r
        Content-Length: 12\r
        \r
        Unauthorized
        """

        send(response, connection: connection)
    }
    
    /// Verifies request freshness and HMAC signature before decrypting the payload.
    private func validateSignature(requestText: String, body: String) -> Bool {
        guard !pairingToken.isEmpty else {
            print("Pairing token is empty")
            return false
        }

        guard let timestamp = headerValue("X-NotifyBridge-Timestamp", from: requestText),
              let nonce = headerValue("X-NotifyBridge-Nonce", from: requestText),
              let signature = headerValue("X-NotifyBridge-Signature", from: requestText) else {
            print("Missing signature headers")
            return false
        }

        guard let timestampMillis = Int64(timestamp) else {
            print("Invalid timestamp")
            return false
        }

        let nowMillis = Int64(Date().timeIntervalSince1970 * 1000)
        let diff = abs(nowMillis - timestampMillis)

        // Reject replayed or delayed requests older than one minute.
        guard diff < 60_000 else {
            print("Request expired")
            return false
        }

        let message = "\(timestamp)\n\(nonce)\n\(body)"
        let expectedSignature = CryptoManager.hmacSha256Base64(
            secret: pairingToken,
            message: message
        )

        if expectedSignature != signature {
            print("Invalid signature")
            return false
        }

        return true
    }

    /// Extracts a single header value from the raw request text.
    private func headerValue(_ name: String, from requestText: String) -> String? {
        let lines = requestText.components(separatedBy: "\r\n")
        let prefix = "\(name):"

        return lines
            .first { $0.lowercased().hasPrefix(prefix.lowercased()) }?
            .dropFirst(prefix.count)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Clears the paired device state and rotates the pairing token.
    func resetPairing() {
        let token = UUID().uuidString

        pairingToken = token
        pairingCompleted = false
        lastClientAddress = ""
        lastConnectionDate = nil

        UserDefaults.standard.set(token, forKey: "pairingToken")
        UserDefaults.standard.set(false, forKey: "pairingCompleted")
        UserDefaults.standard.removeObject(forKey: "lastClientAddress")
        UserDefaults.standard.removeObject(forKey: "lastConnectionDate")
    }
}
