# iOS Setup Guide

## 1. Info.plist Permissions

You must add usage descriptions to `ios/Runner/Info.plist` to explain why you need location and motion data.

Open `Info.plist` and add the following keys:

```xml
<dict>
    <!-- Background Location -->
    <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
    <string>This app needs access to location when in the background to track your trips.</string>
    <key>NSLocationAlwaysUsageDescription</key>
    <string>This app needs access to location when in the background to track your trips.</string>
    <key>NSLocationWhenInUseUsageDescription</key>
    <string>This app needs access to location to show your current position.</string>
    
    <!-- Motion & Activity -->
    <key>NSMotionUsageDescription</key>
    <string>This app uses motion data to detect when you are moving to save battery.</string>
    
    <!-- Background Modes -->
    <key>UIBackgroundModes</key>
    <array>
        <string>location</string>
        <string>fetch</string>
        <string>processing</string>
    </array>
    ...
</dict>
```

## 2. Capabilities

In Xcode:
1.  Open your project (`Runner.xcworkspace`).
2.  Select the **Runner** target.
3.  Go to the **Signing & Capabilities** tab.
4.  Click **+ Capability**.
5.  Search for **Background Modes**.
6.  Check **Location updates**.
7.  Check **Background fetch**.
8.  Check **Background processing**.

## 3. Privacy - Motion Data

Apple requires `NSMotionUsageDescription` if you use `CMMotionActivityManager`. If you omit this, the app will crash when motion detection starts.

## 4. Simulator Testing

*   **Location:** Use *Features > Location > Freeway Drive* in the Simulator to test tracking.
*   **Motion:** The Simulator **cannot** simulate Motion Activity (Walking, Running). `CMMotionActivityManager` will report `stationary` or be unavailable. You must test Motion Detection features on a **physical device**.
