/// Represents a circular geofence
class Geofence {
  /// Unique identifier for the geofence
  final String id;

  /// Latitude of the center point
  final double latitude;

  /// Longitude of the center point
  final double longitude;

  /// Radius in meters
  final double radius;

  /// Notification text to show when entering the geofence
  /// If null, no notification is shown
  final String? notifyOnEntry;

  /// Notification text to show when exiting the geofence
  /// If null, no notification is shown
  final String? notifyOnExit;

  /// Notification text to show when dwelling in the geofence
  /// If null, no notification is shown
  final String? notifyOnDwell;

  /// Delay in milliseconds before dwell event is triggered
  /// Default: 300000 (5 minutes)
  final int loiteringDelay;

  const Geofence({
    required this.id,
    required this.latitude,
    required this.longitude,
    required this.radius,
    this.notifyOnEntry,
    this.notifyOnExit,
    this.notifyOnDwell,
    this.loiteringDelay = 300000,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'latitude': latitude,
      'longitude': longitude,
      'radius': radius,
      'notifyOnEntry': notifyOnEntry,
      'notifyOnExit': notifyOnExit,
      'notifyOnDwell': notifyOnDwell,
      'loiteringDelay': loiteringDelay,
    };
  }

  factory Geofence.fromJson(Map<String, dynamic> json) {
    return Geofence(
      id: json['id'] as String,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      radius: (json['radius'] as num).toDouble(),
      notifyOnEntry: json['notifyOnEntry'] as String?,
      notifyOnExit: json['notifyOnExit'] as String?,
      notifyOnDwell: json['notifyOnDwell'] as String?,
      loiteringDelay: json['loiteringDelay'] as int? ?? 300000,
    );
  }
}
