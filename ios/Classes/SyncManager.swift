import Foundation

class SyncManager {
    private let database = LocationDatabase.shared
    private let defaults = UserDefaults.standard
    private var isSyncing = false
    private var syncTimer: Timer?
    
    func start() {
        if syncTimer != nil { return }
        
        let config = loadConfig()
        let interval = Double(config["syncInterval"] as? Int ?? 300)
        
        if config["syncUrl"] as? String == nil {
            print("SyncManager: No URL configured")
            return
        }
        
        print("SyncManager: Starting with interval \(interval)s")
        
        // Schedule timer
        syncTimer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { [weak self] _ in
            self?.performSync()
        }
        // Fire immediately
        performSync()
    }
    
    func stop() {
        syncTimer?.invalidate()
        syncTimer = nil
        print("SyncManager: Stopped")
    }
    
    private func loadConfig() -> [String: Any] {
        return defaults.dictionary(forKey: "tracker_config") ?? [:]
    }
    
    private func performSync() {
        if isSyncing { return }
        isSyncing = true
        
        let config = loadConfig()
        guard let urlString = config["syncUrl"] as? String,
              let url = URL(string: urlString) else {
            isSyncing = false
            return
        }
        
        let batchSize = 100 // Hardcoded for simplicity or load from config
        let locations = database.getUnsyncedLocations(limit: batchSize)
        
        if locations.isEmpty {
            // print("SyncManager: No locations to sync")
            isSyncing = false
            return
        }
        
        print("SyncManager: Syncing \(locations.count) locations...")
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Prepare JSON body
        // Convert [String: Any] to JSON data is tricky if Any isn't JSON compatible, but our DB generic dict is usually fine
        // We need to make sure 'ids' are extracted for marking as synced
        
        let ids = locations.compactMap { $0["id"] as? Int64 }
        
        // Clean payload for server (remove local DB id probably, or keep it?)
        // Let's send exactly what we have, server can ignore ID if needed
        
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: locations, options: [])
            request.httpBody = jsonData
            
            let task = URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
                guard let self = self else { return }
                
                if let error = error {
                    print("SyncManager: Error \(error)")
                    self.isSyncing = false
                    return
                }
                
                if let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) {
                    print("SyncManager: Success! Synced \(ids.count) records")
                    self.database.markAsSynced(ids: ids)
                } else {
                    print("SyncManager: Server returned error")
                }
                
                self.isSyncing = false
            }
            task.resume()
            
        } catch {
            print("SyncManager: JSON error \(error)")
            isSyncing = false
        }
    }
}
