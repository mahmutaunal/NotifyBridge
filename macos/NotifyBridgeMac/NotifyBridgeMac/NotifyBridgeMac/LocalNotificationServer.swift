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
import Security

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
    @Published var lastClientName: String = UserDefaults.standard.string(forKey: "lastClientName") ?? ""
    @Published var lastClientHeartbeatDate: Date? = UserDefaults.standard.object(forKey: "lastClientHeartbeatDate") as? Date
    
    @Published var pairingCode: String = UUID().uuidString
    private var pairingCodeCreatedAt = Date()
    
    private let pairingCodeLifetime: TimeInterval = 120
    private let pairingCodeRefreshThreshold: TimeInterval = 90

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
        let macName = Host.current().localizedName ?? String(localized: "default_mac_name")

        var components = URLComponents(string: "https://www.alpwarestudio.com/notifybridge/pair")
        components?.queryItems = [
            URLQueryItem(name: "host", value: pairingHost),
            URLQueryItem(name: "port", value: "8787"),
            URLQueryItem(name: "code", value: pairingCode),
            URLQueryItem(name: "name", value: macName),
            URLQueryItem(name: "fingerprint", value: TLSIdentityManager.shared.fingerprint ?? "")
        ]

        return components?.url?.absoluteString ?? ""
    }
    
    var isClientOnline: Bool {
        guard let lastClientHeartbeatDate else { return false }
        return Date().timeIntervalSince(lastClientHeartbeatDate) < 45
    }

    private let port: NWEndpoint.Port = 8787
    private var listener: NWListener?
    private let presenter = NotificationPresenter()
    private lazy var ingressService = NotificationIngressService(presenter: presenter)
    let pairedDevices = PairedAndroidDeviceStore.shared
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
            guard let identity = TLSIdentityManager.shared.identity else {
                print("TLS identity is missing")
                return
            }

            let tlsOptions = NWProtocolTLS.Options()
            sec_protocol_options_set_local_identity(
                tlsOptions.securityProtocolOptions,
                sec_identity_create(identity)!
            )

            let parameters = NWParameters(tls: tlsOptions)
            parameters.allowLocalEndpointReuse = true

            let listener = try NWListener(using: parameters, on: port)
            self.listener = listener

            listener.newConnectionHandler = { [weak self] connection in
                self?.handle(connection)
            }

            listener.stateUpdateHandler = { [weak self, weak listener] state in
                DispatchQueue.main.async {
                    guard let self, let listener, self.listener === listener else { return }
                    switch state {
                    case .ready:
                        self.isRunning = true
                        self.bonjourPublisher.start()
                        print("NotifyBridge server running on port 8787")

                    case .failed(let error):
                        self.isRunning = false
                        self.bonjourPublisher.stop()
                        self.listener = nil
                        print("Server failed:", error.localizedDescription)

                    case .cancelled:
                        self.isRunning = false
                        self.bonjourPublisher.stop()
                        self.listener = nil
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

    /// Rebinds the listener and republishes Bonjour after wake or network changes.
    func restart() {
        stop()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            self?.start()
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
        receiveRequestData(connection, accumulated: Data())
    }
    
    private func receiveRequestData(
        _ connection: NWConnection,
        accumulated: Data
    ) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 64 * 1024) { [weak self] data, _, isComplete, error in
            guard let self else { return }

            if let error {
                print("Receive error:", error.localizedDescription)
                connection.cancel()
                return
            }

            var buffer = accumulated

            if let data, !data.isEmpty {
                buffer.append(data)
            }

            if buffer.isEmpty {
                connection.cancel()
                return
            }

            if isComplete || self.isCompleteHttpRequest(buffer) {
                self.handleRequestData(buffer, connection: connection)
            } else {
                self.receiveRequestData(connection, accumulated: buffer)
            }
        }
    }

    private func isCompleteHttpRequest(_ data: Data) -> Bool {
        guard let requestText = String(data: data, encoding: .utf8) else {
            return false
        }

        guard let bodyRange = httpBodyRange(in: requestText) else {
            return false
        }

        let headers = String(requestText[..<bodyRange.lowerBound])
        let body = String(requestText[bodyRange.upperBound...])

        guard let contentLength = headerValue("Content-Length", from: headers)
            .flatMap({ Int($0) }) else {
            return true
        }

        return body.utf8.count >= contentLength
    }

    private func httpBodyRange(in requestText: String) -> Range<String.Index>? {
        requestText.range(of: "\r\n\r\n") ?? requestText.range(of: "\n\n")
    }

    private func httpBody(from requestText: String) -> String? {
        guard let bodyRange = httpBodyRange(in: requestText) else {
            return nil
        }

        return String(requestText[bodyRange.upperBound...])
    }
    
    /// Validates, decrypts, and presents a notification request from the Android app.
    private func handleRequestData(_ data: Data, connection: NWConnection) {
        guard let requestText = String(data: data, encoding: .utf8) else {
            sendBadRequest(connection)
            return
        }
        
        if requestText.hasPrefix("POST /pair") {
            handlePairRequest(requestText: requestText, connection: connection)
            return
        }
        
        if requestText.hasPrefix("GET /health") {
            guard validateHealthToken(requestText: requestText) else {
                sendUnauthorized(connection)
                return
            }

            markClientHeartbeat(connection: connection, requestText: requestText)
            sendOk(connection)
            return
        }
        
        if requestText.hasPrefix("POST /unpair") {
            guard validateHealthToken(requestText: requestText) else {
                sendUnauthorized(connection)
                return
            }

            if let deviceId = headerValue("X-NotifyBridge-Device-Id", from: requestText) {
                pairedDevices.remove(id: deviceId)
            } else {
                resetPairing()
            }
            sendOk(connection)
            return
        }
        
        if requestText.hasPrefix("GET /actions") {
            guard validateHealthToken(requestText: requestText) else {
                sendUnauthorized(connection)
                return
            }

            sendPendingActions(connection)
            return
        }

        guard requestText.hasPrefix("POST /notify") else {
            sendNotFound(connection)
            return
        }

        guard let body = httpBody(from: requestText) else {
            sendBadRequest(connection)
            return
        }
        
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
            
            let deviceId = headerValue("X-NotifyBridge-Device-Id", from: requestText)
                ?? payload.deviceId
                ?? "unknown-device"

            ingressService.handle(
                payload: payload,
                deviceId: deviceId,
                shouldPresent: notificationsEnabled
            ) { result in
                switch result {
                case .success:
                    DispatchQueue.main.async {
                        self.lastPayloadText = """
                        \(payload.appName ?? payload.packageName)
                        \(payload.title ?? "")
                        \(payload.text ?? "")
                        """
                    }
                    self.markPairingCompleted(connection: connection, deviceName: payload.deviceName)
                    self.sendOk(connection)
                case .failure(let error):
                    print("History persistence error:", error.localizedDescription)
                    self.sendServerError(connection)
                }
            }

        } catch {
            print("JSON decode error:", error.localizedDescription)
            print("Body:", body)
            sendBadRequest(connection)
        }
    }
    
    /// Persists pairing state after the first valid notification request is received.
    private func markPairingCompleted(
        connection: NWConnection,
        deviceName: String?
    ) {
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
            self.lastClientName = deviceName?.isEmpty == false
                ? deviceName!
                : address
            self.lastConnectionDate = Date()

            UserDefaults.standard.set(true, forKey: "pairingCompleted")
            UserDefaults.standard.set(address, forKey: "lastClientAddress")
            UserDefaults.standard.set(self.lastClientName, forKey: "lastClientName")
            UserDefaults.standard.set(Date(), forKey: "lastConnectionDate")
        }
    }

    private func sendServerError(_ connection: NWConnection) {
        let response = "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        connection.send(content: response.data(using: .utf8), completion: .contentProcessed { _ in connection.cancel() })
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
    
    private func sendJson(_ json: String, connection: NWConnection) {
        let response = """
        HTTP/1.1 200 OK\r
        Content-Type: application/json\r
        Content-Length: \(json.utf8.count)\r
        \r
        \(json)
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
    
    private func handlePairRequest(requestText: String, connection: NWConnection) {
        guard let body = httpBody(from: requestText) else {
            sendBadRequest(connection)
            return
        }

        guard let bodyData = body.data(using: .utf8),
              let request = try? JSONDecoder().decode(PairingRequest.self, from: bodyData) else {
            sendBadRequest(connection)
            return
        }

        guard request.code == pairingCode else {
            sendUnauthorized(connection)
            return
        }

        let age = Date().timeIntervalSince(pairingCodeCreatedAt)
        guard age < pairingCodeLifetime else {
            rotatePairingCode()
            sendUnauthorized(connection)
            return
        }

        let response = PairingResponse(
            type: "notifybridge_pairing_response",
            host: pairingHost,
            port: 8787,
            secret: pairingToken,
            name: Host.current().localizedName ?? String(localized: "default_mac_name")
        )

        guard let responseData = try? JSONEncoder().encode(response),
              let responseJson = String(data: responseData, encoding: .utf8) else {
            sendBadRequest(connection)
            return
        }

        rotatePairingCode()
        
        DispatchQueue.main.async {
            self.pairingCompleted = true
            self.lastClientName = request.deviceName ?? String(localized: "unknown_client_device")
            self.lastConnectionDate = Date()
            self.pairedDevices.register(id: request.deviceId, name: self.lastClientName)

            UserDefaults.standard.set(true, forKey: "pairingCompleted")
            UserDefaults.standard.set(self.lastClientName, forKey: "lastClientName")
            UserDefaults.standard.set(Date(), forKey: "lastConnectionDate")
        }
        
        sendJson(responseJson, connection: connection)
    }

    private func rotatePairingCode() {
        pairingCode = UUID().uuidString
        pairingCodeCreatedAt = Date()
    }
    
    func refreshPairingCodeIfNeeded(force: Bool = false) {
        let age = Date().timeIntervalSince(pairingCodeCreatedAt)

        if force || age > pairingCodeRefreshThreshold {
            rotatePairingCode()
        }
    }
    
    private func markClientHeartbeat(connection: NWConnection, requestText: String? = nil) {
        let address: String

        switch connection.endpoint {
        case .hostPort(let host, _):
            address = "\(host)"
        default:
            address = String(localized: "unknown_client_device")
        }

        let deviceId = requestText.flatMap { self.headerValue("X-NotifyBridge-Device-Id", from: $0) }
        DispatchQueue.main.async {
            self.lastClientAddress = address
            self.lastClientHeartbeatDate = Date()

            UserDefaults.standard.set(address, forKey: "lastClientAddress")
            UserDefaults.standard.set(Date(), forKey: "lastClientHeartbeatDate")
            if let deviceId { self.pairedDevices.heartbeat(id: deviceId, address: address) }
        }
    }
    
    private func validateHealthToken(requestText: String) -> Bool {
        guard let token = headerValue("X-NotifyBridge-Token", from: requestText) else {
            return false
        }

        return token == pairingToken
    }
    
    private func sendPendingActions(_ connection: NWConnection) {
        let actions = NotificationActionQueue.shared.drain()

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601

        guard let data = try? encoder.encode(actions),
              let json = String(data: data, encoding: .utf8) else {
            sendBadRequest(connection)
            return
        }

        print("Sending pending actions:", actions.count)

        sendJson(json, connection: connection)
    }

    /// Clears the paired device state and rotates the pairing token.
    func resetPairing() {
        let token = UUID().uuidString

        pairingToken = token
        pairingCompleted = false
        lastClientAddress = ""
        lastConnectionDate = nil
        lastClientName = ""
        lastClientHeartbeatDate = nil

        UserDefaults.standard.set(token, forKey: "pairingToken")
        UserDefaults.standard.set(false, forKey: "pairingCompleted")
        UserDefaults.standard.removeObject(forKey: "lastClientAddress")
        UserDefaults.standard.removeObject(forKey: "lastConnectionDate")
        UserDefaults.standard.removeObject(forKey: "lastClientName")
        UserDefaults.standard.removeObject(forKey: "lastClientHeartbeatDate")
    }
}

struct PairingRequest: Codable {
    let code: String
    let deviceName: String?
    let deviceId: String
}
