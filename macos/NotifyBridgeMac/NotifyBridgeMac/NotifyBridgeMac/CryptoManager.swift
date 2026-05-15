//
//  CryptoManager.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import Foundation
import CryptoKit

/// Handles AES-GCM decryption and HMAC validation used by the local pairing protocol.
enum CryptoManager {

    /// Decrypts a payload received from the Android client.
    static func decryptAesGcm(
        secret: String,
        encryptedPayload: EncryptedPayload
    ) throws -> String {
        let keyData = SHA256.hash(data: Data(secret.utf8))
        let key = SymmetricKey(data: Data(keyData))

        guard let ivData = Data(base64Encoded: encryptedPayload.iv),
              let combinedCipherData = Data(base64Encoded: encryptedPayload.ciphertext) else {
            throw CryptoError.invalidBase64
        }

        guard combinedCipherData.count > 16 else {
            throw CryptoError.invalidCiphertext
        }

        // The last 16 bytes contain the AES-GCM authentication tag.
        let cipherText = combinedCipherData.prefix(combinedCipherData.count - 16)
        let tag = combinedCipherData.suffix(16)

        let sealedBox = try AES.GCM.SealedBox(
            nonce: AES.GCM.Nonce(data: ivData),
            ciphertext: cipherText,
            tag: tag
        )

        let decryptedData = try AES.GCM.open(sealedBox, using: key)

        guard let text = String(data: decryptedData, encoding: .utf8) else {
            throw CryptoError.invalidUtf8
        }

        return text
    }

    /// Generates a Base64 encoded HMAC-SHA256 signature for request verification.
    static func hmacSha256Base64(
        secret: String,
        message: String
    ) -> String {
        let key = SymmetricKey(data: Data(secret.utf8))

        let authenticationCode = HMAC<SHA256>.authenticationCode(
            for: Data(message.utf8),
            using: key
        )

        return Data(authenticationCode).base64EncodedString()
    }

    enum CryptoError: Error {
        case invalidBase64
        case invalidUtf8
        case invalidCiphertext
    }
}
