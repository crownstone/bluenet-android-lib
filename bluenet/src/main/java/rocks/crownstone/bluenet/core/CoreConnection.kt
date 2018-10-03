package rocks.crownstone.bluenet.core

import android.bluetooth.*
import android.content.Context
import android.util.Log
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.resolve
import rocks.crownstone.bluenet.DeviceAddress
import rocks.crownstone.bluenet.EventBus

/**
 * Class that adds connection functions to the bluetooth LE core class.
 */
open class CoreConnection(appContext: Context, evtBus: EventBus) : CoreInit(appContext, evtBus) {
	private var currentGatt: BluetoothGatt? = null

//	private var


	@Synchronized fun connect(address: DeviceAddress, timeout: Int): Promise<Unit, Exception> {
		Log.i(TAG, "connect $address")
		if (!isBleReady()) {
			return Promise.ofFail(Exception("BLE not ready"))
		}
		val gatt = this.currentGatt
		val deferred = deferred<Unit, Exception>()

		if (gatt != null) {
			Log.d(TAG, "currentGatt already open")
			if (gatt.device.address != address) {
				return Promise.ofFail(Exception("busy with another device"))
			}
			val state = bleManager.getConnectionState(gatt.device, BluetoothProfile.GATT)
			Log.i(TAG, "state=${getStateString(state)}")
			when (state) {
				BluetoothProfile.STATE_CONNECTED -> {
					deferred.resolve()
				}
				BluetoothProfile.STATE_DISCONNECTED -> {
					if (promises.isBusy()) {
						return Promise.ofFail(Exception("busy"))
					}
					promises.setBusy(Action.CONNECT, deferred) // Resolve later
					Log.d(TAG, "currentGatt.connect")
					gatt.connect()
					// TODO: timeout
				}
				else -> {
					deferred.reject(Exception("wrong state: busy"))
				}
			}
		}
		else {
			val device: BluetoothDevice
			try {
				device = bleAdapter.getRemoteDevice(address)
			}
			catch (e: IllegalArgumentException) {
				return Promise.ofFail(Exception("invalid address"))
			}
			val state = bleManager.getConnectionState(device, BluetoothProfile.GATT)
			Log.i(TAG, "state=${getStateString(state)}")
			when (state) {
				BluetoothProfile.STATE_CONNECTED -> {
					// This shouldn't happen, maybe when another app connected?
					deferred.reject(Exception("currentGatt is null"))
				}
				BluetoothProfile.STATE_DISCONNECTED -> {
					if (promises.isBusy()) {
						return Promise.ofFail(Exception("busy"))
					}
					promises.setBusy(Action.CONNECT, deferred) // Resolve later
					Log.d(TAG, "device.connectGatt")
					if (android.os.Build.VERSION.SDK_INT >= 23) {
						this.currentGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
					}
					else {
						this.currentGatt = device.connectGatt(context, false, gattCallback)
					}
					// TODO: timeout
				}
				else -> {
					deferred.reject(Exception("wrong state: busy"))
				}
			}
		}
		return deferred.promise
	}

	/**
	 * Disconnects from device, does not release the BluetoothGatt resources.
	 *
	 */
	@Synchronized fun disconnect(): Promise<Unit, Exception> {
		Log.i(TAG, "disconnect")
		if (!isBleReady()) {
//			return Promise.ofFail(Exception("BLE not ready"))
			return Promise.ofSuccess(Unit) // Always disconnected when BLE is off
		}
		val gatt = this.currentGatt
		if (gatt == null) {
			Log.d(TAG, "already closed")
			return Promise.ofSuccess(Unit)
		}
		val deferred = deferred<Unit, Exception>()
		val state = bleManager.getConnectionState(gatt.device, BluetoothProfile.GATT)
		Log.i(TAG, "state=${getStateString(state)}")
		when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> {
				deferred.resolve()
			}
			BluetoothProfile.STATE_CONNECTED -> {
				if (promises.isBusy()) {
					return Promise.ofFail(Exception("busy"))
				}
				promises.setBusy(Action.DISCONNECT, deferred) // Resolve later
				Log.d(TAG, "currentGatt.disconnect")
				gatt.disconnect()
			}
			else -> {
				deferred.reject(Exception("wrong state: busy"))
			}
		}
		return deferred.promise
	}

//	/**
//	 * Disconnects from device and releases BluetoothGatt resources.
//	 */
//	@Synchronized fun close(clearCache: Boolean): Promise<Unit, Exception> {
//		Log.i(TAG, "close")
//		if (!isBleReady()) {
////			return Promise.ofFail(Exception("BLE not ready"))
//			return Promise.ofSuccess(Unit) // Always closed when BLE is off?? // TODO: check this
//		}
//		val currentGatt = this.currentGatt
//		if (currentGatt == null) {
//			Log.d(TAG, "already closed")
//			return Promise.ofSuccess(Unit)
//		}
//		if (isBusy()) {
//			Log.w(TAG, "busy")
//			return Promise.ofFail(Exception("busy"))
//		}
//		val deferred = deferred<Unit, Exception>()
//		val state = currentGatt.getConnectionState(currentGatt.device)
//		Log.i(TAG, "state=${getStateString(state)}")
//		when (state) {
//			BluetoothProfile.STATE_DISCONNECTED -> {
//				close(currentGatt)
//				deferred.resolve()
//			}
//			BluetoothProfile.STATE_CONNECTED -> {
////				if (isBusy(Action.DISCONNECT)) {
////					return Promise.ofFail(Exception("busy"))
////				}
////				setBusy(Action.DISCONNECT, deferred) // Resolve later
//				close(currentGatt)
//				deferred.resolve()
//			}
//			else -> {
//				// This shouldn't happen: busy check should've caught this
//				deferred.reject(Exception("wrong state"))
//			}
//		}
//		return deferred.promise
//	}
	/**
	 * Disconnects from device, releases BluetoothGatt resources, and cleans up.
	 *
	 * TODO: clearCache
	 */
	@Synchronized fun close(clearCache: Boolean): Boolean {
		Log.i(TAG, "close")
		if (!isBleReady()) {
			return true // Always closed when BLE is off?? // TODO: check this
		}
		val gatt = this.currentGatt
		if (gatt == null) {
			Log.d(TAG, "already closed")
			return true
		}
		if (promises.isBusy()) {
			Log.w(TAG, "busy")
			return false
		}
		val state = bleManager.getConnectionState(gatt.device, BluetoothProfile.GATT)
		Log.i(TAG, "state=${getStateString(state)}")
		when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> {
				closeFinal(clearCache)
				return true
			}
			BluetoothProfile.STATE_CONNECTED -> {
				closeFinal(clearCache)
				return true
			}
			else -> {
				Log.w(TAG, "wrong state: busy")
				return false
			}
		}
	}

	@Synchronized private fun closeFinal(clearCache: Boolean) {
		Log.d(TAG, "currentGatt.close")
		if (clearCache) {
			refreshGatt()
		}
		currentGatt?.close()
		currentGatt = null
	}

	/**
	 * Refresh the device cache when already connected
	 */
	@Synchronized fun refreshDeviceCache(): Promise<Unit, Exception> {
		Log.i(TAG, "refreshDeviceCache")
		val gatt = this.currentGatt
		if (gatt == null) {
			return Promise.ofFail(Exception("not connected"))
		}
		val deferred = deferred<Unit, Exception>()
		val state = bleManager.getConnectionState(gatt.device, BluetoothProfile.GATT)
		Log.i(TAG, "state=${getStateString(state)}")
		when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> {
				deferred.reject(Exception("not connected"))
			}
			BluetoothProfile.STATE_CONNECTED -> {
				// Need to wait some time after refresh call, so set to busy
				if (promises.isBusy()) {
					return Promise.ofFail(Exception("busy"))
				}
				promises.setBusy(Action.REFRESH, deferred) // Resolve later
				Log.d(TAG, "currentGatt.disconnect")
				refreshGatt()
			}
			else -> {
				deferred.reject(Exception("wrong state: busy"))
			}
		}
		return deferred.promise
	}

	// The refresh() method does perform the service discovery, but you are not aware of it (no callback is called).
	// My suggestion would be either to call it after you disconnected (on onConnectionStateChanged->STATE_DISCONNECTED, before you call close() )
	// or after you connected but delay calling discoverServices() about 600ms.
	// If you are bonded, shortly after you get state connected the device may receive the Service Changed indication and may refresh services automatically.
	// See https://devzone.nordicsemi.com/f/nordic-q-a/10146/refresh-services-in-nrf-master-control-panel/37626#37626
	// See https://devzone.nordicsemi.com/f/nordic-q-a/4783/nrftoobox-on-android-not-recognizing-changes-in-application-type-running-on-nordic-pcb
	@Synchronized private fun refreshGatt(): Boolean {
		Log.i(TAG, "refreshDeviceCache")
		var success = false
		try {
			val refresh = currentGatt?.javaClass?.getMethod("refresh")
			if (refresh != null) {
				success = refresh.invoke(currentGatt) as Boolean
				Log.d(TAG, "Refreshing result: $success")
			}
		}
		catch (e: Exception) {
			Log.e(TAG, "Refreshing failed: an exception occurred while refreshing device", e)
			return false
		}
		return success
	}

	private fun getStateString(state: Int): String {
		return when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
			BluetoothProfile.STATE_CONNECTING-> "STATE_CONNECTING"
			BluetoothProfile.STATE_CONNECTED -> "STATE_CONNECTED"
			BluetoothProfile.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
			else -> "STATE_UNKNOWN"
		}
	}

//	private fun <V> setBusy(action: Action, deferred: Deferred<V, Exception>) {
//		when (action) {
//			Action.CONNECT -> {
//				unitPromise = deferred as Deferred<Unit, Exception> // Can't check :(
//			}
//		}
//	}


	private val gattCallback = object: BluetoothGattCallback() {
		override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
			val address = gatt?.device?.address
			Log.i(TAG, "onConnectionStateChange address=$address status=$status newState=$newState")

			if (status != BluetoothGatt.GATT_SUCCESS) {
				// TODO
				Log.e(TAG, "onConnectionStateChange address=$address status=$status newState=$newState")
			}

			if (gatt == null || address == null || address != currentGatt?.device?.address) {
				return
			}

			when (newState) {
				BluetoothProfile.STATE_CONNECTED -> {
					promises.resolve(Action.CONNECT)
				}
				BluetoothProfile.STATE_DISCONNECTED -> {
					promises.resolve(Action.DISCONNECT)
				}
			}
		}

		override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
			Log.i(TAG, "onServicesDiscovered status=$status")
		}

		override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
			Log.i(TAG, "onCharacteristicRead characteristic=$characteristic status=$status")
		}

		override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
			Log.i(TAG, "onCharacteristicChanged characteristic=$characteristic")
		}

		override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
			Log.i(TAG, "onCharacteristicWrite characteristic=$characteristic status=$status")
		}

		override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
			Log.i(TAG, "onDescriptorWrite descriptor=$descriptor status=$status")
		}

		override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
			Log.i(TAG, "onDescriptorRead descriptor=$descriptor status=$status")
		}
	}
}