import Flutter
import UIKit
import CoreLocation
import UserNotifications

public class BackgroundTrackerPlugin: NSObject, FlutterPlugin, CLLocationManagerDelegate {
  private var locationManager: CLLocationManager?
  private var channel: FlutterMethodChannel?
  private let database = LocationDatabase.shared
  private var config: TrackerConfig?
  private let defaults = UserDefaults.standard
  private let configKey = "tracker_config"
  private let configKey = "tracker_config"
  private var activityRecognizer: ActivityRecognizer?
  private var currentActivity: String = "UNKNOWN"
  private let geofenceManager = GeofenceManager.shared
  private let syncManager = SyncManager()

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "background_tracker", binaryMessenger: registrar.messenger())
    let instance = BackgroundTrackerPlugin()
    instance.channel = channel
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getPlatformVersion":
      result("iOS " + UIDevice.current.systemVersion)
    case "startTracking":
      startTracking()
      result(true)
    case "stopTracking":
      stopTracking()
      result(true)
    case "requestPermissions":
      locationManager?.requestAlwaysAuthorization()
      result(true)
    case "configure":
      if let args = call.arguments as? [String: Any] {
        defaults.set(args, forKey: configKey)
        config = TrackerConfig(from: args)
        updateLocationManager()
      }
      result(nil)
    case "getLocations":
      let args = call.arguments as? [String: Any]
      let limit = args?["limit"] as? Int ?? 100
      let locations = database.getUnsyncedLocations(limit: limit)
      result(locations)
    case "getLocationCount":
      let count = database.getLocationCount()
      result(count)
    case "clearDatabase":
      database.clearAll()
      result(nil)
    case "addGeofence":
        if let args = call.arguments as? [String: Any],
           let id = args["id"] as? String,
           let lat = args["latitude"] as? Double,
           let lng = args["longitude"] as? Double,
           let radius = args["radius"] as? Double {
            
            let region = geofenceManager.createRegion(id: id, lat: lat, lng: lng, radius: radius)
            locationManager?.startMonitoring(for: region)
            
            geofenceManager.saveGeofenceMeta(
                id: id,
                enterMsg: args["notifyOnEntry"] as? String,
                exitMsg: args["notifyOnExit"] as? String,
                dwellMsg: args["notifyOnDwell"] as? String
            )
            result(nil)
        } else {
            result(FlutterError(code: "INVALID_ARGS", message: "Missing geofence args", details: nil))
        }
    case "removeGeofence":
        if let id = call.arguments as? String {
            // Find monitored region
            if let regions = locationManager?.monitoredRegions {
                for region in regions {
                    if region.identifier == id {
                        locationManager?.stopMonitoring(for: region)
                    }
                }
            }
            geofenceManager.removeGeofenceMeta(id: id)
            result(nil)
        } else {
            result(FlutterError(code: "INVALID_ARGS", message: "Missing id", details: nil))
        }
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  private func startTracking() {
    if locationManager == nil {
      locationManager = CLLocationManager()
      locationManager?.delegate = self
      locationManager?.allowsBackgroundLocationUpdates = true
      locationManager?.pausesLocationUpdatesAutomatically = false
      
      // Load saved config
      if let savedConfig = defaults.dictionary(forKey: configKey) {
          config = TrackerConfig(from: savedConfig)
      }
      
      // Initialize activity recognizer
      activityRecognizer = ActivityRecognizer { [weak self] (activity, confidence) in
          self?.handleActivityChange(activity)
      }
      
      updateLocationManager()
    }

    locationManager?.requestAlwaysAuthorization()
    locationManager?.startUpdatingLocation()
    
    if config?.enableActivityRecognition == true {
        activityRecognizer?.start()
    }
    
    syncManager.start()
  }
  
  private func stopTracking() {
    locationManager?.stopUpdatingLocation()
    activityRecognizer?.stop()
    syncManager.stop()
  }
  
  private func handleActivityChange(_ activity: String) {
    currentActivity = activity
    
    guard let config = config, config.enableActivityRecognition else { return }
    
    // Adaptive tracking logic
    switch activity {
    case "STILL":
        // Pause updates or switch to significant changes
        // locationManager?.stopUpdatingLocation() 
        // Better: monitoringSignificantLocationChanges or strict filter
        locationManager?.distanceFilter = 50 // Relax filter
        
        if config.enableMotionDetection {
             // If motion detection is ON, we might want to pause strictly
             // But let's just relax for now to avoid complexity of restart
             locationManager?.pausesLocationUpdatesAutomatically = true
        }
        
    case "WALKING":
        locationManager?.distanceFilter = 10
        locationManager?.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        locationManager?.pausesLocationUpdatesAutomatically = false
        
    case "RUNNING":
        locationManager?.distanceFilter = 5
        locationManager?.desiredAccuracy = kCLLocationAccuracyBest
        locationManager?.pausesLocationUpdatesAutomatically = false
        
    case "DRIVING":
        locationManager?.distanceFilter = 50
        locationManager?.desiredAccuracy = kCLLocationAccuracyNavigation
        locationManager?.pausesLocationUpdatesAutomatically = false
        
    default:
        break
    }
  }
  
  private func updateLocationManager() {
    guard let config = config else {
        // Default settings
        locationManager?.distanceFilter = 10
        locationManager?.desiredAccuracy = kCLLocationAccuracyBest
        return
    }
    
    locationManager?.distanceFilter = config.distanceFilter
    locationManager?.desiredAccuracy = config.clAccuracy
    
    // iOS doesn't have direct "interval" based tracking without custom logic
    // but distance filter handles most cases. 
    // For time-based, we might need a timer, but let's stick to distance for now as it's efficient.
  }

  private func stopTracking() {
    locationManager?.stopUpdatingLocation()
  }

  // MARK: - CLLocationManagerDelegate
  public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    guard let location = locations.last else { return }
    
    let locationData: [String: Any] = [
      "latitude": location.coordinate.latitude,
      "longitude": location.coordinate.longitude
    ]
    
    channel?.invokeMethod("onLocation", arguments: locationData)
    
    // Store in database
    database.insertLocation(
        latitude: location.coordinate.latitude,
        longitude: location.coordinate.longitude,
        accuracy: location.horizontalAccuracy,
        altitude: location.altitude,
        speed: location.speed,
        bearing: location.course,
        timestamp: location.timestamp.timeIntervalSince1970 * 1000,
        activity: currentActivity,
        batteryLevel: Int(UIDevice.current.batteryLevel * 100)
    )
  }

  public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    print("Location update failed: \(error.localizedDescription)")
  }
  
  // MARK: - Geofencing Delegate Methods
    public func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        handleGeofenceEvent(region: region, type: "ENTER")
    }
    
    public func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        handleGeofenceEvent(region: region, type: "EXIT")
    }
    
    private func handleGeofenceEvent(region: CLRegion, type: String) {
        guard let region = region as? CLCircularRegion else { return }
        let id = region.identifier
        
        print("Geofence \(type): \(id)")
        
        // Notify Flutter
        let args: [String: Any] = ["id": id, "transition": type]
        channel?.invokeMethod("onGeofenceEvent", arguments: args)
        
        // Show local notification if configured
        if let meta = geofenceManager.getGeofenceMeta(id: id) {
            let msgKey = type.lowercased()
            if let message = meta[msgKey], !message.isEmpty {
                showNotification(body: message)
            }
        }
    }
    
    private func showNotification(body: String) {
        let content = UNMutableNotificationContent()
        content.title = "Background Tracker"
        content.body = body
        content.sound = .default
        
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
}
