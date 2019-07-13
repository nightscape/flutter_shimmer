package band.eva.flutter_shimmer

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.*

/**
 * FlutterShimmerPlugin
 */
class FlutterShimmerPlugin(val registrar: Registrar) : EventChannel.StreamHandler, MethodChannel.MethodCallHandler {
    private var eventSink: EventChannel.EventSink? = null
    private var manager: ShimmerSensorHandler? = null

    private// device doesn't support bluetooth
    // bluetooth is off, ask user to on it.
    // Do whatever you want to do with your bluetoothAdapter
    val bondedDevices: List<Map<String, String>>
        get() {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val bondedDevices = ArrayList<Map<String, String>>()
            if (bluetoothAdapter == null) {
            } else {
                if (!bluetoothAdapter.isEnabled) {
                    val enableAdapter = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    registrar!!.activity().startActivityForResult(enableAdapter, 0)
                }
                val all_devices = bluetoothAdapter.bondedDevices
                if (all_devices.size > 0) {
                    for (currentDevice in all_devices) {
                        val deviceInfo = HashMap<String, String>()
                        deviceInfo["name"] = currentDevice.name
                        deviceInfo["address"] = currentDevice.address
                        bondedDevices.add(deviceInfo)
                    }
                }
            }
            return bondedDevices
        }

    override fun onListen(o: Any?, eventSink: EventChannel.EventSink) {
        this.eventSink = eventSink
    }

    override fun onCancel(o: Any?) {
        this.manager?.stopService()
        this.eventSink = null
    }

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        if (methodCall.method == CONNECT_DEVICE) {
            connectToDevice(methodCall)
            eventSink!!.success(emptyMap<String, String>())
            result.success("CONNECTED")
        } else if (methodCall.method == GET_BONDED_DEVICES) {
            val bondedDevices = bondedDevices
            result.success(bondedDevices)
        } else if (methodCall.method == DISCONNECT) {
            manager?.stopService()
            result.success("DISCONNECTED")
        } else {
            result.notImplemented()
        }
    }

    private fun connectToDevice(methodCall: MethodCall) {
        val macAddress = methodCall.argument<String>("macAddress")
        if (manager == null || !manager!!.macAddress.equals(macAddress)) {
            manager = ShimmerSensorHandler(registrar!!.activity(), this.eventSink!!, macAddress!!)
        }
        manager!!.startService()
    }

    companion object {
        internal var CONNECT_DEVICE = "connectDevice"
        internal var GET_BONDED_DEVICES = "getBondedDevices"
        internal var DISCONNECT = "disconnectDevice"

        /**
         * Plugin registration.
         */
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            // Set up plugin instance
            val plugin = FlutterShimmerPlugin(registrar)

            // Set up method channel
            val methodChannel = MethodChannel(registrar.messenger(), "shimmer.method_channel")
            methodChannel.setMethodCallHandler(plugin)

            // Set up event channel
            val eventChannel = EventChannel(registrar.messenger(), "shimmer.event_channel")
            eventChannel.setStreamHandler(plugin)
        }
    }

}
