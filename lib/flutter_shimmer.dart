import 'dart:async';
import 'dart:io' show Platform;

import 'package:flutter/services.dart';

import 'bluetooth_device.dart';

/// Custom Exception for the plugin,
/// thrown whenever the plugin is used on platforms other than Android
class ShimmerException implements Exception {
  String _cause;

  ShimmerException(this._cause);

  @override
  String toString() {
    return _cause;
  }
}

/// The main plugin class which establishes a [MethodChannel] and an [EventChannel].
class FlutterShimmer {
  MethodChannel _methodChannel = MethodChannel('shimmer.method_channel');
  EventChannel _eventChannel = EventChannel('shimmer.event_channel');
  Stream<Map<String, dynamic>> _shimmerStream;
  Stream<Map<String, dynamic>> get stream => _shimmerStream;

  StreamSubscription<Map<String, dynamic>> subscription;
  FlutterShimmer() {
    _shimmerStream = _eventChannel
        .receiveBroadcastStream()
        .map((d) => Map<String, dynamic>.from(d));
    subscription = _shimmerStream.listen(null);
  }

  Future<void> connectDevice(Map<String, dynamic> args) {
    if (Platform.isAndroid) {
      return _methodChannel.invokeMethod("connectDevice", args);
    } else {
      return Future.error(
          ShimmerException('Shimmer API exclusively available on Android!'));
    }
  }

  Future<List<BluetoothDevice>> getBondedDevices() {
    return _methodChannel
        .invokeListMethod<Map<dynamic, dynamic>>("getBondedDevices")
        .then((l) => l
            .map((m) =>
                BluetoothDevice(m["name"] as String, m["address"] as String))
            .toList());
  }

  Future<void> disconnect() {
    return _methodChannel.invokeMethod("disconnectDevice");
  }
}
