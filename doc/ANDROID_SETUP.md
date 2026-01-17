# Android Setup Guide

## 1. Permissions

The plugin automatically requests permissions, but you must declare them in your `AndroidManifest.xml`.

Add the following permissions to `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.your_app">

    <!-- Location Permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

    <!-- Service Permissions -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    
    <!-- Motion & Acitvity -->
    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
    <uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION" />
    
    <!-- Boot & Network -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application ...>
       ...
    </application>
</manifest>
```

## 2. Service Declaration

The plugin's services are automatically merged from its library manifest, but ensure your `minSdkVersion` is at least **21**.

In `android/app/build.gradle`:

```gradle
defaultConfig {
    minSdkVersion 21
    ...
}
```

## 3. Google Play Services

If you are using Activity Recognition or Geofencing, you depend on Google Play Services.

In `android/app/build.gradle`, ensure you have:

```gradle
dependencies {
    implementation 'com.google.android.gms:play-services-location:21.0.1'
}
```
*(Note: The plugin adds this automatically, but valid version matching is good practice).*

## 4. Background Location (Android 10+)

For Android 10 (API 29) and above, "Always Allow" permission is required for background location.
The `requestPermissions()` method in the plugin handles the dialog flow.

*   **Android 10:** User must select "Allow all the time" in settings if not prompted directly.
*   **Android 11+:** The system dialog will take the user to the Settings page.

## 5. Notification Customization

To customize the persistent notification:
(Future Feature: currently uses default title "Background Tacking" and icon `ic_launch`).
