//
//  BonjourPublisher.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import Foundation

/// Publishes the Mac app on the local network so Android devices can discover it automatically.
final class BonjourPublisher: NSObject, NetServiceDelegate {

    private var service: NetService?

    /// Starts advertising the NotifyBridge service via Bonjour.
    func start(port: Int32 = 8787) {
        service = NetService(
            domain: "local.",
            type: "_notifybridge._tcp.",
            name: Host.current().localizedName ?? String(localized: "bonjour_default_service_name"),
            port: port
        )

        service?.delegate = self
        service?.includesPeerToPeer = true
        service?.publish()
    }

    /// Stops advertising the service and releases the active Bonjour instance.
    func stop() {
        service?.stop()
        service = nil
    }

    func netServiceDidPublish(_ sender: NetService) {
        print("Bonjour published:", sender.name)
    }

    func netService(_ sender: NetService, didNotPublish errorDict: [String : NSNumber]) {
        print("Bonjour publish failed:", errorDict)
    }
}
