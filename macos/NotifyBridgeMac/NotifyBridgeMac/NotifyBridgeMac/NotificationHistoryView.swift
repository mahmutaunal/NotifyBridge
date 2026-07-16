import SwiftUI

struct NotificationHistoryView: View {
    @ObservedObject private var store = NotificationHistoryStore.shared
    @State private var searchText = ""
    @State private var selectedID: String?

    private var filtered: [NotificationHistoryRecord] {
        guard !searchText.isEmpty else { return store.records }
        return store.records.filter { record in
            [record.appName, record.packageName, record.title, record.text, record.deviceName]
                .compactMap { $0 }.contains { $0.localizedCaseInsensitiveContains(searchText) }
        }
    }

    var body: some View {
        NavigationSplitView {
            List(filtered, selection: $selectedID) { record in
                VStack(alignment: .leading, spacing: 4) {
                    HStack { Text(record.appName ?? record.packageName).fontWeight(.semibold); Spacer(); Text(record.postedAt, style: .time).font(.caption).foregroundStyle(.secondary) }
                    if let title = record.title, !title.isEmpty { Text(title).lineLimit(1) }
                    if let text = record.text, !text.isEmpty { Text(text).font(.caption).foregroundStyle(.secondary).lineLimit(2) }
                    Text("\(record.lifecycleState) • \(record.deviceName ?? record.deviceId)").font(.caption2).foregroundStyle(.tertiary)
                }
                .padding(.vertical, 4)
                .tag(record.id)
            }
            .searchable(text: $searchText, prompt: "Bildirimlerde ara")
            .navigationTitle("Bildirim Geçmişi")
            .toolbar { Button(role: .destructive) { store.clear() } label: { Image(systemName: "trash") } }
        } detail: {
            if let id = selectedID, let record = store.records.first(where: { $0.id == id }) {
                NotificationHistoryDetail(record: record)
                    .onAppear { store.markSeen(record.id) }
            } else {
                ContentUnavailableView("Bir bildirim seçin", systemImage: "bell.badge")
            }
        }
        .frame(minWidth: 820, minHeight: 520)
    }
}

private struct NotificationHistoryDetail: View {
    let record: NotificationHistoryRecord
    @ObservedObject private var store = NotificationHistoryStore.shared
    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(record.appName ?? record.packageName).font(.largeTitle.bold())
            if let title = record.title { Text(title).font(.title2) }
            if let text = record.text { Text(text).font(.body).textSelection(.enabled) }
            Divider()
            LabeledContent("Android cihaz", value: record.deviceName ?? record.deviceId)
            LabeledContent("Alınma", value: record.receivedAt.formatted(date: .abbreviated, time: .standard))
            LabeledContent("Durum", value: record.lifecycleState)
            Spacer()
            HStack { Button("Metni Kopyala") { NSPasteboard.general.clearContents(); NSPasteboard.general.setString([record.title, record.text].compactMap{$0}.joined(separator: "\n"), forType: .string) }; Spacer(); Button("Sil", role: .destructive) { store.delete(record.id) } }
        }.padding(28)
    }
}
