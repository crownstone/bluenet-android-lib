/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet

import android.app.Activity
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.connection.*
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.scanning.BleScanner
import rocks.crownstone.bluenet.scanhandling.IbeaconRanger
import rocks.crownstone.bluenet.scanhandling.NearestDevices
import rocks.crownstone.bluenet.scanhandling.ScanHandler
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.util.*

/**
 * The main library class.
 *
 * This is the main class a user should use.
 *
 * @param looper Optional looper to be used by the library.
 */
class Bluenet(looper: Looper? = null) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = EventBus()
	private val looper: Looper
	private val handler: Handler
	private lateinit var context: Context
	private lateinit var bleCore: BleCore
	private lateinit var connection: ExtConnection
	private var bleScanner: BleScanner? = null
	private var scanHandler: ScanHandler? = null
	private lateinit var service: BackgroundServiceManager
	private val encryptionManager = EncryptionManager()
	private val nearestDevices = NearestDevices(eventBus)

	// State
	private var initialized = false
//	private var scannerReadyPromises = ArrayList<Deferred<Unit, Exception>>()
	private var scannerReadyPromise: Deferred<Unit, Exception>? = null

	// Public variables, used to write and read services.
	lateinit var setup: Setup; private set
	lateinit var control: Control; private set
	lateinit var config: Config; private set
	lateinit var state: State; private set
	lateinit var deviceInfo: DeviceInfo; private set
	lateinit var dfu: Dfu; private set

	// Public variables
	lateinit var iBeaconRanger: IbeaconRanger; private set

	init {
		if (looper != null) {
			this.looper = looper
		}
		else {
			// Current thread:
			this.looper = Looper.myLooper()

//			// Own thread:
//			val handlerThread = HandlerThread("Bluenet")
//			handlerThread.start()
//			this.looper = handlerThread.looper

//			// Mainthread:
//			this.looper = Looper.getMainLooper()
		}
		handler = Handler(this.looper)
	}

	/**
	 * Init the library with background service.
	 *
	 * @param appContext Context of the app
	 * @return Promise that resolves when initialized.
	 */
	@Synchronized
	fun init(appContext: Context): Promise<Unit, Exception> {
		return initBluenet(appContext, null, null)
	}

	/**
	 * Init the library with foreground service.
	 *
	 * This will show an ongoing notification, which makes it less likely for the app to get killed for battery saving.
	 *
	 * @param appContext Context of the app
	 * @param notificationId Notification ID used as reference to modify the notification later on. Should be a unique number.
	 * @param notification   The notification.
	 * @return Promise that resolves when initialized.
	 */
	@Synchronized
	fun init(appContext: Context, notificationId: Int, notification: Notification): Promise<Unit, Exception> {
		return initBluenet(appContext, notificationId, notification)
	}

	@Synchronized
	private fun initBluenet(appContext: Context, notificationId: Int?, notification: Notification?): Promise<Unit, Exception> {
		Log.i(TAG, "init")
		if (initialized) {
			return Promise.ofSuccess(Unit)
		}
		if (appContext is Activity) {
			Log.e(TAG, "Context cannot be activity, use getApplicationContext() instead.")
			return Promise.ofFail(java.lang.Exception("Context cannot be activity, use getApplicationContext() instead."))
//			context = appContext.application // Still got "leaked ServiceConnection"
//			context = appContext.applicationContext // Still got "leaked ServiceConnection"
		}
		else {
			context = appContext
		}

		eventBus.subscribe(BluenetEvent.CORE_SCANNER_READY, ::onCoreScannerReady)
		eventBus.subscribe(BluenetEvent.CORE_SCANNER_NOT_READY, ::onCoreScannerNotReady)
		eventBus.subscribe(BluenetEvent.LOCATION_PERMISSION_GRANTED, ::onPermissionGranted)

		bleCore = BleCore(context, eventBus, looper)
		bleCore.initBle()
		connection = ExtConnection(eventBus, bleCore, encryptionManager)
		setup = Setup(eventBus, connection)
		control = Control(eventBus, connection)
		config = Config(eventBus, connection)
		state = State(eventBus, connection)
		deviceInfo = DeviceInfo(eventBus, connection)
		dfu = Dfu(eventBus, connection, context)
		iBeaconRanger = IbeaconRanger(eventBus, looper)

		service = BackgroundServiceManager(appContext, eventBus, looper)
		if (notificationId == null || notification == null) {
			return service.runInBackground()
					.success {
						Log.i(TAG, "init success")
						initialized = true
						eventBus.emit(BluenetEvent.INITIALIZED)
					}
		}
		return service.runInForeground(notificationId, notification)
				.success {
					Log.i(TAG, "init success")
					initialized = true
					eventBus.emit(BluenetEvent.INITIALIZED)
				}
	}

	/**
	 * De-init the library.
	 *
	 * Cleans up everything that isn't automatically cleaned.
	 */
	@Synchronized
	fun destroy() {
		bleScanner?.stopScan()
		connection.disconnect()


		iBeaconRanger.destroy()
		service.destroy()
		// TODO: more?
	}

	/**
	 * Initializes the scanner.
	 *
	 * @param activity Activity that will be used to ask for permissions (if needed).
	 *                 The activity should have Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls Bluenet.handlePermissionResult().
	 * @return Promise that resolves when initialized (rejected when BLE hardware or location permission is missing).
	 *         When resolved, you can already call startScanning(), but it will give no result until scanner is ready.
	 */
	@Synchronized
	fun initScanner(activity: Activity): Promise<Unit, Exception> {
		Log.i(TAG, "initScanner")
		return bleCore.getLocationPermission(activity)
				.success {
					initScanner()
				}
	}

	@Synchronized
	private fun initScanner() {
		bleCore.initScanner()
		if (bleScanner == null) {
			bleScanner = BleScanner(eventBus, bleCore, looper)
		}
		if (scanHandler == null) {
			scanHandler = ScanHandler(eventBus, encryptionManager)
		}
	}

	/**
	 * @return True when location permission is granted.
	 */
	@Synchronized
	fun isLocationPermissionGranted(): Boolean {
		return bleCore.isLocationPermissionGranted()
	}

	/**
	 * @return True when bluetooth is enabled.
	 */
	@Synchronized
	fun isBleEnabled(): Boolean {
		return bleCore.isBleEnabled()
	}

	/**
	 * @return True when location service is enabled.
	 */
	@Synchronized
	fun isLocationServiceEnabled(): Boolean {
		return bleCore.isLocationServiceEnabled()
	}

	/**
	 * @return True when scanner is ready to start scanning.
	 */
	@Synchronized
	fun isScannerReady(): Boolean {
		return (initialized) && (bleScanner != null) && (bleCore.isScannerReady())
	}

	/**
	 * Check if ready to scan and connect.
	 *
	 * Can safely be called multiple times without waiting for the result.
	 *
	 * @return Promise that resolves when ready to scan and connect, never rejected.
	 */
	@Synchronized
	fun isReadyPromise(): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		checkReady(deferred)
		return deferred.promise
	}

	@Synchronized
	private fun checkReady(deferred: Deferred<Unit, Exception>) {
		if (isScannerReady()) {
			deferred.resolve()
			return
		}
		Util.waitPromise(100, handler)
				.success { checkReady(deferred) }
	}

	/**
	 * Try to make the scanner ready to scan.
	 * You can wait for the event SCANNER_READY.
	 * @param activity Activity to be used to ask for requests.
	 *                 The activity should have Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls Bluenet.handlePermissionResult().
	 *                 The activity should implement Activity.onActivityResult(),
	 *                 and from there call Bluenet.handleActivityResult().
	 */
	@Synchronized
	fun tryMakeScannerReady(activity: Activity) {
		initScanner(activity)
		bleCore.tryMakeScannerReady(activity)
	}

	/**
	 * Make the scanner ready to scan.
	 *
	 * @param activity Activity that will be used to for requests (if needed).
	 *                 The activity should have Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls Bluenet.handlePermissionResult().
	 *                 The activity should implement Activity.onActivityResult(),
	 *                 and from there call Bluenet.handleActivityResult().
	 * @return Promise that resolves when ready to scan, never rejected, except when this function has not been resolved yet.
	 */
	@Synchronized
	fun makeScannerReady(activity: Activity): Promise<Unit, Exception> {
		Log.i(TAG, "makeScannerReady")
		if (isScannerReady()) {
			return Promise.ofSuccess(Unit)
		}
		if (scannerReadyPromise != null) {
			return Promise.ofFail(Errors.Busy())
		}
		val deferred = deferred<Unit, Exception>()
//		scannerReadyPromises.add(deferred)
		scannerReadyPromise = deferred

		bleCore.tryMakeScannerReady(activity)
		return deferred.promise
	}

	/**
	 * Set the keys for encryption and decryption.
	 *
	 * The keys consist of a KeySet per sphere.
	 * SphereId is usually the sphere id that is used in the cloud, but can be any string you like.
	 * Currently, the iBeacon UUID is used to select which KeySet should be used,
	 * so that should match with the iBeacon UUID that is used for setup.
	 */
	@Synchronized
	fun loadKeys(keys: Keys) {
		encryptionManager.setKeys(keys)
	}

	/**
	 * Clear all stored keys for encryption and decryption.
	 */
	@Synchronized
	fun clearKeys() {
		encryptionManager.clearKeys()
	}

	/**
	 * Handles an enable request result.
	 *
	 * @return return true if permission result was handled, false otherwise.
	 */
	@Synchronized
	fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
		return bleCore.handleActivityResult(requestCode, resultCode, data)
	}

	/**
	 * Checks and requests location permission, required for scanning.
	 *
	 * @param activity Activity to be used to ask for permissions.
	 *                 The activity can implement Activity.onRequestPermissionsResult() to see if the user canceled.
	 *                 The request code will be BleCore.REQ_CODE_PERMISSIONS_LOCATION
	 */
	@Synchronized
	fun requestLocationPermission(activity: Activity) {
		bleCore.requestLocationPermission(activity)
	}

	/**
	 * Handles a permission request result, simply passed on from Activity.onRequestPermissionsResult().
	 *
	 * @return return true if permission result was handled, false otherwise.
	 */
	@Synchronized
	fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
		return bleCore.handlePermissionResult(requestCode, permissions, grantResults)
	}

	/**
	 * Subscribe on an event.
	 *
	 * @param eventType Type of event.
	 * @param callback  Function be called, see BluenetEvent for the type data.
	 * @return subscription ID that should be used to unsubscribe.
	 */
	@Synchronized
	fun subscribe(eventType: BluenetEvent, callback: EventCallback) : SubscriptionId {
		return eventBus.subscribe(eventType, callback)
	}

	/**
	 * Subscribe on an event.
	 *
	 * @param eventType Type of event.
	 * @param callback  Function be called, see BluenetEvent for the type data.
	 * @return subscription ID that should be used to unsubscribe.
	 */
	@Synchronized
	fun subscribe(eventType: EventType, callback: EventCallback) : SubscriptionId {
		return eventBus.subscribe(eventType, callback)
	}

	/**
	 * Unsubscribe from an event.
	 *
	 * @param id: subscription ID that was given when subscribing.
	 */
	@Synchronized
	fun unsubscribe(id: SubscriptionId) {
		eventBus.unsubscribe(id)
	}

	/**
	 * Run bluenet service in foreground. This will show an ongoing notification, which makes it less likely for the app to get killed for battery saving.
	 *
	 * @param notificationId Notification ID used as reference to modify the notification later on. Should be a unique number.
	 * @param notification   The notification.
	 * @return Promise that resolves when service runs in foreground.
	 */
	@Synchronized
	fun runInForeground(notificationId: Int, notification: Notification): Promise<Unit, Exception> {
		return service.runInForeground(notificationId, notification)
	}

	/**
	 * Run bluenet service in background. This will make the app more likely to get killed for battery saving.
	 *
	 * @return Promise that resolves when service runs in background.
	 */
	@Synchronized
	fun runInBackground(): Promise<Unit, Exception> {
		return service.runInBackground()
	}

	/**
	 * Filter for Crownstones with service data.
	 *
	 * Can be combined with other filters.
	 *
	 * @param enable Whether to enable this filter.
	 */
	@Synchronized
	fun filterForCrownstones(enable: Boolean) {
		when (enable) {
			true ->  bleScanner?.filterManager?.addCrownstoneFilter()
			false -> bleScanner?.filterManager?.remCrownstoneFilter()
		}
	}

	/**
	 * Filter for iBeacons.
	 *
	 * Can be combined with other filters.
	 *
	 * @param enable Whether to enable this filter.
	 */
	@Synchronized
	fun filterForIbeacons(enable: Boolean) {
		when (enable) {
			true ->  bleScanner?.filterManager?.addIbeaconFilter()
			false -> bleScanner?.filterManager?.remIbeaconFilter()
		}
	}

	/**
	 * Set scan interval: trade off between battery usage and response time.
	 */
	@Synchronized
	fun setScanInterval(scanMode: ScanMode) {
		bleScanner?.setScanInterval(scanMode)
	}

	/**
	 * Start scanning.
	 *
	 * Note: At least one filter is required to be able to scan in the background.
	 */
	@Synchronized
	fun startScanning() {
		Log.i(TAG, "startScanning")
		bleScanner?.startScan()
	}

	/**
	 * Stop scanning.
	 */
	@Synchronized
	fun stopScanning() {
		Log.i(TAG, "stopScanning")
		bleScanner?.stopScan()
	}

//	@Deprecated("subscribe to nearest events instead")
//	@Synchronized
//	fun getNearestValidated(): NearestDeviceListEntry? {
//		return nearestDevices.nearestValidated.getNearest()
//	}

//	@Synchronized
//	fun trackIbeacon(ibeaconUuid: UUID, referenceId: String) {
//		iBeaconRanger.track(ibeaconUuid, referenceId)
//	}
//
//	@Synchronized
//	fun stopTrackingIbeacon(ibeaconUuid: UUID) {
//		iBeaconRanger.stopTracking(ibeaconUuid)
//	}

//	@Synchronized
//	fun setBackground(background: Boolean, notificationId: Int?, notification: Notification?) : Promise<Unit, Exception> {
//		Log.i(TAG, "setBackground $background")
//		if (!background) {
//			return service.runInBackground()
//		}
//		if (notificationId == null || notification == null) {
//			val deferred = deferred<Unit, Exception>()
//			deferred.reject(Exception("Invalid notification"))
//			return deferred.promise
//		}
//		return service.runInForeground(notificationId, notification)
//	}

	/**
	 * Connect to a device.
	 *
	 * @param address   MAC address of the device.
	 * @param timeoutMs Optional: timeout in ms.
	 * @return Promise that resolves when connected.
	 */
	@Synchronized
	fun connect(address: DeviceAddress, timeoutMs: Long = BluenetConfig.TIMEOUT_CONNECT): Promise<Unit, Exception> {
		Log.i(TAG, "connect $address")
		return connection.connect(address, timeoutMs)
	}

	/**
	 * Disconnect from a device.
	 *
	 * @param clearCache True to clear the services cache after disconnecting. Handy when you expect them to change.
	 * @return Promise that resolves when disconnected.
	 */
	@Synchronized
	fun disconnect(clearCache: Boolean = false): Promise<Unit, Exception> {
		Log.i(TAG, "disconnect clearCache=$clearCache")
		return connection.disconnect(clearCache)
	}

	@Synchronized
	private fun onCoreScannerReady(data: Any) {
		initScanner()
		eventBus.emit(BluenetEvent.SCANNER_READY)
//		for (deferred in scannerReadyPromises) {
//			deferred.resolve()
//		}
//		scannerReadyPromises.clear()
		scannerReadyPromise?.resolve()
	}

	@Synchronized
	private fun onCoreScannerNotReady(data: Any) {
		eventBus.emit(BluenetEvent.SCANNER_NOT_READY)
	}

	@Synchronized
	private fun onPermissionGranted(data: Any) {
		initScanner()
	}
}
