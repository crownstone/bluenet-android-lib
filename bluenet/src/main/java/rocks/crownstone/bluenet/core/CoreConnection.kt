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
	private var gatt: BluetoothGatt? = null

	private var currentAction = Action.NONE

	// Keep up promises
	private var unitPromise: Deferred<Unit, Exception>? = null

//	private var


	@Synchronized fun connect(address: DeviceAddress, timeout: Int): Promise<Unit, Exception> {
		Log.i(TAG, "connect $address")
		if (!isBleReady()) {
			return Promise.ofFail(Exception("BLE not ready"))
		}
		val gatt = this.gatt
		val deferred = deferred<Unit, Exception>()

		if (gatt != null) {
			Log.d(TAG, "gatt already open")
			if (gatt.device.address != address) {
				return Promise.ofFail(Exception("busy with another device"))
			}
			val state = gatt.getConnectionState(gatt.device)
			Log.i(TAG, "state=${getStateString(state)}")
			when (state) {
				BluetoothProfile.STATE_CONNECTED -> {
					deferred.resolve()
				}
				BluetoothProfile.STATE_DISCONNECTED -> {
					if (isBusy(Action.CONNECT)) {
						return Promise.ofFail(Exception("busy"))
					}
					setBusy(Action.CONNECT, deferred) // Resolve later
					Log.d(TAG, "gatt.connect")
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
					deferred.reject(Exception("gatt is null"))
				}
				BluetoothProfile.STATE_DISCONNECTED -> {
					if (isBusy()) {
						return Promise.ofFail(Exception("busy"))
					}
					setBusy(Action.CONNECT, deferred) // Resolve later
					Log.d(TAG, "device.connectGatt")
					this.gatt = device.connectGatt(context, false, gattCallback)
//					this.gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_AUTO, BluetoothDevice.PHY_LE_1M_MASK, handler)
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
		val gatt = this.gatt
		if (gatt == null) {
			Log.d(TAG, "already closed")
			return Promise.ofSuccess(Unit)
		}
		val deferred = deferred<Unit, Exception>()
		val state = gatt.getConnectionState(gatt.device)
		Log.i(TAG, "state=${getStateString(state)}")
		when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> {
				deferred.resolve()
			}
			BluetoothProfile.STATE_CONNECTED -> {
				if (isBusy()) {
					return Promise.ofFail(Exception("busy"))
				}
				setBusy(Action.DISCONNECT, deferred) // Resolve later
				Log.d(TAG, "gatt.disconnect")
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
//		val gatt = this.gatt
//		if (gatt == null) {
//			Log.d(TAG, "already closed")
//			return Promise.ofSuccess(Unit)
//		}
//		if (isBusy()) {
//			Log.w(TAG, "busy")
//			return Promise.ofFail(Exception("busy"))
//		}
//		val deferred = deferred<Unit, Exception>()
//		val state = gatt.getConnectionState(gatt.device)
//		Log.i(TAG, "state=${getStateString(state)}")
//		when (state) {
//			BluetoothProfile.STATE_DISCONNECTED -> {
//				close(gatt)
//				deferred.resolve()
//			}
//			BluetoothProfile.STATE_CONNECTED -> {
////				if (isBusy(Action.DISCONNECT)) {
////					return Promise.ofFail(Exception("busy"))
////				}
////				setBusy(Action.DISCONNECT, deferred) // Resolve later
//				close(gatt)
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
		val gatt = this.gatt
		if (gatt == null) {
			Log.d(TAG, "already closed")
			return true
		}
		if (isBusy()) {
			Log.w(TAG, "busy")
			return false
		}
		val state = gatt.getConnectionState(gatt.device)
		Log.i(TAG, "state=${getStateString(state)}")
		when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> {
				close()
				return true
			}
			BluetoothProfile.STATE_CONNECTED -> {
				close()
				return true
			}
			else -> {
				Log.w(TAG, "wrong state: busy")
				return false
			}
		}
	}

	@Synchronized private fun closeFinal(clearCache: Boolean) {
		Log.d(TAG, "gatt.close")
		if (clearCache) {
			refreshGatt()
		}
		gatt?.close()
		gatt = null
		currentAction = Action.NONE
	}

	/**
	 * Refresh the device cache when already connected
	 */
	@Synchronized fun refreshDeviceCache(): Promise<Unit, Exception> {
		Log.i(TAG, "refreshDeviceCache")
//		val gatt = this.gatt
		if (gatt == null) {
			return Promise.ofFail(Exception("not connected"))
		}
		val deferred = deferred<Unit, Exception>()
		val state = gatt.getConnectionState(gatt.device)
		Log.i(TAG, "state=${getStateString(state)}")
		when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> {
				deferred.reject(Exception("not connected"))
			}
			BluetoothProfile.STATE_CONNECTED -> {
				if (isBusy()) {
					return Promise.ofFail(Exception("busy"))
				}
				setBusy(Action.REFRESH, deferred) // Resolve later
				Log.d(TAG, "gatt.disconnect")
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
			val refresh = gatt?.javaClass?.getMethod("refresh")
			if (refresh != null) {
				success = refresh.invoke(gatt) as Boolean
				Log.d(TAG, "Refreshing result: $success")
			}
		}
		catch (e: Exception) {
			Log.e(TAG, "Refreshing failed: an exception occurred while refreshing device", e)
			return false
		}
		return success
	}

	@Synchronized private fun isBusy(): Boolean {
		// TODO: more sanity checks?
		if (currentAction == Action.NONE) {
			if (unitPromise != null) {
				Log.e(TAG, "promise is not null")
			}
			return true
		}
		return false
	}

	@Synchronized private fun setBusy(action: Action, deferred: Deferred<Unit, Exception>): Boolean {
		if (isBusy()) {
			return false
		}
		when (action) {
			Action.CONNECT -> {
				unitPromise = deferred
				currentAction = action
			}
			else -> {
				Log.e(TAG, "wrong action or promise type")
				return false
			}
		}
		return true
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


	enum class Action {
		NONE,
		CONNECT,
		DISCONNECT,
		DISCOVER,
		READ,
		WRITE,
		SUBSCRIBE,
		UNSUBSCRIBE,
		REFRESH,
	}

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
}