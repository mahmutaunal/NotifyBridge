//
//  PairingResponse.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 22.05.2026.
//

import Foundation

struct PairingResponse: Codable {
    let type: String
    let host: String
    let port: Int
    let secret: String
    let name: String
}
