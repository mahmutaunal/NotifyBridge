import Foundation

final class NotificationIngressService {
    private let presenter: NotificationPresenter

    init(presenter: NotificationPresenter) {
        self.presenter = presenter
    }

    func handle(payload: NotificationPayload, deviceId: String, shouldPresent: Bool, completion: @escaping (Result<Void, Error>) -> Void) {
        Task { @MainActor in
            do {
                _ = try NotificationHistoryStore.shared.upsert(payload, deviceId: deviceId)
                if shouldPresent && payload.eventType != "notification_removed" {
                    presenter.show(payload: payload)
                }
                completion(.success(()))
            } catch {
                completion(.failure(error))
            }
        }
    }
}
