package rocks.crownstone.bluenet.core

import android.bluetooth.*
import android.content.Context
import android.util.Log
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Util
import java.util.*

/**
 * Class that adds connection functions to the bluetooth LE core class.
 */
open class CoreConnection(appContext: Context, evtBus: EventBus) : CoreInit(appContext, evtBus) {
	private var currentGatt: BluetoothGatt? = null
	private var services: List<BluetoothGattService>? = null

	// The notification callbacks are stored in a dedicated eventbus.
	// An eventbus is used so that we can later subscribe to notifications multiple times, and so it can be cleaned up easily.
	// TODO: cleanup on close or on connect.
	private val notificationEventBus = EventBus()

	init {
		evtBus.subscribe(BluenetEvent.BLE_TURNED_OFF, ::onBleTurnedOff)
	}

	/**
	 * Connect to a device.
	 *
	 * @param address Address of the device.
	 * @param timeout Reject promise after this time.
	 * @return Promise
	 */
	@Synchronized fun connect(address: DeviceAddress, timeout: Int): Promise<Unit, Exception> {
		Log.i(TAG, "connect $address")
		if (!isBleReady()) {
			return Promise.ofFail(Errors.BleNotReady())
		}
		val gatt = this.currentGatt
		val deferred = deferred<Unit, Exception>()

		if (gatt != null) {
			Log.d(TAG, "gatt already open")
			if (gatt.device.address != address) {
				return Promise.ofFail(Errors.BusyOtherDevice())
			}
			val state = bleManager.getConnectionState(gatt.device, BluetoothProfile.GATT)
			Log.i(TAG, "state=${getStateString(state)}")
			when (state) {
				BluetoothProfile.STATE_CONNECTED -> {
					deferred.resolve()
				}
				BluetoothProfile.STATE_DISCONNECTED -> {
					if (promises.isBusy()) {
						return Promise.ofFail(Errors.Busy())
					}
					promises.setBusy(Action.CONNECT, deferred) // Resolve later in onGattConnectionStateChange
					Log.d(TAG, "gatt.connect")
					gatt.connect() // When doing this on a gatt that's invalid (due to bluetooth being turned off), this throws a android.os.DeadObjectException
					// TODO: timeout
				}
				else -> {
					if (promises.isBusy()) {
						return Promise.ofFail(Errors.Busy())
					}
					deferred.reject(Errors.BusyWrongState())
				}
			}
		}
		else {
			val device: BluetoothDevice
			try {
				device = bleAdapter.getRemoteDevice(address)
			}
			catch (e: IllegalArgumentException) {
				return Promise.ofFail(Errors.AddressInvalid())
			}
			val state = bleManager.getConnectionState(device, BluetoothProfile.GATT)
			Log.i(TAG, "state=${getStateString(state)}")
			when (state) {
				BluetoothProfile.STATE_CONNECTED -> {
					// This shouldn't happen, maybe when another app connected?
					deferred.reject(Errors.GattNull())
				}
				BluetoothProfile.STATE_DISCONNECTED -> {
					if (promises.isBusy()) {
						return Promise.ofFail(Errors.Busy())
					}
					promises.setBusy(Action.CONNECT, deferred) // Resolve later in onGattConnectionStateChange
					Log.d(TAG, "device.connectGatt")
					if (android.os.Build.VERSION.SDK_INT >= 23) {
						this.currentGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
					}
					else {
						this.currentGatt = device.connectGatt(context, false, gattCallback)
					}
					Log.d(TAG, "gatt=${this.currentGatt}")
					// TODO: timeout
				}
				else -> {
					if (promises.isBusy()) {
						return Promise.ofFail(Errors.Busy())
					}
					deferred.reject(Errors.BusyWrongState())
				}
			}
		}
		return deferred.promise
	}

	/**
	 * Disconnects from device, does not release the BluetoothGatt resources.
	 *
	 * @return Promise
	 */
	@Synchronized fun disconnect(): Promise<Unit, Exception> {
		Log.i(TAG, "disconnect")
		if (!isBleReady()) {
//			return Promise.ofFail(Errors.BleNotReady())
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
					return Promise.ofFail(Errors.Busy())
				}
				promises.setBusy(Action.DISCONNECT, deferred) // Resolve later in onGattConnectionStateChange
				Log.d(TAG, "gatt.disconnect")
				gatt.disconnect()
			}
			else -> {
				deferred.reject(Errors.BusyWrongState())
			}
		}
		return deferred.promise
	}

	/**
	 * Get the device we're connected to.
	 *
	 * @return Device, or null when not connected.
	 */
	@Synchronized fun getConnectedDevice(): BluetoothDevice? {
		if (getConnectionState() == ConnectionState.CONNECTED) {
			return currentGatt?.device
		}
		return null
	}

	/**
	 * @return Whether the service is available.
	 */
	@Synchronized fun hasService(serviceUuid: UUID): Boolean {
		Log.d(TAG, "hasService $serviceUuid")
		return currentGatt?.getService(serviceUuid) != null
	}

	/**
	 * @return Whether the characteristic is available.
	 */
	@Synchronized fun hasCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): Boolean {
		Log.d(TAG, "hasCharacteristic $characteristicUuid $currentGatt ${currentGatt?.getService(serviceUuid)} ${currentGatt?.getService(serviceUuid)?.getCharacteristic(characteristicUuid)}")
		return currentGatt?.getService(serviceUuid)?.getCharacteristic(characteristicUuid) != null
	}

	/**
	 * Get the address of the device we're connected to.
	 *
	 * @return Address of the device, or null when not connected.
	 */
	@Synchronized fun getConnectedAddress(): DeviceAddress? {
		if (getConnectionState() == ConnectionState.CONNECTED) {
			return currentGatt?.device?.address
		}
		return null
	}

	@Synchronized private fun onGattConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
		val address = gatt?.device?.address
		Log.i(TAG, "onConnectionStateChange address=$address status=$status newState=$newState=${getStateString(newState)}")

		if (status != BluetoothGatt.GATT_SUCCESS) {
			Log.e(TAG, "onConnectionStateChange address=$address status=$status newState=$newState=${getStateString(newState)}")
			// TODO: handle error
			// See https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h
			// 8   GATT_CONN_TIMEOUT / GATT_INSUF_AUTHORIZATION
			//     [05-10-2018] (oneplus 3) When I get this error (during discovery), every next discovery fails too, until i turn bluetooth off and on.
			//                              Maybe try to increase the "connection supervision timeout" on the crownstone.
			// 19  GATT_CONN_TERMINATE_PEER_USER   Getting this status when device disconnected us.
			// 22  GATT_CONN_TERMINATE_LOCAL_HOST  [10-10-2017] Getting this error on a samsung s7 a lot, seems to happen when out of reach.
			// 34  GATT_CONN_LMP_TIMEOUT
			// 133 GATT_ERROR                      [11-01-2017] This error seems rather common, retry usually helps.
			// 135 GATT_ILLEGAL_PARAMETER          [04-10-2018] Got this error after calling gatt.disconnect
		}

		if (gatt == null || gatt != currentGatt || address == null || address != currentGatt?.device?.address) {
			// [05-10-2018] Got this event after a gatt.close().
			Log.e(TAG, "gatt=$gatt currentGatt=$currentGatt address=${currentGatt?.device?.address}")
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
		if (status != BluetoothGatt.GATT_SUCCESS) {
			close(true) // TODO: refresh?
		}
	}


	/**
	 * Disconnects from device, releases BluetoothGatt resources, and cleans up.
	 *
	 * @param clearCache Whether to clear the cache, this is useful when you expect the services to change next time you connect.
	 * @return Promise
	 */
	@Synchronized fun close(clearCache: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "close")
//		if (!isBleReady()) {
//			return Promise.ofSuccess(Unit) // Always closed when BLE is off?? // TODO: check this
//		}
		val gatt = this.currentGatt
		if (gatt == null) {
			Log.d(TAG, "already closed")
			return Promise.ofSuccess(Unit)
		}
		if (promises.isBusy()) {
			Log.w(TAG, "busy")
			return Promise.ofFail(Errors.Busy())
		}
		val deferred = deferred<Unit, Exception>()
		val state = bleManager.getConnectionState(gatt.device, BluetoothProfile.GATT)
		Log.i(TAG, "state=${getStateString(state)}")
		when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> {
				closeFinal(clearCache)
				deferred.resolve()
			}
			BluetoothProfile.STATE_CONNECTED -> {
				closeFinal(clearCache)
				deferred.resolve()
			}
			else -> {
				deferred.reject(Errors.BusyWrongState())
			}
		}
		return deferred.promise
	}

	@Synchronized private fun closeFinal(clearCache: Boolean) {
		if (clearCache) {
			refreshGatt()
		}
		Log.d(TAG, "gatt.close")
		currentGatt?.close()
		currentGatt = null
	}

	/**
	 * Refresh the device cache when already connected.
	 *
	 * @return Promise
	 */
	@Synchronized fun refreshDeviceCache(): Promise<Unit, Exception> {
		Log.i(TAG, "refreshDeviceCache")
		if (promises.isBusy()) {
			return Promise.ofFail(Errors.Busy())
		}
		val state = getConnectionState()
		if (state != ConnectionState.CONNECTED) {
			return Promise.ofFail(getNotConnectedError(state))
		}
		val deferred = deferred<Unit, Exception>()
		promises.setBusy(Action.REFRESH, deferred) // Resolve later
		refreshGatt()
		// Wait some time before resolving
		handler.postDelayed({ promises.resolve(Action.REFRESH) }, 1000)
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

	/**
	 * Start discovery of services.
	 *
	 * @param forceDiscover When set to false, the cache can be used.
	 * @return Promise
	 */
	@Synchronized fun discoverServices(forceDiscover: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "discoverServices")
//		if (!isBleReady()) {
//			return Promise.ofFail(Errors.BleNotReady())
//		}
		if (promises.isBusy()) {
			return Promise.ofFail(Errors.Busy())
		}
		val state = getConnectionState()
		if (state != ConnectionState.CONNECTED) {
			return Promise.ofFail(getNotConnectedError(state))
		}
		val deferred = deferred<Unit, Exception>()
		val gatt = this.currentGatt!! // Already checked in getConnectionState()
		if (!forceDiscover && services != null) {
			// Use cached results
			return Promise.ofSuccess(Unit)
		}
		promises.setBusy(Action.DISCOVER, deferred) // Resolve later in onGattServicesDiscovered
		Log.d(TAG, "gatt.discoverServices")
		val result = gatt.discoverServices()
		if (!result) {
			promises.reject(Errors.DiscoveryStart())
		}
		return deferred.promise
//				.then {
//					Util.waitPromise(200, handler)
//				}.unwrap()
	}

	@Synchronized private fun onGattServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
		Log.i(TAG, "onServicesDiscovered status=$status")
		if (gatt == null || gatt != currentGatt) {
			Log.e(TAG, "gatt=$gatt currentGatt=$currentGatt")
			return
		}
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Log.e(TAG, "onServicesDiscovered status=$status")
			// TODO: handle error
			// [05-10-2018] It seems like this error is always followed by an onConnectionStateChange
			promises.reject(Errors.Gatt(status))
			return
		}
		services = gatt.services
		promises.resolve(Action.DISCOVER)
	}

	@Synchronized fun write(serviceUuid: UUID, characteristicUuid: UUID, data: ByteArray): Promise<Unit, Exception> {
		Log.i(TAG, "write serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid data=${Conversion.bytesToString(data)}")
//		if (!isBleReady()) {
//			return Promise.ofFail(Errors.BleNotReady())
//		}
		if (promises.isBusy()) {
			return Promise.ofFail(Errors.Busy())
		}
		val state = getConnectionState()
		if (state != ConnectionState.CONNECTED) {
			Log.e(TAG, "not connected")
			return Promise.ofFail(getNotConnectedError(state))
		}
		val deferred = deferred<Unit, Exception>()
		val gatt = this.currentGatt!! // Already checked in getConnectionState()
		val char = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
		if (char == null) {
			Log.e(TAG, "characteristic not found: $characteristicUuid")
			return Promise.ofFail(Errors.CharacteristicNotFound())
		}
		promises.setBusy(Action.WRITE, deferred) // Resolve later in onGattCharacteristicWrite
		char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
		Log.d(TAG, "gatt.writeCharacteristic")
		val result = char.setValue(data) && gatt.writeCharacteristic(char)
		if (!result) {
			promises.reject(Errors.WriteFailed())
		}
		return deferred.promise
	}

	@Synchronized private fun onGattCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
		Log.i(TAG, "onCharacteristicWrite characteristic=$characteristic status=$status")
		if (gatt == null || gatt != currentGatt || characteristic == null) {
			Log.e(TAG, "gatt=$gatt currentGatt=$currentGatt characteristic=${characteristic?.uuid}")
			return
		}
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Log.e(TAG, "onCharacteristicWrite characteristic=${characteristic.uuid} status=$status")
			// TODO: handle error
			// [05-10-2018] It seems like this error is always followed by an onConnectionStateChange
			promises.reject(Errors.Gatt(status))
			return
		}
		// TODO: check if correct characteristic was written
		promises.resolve(Action.WRITE)
	}

	@Synchronized fun read(serviceUuid: UUID, characteristicUuid: UUID): Promise<ByteArray, Exception> {
		Log.i(TAG, "read serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid")
//		if (!isBleReady()) {
//			return Promise.ofFail(Errors.BleNotReady())
//		}
		if (promises.isBusy()) {
			return Promise.ofFail(Errors.Busy())
		}
		val state = getConnectionState()
		if (state != ConnectionState.CONNECTED) {
			Log.e(TAG, "not connected")
			return Promise.ofFail(getNotConnectedError(state))
		}
		val deferred = deferred<ByteArray, Exception>()
		val gatt = this.currentGatt!! // Already checked in getConnectionState()
		val char = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
		if (char == null) {
			Log.e(TAG, "characteristic not found: $characteristicUuid")
			return Promise.ofFail(Errors.CharacteristicNotFound())
		}
		promises.setBusy(Action.READ, deferred) // Resolve later in onGattCharacteristicRead
		Log.d(TAG, "gatt.readCharacteristic")
		val result = gatt.readCharacteristic(char)
		if (!result) {
			promises.reject(Errors.ReadFailed())
		}
		return deferred.promise
	}

	@Synchronized private fun onGattCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
		Log.i(TAG, "onCharacteristicRead characteristic=$characteristic status=$status")
		if (gatt == null || gatt != currentGatt || characteristic == null) {
			Log.e(TAG, "gatt=$gatt currentGatt=$currentGatt characteristic=${characteristic?.uuid}")
			return
		}
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Log.e(TAG, "onCharacteristicRead characteristic=${characteristic.uuid} status=$status")
			// TODO: handle error
			// [05-10-2018] It seems like this error is always followed by an onConnectionStateChange
			promises.reject(Errors.Gatt(status))
			return
		}
		// TODO: check if correct characteristic was read
		promises.resolve(Action.READ, characteristic.value)
	}

	/**
	 * Subscribe to a characteristic.
	 *
	 * Notifications will be sent to the callback.
	 *
	 * @return Promise that resolves when subscription succeeded. Value should be used to unsubscribe.
	 */
	@Synchronized fun subscribe(serviceUuid: UUID, characteristicUuid: UUID, callback: (ByteArray) -> Unit): Promise<SubscriptionId, Exception> {
		Log.i(TAG, "subscribe serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid callback=$callback")
//		if (!isBleReady()) {
//			return Promise.ofFail(Errors.BleNotReady())
//		}
		if (promises.isBusy()) {
			return Promise.ofFail(Errors.Busy())
		}
		val state = getConnectionState()
		if (state != ConnectionState.CONNECTED) {
			return Promise.ofFail(getNotConnectedError(state))
		}
		val deferred = deferred<Unit, Exception>()
		val gatt = this.currentGatt!! // Already checked in getConnectionState()
		val char = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
		if (char == null) {
			return Promise.ofFail(Errors.CharacteristicNotFound())
		}
		if (notificationEventBus.hasListeners("$characteristicUuid")) {
			// TODO: support multiple subscriptions to same characteristic
			return Promise.ofFail(Errors.SubscribeAlreadySubscribed())
		}

		promises.setBusy(Action.SUBSCRIBE, deferred) // Resolve later in onGattDescriptorWrite
		Log.d(TAG, "gatt.setCharacteristicNotification")
		var result = gatt.setCharacteristicNotification(char, true)
		if (!result) {
			promises.reject(Errors.SubscribeFailed("set notifications"))
		}
		val descriptor = char.getDescriptor(BluenetProtocol.DESCRIPTOR_CHAR_CONFIG_UUID)
		result = (descriptor != null) && descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
		if (!result) {
			promises.reject(Errors.SubscribeFailed("set descriptor value"))
		}
		Log.d(TAG, "gatt.writeDescriptor")
		result = gatt.writeDescriptor(descriptor)
		if (!result) {
			promises.reject(Errors.SubscribeFailed("write descriptor"))
		}
		return deferred.promise
				.then {
					// Store the callback in the event bus, return unsub id
					return@then notificationEventBus.subscribe("${descriptor.characteristic.uuid}", { data: Any -> callback(data as ByteArray) })
				}
	}

	/**
	 * Unsubscribe from a characteristic.
	 *
	 * @return Promise that resolves when unsubscribe was successful.
	 */
	@Synchronized fun unsubscribe(serviceUuid: UUID, characteristicUuid: UUID, subscriptionId: SubscriptionId): Promise<Unit, Exception> {
		Log.i(TAG, "unsubscribe serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid subscriptionId=$subscriptionId")
//		if (!isBleReady()) {
//			return Promise.ofFail(Errors.BleNotReady())
//		}
		if (promises.isBusy()) {
			return Promise.ofFail(Errors.Busy())
		}
		val state = getConnectionState()
		if (state != ConnectionState.CONNECTED) {
			return Promise.ofFail(getNotConnectedError(state))
		}
		val deferred = deferred<Unit, Exception>()
		val gatt = this.currentGatt!! // Already checked in getConnectionState()
		val char = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
		if (char == null) {
			return Promise.ofFail(Errors.CharacteristicNotFound())
		}
		notificationEventBus.unsubscribe(subscriptionId)
		promises.setBusy(Action.UNSUBSCRIBE, deferred) // Resolve later in onGattDescriptorWrite
		Log.d(TAG, "gatt.setCharacteristicNotification")
		var result = gatt.setCharacteristicNotification(char, false)
		if (!result) {
			promises.reject(Errors.UnsubscribeFailed("set notificaions"))
		}
		val descriptor = char.getDescriptor(BluenetProtocol.DESCRIPTOR_CHAR_CONFIG_UUID)
		result = (descriptor != null) && descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
		if (!result) {
			promises.reject(Errors.UnsubscribeFailed("set descriptor value"))
		}
		Log.d(TAG, "gatt.writeDescriptor")
		result = gatt.writeDescriptor(descriptor)
		if (!result) {
			promises.reject(Errors.UnsubscribeFailed("write descriptor"))
		}
		return deferred.promise
	}

	@Synchronized private fun onGattDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
		Log.i(TAG, "onDescriptorWrite descriptor=$descriptor status=$status")
		if (gatt == null || gatt != currentGatt || descriptor == null || descriptor.uuid != BluenetProtocol.DESCRIPTOR_CHAR_CONFIG_UUID) {
			Log.e(TAG, "gatt=$gatt currentGatt=$currentGatt descriptor=$descriptor")
			return
		}
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Log.e(TAG, "onDescriptorWrite descriptor=$descriptor status=$status")
			// TODO: handle error
			promises.reject(Errors.Gatt(status))
		}
		Log.i(TAG, "value=${Conversion.bytesToString(descriptor.value)}")
		when (descriptor.value) {
			BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE -> {
				// Subscribed
				promises.resolve(Action.SUBSCRIBE)
			}
			BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE -> {
				// Unsubscribed
				promises.resolve(Action.UNSUBSCRIBE)
			}
			else -> {
				Log.e(TAG, "value=${Conversion.bytesToString(descriptor.value)}")
				promises.reject(Errors.Parse("unexpected value"))
			}
		}
	}

	@Synchronized private fun onGattCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
		Log.i(TAG, "onCharacteristicChanged characteristic=$characteristic")
		if (gatt == null || gatt != currentGatt || characteristic == null || characteristic.value == null) {
			Log.e(TAG, "gatt=$gatt currentGatt=$currentGatt characteristic=$characteristic")
			return
		}
		notificationEventBus.emit("${characteristic.uuid}", characteristic.value)
	}

	/**
	 * Subscribes for notifications, calls the callback with merged multipart notifications.
	 *
	 * @return Promise that resolves when subscription succeeded. Value should be used to unsubscribe.
	 */
	@Synchronized fun subscribeMergedNotifications(serviceUuid: UUID, characteristicUuid: UUID, callback: (ByteArray) -> Unit): Promise<SubscriptionId, Exception> {
		Log.i(TAG, "subscribeMergedNotifications serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid")
		val notificationMerger = MultipartNotificationMerger(callback)
		val notificationCallback = fun(data: ByteArray) {
			// Called on each notification
			notificationMerger.onData(data)
		}
		return subscribe(serviceUuid, characteristicUuid, notificationCallback)
	}

	/**
	 * Subscribes for notifications, executes write command, waits for a complete multipart notification to be received, and unsubscribes.
	 *
	 * @return Promise with multipart notification when resolved.
	 */
	@Synchronized fun getSingleMergedNotification(serviceUuid: UUID, characteristicUuid: UUID, writeCommand: () -> Promise<Unit, Exception>): Promise<ByteArray, Exception> {
		Log.i(TAG, "getSingleMergedNotification serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid")
		val deferred = deferred<ByteArray, Exception>()
		var unsubId = UUID(0,0)
		// TODO: timeout

		val notificationMerger = MultipartNotificationMerger({ mergedNotification: ByteArray ->
			// Called when notifications are merged
			unsubscribe(serviceUuid, characteristicUuid, unsubId)
					.success {
						deferred.resolve(mergedNotification)
					}
					.fail {
						deferred.reject(it)
					}
		})

		val notificationCallback = fun (data: ByteArray) {
			// Called on each notification
			notificationMerger.onData(data)
		}

		subscribe(serviceUuid, characteristicUuid, notificationCallback)
				.then {
					unsubId = it
					writeCommand()
				}.unwrap()
				.fail {
					deferred.reject(it)
				}

		return deferred.promise
	}

	/**
	 * Subscribes for notifications, calls the callback with merged multipart notifications. Resolves when callback returns done.
	 *
	 * @writeCommand Function that is executed after subscribing to notifications.
	 * @callback     Function that processes the notifications and returns the result.
	 * @timeoutMs    Timeout in ms after which the promise is rejected. Set to 0 for no timeout at all.
	 *
	 * @return Promise that resolves when the caller is done receiving merged notifications.
	 */
	@Synchronized fun getMultipleMergedNotifications(serviceUuid: UUID, characteristicUuid: UUID, writeCommand: () -> Promise<Unit, Exception>, callback: ProcessCallback, timeoutMs: Long): Promise<Unit, Exception> {
		Log.i(TAG, "getMultipleMergedNotifications serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid")
		val deferred = deferred<Unit, Exception>()
		var unsubId = UUID(0,0)
		var done = false

		val timeoutRunnable = Runnable {
			Log.i(TAG, "getMultipleMergedNotifications timeout")
			if (!done) {
				done = true
				unsubscribe(serviceUuid, characteristicUuid, unsubId)
						.always {
							deferred.reject(Errors.Timeout())
						}
			}
		}

		if (timeoutMs > 0) {
			handler.postDelayed(timeoutRunnable, timeoutMs)
		}

		fun onDone() {
			if (!done) {
				handler.removeCallbacks(timeoutRunnable)
				done = true
				deferred.resolve()
			}
		}

		fun onError(error: Exception) {
			if (!done) {
				handler.removeCallbacks(timeoutRunnable)
				done = true
				deferred.reject(error)
			}
		}

		val notificationCallback = fun (mergedNotification: ByteArray) {
			when (callback(mergedNotification)) {
				ProcessResult.NOT_DONE -> {
					// Continue waiting for notifications
				}
				ProcessResult.DONE -> {
					unsubscribe(serviceUuid, characteristicUuid, unsubId)
							.success { onDone() }
							.fail { onError(it) }
				}
				ProcessResult.ERROR -> {
					unsubscribe(serviceUuid, characteristicUuid, unsubId)
							.always { onError(Errors.ProcessCallback()) }
				}
			}
		}

		subscribeMergedNotifications(serviceUuid, characteristicUuid, notificationCallback)
				.then {
					unsubId = it
					writeCommand()
				}.unwrap()
				.fail {
					onError(it)
				}

		return deferred.promise
	}



	@Synchronized private fun onBleTurnedOff(data: Any?) {
		// //TODO: When bluetooth is turned off, we should close the gatt?
		// Reject current action before closing.
		promises.reject(Errors.BleNotReady())
		close(true)
		// Or just set gatt to null?
	}

	private val gattCallback = object: BluetoothGattCallback() {
		override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
			onGattConnectionStateChange(gatt, status, newState)
		}

		override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
			onGattServicesDiscovered(gatt, status)
		}

		override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
			onGattCharacteristicRead(gatt, characteristic, status)
		}

		override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
			onGattCharacteristicWrite(gatt, characteristic, status)
		}

		override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
			onGattCharacteristicChanged(gatt, characteristic)
		}

		override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
			onGattDescriptorWrite(gatt, descriptor, status)
		}

		override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
			Log.i(TAG, "onDescriptorRead descriptor=$descriptor status=$status")
		}
	}

	@Synchronized private fun getConnectionState(): ConnectionState {
		val gatt = this.currentGatt
		if (gatt == null) {
			Log.d(TAG, "gatt is null")
			return ConnectionState.CLOSED
		}
		val state = bleManager.getConnectionState(gatt.device, BluetoothProfile.GATT)
		Log.d(TAG, "state=${getStateString(state)}")
		return when (state) {
			BluetoothProfile.STATE_DISCONNECTED -> ConnectionState.DISCONNECTED
			BluetoothProfile.STATE_CONNECTING-> ConnectionState.CONNECTING
			BluetoothProfile.STATE_CONNECTED -> ConnectionState.CONNECTED
			BluetoothProfile.STATE_DISCONNECTING -> ConnectionState.DISCONNECTING
			else -> ConnectionState.UNKNOWN
		}
	}

	@Synchronized private fun getNotConnectedError(state: ConnectionState): Exception {
		return when (state) {
			ConnectionState.CLOSED -> Errors.NotConnected()
			ConnectionState.DISCONNECTED -> Errors.NotConnected()
			else -> Errors.BusyWrongState() // This shouldn't happen
		}
	}

	enum class ConnectionState {
		UNKNOWN,
		CLOSED,
		DISCONNECTED,
		CONNECTED,
		CONNECTING,
		DISCONNECTING,
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
}