import 'package:equatable/equatable.dart';

class BluetoothDevice extends Equatable {
  final String name;
  final String address;

  BluetoothDevice(this.name, this.address);

  @override
  List<Object> get props => [this.name, this.address];
}