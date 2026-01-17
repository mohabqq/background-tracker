/// Accuracy mode for location tracking
enum AccuracyMode {
  /// Highest accuracy, uses GPS primarily (high battery usage)
  high,
  
  /// Balanced accuracy, uses GPS + Network (moderate battery usage)
  balanced,
  
  /// Low power mode, uses Network primarily (low battery usage)
  lowPower,
  
  /// AI-powered adaptive mode, adjusts based on usage patterns
  adaptive,
}

/// Configuration for the Background Tracker
class TrackerConfig {
  /// Accuracy mode for location tracking
  final AccuracyMode accuracyMode;
  
  /// Minimum distance (in meters) before location update is triggered
  /// Default: 10 meters
  final double distanceFilter;
  
  /// Minimum time interval (in seconds) between location updates
  /// Default: 10 seconds
  final int timeInterval;
  
  /// Enable AI-powered adaptive tracking (learns user patterns)
  /// Default: true
  final bool enableAdaptiveTracking;
  
  /// Battery level threshold (%) to switch to low power mode
  /// Default: 20%
  final int lowBatteryThreshold;
  
  /// Enable motion detection to pause tracking when stationary
  /// Default: true
  final bool enableMotionDetection;
  
  /// Time (in seconds) of no motion before pausing tracking
  /// Default: 300 seconds (5 minutes)
  final int stationaryTimeout;
  
  /// Enable activity recognition (walking, running, driving, etc.)
  /// Default: true
  final bool enableActivityRecognition;
  
  /// Maximum database size in MB
  /// Default: 50 MB
  final int maxDatabaseSize;
  
  /// Number of days to retain location data
  /// Default: 7 days
  final int dataRetentionDays;
  
  /// URL for automatic location sync
  /// If null, auto-sync is disabled
  final String? syncUrl;
  
  /// HTTP headers for sync requests (e.g., authentication)
  final Map<String, String>? syncHeaders;
  
  /// Number of locations to batch in each sync request
  /// Default: 100
  final int syncBatchSize;
  
  /// Interval (in seconds) between automatic syncs
  /// Default: 300 seconds (5 minutes)
  final int syncInterval;

  const TrackerConfig({
    this.accuracyMode = AccuracyMode.balanced,
    this.distanceFilter = 10.0,
    this.timeInterval = 10,
    this.enableAdaptiveTracking = true,
    this.lowBatteryThreshold = 20,
    this.enableMotionDetection = true,
    this.stationaryTimeout = 300,
    this.enableActivityRecognition = true,
    this.maxDatabaseSize = 50,
    this.dataRetentionDays = 7,
    this.syncUrl,
    this.syncHeaders,
    this.syncBatchSize = 100,
    this.syncInterval = 300,
  });

  /// Create a copy with modified fields
  TrackerConfig copyWith({
    AccuracyMode? accuracyMode,
    double? distanceFilter,
    int? timeInterval,
    bool? enableAdaptiveTracking,
    int? lowBatteryThreshold,
    bool? enableMotionDetection,
    int? stationaryTimeout,
    bool? enableActivityRecognition,
    int? maxDatabaseSize,
    int? dataRetentionDays,
    String? syncUrl,
    Map<String, String>? syncHeaders,
    int? syncBatchSize,
    int? syncInterval,
  }) {
    return TrackerConfig(
      accuracyMode: accuracyMode ?? this.accuracyMode,
      distanceFilter: distanceFilter ?? this.distanceFilter,
      timeInterval: timeInterval ?? this.timeInterval,
      enableAdaptiveTracking: enableAdaptiveTracking ?? this.enableAdaptiveTracking,
      lowBatteryThreshold: lowBatteryThreshold ?? this.lowBatteryThreshold,
      enableMotionDetection: enableMotionDetection ?? this.enableMotionDetection,
      stationaryTimeout: stationaryTimeout ?? this.stationaryTimeout,
      enableActivityRecognition: enableActivityRecognition ?? this.enableActivityRecognition,
      maxDatabaseSize: maxDatabaseSize ?? this.maxDatabaseSize,
      dataRetentionDays: dataRetentionDays ?? this.dataRetentionDays,
      syncUrl: syncUrl ?? this.syncUrl,
      syncHeaders: syncHeaders ?? this.syncHeaders,
      syncBatchSize: syncBatchSize ?? this.syncBatchSize,
      syncInterval: syncInterval ?? this.syncInterval,
    );
  }

  /// Convert to JSON for platform channel communication
  Map<String, dynamic> toJson() {
    return {
      'accuracyMode': accuracyMode.name,
      'distanceFilter': distanceFilter,
      'timeInterval': timeInterval,
      'enableAdaptiveTracking': enableAdaptiveTracking,
      'lowBatteryThreshold': lowBatteryThreshold,
      'enableMotionDetection': enableMotionDetection,
      'stationaryTimeout': stationaryTimeout,
      'enableActivityRecognition': enableActivityRecognition,
      'maxDatabaseSize': maxDatabaseSize,
      'dataRetentionDays': dataRetentionDays,
      'syncUrl': syncUrl,
      'syncHeaders': syncHeaders,
      'syncBatchSize': syncBatchSize,
      'syncInterval': syncInterval,
    };
  }

  /// Create from JSON
  factory TrackerConfig.fromJson(Map<String, dynamic> json) {
    return TrackerConfig(
      accuracyMode: AccuracyMode.values.firstWhere(
        (e) => e.name == json['accuracyMode'],
        orElse: () => AccuracyMode.balanced,
      ),
      distanceFilter: (json['distanceFilter'] as num?)?.toDouble() ?? 10.0,
      timeInterval: json['timeInterval'] as int? ?? 10,
      enableAdaptiveTracking: json['enableAdaptiveTracking'] as bool? ?? true,
      lowBatteryThreshold: json['lowBatteryThreshold'] as int? ?? 20,
      enableMotionDetection: json['enableMotionDetection'] as bool? ?? true,
      stationaryTimeout: json['stationaryTimeout'] as int? ?? 300,
      enableActivityRecognition: json['enableActivityRecognition'] as bool? ?? true,
      maxDatabaseSize: json['maxDatabaseSize'] as int? ?? 50,
      dataRetentionDays: json['dataRetentionDays'] as int? ?? 7,
      syncUrl: json['syncUrl'] as String?,
      syncHeaders: (json['syncHeaders'] as Map<dynamic, dynamic>?)?.cast<String, String>(),
      syncBatchSize: json['syncBatchSize'] as int? ?? 100,
      syncInterval: json['syncInterval'] as int? ?? 300,
    );
  }
}
