
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter_background_tracker_pro/background_tracker.dart';
import 'package:flutter_background_tracker_pro/config/tracker_config.dart';
import 'package:flutter_background_tracker_pro/models/geofence.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _locationStatus = "Not Tracking";
  List<String> _locations = [];
  int _totalLocations = 0;

  @override
  void initState() {
    super.initState();
    initPlatformState();
    
    BackgroundTracker.onLocation.listen((event) {
      setState(() {
        if (_locationStatus == "Waiting for location...") {
          _locationStatus = "Tracking Started";
        }
        _locations.add("Lat: ${event['latitude']}, Lng: ${event['longitude']}");
      });
    });
    
    // Listen for geofence events (needs separate stream exposure in main plugin)
    // For now, we'll just log it or add to list if we exposed it via a Stream
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    try {
      platformVersion =
          await BackgroundTracker().getPlatformVersion() ?? 'Unknown platform version';
    } catch (e) {
      platformVersion = 'Failed to get platform version.';
    }

    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  void _startTracking() async {
    bool granted = await BackgroundTracker.requestPermissions();
    if (!granted) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Permissions denied. Please allow location access.")),
        );
      }
      return;
    }
    
    // Configure with advanced settings
    await BackgroundTracker.configure(
      const TrackerConfig(
        accuracyMode: AccuracyMode.balanced,
        distanceFilter: 10.0,
        timeInterval: 10,
        enableMotionDetection: true,
        enableActivityRecognition: true,
        syncUrl: "https://requestbin.io/123", // Example placeholder
        syncInterval: 60, // Sync every 60 seconds
      ),
    );
    
    await BackgroundTracker.startTracking();
    setState(() {
      _locationStatus = "Waiting for location...";
    });
  }

  void _stopTracking() async {
    await BackgroundTracker.stopTracking();
    setState(() {
      _locationStatus = "Tracking Stopped";
    });
  }

  void _loadStoredLocations() async {


    final count = await BackgroundTracker.getLocationCount();
    setState(() {
      _totalLocations = count;
    });
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Total stored locations: $count")),
      );
    }
  }

  void _clearDatabase() async {
    await BackgroundTracker.clearDatabase();
    setState(() {
      _totalLocations = 0;
    });
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Database cleared!")),
      );
    }
  }

  void _addGeofence() async {
    // Add a geofence at current location (approximate for demo)
    // In real app, you'd get actual location first
    await BackgroundTracker.addGeofence(
      Geofence(
        id: "test_geofence_${DateTime.now().millisecondsSinceEpoch}",
        latitude: 29.7742649, // Example lat
        longitude: 31.2701922, // Example lng
        radius: 100, // 100 meters
        notifyOnEntry: "Entered Geofence!",
        notifyOnExit: "Exited Geofence!",
      ),
    );
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Geofence added!")),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Background Tracker Example'),
        ),
        body: Column(
          children: [
            Text('Running on: $_platformVersion\n'),
            Text('Status: $_locationStatus', style: const TextStyle(fontWeight: FontWeight.bold)),
            Text('Stored: $_totalLocations locations', style: const TextStyle(fontSize: 12)),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                ElevatedButton(onPressed: _startTracking, child: const Text("Start")),
                const SizedBox(width: 10),
                ElevatedButton(onPressed: _stopTracking, child: const Text("Stop")),
              ],
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                ElevatedButton(onPressed: _loadStoredLocations, child: const Text("Load DB")),
                const SizedBox(width: 10),
                ElevatedButton(onPressed: _clearDatabase, child: const Text("Clear DB")),
                const SizedBox(width: 10),
                ElevatedButton(onPressed: _addGeofence, child: const Text("Add Geofence")),
              ],
            ),
            const Divider(),
            Expanded(
              child: ListView.builder(
                itemCount: _locations.length,
                itemBuilder: (context, index) {
                  return ListTile(title: Text(_locations[index]));
                },
              ),
            )
          ],
        ),
      ),
    );
  }
}
