import Foundation
import CoreMotion

class ActivityRecognizer {
    private let activityManager = CMMotionActivityManager()
    private var isTracking = false
    private let onActivityChanged: (String, Int) -> Void
    
    // Last known state
    private var currentActivity = "UNKNOWN"
    private var currentConfidence = 0
    
    init(onActivityChanged: @escaping (String, Int) -> Void) {
        self.onActivityChanged = onActivityChanged
    }
    
    func start() {
        guard CMMotionActivityManager.isActivityAvailable() else {
            print("Activity recognition not available on this device")
            return
        }
        
        if isTracking { return }
        isTracking = true
        
        activityManager.startActivityUpdates(to: OperationQueue.main) { [weak self] (activity: CMMotionActivity?) in
            guard let self = self, let activity = activity else { return }
            
            let (type, confidence) = self.parseActivity(activity)
            
            // Only update if changed or significant
            if type != self.currentActivity {
                self.currentActivity = type
                self.currentConfidence = confidence
                self.onActivityChanged(type, confidence)
                // print("iOS Activity Changed: \(type) (\(confidence)%)")
            }
        }
    }
    
    func stop() {
        activityManager.stopActivityUpdates()
        isTracking = false
    }
    
    private func parseActivity(_ activity: CMMotionActivity) -> (String, Int) {
        var type = "UNKNOWN"
        
        if activity.stationary { type = "STILL" }
        else if activity.walking { type = "WALKING" }
        else if activity.running { type = "RUNNING" }
        else if activity.automotive { type = "DRIVING" }
        else if activity.cycling { type = "CYCLING" }
        
        let confidence: Int
        switch activity.confidence {
        case .high: confidence = 100
        case .medium: confidence = 75
        case .low: confidence = 50
        @unknown default: confidence = 0
        }
        
        return (type, confidence)
    }
    
    func getActivity() -> String {
        return currentActivity
    }
}
