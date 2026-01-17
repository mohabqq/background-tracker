# Flutter Background Tracker Pro

![Pub Version](https://img.shields.io/pub/v/flutter_background_tracker_pro)
![License](https://img.shields.io/github/license/mohabqq/flutter_background_tracker_pro)
![Build Status](https://img.shields.io/github/actions/workflow/status/mohabqq/flutter_background_tracker_pro/main.yml)
![Platform](https://img.shields.io/badge/platform-android%20|%20ios-blue)

A powerful, battery-efficient background location tracking and geofencing plugin for Flutter. Designed to be a robust open-source alternative to paid solutions, offering "Headless" operation, motion detection, and offline synchronization.

## 🚀 Features

*   **Background Tracking**: Persistent tracking even when the app is killed or in the background.
    *   **Android**: Foreground Service with "Sticky" behavior.
    *   **iOS**: Background Location Updates & Significant Location Changes.
*   **Battery Efficiency**:
    *   **Motion Detection**: Auto-pauses tracking when the device is still (Accelerometer/CoreMotion).
    *   **Activity Recognition**: Smartly adjusts accuracy based on activity (Still, Walking, Running, Driving).
    *   **Battery Optimizer**: Reduces frequency or switches to Network provider on low battery (<15%).
*   **Offline Support**:
    *   **SQLite Persistence**: Robust local storage to prevent data loss.
    *   **Auto-Sync**: Background HTTP sync manager with batching and retries.
*   **Geofencing**:
    *   Create circular regions with monitoring events (ENTER, EXIT, DWELL).
*   **Privacy Conscious**: Fully configurable notification titles and descriptions.

## 📦 Installation

Add this to your `pubspec.yaml`:

```yaml
dependencies:
  flutter_background_tracker_pro: ^0.1.0
```

## 🛠️ Configuration

### Android Setup
Detailed instructions in [doc/ANDROID_SETUP.md](doc/ANDROID_SETUP.md).

### iOS Setup
Detailed instructions in [doc/IOS_SETUP.md](doc/IOS_SETUP.md).

## ⚡ Usage

### 1. Initialize & Configure

```dart
import 'package:flutter_background_tracker_pro/background_tracker.dart';
import 'package:flutter_background_tracker_pro/config/tracker_config.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Configure the tracker
  await BackgroundTracker.configure(
    const TrackerConfig(
      accuracyMode: AccuracyMode.balanced,
      distanceFilter: 10.0,      // meters
      timeInterval: 10,          // seconds
      enableMotionDetection: true,
      enableActivityRecognition: true,
      syncUrl: "https://your-api.com/locations",
      syncInterval: 300,         // seconds
    ),
  );
  
  runApp(MyApp());
}
```

### 2. Start Tracking

```dart
// Check permissions first
bool granted = await BackgroundTracker.requestPermissions();
if (granted) {
  await BackgroundTracker.startTracking();
}
```

### 3. Handle Locations

```dart
// Listen to the stream
BackgroundTracker.onLocation.listen((event) {
  print("Location: ${event['latitude']}, ${event['longitude']}");
});

// Or get stored locations manually
List<LocationPoint> history = await BackgroundTracker.getLocations(limit: 50);
```

### 4. Geofencing

```dart
await BackgroundTracker.addGeofence(
  Geofence(
    id: "home_zone",
    latitude: 37.422,
    longitude: -122.084,
    radius: 200, // meters
    notifyOnEntry: "Welcome Home!",
    notifyOnExit: "Goodbye!",
  ),
);
```

## 🔋 Benchmarks

| Activity | Current | Impact |
|:--- | :---: | :---: |
| **Still** | ~0% | GPS paused, sensors low power |
| **Walking** | Low | GPS balanced (10-20m accuracy) |
| **Driving** | Medium | GPS high acc nav mode |

## 🤝 Contributing

Contributions are welcome! Please read [SUPPORT.md](SUPPORT.md) for details.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
