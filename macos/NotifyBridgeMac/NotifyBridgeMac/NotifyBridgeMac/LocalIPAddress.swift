//
//  LocalIPAddress.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import Foundation

/// Resolves the current local Wi-Fi IPv4 address used for pairing and local communication.
enum LocalIPAddress {

    /// Returns the IPv4 address of the primary Wi-Fi interface (`en0`).
    static func getWiFiAddress() -> String {
        var address = "127.0.0.1"

        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else {
            return address
        }

        var ptr = ifaddr
        while ptr != nil {
            defer { ptr = ptr?.pointee.ifa_next }

            guard let interface = ptr?.pointee else { continue }

            let addrFamily = interface.ifa_addr.pointee.sa_family
            let name = String(cString: interface.ifa_name)

            // `en0` is typically the primary Wi-Fi interface on macOS.
            if addrFamily == UInt8(AF_INET), name == "en0" {
                var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))

                getnameinfo(
                    interface.ifa_addr,
                    socklen_t(interface.ifa_addr.pointee.sa_len),
                    &hostname,
                    socklen_t(hostname.count),
                    nil,
                    socklen_t(0),
                    NI_NUMERICHOST
                )

                address = String(cString: hostname)
            }
        }

        freeifaddrs(ifaddr)
        return address
    }
}
