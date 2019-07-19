package band.eva.flutter_shimmer


import android.app.Activity
import android.util.Log

import java.util.HashMap

import io.flutter.plugin.common.EventChannel

import android.os.Handler
import android.os.Message

import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.bluetooth.ShimmerBluetooth
import com.shimmerresearch.driver.FormatCluster
import com.shimmerresearch.driver.ObjectCluster

import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.CallbackObject
import com.shimmerresearch.driver.ShimmerDevice
import com.shimmerresearch.managers.bluetoothManager.ShimmerBluetoothManager

class ShimmerSensorHandler(private val activity: Activity, val eventSink: EventChannel.EventSink, val macAddress: String) : SensorHandler {
    private var btManager: ShimmerBluetoothManagerAndroid? = null

    private var shimmerDevice: ShimmerDevice? = null

    init {
        try {
            val handler = ShimmerHandler(eventSink)
            btManager = ShimmerBluetoothManagerAndroid(activity, handler)
            handler.setBtManager(btManager!!)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    inner class ShimmerHandler(private val eventSink: EventChannel.EventSink) : Handler() {

        private var btManager: ShimmerBluetoothManager? = null

        override fun handleMessage(msg: Message) {

            when (msg.what) {
                ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET -> if (msg.obj is ObjectCluster) {
                    val m = HashMap<String, Any>()
                    val obj = msg.obj as ObjectCluster
                    if (obj != null) {
                        m["Time"] = obj.mSystemTimeStamp
                        m["sensor"] = obj.shimmerName
                        for ((key, value) in obj.mPropertyCluster.entries()) {
                            m[key] = value.mData
                        }
                    }
                    eventSink.success(m)

                }
                ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE -> {
                    var state: ShimmerBluetooth.BT_STATE? = null
                    var macAddress = ""

                    if (msg.obj is ObjectCluster) {
                        state = (msg.obj as ObjectCluster).mState
                        macAddress = (msg.obj as ObjectCluster).macAddress
                    } else if (msg.obj is CallbackObject) {
                        state = (msg.obj as CallbackObject).mState
                        macAddress = (msg.obj as CallbackObject).mBluetoothAddress
                    }

                    Log.d(LOG_TAG, "Shimmer state changed! Shimmer = $macAddress, new state = $state")

                    when (state) {
                        ShimmerBluetooth.BT_STATE.CONNECTED -> {
                            Log.i(LOG_TAG, "Shimmer [$macAddress] is now CONNECTED")
                            shimmerDevice = btManager!!.getShimmerDeviceBtConnectedFromMac(macAddress) // TODO: Was (shimmerBtAdd);
                            if (shimmerDevice != null) {
                                Log.i(LOG_TAG, "Got the ShimmerDevice!")
                                shimmerDevice!!.startStreaming()
                            } else {
                                Log.i(LOG_TAG, "ShimmerDevice returned is NULL!")
                            }
                        }
                        ShimmerBluetooth.BT_STATE.CONNECTING -> Log.i(LOG_TAG, "Shimmer [$macAddress] is CONNECTING")
                        ShimmerBluetooth.BT_STATE.STREAMING -> Log.i(LOG_TAG, "Shimmer [$macAddress] is now STREAMING")
                        ShimmerBluetooth.BT_STATE.STREAMING_AND_SDLOGGING -> Log.i(LOG_TAG, "Shimmer [$macAddress] is now STREAMING AND LOGGING")
                        ShimmerBluetooth.BT_STATE.SDLOGGING -> Log.i(LOG_TAG, "Shimmer [$macAddress] is now SDLOGGING")
                        ShimmerBluetooth.BT_STATE.DISCONNECTED -> Log.i(LOG_TAG, "Shimmer [$macAddress] has been DISCONNECTED")
                    }
                }
                ShimmerBluetooth.MSG_IDENTIFIER_NOTIFICATION_MESSAGE -> if (msg.obj is CallbackObject) {
                    val ind = (msg.obj as CallbackObject).mIndicator
                    //FULLY_INITIALIZED state is returned when Shimmer is connected or after Shimmer has been configured
                    Log.i(LOG_TAG, "Shimmer notification $ind")
                }
                Shimmer.MESSAGE_ACK_RECEIVED -> Log.i(LOG_TAG, "Ack received from Shimmer")
                Shimmer.MESSAGE_DEVICE_NAME -> Log.i(LOG_TAG, "Device name received")


                Shimmer.MESSAGE_TOAST -> Log.i(LOG_TAG, msg.data.getString(Shimmer.TOAST))
            }

            super.handleMessage(msg)
        }

        fun setBtManager(btManager: ShimmerBluetoothManager) {
            this.btManager = btManager
        }
    }

    override fun startService() {
        try {

            btManager!!.connectShimmerThroughBTAddress(macAddress)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    override fun stopService() {
        //Disconnect the Shimmer device when app is stopped
        if (shimmerDevice != null) {
            if (shimmerDevice!!.isSDLogging) {
                shimmerDevice!!.stopSDLogging()
                Log.d(LOG_TAG, "Stopped Shimmer Logging")
            } else if (shimmerDevice!!.isStreaming) {
                shimmerDevice!!.stopStreaming()
                Log.d(LOG_TAG, "Stopped Shimmer Streaming")
            } else {
                shimmerDevice!!.stopStreamingAndLogging()
                Log.d(LOG_TAG, "Stopped Shimmer Streaming and Logging")
            }
        }
        btManager!!.disconnectAllDevices()
        Log.i(LOG_TAG, "Shimmer DISCONNECTED")
    }

    companion object {
        internal val LOG_TAG = "ShimmerSensorHandler"
    }


}
