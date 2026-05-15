//
//  EncryptedPayload.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import Foundation

/// Represents an encrypted notification payload received from the Android client.
struct EncryptedPayload: Codable {
    let iv: String
    let ciphertext: String
}
