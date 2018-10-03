package rocks.crownstone.bluenet

import android.app.Activity
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.util.Log
import nl.komponents.kovenant.*
import nl.komponents.kovenant.ui.successUi
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.scanparsing.ScanHandler

class Bluenet {
	private val TAG = this::class.java.canonicalName
	private val eventBus = EventBus()
	private lateinit var context: Context
	private lateinit var bleCore: BleCore
	private var bleScanner: BleScanner? = null
	private var scanHandler: ScanHandler? = null
	private lateinit var service: BleServiceManager
	private val encryptionManager = EncryptionManager()

	private var initialized = false

	private var scannerReadyPromise: Deferred<Unit, Exception>? = null

	/**
	 * Init the library.
	 *
	 * @param appContext Context of the app
	 * @return Promise that resolves when initialized.
	 */
	@Synchronized fun init(appContext: Context): Promise<Unit, Exception> {
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

		bleCore = BleCore(context, eventBus)
		bleCore.initBle()
		service = BleServiceManager(appContext, eventBus)
		return service.runInBackground()
				.then {
					initialized = true
					eventBus.emit(BluenetEvent.INITIALIZED)
				}
	}

	/**
	 * Initializes the scanner.
	 *
	 * @param activity Activity that will be used to ask for permissions (if needed).
	 *                 The activity must has Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls Bluenet.handlePermissionResult().
	 * @return Promise that resolves when initialized (rejected when BLE hardware or location permission is missing).
	 *         When resolved, you can already call startScanning(), but it will give no result until scanner is ready.
	 */
	@Synchronized fun initScanner(activity: Activity): Promise<Unit, Exception> {
		Log.i(TAG, "initScanner")
		return bleCore.getLocationPermission(activity)
				.then {
					initScanner()
				}
	}

	@Synchronized private fun initScanner() {
		bleCore.initScanner()
		if (bleScanner == null) {
			bleScanner = BleScanner(eventBus, bleCore)
		}
		if (scanHandler == null) {
			scanHandler = ScanHandler(eventBus, encryptionManager)
		}
	}

	/**
	 * @return True when scanner is ready.
	 */
	@Synchronized fun isScannerReady(): Boolean {
		return (bleScanner != null) && (bleCore.isScannerReady())
	}

	/**
	 * Try to make the scanner ready to scan.
	 * You can wait for the event SCANNER_READY.
	 * @param activity Activity to be used to ask for requests.
	 */
	@Synchronized fun tryMakeScannerReady(activity: Activity) {
		initScanner(activity)
		bleCore.tryMakeScannerReady(activity)
	}

	/**
	 * Make the scanner ready to scan.
	 * @param activity Activity that will be used to for requests (if needed).
	 *                 The activity must has Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls Bluenet.handlePermissionResult().
	 *                 The activity should implement Activity.onActivityResult(),
	 *                 and from there call Bluenet.handleActivityResult().
	 * @return Promise that resolves when ready to scan, rejected only when already waiting to be resolved.
	 */
	@Synchronized fun makeScannerReady(activity: Activity): Promise<Unit, Exception> {
		Log.i(TAG, "makeScannerReady")
//		return initScanner(activity)
//				.then {
//					bleCore.makeScannerReady(activity)
//				}.unwrap()
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise

		if (isScannerReady()) {
			deferred.resolve()
			return promise
		}

		if (scannerReadyPromise != null) {
			deferred.reject(Exception("busy"))
			return promise
		}
		scannerReadyPromise = deferred

		bleCore.tryMakeScannerReady(activity)
		return promise
	}

	/**
	 * Set the keys.
	 */
	fun loadSphereData(keys: Keys) {
		encryptionManager.setKeys(keys)
	}

	/**
	 * Handles an enable request result.
	 *
	 * @return return true if permission result was handled, false otherwise.
	 */
	@Synchronized fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
		return bleCore.handleActivityResult(requestCode, resultCode, data)
	}

	/**
	 * Handles a permission request result, simply passed on from Activity.onRequestPermissionsResult().
	 *
	 * @return return true if permission result was handled, false otherwise.
	 */
	@Synchronized fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
		return bleCore.handlePermissionResult(requestCode, permissions, grantResults)
	}



	fun subscribe(eventType: BluenetEvent, callback: EventCallback) : SubscriptionId {
		return eventBus.subscribe(eventType, callback)
	}

	fun subscribe(eventType: EventType, callback: EventCallback) : SubscriptionId {
		return eventBus.subscribe(eventType, callback)
	}


	@Synchronized fun startScanning() {
		Log.i(TAG, "startScanning")
		bleScanner?.startScan()
	}

	@Synchronized fun stopScanning() {
		Log.i(TAG, "stopScanning")
		bleScanner?.stopScan()
	}

	@Synchronized fun setBackground(background: Boolean, notificationId: Int?, notification: Notification?) : Promise<Unit, Exception> {
		Log.i(TAG, "setBackground $background")
		if (!background) {
			return service.runInBackground()
		}
		if (notificationId == null || notification == null) {
			val deferred = deferred<Unit, Exception>()
			deferred.reject(Exception("Invalid notification"))
			return deferred.promise
		}
		return service.runInForeground(notificationId, notification)
	}


	fun connect(address: DeviceAddress): Promise<Unit, Exception> {
		Log.i(TAG, "connect $address")
		return bleCore.connect(address, 0)
	}

	fun disconnect(clearCache: Boolean = false): Promise<Unit, Exception> {
		Log.i(TAG, "disconnect clearCache=$clearCache")
		if (clearCache) {
			return bleCore.disconnect()
					.always { bleCore.close(true) }
		}
		bleCore.close(false)
		return Promise.ofSuccess(Unit)
	}

	@Synchronized private fun onCoreScannerReady(data: Any) {
		initScanner()
		eventBus.emit(BluenetEvent.SCANNER_READY)
		if (scannerReadyPromise != null) {
			scannerReadyPromise?.resolve()
			scannerReadyPromise = null
		}
	}

	private fun onCoreScannerNotReady(data: Any) {
		eventBus.emit(BluenetEvent.SCANNER_NOT_READY)
	}

	private fun onPermissionGranted(data: Any) {
		initScanner()
	}
}