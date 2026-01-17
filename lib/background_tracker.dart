
import 'background_tracker_platform_interface.dart';
import 'config/tracker_config.dart';
import 'models/location_point.dart';
import 'models/geofence.dart';

class BackgroundTracker {
  Future<String?> getPlatformVersion() {
    return BackgroundTrackerPlatform.instance.getPlatformVersion();
  }

  /// Configure the tracker with advanced settings
  static Future<void> configure(TrackerConfig config) {
    return BackgroundTrackerPlatform.instance.configure(config);
  }

  static Future<bool> requestPermissions() {
    return BackgroundTrackerPlatform.instance.requestPermissions();
  }

  static Future<void> startTracking() {
    return BackgroundTrackerPlatform.instance.startTracking();
  }

  static Future<void> stopTracking() {
    return BackgroundTrackerPlatform.instance.stopTracking();
  }

  /// Get stored locations from database
  static Future<List<LocationPoint>> getLocations({int? limit}) {
    return BackgroundTrackerPlatform.instance.getLocations(limit: limit);
  }

  /// Get count of stored locations
  static Future<int> getLocationCount() {
    return BackgroundTrackerPlatform.instance.getLocationCount();
  }

  /// Clear all stored locations
  static Future<void> clearDatabase() {
    return BackgroundTrackerPlatform.instance.clearDatabase();
  }

  /// Add a geofence
  static Future<void> addGeofence(Geofence geofence) {
    return BackgroundTrackerPlatform.instance.addGeofence(geofence);
  }

  /// Remove a geofence by ID
  static Future<void> removeGeofence(String geofenceId) {
    return BackgroundTrackerPlatform.instance.removeGeofence(geofenceId);
  }

  static Stream<Map<String, dynamic>> get onLocation {
    return BackgroundTrackerPlatform.instance.onLocation;
  }
}
