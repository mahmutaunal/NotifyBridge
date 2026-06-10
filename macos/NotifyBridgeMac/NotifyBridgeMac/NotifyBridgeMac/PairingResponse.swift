//
//  PairingResponse.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 22.05.2026.
//

import Foundation

/// Response returned to Android after a successful pairing request.
struct PairingResponse: Codable {
    let type: String
    let host: String
    let port: Int
    let secret: String
    let name: String
}
