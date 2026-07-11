import Foundation
import Combine

struct PairedAndroidDevice: Codable, Identifiable, Hashable {
    let id: String
    var name: String
    var lastAddress: String
    var pairedAt: Date
    var lastSeenAt: Date?
    var isEnabled: Bool

    var isOnline: Bool {
        guard let lastSeenAt else { return false }
        return Date().timeIntervalSince(lastSeenAt) < 45
    }
}

final class PairedAndroidDeviceStore: ObservableObject {
    static let shared = PairedAndroidDeviceStore()
    @Published private(set) var devices: [PairedAndroidDevice] = []
    private let key = "pairedAndroidDevicesV2"

    private init() { load() }

    func register(id: String, name: String, address: String = "") {
        if let index = devices.firstIndex(where: { $0.id == id }) {
            devices[index].name = name
            if !address.isEmpty { devices[index].lastAddress = address }
            devices[index].lastSeenAt = Date()
        } else {
            devices.append(PairedAndroidDevice(id: id, name: name, lastAddress: address, pairedAt: Date(), lastSeenAt: Date(), isEnabled: true))
        }
        save()
    }

    func heartbeat(id: String, address: String) {
        guard let index = devices.firstIndex(where: { $0.id == id }) else { return }
        devices[index].lastAddress = address
        devices[index].lastSeenAt = Date()
        save()
    }

    func remove(id: String) { devices.removeAll { $0.id == id }; save() }
    func setEnabled(id: String, enabled: Bool) {
        guard let index = devices.firstIndex(where: { $0.id == id }) else { return }
        devices[index].isEnabled = enabled; save()
    }
    func isAllowed(id: String) -> Bool { devices.first(where: { $0.id == id })?.isEnabled == true }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([PairedAndroidDevice].self, from: data) else { return }
        devices = decoded
    }
    private func save() {
        if let data = try? JSONEncoder().encode(devices) { UserDefaults.standard.set(data, forKey: key) }
    }
}
