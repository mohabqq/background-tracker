/// Represents a single location point
class LocationPoint {
  final double latitude;
  final double longitude;
  final double? accuracy;
  final double? altitude;
  final double? speed;
  final double? bearing;
  final DateTime timestamp;
  final String? activity;
  final int? batteryLevel;
  final bool isSynced;

  const LocationPoint({
    required this.latitude,
    required this.longitude,
    this.accuracy,
    this.altitude,
    this.speed,
    this.bearing,
    required this.timestamp,
    this.activity,
    this.batteryLevel,
    this.isSynced = false,
  });

  /// Create from JSON
  factory LocationPoint.fromJson(Map<String, dynamic> json) {
    return LocationPoint(
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      accuracy: (json['accuracy'] as num?)?.toDouble(),
      altitude: (json['altitude'] as num?)?.toDouble(),
      speed: (json['speed'] as num?)?.toDouble(),
      bearing: (json['bearing'] as num?)?.toDouble(),
      timestamp: DateTime.fromMillisecondsSinceEpoch(json['timestamp'] as int),
      activity: json['activity'] as String?,
      batteryLevel: json['batteryLevel'] as int?,
      isSynced: json['isSynced'] as bool? ?? false,
    );
  }

  /// Convert to JSON
  Map<String, dynamic> toJson() {
    return {
      'latitude': latitude,
      'longitude': longitude,
      'accuracy': accuracy,
      'altitude': altitude,
      'speed': speed,
      'bearing': bearing,
      'timestamp': timestamp.millisecondsSinceEpoch,
      'activity': activity,
      'batteryLevel': batteryLevel,
      'isSynced': isSynced,
    };
  }

  @override
  String toString() {
    return 'LocationPoint(lat: $latitude, lng: $longitude, time: $timestamp)';
  }
}
