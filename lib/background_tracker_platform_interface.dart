import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'background_tracker_method_channel.dart';
import 'config/tracker_config.dart';
import 'models/location_point.dart';
import 'models/geofence.dart';

abstract class BackgroundTrackerPlatform extends PlatformInterface {
  /// Constructs a BackgroundTrackerPlatform.
  BackgroundTrackerPlatform() : super(token: _token);

  static final Object _token = Object();

  static BackgroundTrackerPlatform _instance = MethodChannelBackgroundTracker();

  /// The default instance of [BackgroundTrackerPlatform] to use.
  ///
  /// Defaults to [MethodChannelBackgroundTracker].
  static BackgroundTrackerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [BackgroundTrackerPlatform] when
  /// they register themselves.
  static set instance(BackgroundTrackerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<bool> requestPermissions() {
    throw UnimplementedError('requestPermissions() has not been implemented.');
  }

  Future<void> configure(TrackerConfig config) {
    throw UnimplementedError('configure() has not been implemented.');
  }

  Future<void> startTracking() {
    throw UnimplementedError('startTracking() has not been implemented.');
  }

  Future<void> stopTracking() {
    throw UnimplementedError('stopTracking() has not been implemented.');
  }

  Future<List<LocationPoint>> getLocations({int? limit}) {
    throw UnimplementedError('getLocations() has not been implemented.');
  }

  Future<int> getLocationCount() {
    throw UnimplementedError('getLocationCount() has not been implemented.');
  }

  Future<void> clearDatabase() {
    throw UnimplementedError('clearDatabase() has not been implemented.');
  }

  Future<void> addGeofence(Geofence geofence) {
    throw UnimplementedError('addGeofence() has not been implemented.');
  }

  Future<void> removeGeofence(String geofenceId) {
    throw UnimplementedError('removeGeofence() has not been implemented.');
  }

  Stream<Map<String, dynamic>> get onLocation {
    throw UnimplementedError('onLocation has not been implemented.');
  }
}
