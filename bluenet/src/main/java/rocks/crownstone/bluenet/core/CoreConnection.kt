package rocks.crownstone.bluenet.core

import android.bluetooth.*
import android.content.Context
import android.util.Log
import nl.komponents.kovenant.Promise
import rocks.crownstone.bluenet.DeviceAddress
import rocks.crownstone.bluenet.EventBus

/**
 * Class that adds connection functions to the bluetooth LE core class.
 */
open class CoreConnection(appContext: Context, evtBus: EventBus) : CoreInit(appContext, evtBus) {
	internal var gatt: BluetoothGatt? = null


	private val gattCallback = object: BluetoothGattCallback() {
		override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

		}

		override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

		}

		override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
		}
		override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
		}
		override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
		}

		override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
		}
		override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
		}
	}



	fun connect(address: DeviceAddress, timeout: Int): Promise<Unit, Exception> {

//		bleManager.getConnectedDevices(BluetoothProfile.GATT)
		val device = bleAdapter.getRemoteDevice(address)
		val state = bleManager.getConnectionState(device, BluetoothProfile.GATT)
		when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> Log.i(TAG,"STATE_DISCONNECTED")
			BluetoothProfile.STATE_CONNECTING-> Log.i(TAG,"STATE_CONNECTING")
			BluetoothProfile.STATE_CONNECTED -> Log.i(TAG,"STATE_CONNECTED")
			BluetoothProfile.STATE_DISCONNECTING -> Log.i(TAG,"STATE_DISCONNECTING")
		}

		device.connectGatt(context, false, )

		bleManager.
	}
}