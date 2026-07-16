import Foundation
import Combine

struct NotificationHistoryRecord: Codable, Identifiable, Hashable {
    let id: String
    var sourceKey: String
    var deviceId: String
    var deviceName: String?
    var packageName: String
    var appName: String?
    var title: String?
    var text: String?
    var postedAt: Date
    var updatedAt: Date
    var removedAt: Date?
    var lifecycleState: String
    var isSeen: Bool
    var receivedAt: Date
    var canDismiss: Bool
    var canOpenOnPhone: Bool
    var canReply: Bool
}

@MainActor
final class NotificationHistoryStore: ObservableObject {
    static let shared = NotificationHistoryStore()
    @Published private(set) var records: [NotificationHistoryRecord] = []
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()
    private let retentionDays = 7
    private let maximumRecords = 5_000

    private var fileURL: URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        let directory = base.appendingPathComponent("NotifyBridge", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory.appendingPathComponent("notification-history.json")
    }

    private init() { load() }

    @discardableResult
    func upsert(_ payload: NotificationPayload, deviceId: String) throws -> NotificationHistoryRecord {
        let historyId = payload.historyId ?? payload.notificationKey ?? UUID().uuidString
        let sourceKey = payload.notificationKey ?? historyId
        let now = Date()
        let postedAt = Date(timeIntervalSince1970: TimeInterval(payload.postTime) / 1000)
        var record = NotificationHistoryRecord(
            id: historyId, sourceKey: sourceKey, deviceId: deviceId, deviceName: payload.deviceName,
            packageName: payload.packageName, appName: payload.appName, title: payload.title, text: payload.text,
            postedAt: postedAt, updatedAt: now, removedAt: payload.eventType == "notification_removed" ? now : nil,
            lifecycleState: payload.eventType == "notification_removed" ? "REMOVED" : "ACTIVE", isSeen: false,
            receivedAt: now, canDismiss: payload.canDismiss ?? true, canOpenOnPhone: payload.canOpenOnPhone ?? true,
            canReply: payload.canReply ?? false
        )
        if let index = records.firstIndex(where: { $0.id == historyId || ($0.sourceKey == sourceKey && $0.deviceId == deviceId) }) {
            record.isSeen = records[index].isSeen
            records[index] = record
        } else {
            records.insert(record, at: 0)
        }
        cleanup()
        try persist()
        return record
    }

    func markSeen(_ id: String) { mutate(id) { $0.isSeen = true } }
    func delete(_ id: String) { records.removeAll { $0.id == id }; try? persist() }
    func clear() { records.removeAll(); try? persist() }

    private func mutate(_ id: String, change: (inout NotificationHistoryRecord) -> Void) {
        guard let index = records.firstIndex(where: { $0.id == id }) else { return }
        change(&records[index]); try? persist()
    }

    private func cleanup() {
        let cutoff = Calendar.current.date(byAdding: .day, value: -retentionDays, to: Date()) ?? .distantPast
        records = Array(records.filter { $0.postedAt >= cutoff }.sorted { $0.postedAt > $1.postedAt }.prefix(maximumRecords))
    }

    private func load() {
        guard let data = try? Data(contentsOf: fileURL), let decoded = try? decoder.decode([NotificationHistoryRecord].self, from: data) else { return }
        records = decoded; cleanup()
    }

    private func persist() throws {
        let data = try encoder.encode(records)
        try data.write(to: fileURL, options: [.atomic])
    }
}
