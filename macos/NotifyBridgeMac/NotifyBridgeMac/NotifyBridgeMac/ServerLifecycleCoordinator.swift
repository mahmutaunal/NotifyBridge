import AppKit
import Foundation
import Network

/// Restarts the local listener and Bonjour advertisement after wake, app activation, or LAN changes.
final class ServerLifecycleCoordinator {
    private weak var server: LocalNotificationServer?
    private let pathMonitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "com.alpware.notifybridge.network-monitor")
    private var observers: [NSObjectProtocol] = []
    private var restartWorkItem: DispatchWorkItem?

    init(server: LocalNotificationServer) {
        self.server = server

        observers.append(NotificationCenter.default.addObserver(
            forName: NSWorkspace.didWakeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in self?.scheduleRestart() })

        observers.append(NotificationCenter.default.addObserver(
            forName: NSApplication.didBecomeActiveNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in self?.ensureRunning() })

        pathMonitor.pathUpdateHandler = { [weak self] path in
            guard path.status == .satisfied else { return }
            self?.scheduleRestart(delay: 0.8)
        }
        pathMonitor.start(queue: monitorQueue)
    }

    deinit {
        pathMonitor.cancel()
        observers.forEach(NotificationCenter.default.removeObserver)
    }

    func ensureRunning() {
        DispatchQueue.main.async { [weak self] in self?.server?.start() }
    }

    private func scheduleRestart(delay: TimeInterval = 0.35) {
        restartWorkItem?.cancel()
        let item = DispatchWorkItem { [weak self] in
            DispatchQueue.main.async { self?.server?.restart() }
        }
        restartWorkItem = item
        monitorQueue.asyncAfter(deadline: .now() + delay, execute: item)
    }
}
