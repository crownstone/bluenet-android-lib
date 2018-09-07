package rocks.crownstone.bluenet

import android.app.Activity
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.util.Log
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then

class Bluenet {
	private val TAG = this::class.java.canonicalName
	private val eventBus = EventBus()
	private lateinit var context: Context
	private lateinit var bleCore: BleCore
	private lateinit var bleScanner: BleScanner
	private lateinit var service: BleServiceManager

	/**
	 * Init the library.
	 *
	 * @param appContext Context of the app
	 * @return Promise that resolves when initialized.
	 */
	fun init(appContext: Context): Promise<Unit, Exception> {
		context = appContext
		bleCore = BleCore(context, eventBus)
		bleCore.initBle()
		service = BleServiceManager(appContext, eventBus)
		return service.runInBackground()
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
	fun initScanner(activity: Activity): Promise<Unit, Exception> {
		return bleCore.getLocationPermission(activity)
				.then {
					bleCore.initScanner()
					bleCore.requestEnableLocationService(activity)
					bleScanner = BleScanner(eventBus, bleCore)
				}
	}

	/**
	 * @return True when scanner is ready.
	 */
	fun isScannerReady(): Boolean {
		return bleCore.isScannerReady()
	}

	/**
	 * Try to make the scanner ready to scan.
	 * @param activity Activity to be used to ask for requests.
	 */
	fun tryMakeScannerReady(activity: Activity) {
		bleCore.tryMakeScannerReady(activity)
	}

	/**
	 * Make the scanner ready to scan.
	 * @param activity Activity that will be used to for requests (if needed).
	 *                 The activity must has Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls Bluenet.handlePermissionResult().
	 *                 The activity should implement Activity.onActivityResult(),
	 *                 and from there call Bluenet.handleActivityResult().
	 * @return Promise that resolves when ready to scan.
	 */
	fun makeScannerReady(activity: Activity): Promise<Unit, Exception> {
		return bleCore.makeScannerReady(activity)
	}

	/**
	 * Handles an enable request result.
	 *
	 * @return return true if permission result was handled, false otherwise.
	 */
	fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
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


	fun startScanning() {
		bleScanner.startScan()
	}

	fun stopScanning() {
		bleScanner.stopScan()
	}

	fun setBackground(background: Boolean, notificationId: Int?, notification: Notification?) : Promise<Unit, Exception> {
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


//	fun connect(): Promise<Unit, Exception> {
//
//	}

//	fun disconnect(): Promise<Unit, Exception> {
//
//	}


	private fun onLocationPermissionMissing() {

	}
}