import 'package:equatable/equatable.dart';

class BluetoothDevice extends Equatable {
  final String name;
  final String address;

  BluetoothDevice(this.name, this.address) : super([name, address]);
}