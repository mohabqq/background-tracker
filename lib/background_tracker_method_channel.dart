import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'background_tracker_platform_interface.dart';
import 'config/tracker_config.dart';
import 'models/location_point.dart';
import 'models/geofence.dart';

/// An implementation of [BackgroundTrackerPlatform] that uses method channels.
class MethodChannelBackgroundTracker extends BackgroundTrackerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('background_tracker');

  final StreamController<Map<String, dynamic>> _locationController =
      StreamController<Map<String, dynamic>>.broadcast();

  MethodChannelBackgroundTracker() {
    methodChannel.setMethodCallHandler(_handleMethodCall);
  }

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    if (call.method == 'onLocation') {
      final Map<dynamic, dynamic> args = call.arguments;
      final Map<String, dynamic> location = Map<String, dynamic>.from(args);
      _locationController.add(location);
    }
  }

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<bool> requestPermissions() async {
    return await methodChannel.invokeMethod<bool>('requestPermissions') ?? false;
  }

  @override
  Future<void> configure(TrackerConfig config) async {
    await methodChannel.invokeMethod('configure', config.toJson());
  }

  @override
  Future<void> startTracking() async {
    await methodChannel.invokeMethod('startTracking');
  }

  @override
  Future<void> stopTracking() async {
    await methodChannel.invokeMethod('stopTracking');
  }

  @override
  Future<List<LocationPoint>> getLocations({int? limit}) async {
    final List<dynamic>? result = await methodChannel.invokeMethod('getLocations', {'limit': limit});
    if (result == null) return [];
    return result.map((e) => LocationPoint.fromJson(Map<String, dynamic>.from(e))).toList();
  }

  @override
  Future<int> getLocationCount() async {
    return await methodChannel.invokeMethod<int>('getLocationCount') ?? 0;
  }

  @override
  Future<void> clearDatabase() async {
    await methodChannel.invokeMethod('clearDatabase');
  }

  @override
  Future<void> addGeofence(Geofence geofence) async {
    await methodChannel.invokeMethod('addGeofence', geofence.toJson());
  }

  @override
  Future<void> removeGeofence(String geofenceId) async {
    await methodChannel.invokeMethod('removeGeofence', geofenceId);
  }

  @override
  Stream<Map<String, dynamic>> get onLocation => _locationController.stream;
}
