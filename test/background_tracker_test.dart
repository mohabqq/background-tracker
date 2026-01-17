import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_background_tracker_pro/background_tracker.dart';
import 'package:flutter_background_tracker_pro/background_tracker_platform_interface.dart';
import 'package:flutter_background_tracker_pro/background_tracker_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockBackgroundTrackerPlatform
    with MockPlatformInterfaceMixin
    implements BackgroundTrackerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final BackgroundTrackerPlatform initialPlatform = BackgroundTrackerPlatform.instance;

  test('$MethodChannelBackgroundTracker is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelBackgroundTracker>());
  });

  test('getPlatformVersion', () async {
    BackgroundTracker backgroundTrackerPlugin = BackgroundTracker();
    MockBackgroundTrackerPlatform fakePlatform = MockBackgroundTrackerPlatform();
    BackgroundTrackerPlatform.instance = fakePlatform;

    expect(await backgroundTrackerPlugin.getPlatformVersion(), '42');
  });
}
