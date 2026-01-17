import Foundation
import CoreLocation

struct TrackerConfig {
    let accuracyMode: String
    let distanceFilter: Double
    let timeInterval: Int
    let enableMotionDetection: Bool
    let enableActivityRecognition: Bool
    let syncUrl: String?
    let syncInterval: Int
    
    init(from dictionary: [String: Any]) {
        self.accuracyMode = dictionary["accuracyMode"] as? String ?? "balanced"
        self.distanceFilter = dictionary["distanceFilter"] as? Double ?? 10.0
        self.timeInterval = dictionary["timeInterval"] as? Int ?? 10
        self.enableMotionDetection = dictionary["enableMotionDetection"] as? Bool ?? false
        self.enableActivityRecognition = dictionary["enableActivityRecognition"] as? Bool ?? false
        self.syncUrl = dictionary["syncUrl"] as? String
        self.syncInterval = dictionary["syncInterval"] as? Int ?? 300
    }
    
    var clAccuracy: CLLocationAccuracy {
        switch accuracyMode {
        case "high": return kCLLocationAccuracyBest
        case "lowPower": return kCLLocationAccuracyKilometer
        case "balanced": return kCLLocationAccuracyNearestTenMeters
        default: return kCLLocationAccuracyNearestTenMeters
        }
    }
}
