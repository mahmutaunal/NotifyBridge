import Foundation
import Crypto
import X509
import SwiftASN1
import Security

/// Creates and stores the self-signed TLS identity used by the local HTTPS server.
final class TLSIdentityManager {

    /// Shared TLS identity manager used by the Mac app server.
    static let shared = TLSIdentityManager()

    /// Keychain-backed identity containing the certificate and private key.
    private(set) var identity: SecIdentity?
    /// DER-encoded certificate shared with the Android client during pairing.
    private(set) var certificateDer: Data?
    /// SHA-256 certificate fingerprint used by Android for TLS pinning.
    private(set) var fingerprint: String?

    private init() {
        createIdentity()
    }

    /// Generates a self-signed certificate and stores the TLS identity in Keychain.
    private func createIdentity() {
        do {
            // Generate a fresh elliptic-curve private key for the local TLS identity.
            let privateKey = P256.Signing.PrivateKey()
            let publicKey = privateKey.publicKey

            // Use a minimal subject name because the certificate is pinned, not publicly trusted.
            let distinguishedName = try DistinguishedName {
                CommonName("NotifyBridge")
            }

            let extensions = try Certificate.Extensions {
                BasicConstraints.notCertificateAuthority
                KeyUsage(digitalSignature: true)
            }

            // Build a self-signed certificate for local HTTPS communication.
            let certificate = try Certificate(
                version: .v3,
                serialNumber: .init(),
                publicKey: .init(publicKey),
                notValidBefore: Date(),
                notValidAfter: Calendar.current.date(
                    byAdding: .year,
                    value: 10,
                    to: Date()
                )!,
                issuer: distinguishedName,
                subject: distinguishedName,
                signatureAlgorithm: .ecdsaWithSHA256,
                extensions: extensions,
                issuerPrivateKey: Certificate.PrivateKey(privateKey)
            )

            // Serialize the certificate so it can be stored and fingerprinted.
            var serializer = DER.Serializer()
            try serializer.serialize(certificate)

            let certDer = Data(serializer.serializedBytes)
            self.certificateDer = certDer

            // Calculate the certificate fingerprint used by the Android TLS pin.
            self.fingerprint = SHA256
                .hash(data: certDer)
                .map { String(format: "%02x", $0) }
                .joined()

            // Convert the Swift certificate into a Security framework certificate.
            guard let secCertificate = SecCertificateCreateWithData(
                nil,
                certDer as CFData
            ) else {
                print("Failed to create SecCertificate")
                return
            }

            // Convert the generated private key into a SecKey-compatible representation.
            let privateKeyData = privateKey.x963Representation as CFData

            let attributes: [String: Any] = [
                kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
                kSecAttrKeyClass as String: kSecAttrKeyClassPrivate,
                kSecAttrKeySizeInBits as String: 256
            ]

            guard let secPrivateKey = SecKeyCreateWithData(
                privateKeyData,
                attributes as CFDictionary,
                nil
            ) else {
                print("Failed to create SecKey from private key")
                return
            }

            let keyTag = Data("com.alpware.notifybridge.tls.privatekey".utf8)
            let certificateLabel = "NotifyBridge TLS Certificate"

            // Replace any previous TLS private key with the newly generated one.
            SecItemDelete([
                kSecClass as String: kSecClassKey,
                kSecAttrApplicationTag as String: keyTag
            ] as CFDictionary)

            // Store the private key in Keychain so it can be associated with the certificate.
            let addKeyQuery: [String: Any] = [
                kSecClass as String: kSecClassKey,
                kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
                kSecAttrKeyClass as String: kSecAttrKeyClassPrivate,
                kSecAttrKeySizeInBits as String: 256,
                kSecAttrApplicationTag as String: keyTag,
                kSecAttrLabel as String: "NotifyBridge TLS Private Key",
                kSecValueRef as String: secPrivateKey,
                kSecReturnPersistentRef as String: true
            ]

            let addKeyStatus = SecItemAdd(addKeyQuery as CFDictionary, nil)
            guard addKeyStatus == errSecSuccess else {
                print("Failed to add TLS private key to Keychain:", addKeyStatus)
                return
            }

            // Replace any previous TLS certificate stored under the same label.
            SecItemDelete([
                kSecClass as String: kSecClassCertificate,
                kSecAttrLabel as String: certificateLabel
            ] as CFDictionary)

            // Store the generated certificate in Keychain.
            let addCertQuery: [String: Any] = [
                kSecClass as String: kSecClassCertificate,
                kSecAttrLabel as String: certificateLabel,
                kSecValueRef as String: secCertificate,
                kSecReturnPersistentRef as String: true
            ]

            let addCertStatus = SecItemAdd(addCertQuery as CFDictionary, nil)
            guard addCertStatus == errSecSuccess else {
                print("Failed to add TLS certificate to Keychain:", addCertStatus)
                return
            }

            // Create the TLS identity from the stored certificate and matching private key.
            var secIdentity: SecIdentity?

            let status = SecIdentityCreateWithCertificate(
                nil,
                secCertificate,
                &secIdentity
            )

            if status == errSecSuccess, let secIdentity {
                self.identity = secIdentity
            } else {
                print("Failed to create SecIdentity:", status)
                self.identity = findStoredIdentity(for: secCertificate)
            }

        } catch {
            print("TLS identity generation failed:", error)
        }
    }
    /// Finds a Keychain identity whose certificate matches the generated certificate.
    private func findStoredIdentity(for certificate: SecCertificate) -> SecIdentity? {
        // Query all identities because Security does not always return the new identity directly.
        let query: [String: Any] = [
            kSecClass as String: kSecClassIdentity,
            kSecReturnRef as String: true,
            kSecMatchLimit as String: kSecMatchLimitAll
        ]

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess else {
            print("Failed to query TLS identity from Keychain:", status)
            return nil
        }

        let identities = result as? [SecIdentity] ?? [result as! SecIdentity]

        // Match identities by comparing certificate DER data.
        for identity in identities {
            var identityCertificate: SecCertificate?
            SecIdentityCopyCertificate(identity, &identityCertificate)

            if let identityCertificate,
               SecCertificateCopyData(identityCertificate) as Data == SecCertificateCopyData(certificate) as Data {
                return identity
            }
        }

        print("Matching TLS identity was not found in Keychain")
        return nil
    }
}
