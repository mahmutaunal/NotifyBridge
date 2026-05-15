//
//  PairingPayload.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import Foundation

/// Contains the local connection details shared with the Android app during QR pairing.
struct PairingPayload: Codable {
    let type: String
    let host: String
    let port: Int
    let secret: String
    let name: String
}
