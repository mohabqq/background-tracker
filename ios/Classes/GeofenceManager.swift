import Foundation
import CoreLocation

class GeofenceManager {
    static let shared = GeofenceManager()
    private let defaults = UserDefaults.standard
    private let kGeofencePrefsPrefix = "geofence_prefs_"
    
    // Helper to store notification messages for regions
    func saveGeofenceMeta(id: String, enterMsg: String?, exitMsg: String?, dwellMsg: String?) {
        let meta: [String: String] = [
            "enter": enterMsg ?? "",
            "exit": exitMsg ?? "",
            "dwell": dwellMsg ?? ""
        ]
        defaults.set(meta, forKey: kGeofencePrefsPrefix + id)
    }
    
    func getGeofenceMeta(id: String) -> [String: String]? {
        return defaults.dictionary(forKey: kGeofencePrefsPrefix + id) as? [String: String]
    }
    
    func removeGeofenceMeta(id: String) {
        defaults.removeObject(forKey: kGeofencePrefsPrefix + id)
    }
    
    func createRegion(id: String, lat: Double, lng: Double, radius: Double) -> CLCircularRegion {
        let center = CLLocationCoordinate2D(latitude: lat, longitude: lng)
        let region = CLCircularRegion(center: center, radius: radius, identifier: id)
        region.notifyOnEntry = true
        region.notifyOnExit = true
        return region
    }
}
