import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_shimmer/bluetooth_device.dart';
import 'package:flutter_shimmer/flutter_shimmer.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FlutterShimmer shimmerPlugin;
  List<BluetoothDevice> _devices = [];
  BluetoothDevice _device;
  Stream<Map<String, dynamic>> _dataStream;
  bool _connected = false;
  bool _pressed = false;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  Future<void> initPlatformState() async {
    shimmerPlugin = FlutterShimmer();
    List<BluetoothDevice> devices = [];

    try {
      devices = await shimmerPlugin.getBondedDevices();
    } catch (e) {
      // TODO - Error
      devices = [BluetoothDevice(e.toString(), "")];
    }

    if (!mounted) return;
    setState(() {
      _devices = devices;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_dataStream == null) _dataStream = shimmerPlugin.stream;
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Flutter Bluetooth Serial'),
        ),
        body: Container(
          child: ListView(
            children: <Widget>[
              Padding(
                padding: const EdgeInsets.fromLTRB(10.0, 10.0, 10.0, 0.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    Text(
                      'Device:',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    DropdownButton(
                      items: _getDeviceItems(),
                      onChanged: (value) => setState(() => _device = value),
                      value: _device,
                    ),
                    RaisedButton(
                      onPressed:
                          _pressed ? null : _connected ? _disconnect : _connect,
                      child: Text(_connected ? 'Disconnect' : 'Connect'),
                    ),
                  ],
                ),
              ),
              new StreamBuilder(
                  stream: _dataStream,
                  builder: (context, asyncSnapshot) {
                    if (asyncSnapshot.hasError) {
                      return new Text("Error!");
                    } else if (asyncSnapshot.data == null) {
                      return Text("Waiting");
                    } else {
                      return Text(asyncSnapshot.data.toString());
                    }
                  })
            ],
          ),
        ),
      ),
    );
  }

  List<DropdownMenuItem<BluetoothDevice>> _getDeviceItems() {
    List<DropdownMenuItem<BluetoothDevice>> items = [];
    if (_devices.isEmpty) {
      items.add(DropdownMenuItem(
        child: Text('NONE'),
      ));
    } else {
      _devices.forEach((device) {
        items.add(DropdownMenuItem(
          child: Text(device.name),
          value: device,
        ));
      });
    }
    return items;
  }

  void _connect() {
    if (_device == null) {
      show('No device selected.');
    } else {
      shimmerPlugin.connectDevice(_device);
      setState(() {
        _connected = true;
      });
    }
  }

  void _disconnect() {
    shimmerPlugin.disconnect();
    setState(() {
      _connected = false;
    });
  }

  Future show(
    String message, {
    Duration duration: const Duration(seconds: 3),
  }) async {
    await new Future.delayed(new Duration(milliseconds: 100));
    Scaffold.of(context).showSnackBar(
      new SnackBar(
        content: new Text(
          message,
          style: new TextStyle(
            color: Colors.white,
          ),
        ),
        duration: duration,
      ),
    );
  }
}
