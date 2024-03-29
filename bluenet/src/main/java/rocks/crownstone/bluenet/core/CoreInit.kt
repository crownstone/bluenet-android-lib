/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.core

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.structs.BluenetEvent
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.util.*

/**
 * Class that initializes the bluetooth LE core.
 */
open class CoreInit(appContext: Context, eventBus: EventBus, looper: Looper) {
	protected val TAG = "BleCore"
	internal val eventBus = eventBus
	internal val context = appContext
	internal val handler = Handler(looper)

	internal lateinit var bleManager: BluetoothManager
	internal lateinit var bleAdapter: BluetoothAdapter
	protected lateinit var scanner: BluetoothLeScanner
	protected lateinit var advertiser: BluetoothLeAdvertiser

	private var bleInitialized = false
	private var bleEnabled = false // Keeps up whether bluetooth is enabled.
	private var scannerInitialized = false
	private var scannerSet = false // Keeps up whether the var "scanner" is set.
	private var advertiserSet = false // Keeps up whether the var "advertiser" is set.
	private var scannerReady = false

	private var locationEnableDialogShown = false

	// Keep up promises
	private var backgroundLocationPermissionPromise: Deferred<Unit, Exception>? = null
	private var permissionsPromise: Deferred<Unit, Exception>? = null
	private var enableBlePromise: Deferred<Unit, Exception>? = null
	private var enableLocationServicePromise: Deferred<Unit, Exception>? = null

	// Keep up if broadcast receivers are registered
	private var receiverRegisteredBle = false
	private var receiverRegisteredLocation = false

	companion object {
		// The permission request code for requesting location (required for ble scanning).
		const val REQ_CODE_PERMISSIONS_ALL = 57001

		// The request code to enable bluetooth.
		const val REQ_CODE_ENABLE_BLUETOOOTH = 57002

		// The request code to enable location services.
		const val REQ_CODE_ENABLE_LOCATION_SERVICE = 57003

		// Timeout for a location service permission request. If timeout expires, promise is rejected or resolved.
		// This only serves as fallback in case handlePermissionResult() is not called.
		private const val PERMISSIONS_REQUEST_TIMEOUT: Long = 5000

		// Timeout for a bluetooth enable request. If timeout expires, promise is rejected.
		private const val BLUETOOTH_ENABLE_TIMEOUT: Long = 5000

		// Timeout for a location service enable request. If timeout expires, promise is rejected.
		private const val LOCATION_SERVICE_ENABLE_TIMEOUT: Long = 10000

		// Which permissions have to be requested.
		private val permissionRequestsBle =
				if (Build.VERSION.SDK_INT < 29) {
					arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
				}
				else if (Build.VERSION.SDK_INT < 31) {
					arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
				}
				else {
					arrayOf(Manifest.permission.BLUETOOTH_SCAN,
							Manifest.permission.BLUETOOTH_CONNECT,
							Manifest.permission.BLUETOOTH_ADVERTISE)
				}

		private val permissionRequestsLocation =
				if (Build.VERSION.SDK_INT < 29) {
					arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
				}
				else if (Build.VERSION.SDK_INT < 31) {
					arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_BACKGROUND_LOCATION)
				}
				else {
					arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_COARSE_LOCATION)
				}

		private val permissionRequestsLocationBackground =
				if (Build.VERSION.SDK_INT < 29) {
					arrayOf()
				}
				else if (Build.VERSION.SDK_INT < 31) {
					arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_BACKGROUND_LOCATION)
				}
				else {
					arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
				}

		private val permissionRequestsAll = arrayOf(permissionRequestsBle, permissionRequestsLocation, permissionRequestsLocationBackground)
	}

	/**
	 * Initializes BLE
	 *
	 * Checks if hardware is available and registers broadcast receiver.
	 * Does not check if BLE is enabled.
	 *
	 * @return True on success.
	 */
	@Synchronized
	fun initBle(): Boolean {
		Log.i(TAG, "initBle")
		if (bleInitialized) {
			checkBleEnabled()
			return true
		}

		// Check if phone has bluetooth LE. Should already have been checked by manifest (android.hardware.bluetooth_le).
		if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Log.e(TAG,"No BLE hardware")
			return false
		}

		bleManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		bleAdapter = bleManager.adapter
//		bleAdapter = BluetoothAdapter.getDefaultAdapter()
		checkBleEnabled()
		setAdvertiser()

		// Register the broadcast receiver for bluetooth action state changes
		// Must be done before attempting to enable bluetooth
		if (!receiverRegisteredBle) {
			context.registerReceiver(receiverBle, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
			receiverRegisteredBle = true
		}

		Log.i(TAG, "initBle success")
		bleInitialized = true
		return true
	}

	/**
	 * Initializes scanner.
	 *
	 * Checks if hardware is available and registers broadcast receivers.
	 * Checks if required permissions are given.
	 * Does not check if location service is enabled.
	 *
	 * @return True on success.
	 */
	@Synchronized
	fun initScanner(): Boolean {
		Log.i(TAG, "initScanner")
		if (scannerInitialized) {
			return true
		}
		if (!bleInitialized) {
			Log.e(TAG, "ble not initialzed")
			return false
		}

		if (!isPermissionsGranted()) {
			Log.w(TAG, "location permission not granted")
//			handler.post { eventBus.emit(BluenetEvent.PERMISSIONS_MISSING) } // Emit event when this function is done.
			eventBus.emit(BluenetEvent.PERMISSIONS_MISSING)
			return false
		}

		// Register the broadcast receiver for location manager changes.
		// Must be done before checking if location service is enabled, but after having location permissions.
		if (!receiverRegisteredLocation) {
			context.registerReceiver(receiverLocation, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
			receiverRegisteredLocation = true
		}

		setScanner()
		scannerInitialized = true
		handler.post { checkScannerReady() } // Execute this a bit later to avoid sending/handling event before this function returns to caller.
		return true
	}

	// Try to set the scanner object
	private fun setScanner() {
		Log.i(TAG, "setScanner")
		if (!scannerSet) {
			val testScanner = bleAdapter.bluetoothLeScanner // This can return null when ble is not enabled (not documented, jeey)
			if (testScanner != null) {
				scanner = testScanner
				scannerSet = true
				Log.i(TAG, "scanner set")
			}
		}
	}

	// Try to set the advertiser object
	private fun setAdvertiser() {
		Log.i(TAG, "setAdvertiser")
		if (Build.VERSION.SDK_INT >= 31 && !isPermissionsGranted(permissionRequestsBle)) {
			Log.i(TAG, "Pemission required for advertiser")
			return
		}

		if (!advertiserSet) {
			val testAdvertiser = bleAdapter.bluetoothLeAdvertiser
			if (testAdvertiser != null) {
				advertiser = testAdvertiser
				advertiserSet = true
				Log.i(TAG, "advertiser set")
			}
		}
	}

	/**
	 * Try to make BLE ready to connect.
	 * @param activity Optional activity to be used for requests.
	 */
	@Synchronized
	fun tryMakeBleReady(activity: Activity?) {
		initBle()
		requestEnableBle(activity)
	}

	/**
	 * Make BLE ready to connect.
	 * @param activity Optional activity to be used for requests.
	 * @return Promise that resolves when ready to connect.
	 */
	@Synchronized
	fun makeBleReady(activity: Activity?): Promise<Unit, Exception> {
		initBle()
		return enableBle(activity)
	}

	/**
	 * Try to make the scanner ready to scan.
	 * @param activity    Activity to be used to ask for requests.
	 * @param explanation Whether to add an explanation to the requests.
	 */
	@Synchronized
	fun tryMakeScannerReady(activity: Activity?, explanation: Boolean = true) {
		Log.i(TAG, "tryMakeScannerReady activity=$activity")
		initBle()
		if (isPermissionsGranted()) {
			Log.i(TAG, "tryMakeScannerReady: initScanner")
			initScanner()
			requestEnableBle(activity)
			requestEnableLocationService(activity, explanation)
			setScanner()

			// Also try to set advertiser, as that now also requires a permission.
			setAdvertiser()
		}
		else {
			requestPermissions(activity)
		}
	}

	/**
	 * Make the scanner ready to scan.
	 * @param activity Activity that will be used to for requests (if needed).
	 *                 The activity must has Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls BleCore.handlePermissionResult().
	 *                 The activity should implement Activity.onActivityResult(),
	 *                 and from there call BleCore.handleActivityResult().
	 * @return Promise that resolves when ready to scan.
	 */
	@Synchronized
	fun makeScannerReady(activity: Activity): Promise<Unit, Exception> {
		Log.i(TAG, "makeScannerReady activity=$activity")
		initBle()
		return getPermissions(activity)
				.then {
					Log.i(TAG, "makeScannerReady: initScanner")
					initScanner()
				}
				.then {
					Log.i(TAG, "makeScannerReady: enableBle")
					enableBle(activity)
				}.unwrap()
				.then {
					Log.i(TAG, "makeScannerReady: enableLocationService")
					// Also try to set advertiser, as that now also requires a permission.
					setAdvertiser()
					enableLocationService(activity)
				}.unwrap()
				.then {
					Log.i(TAG, "makeScannerReady: setScanner")
					setScanner()
				}
	}

	/**
	 * Check if permissions are requestable.
	 *
	 * @param activity Activity to be used to ask for permissions.
	 * @return True when the next set of permissions can be requested with the given activity.
	 */
	@Synchronized
	fun isPermissionRequestable(activity: Activity): Boolean {
		for (list in permissionRequestsAll) {
			if (!isPermissionsGranted(list)) {
				return isPermissionRequestable(activity, list)
			}
		}
		return true
	}

	/**
	 * Check if given permissions are requestable.
	 *
	 * @param activity Activity to be used to ask for permissions.
	 * @param permissions List of permistions to check.
	 * @return False when unable to make the request.
	 */
	@Synchronized
	fun isPermissionRequestable(activity: Activity, permissions: Array<String>): Boolean {
		for (permission in permissions) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
				Log.w(TAG, "shouldShowRequestPermissionRationale $permission")
				return false
			}
		}
		return true
	}

	/**
	 * Checks and requests permissions, required for scanning.
	 *
	 * @param activity Activity to be used to ask for permissions.
	 *                 The activity can implement Activity.onRequestPermissionsResult() to see if the user canceled.
	 *                 The request code will be BleCore.REQ_CODE_PERMISSIONS_ALL
	 * @return False when unable to make the request
	 */
	@Synchronized
	fun requestPermissions(activity: Activity?): Boolean {
		Log.i(TAG, "requestPermissions activity=$activity")
		for (list in permissionRequestsAll) {
			// Request them only one set at a time.
			if (!isPermissionsGranted(list)) {
				return requestPermissions(activity, list)
			}
		}
		return true
	}

	/**
	 * Checks and requests permissions, required for scanning.
	 *
	 * @param activity Activity to be used to ask for permissions.
	 *                 The activity can implement Activity.onRequestPermissionsResult() to see if the user canceled.
	 *                 The request code will be BleCore.REQ_CODE_PERMISSIONS_ALL
	 * @return False when unable to make the request
	 */
	@Synchronized
	fun requestPermissions(activity: Activity?, permissions: Array<String>): Boolean {
		Log.i(TAG, "requestPermissions activity=$activity permissions=${permissions.contentToString()}")
		if (isPermissionsGranted(permissions)) {
			Log.i(TAG, "no need to request")
			return true
		}

		if (!isActivityValid(activity)) {
			Log.w(TAG,"Invalid activity")
			return false
		}

		for (permission in permissions) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity, permission)) {
				Log.w(TAG, "shouldShowRequestPermissionRationale $permission")
				return false
			}
		}

		ActivityCompat.requestPermissions(
				activity as Activity,
				permissions,
				REQ_CODE_PERMISSIONS_ALL)

		return true
	}

	/**
	 * Checks and gets permissions, required for scanning.
	 *
	 * Does not check if location service is enabled.
	 *
	 * @param activity Activity that will be used to ask for permissions (if needed).
	 *                 The activity should have Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls BleCore.handlePermissionResult().
	 * @return Promise that will be resolved when permissions are granted.
	 */
	@Synchronized
	fun getPermissions(activity: Activity?): Promise<Unit, Exception> {
		Log.i(TAG, "getPermissions activity=$activity")
		return getPermissions(activity, 0)
	}

	fun getPermissions(activity: Activity?, index: Int): Promise<Unit, Exception> {
		Log.i(TAG, "getPermissions activity=$activity index=$index")
		if (index >= permissionRequestsAll.size) {
			return Promise.ofSuccess(Unit)
		}
		return getPermissions(activity, permissionRequestsAll[index])
				.then {
					getPermissions(activity, index + 1)
				}
	}

	/**
	 * Checks and gets permissions, required for scanning.
	 *
	 * Does not check if location service is enabled.
	 *
	 * @param activity Activity that will be used to ask for permissions (if needed).
	 *                 The activity should have Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls BleCore.handlePermissionResult().
	 * @return Promise that will be resolved when permissions are granted.
	 */
	@Synchronized
	fun getPermissions(activity: Activity?, permissions: Array<String>): Promise<Unit, Exception> {
		Log.i(TAG, "getPermissions activity=$activity permissions=${permissions.contentToString()}")
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise

		if (isPermissionsGranted(permissions)) {
			deferred.resolve()
			return promise
		}

		if (activity == null || !isActivityValid(activity)) {
			deferred.reject(Exception("Invalid activity"))
			return promise
		}

		if (permissionsPromise != null) {
			deferred.reject(Exception("busy"))
			return promise
		}
		permissionsPromise = deferred

		ActivityCompat.requestPermissions(
				activity,
				permissions,
				REQ_CODE_PERMISSIONS_ALL)

		handler.postDelayed(getPermissionsTimeout, PERMISSIONS_REQUEST_TIMEOUT)

		// Wait for result
		return promise
	}

	private val getPermissionsTimeout = Runnable {
		onPermissionsTimeout()
	}

	@Synchronized
	fun onPermissionsTimeout() {
		Log.i(TAG, "onPermissionsTimeout")
		if (isPermissionsGranted()) {
			permissionsPromise?.resolve()
		}
		else {
			permissionsPromise?.reject(Exception("permissions not granted after timeout"))
		}
		permissionsPromise = null
	}

	/**
	 * Handles a permission request result, simply passed on from Activity.onRequestPermissionsResult().
	 *
	 * @return return true if permission result was handled, false otherwise.
	 */
	fun handlePermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray, activity: Activity? = null): Boolean {
		when (requestCode) {
			REQ_CODE_PERMISSIONS_ALL -> {
				handler.post {
					// Post, so that this code is executed on correct thread.
					// Only lock once on correct thread. (Is the lock even required then?)
					synchronized(this) {
						handler.removeCallbacks(getPermissionsTimeout)
						Log.i(TAG, "handlePermissionResult permissions=${permissions.contentToString()}")

						for (i in permissions.indices) {
							Log.i(TAG, "permission=${permissions[i]} granted=${grantResults[i]}")
							if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
								if (permissionsPromise != null) {
									Log.i(TAG, "A permission has been denied.")
									permissionsPromise?.reject(Exception("permission ${permissions[i]} denied"))
									permissionsPromise = null
								}
								return@post
							}
						}

						// All permissions that have been request this time are granted.
						if (isPermissionsGranted()) {
							eventBus.emit(BluenetEvent.PERMISSIONS_GRANTED)
						}
						permissionsPromise?.resolve()
						permissionsPromise = null

						if (activity != null && !isPermissionsGranted()) {
							// When the user accecpted this set of permissions, try to request the next one.
							requestPermissions(activity)
						}
					}
				}
				return true
			}
		}
		return false
	}

	/**
	 * Requests BLE to be enabled.
	 *
	 * @param activity Optional activity to be used to ask for bluetooth to be enabled.
	 *                 The activity can implement Activity.onActivityResult() to see if the user canceled the request.
	 *                 The request code will be BleCore.REQ_CODE_ENABLE_BLUETOOOTH
	 * @return False when unable to make the request
	 */
	@Synchronized
	fun requestEnableBle(activity: Activity?): Boolean {
		Log.i(TAG, "requestEnableBle activity=$activity")
		if (isBleEnabled()) {
			Log.i(TAG, "no need to request")
			return true
		}

		val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
		if (isActivityValid(activity)) {
//			intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
			activity!!.startActivityForResult(intent, REQ_CODE_ENABLE_BLUETOOOTH)
		}
		// This is possible, but let's not bother the user when there is no activity.
//		else {
//			intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//			context.startActivity(intent)
//		}
		return false
	}

	/**
	 * Requests BLE to be enabled.
	 *
	 * @param activity Optional activity to be used to ask for bluetooth to be enabled.
	 *                 The activity should implement Activity.onActivityResult(),
	 *                 and from there call BleCore.handleActivityResult().
	 * @return Promise that will resolve when BLE was enabled, or rejected when user cancels or on timeout.
	 */
	@Synchronized
	fun enableBle(activity: Activity?): Promise<Unit, Exception> {
		Log.i(TAG, "enableBle")
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise

		if (isBleEnabled()) {
			deferred.resolve()
			return promise
		}

		if (enableBlePromise != null) {
			deferred.reject(Exception("busy"))
			return promise
		}
		enableBlePromise = deferred

		if (!requestEnableBle(activity)) {
			deferred.reject(Exception("unable to request BLE to be enabled"))
			return promise
		}

		// Wait for result, with timeout
		handler.postDelayed(enableBleTimeout, BLUETOOTH_ENABLE_TIMEOUT)
		return promise
	}

	private val enableBleTimeout = Runnable {
		Log.i(TAG, "enableBleTimeout")
		checkBleEnabled()
		onEnableBleResult(isBleEnabled(), "timeout")
	}

	@Synchronized
	private fun onEnableBleResult(enabled: Boolean, reason: String? = null) {
		Log.i(TAG, "onEnableBleResult $enabled ($reason)")
		handler.removeCallbacks(enableBleTimeout)
		if (enabled) {
			setScanner()

			// Also try to set advertiser, as that now also requires a permission.
			setAdvertiser()
			enableBlePromise?.resolve()
		}
		else {
			enableBlePromise?.reject(Exception(reason))
		}
		enableBlePromise = null
	}

	/**
	 * Requests location service to be enabled.
	 *
	 * @param activity    Optional activity to be used to ask for location service to be enabled.
	 *                    The activity can implement Activity.onActivityResult() to see if the user canceled the request.
	 *                    The request code will be BleCore.REQ_CODE_ENABLE_LOCATION_SERVICE
	 * @param explanation Whether to add an explanation to the requests.
	 * @return False when unable to make the request
	 */
	@Synchronized
	fun requestEnableLocationService(activity: Activity?, explanation: Boolean = false): Boolean {
		Log.i(TAG, "requestEnableLocationService activity=$activity")
		if (isLocationServiceEnabled()) {
			Log.i(TAG, "no need to request")
			return true
		}

		// Can also just use Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
		// but that makes the app go immediately to settings on start, which is confusing.

		// Another option is to use the LocationServiceRequestActivity, but then we get a grey view.

		if (isActivityValid(activity)) {
			if (!explanation) {
				val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
				activity!!.startActivityForResult(intent, REQ_CODE_ENABLE_LOCATION_SERVICE)
				return true
			}

			if (locationEnableDialogShown) {
				return true
			}

			locationEnableDialogShown = true
			val builder = AlertDialog.Builder(activity)
			builder.setTitle("Location not enabled")
			builder.setMessage("Location needs to be enabled to scan for bluetooth devices")
			builder.setPositiveButton("Settings") { dialog, which ->
				val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
				activity!!.startActivityForResult(intent, REQ_CODE_ENABLE_LOCATION_SERVICE)
				locationEnableDialogShown = false
			}
			builder.setNegativeButton("Cancel") { dialog, which ->
				locationEnableDialogShown = false
			}
			builder.setCancelable(true)
			builder.setOnCancelListener {
				locationEnableDialogShown = false
			}
			builder.create().show()

			return true
		}
		// This is possible, but let's not bother the user when there is no activity.
//		else {
//			intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//			context.startActivity(intent)
//		}
		return false
	}

	/**
	 * Requests location service to be enabled.
	 *
	 * @param activity Optional activity to be used to ask for location service to be enabled.
	 *                 The activity should implement Activity.onActivityResult(),
	 *                 and from there call BleCore.handleActivityResult().
	 * @return Promise that will resolve when location service was enabled, or rejected when user cancels or on timeout.
	 */
	@Synchronized
	fun enableLocationService(activity: Activity?): Promise<Unit, Exception> {
		Log.i(TAG, "enableLocationService")
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise

		if (isLocationServiceEnabled()) {
			deferred.resolve()
			return promise
		}

		if (enableLocationServicePromise != null) {
			deferred.reject(Exception("busy"))
			return promise
		}
		enableLocationServicePromise = deferred

		if (!requestEnableLocationService(activity)) {
			deferred.reject(Exception("unable to request location service to be enabled"))
			return promise
		}

		// Wait for result, with timeout
		handler.postDelayed(enableLocationServiceTimeout, LOCATION_SERVICE_ENABLE_TIMEOUT)
		return promise
	}

	private val enableLocationServiceTimeout = Runnable {
		Log.i(TAG, "enableLocationServiceTimeout")
		onEnableLocationServiceResult(isLocationServiceEnabled(), "timeout")
	}

	@Synchronized
	private fun onEnableLocationServiceResult(enabled: Boolean, reason: String? = null) {
		Log.i(TAG, "onEnableLocationServiceResult $enabled ($reason)")
		handler.removeCallbacks(enableLocationServiceTimeout)
		if (enabled) {
			setScanner()
			enableLocationServicePromise?.resolve()
		}
		else {
			enableLocationServicePromise?.reject(Exception(reason))
		}
		enableLocationServicePromise = null
	}

	/**
	 * Handles an enable request result.
	 *
	 * @return return true if permission result was handled, false otherwise.
	 */
	fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
		when (requestCode) {
			REQ_CODE_ENABLE_BLUETOOOTH -> {
				handler.post {
					// Post, so that this code is executed on correct thread.
					// Only lock once on correct thread. (Is the lock even required then?)
					synchronized(this) {
						Log.i(TAG, "bluetooth enable result: $resultCode")
						if (resultCode == Activity.RESULT_CANCELED) {
							onEnableBleResult(false, "canceled")
						}
					}
				}
				return true
			}
			REQ_CODE_ENABLE_LOCATION_SERVICE -> {
				handler.post {
					// Post, so that this code is executed on correct thread.
					// Only lock once on correct thread. (Is the lock even required then?)
					synchronized(this) {
						Log.i(TAG, "location services enable result: $resultCode")
						if (resultCode == Activity.RESULT_CANCELED) {
							onEnableLocationServiceResult(false, "canceled")
						}
					}
				}
				return true
			}
		}
		return false
	}


	@Synchronized
	fun isBleReady(useCache: Boolean = false): Boolean {
		val bleEnabledResult = isBleEnabled(useCache)
		Log.v(TAG, "isBleReady bleInitialized=$bleInitialized isBleEnabled=$bleEnabledResult")
		return (bleInitialized && bleEnabledResult)
	}

	@Synchronized
	fun isAdvertiserReady(useCache: Boolean = false): Boolean {
		return advertiserSet && isBleReady(useCache)
	}

	@Synchronized
	fun isScannerReady(): Boolean {
		Log.v(TAG, "isScannerReady scannerInitialized=$scannerInitialized scannerSet=$scannerSet")
//		return (scannerInitialized && isBleReady() && isLocationPermissionGranted() && isLocationServiceEnabled())
		if (scannerInitialized && scannerSet && isBleReady() && isLocationServiceEnabled()) {
			Log.d(TAG, "isScannerReady scannerInitialized=$scannerInitialized scannerSet=$scannerSet")
//			scannerReady = true
			return true
		}
//		scannerReady = false
		return false
	}

//	/**
//	 * Check if everything is ready
//	 */
//	fun isReady(): Boolean {
//		return isScannerReady()
//	}

//	/**
//	 * Try to make everything ready
//	 */
//	fun makeReady(activity: Activity): Boolean {
//		val wasBleReady = isBleReady()
//		val wasScannerReady = isScannerReady()
//		if (!requestLocationPermission(activity)) {
//			return false
//		}
//		if (!initBle()) {
//			return false
//		}
//		if (!requestEnableBle(activity)) {
//			return false
//		}
//		if (!initScanner()) {
//			return false
//		}
//		if (!requestEnableLocationService(activity)) {
//			return false
//		}
//		// Emit events if now ready
//		if (!wasBleReady && isBleReady()) {
//			eventBus.emit(BluenetEvent.BLE_READY)
//		}
//		if (!wasScannerReady && isScannerReady()) {
//			eventBus.emit(BluenetEvent.CORE_SCANNER_READY)
//		}
//		return true
//	}

	/**
	 * Check if activity is valid.
	 *
	 * @param activity The activity to check
	 * @return True when valid.
	 */
	@Synchronized
	private fun isActivityValid(activity: Activity?): Boolean {
		return (activity != null && !activity.isDestroyed)
	}

	/**
	 * Check if bluetooth is enabled. Can only be called after BLE is initialized.
	 *
	 * @useCache True to use cached value (quicker, but might be wrong sometimes).
	 * @return True if bluetooth is enabled.
	 */
	@Synchronized
	fun isBleEnabled(useCache: Boolean = false): Boolean {
		if (!bleInitialized) {
			Log.w(TAG, "isBleEnabled: BLE not initialized")
			return false
		}
		if (useCache) {
			return bleEnabled
		}
		val result = bleAdapter.isEnabled && bleAdapter.state == BluetoothAdapter.STATE_ON
		if (result != bleEnabled) {
			Log.e(TAG, "BleEnabled cache is wrong: cache=$bleEnabled val=$result")
			bleEnabled = result
		}
		return result
	}

	/**
	 * Check if bluetooth is enabled to update the cache. Can only be called after BLE is initialized.
	 */
	@Synchronized
	fun checkBleEnabled() {
		val result = bleAdapter.isEnabled && bleAdapter.state == BluetoothAdapter.STATE_ON
		Log.i(TAG, "isBleEnabled: $result (enabled=${bleAdapter.isEnabled}, state=${bleAdapter.state}, STATE_ON=${BluetoothAdapter.STATE_ON})")
		bleEnabled = result
	}

	/**
	 * Check if location service is enabled.
	 *
	 * @return True when location service is enabled.
	 */
	@Synchronized
	fun isLocationServiceEnabled(): Boolean {
		if (Build.VERSION.SDK_INT < 23) {
			Log.i(TAG, "isLocationServiceEnabled true")
			return true
		}
		val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
		val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
		val result = isGpsEnabled || isNetworkEnabled
		Log.i(TAG, "isLocationServiceEnabled $result")
		// Log status of all providers
		val providers = locationManager.allProviders
		for (provider in providers) {
			Log.d(TAG, "isProviderEnabled $provider ${locationManager.isProviderEnabled(provider)}")
		}
		return result
	}

	/**
	 * Check if permissions are granted, returns true when all are granted.
	 */
	@Synchronized
	fun isPermissionsGranted(): Boolean {
		for (list in permissionRequestsAll) {
			if (isPermissionsGranted(list) == false) {
				return false
			}
		}
		return true
	}

	/**
	 * Check if given permissions are granted, returns true when all are granted.
	 */
	@Synchronized
	fun isPermissionsGranted(permissions: Array<String>): Boolean {
		for (permission in permissions) {
			val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
			val result = permissionCheck == PackageManager.PERMISSION_GRANTED
			Log.i(TAG, "isPermissionGranted $permission $result")
			if (result == false) {
				return false
			}
		}
		return true
	}

	@Synchronized
	private fun checkScannerReady() {
		Log.i(TAG, "checkScannerReady")
		val wasReady = scannerReady
		scannerReady = isScannerReady()
		if (scannerReady && !wasReady) {
				eventBus.emit(BluenetEvent.CORE_SCANNER_READY)
		}
		else if (!scannerReady && wasReady) {
				eventBus.emit(BluenetEvent.CORE_SCANNER_NOT_READY)
		}
	}

	/**
	 * Broadcast receiver used to handle bluetooth events, i.e. turning bluetooth on/off
	 */
	private val receiverBle = object : BroadcastReceiver() {
		@Synchronized
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
				Log.d(TAG, "bluetooth state: ${intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)}")
				when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
					BluetoothAdapter.STATE_ON -> {
						handler.post {
							Log.i(TAG, "bluetooth on")
							checkBleEnabled()
							onEnableBleResult(true)
							/*
							// if bluetooth state turns on because of a reset, then reset was completed
							if (_resettingBle) {
								_resettingBle = false
							}
							*/
							eventBus.emit(BluenetEvent.BLE_TURNED_ON)
							checkScannerReady()
						}
					}
					BluetoothAdapter.STATE_OFF -> {
						handler.post {
							Log.i(TAG, "bluetooth off")
							/*
							// TODO: this has to happen after event has been sent?
							_connections = HashMap<String, Connection>()
							_scanning = false

							// if bluetooth state turns off because of a reset, enable it again
							if (_resettingBle) {
								_bluetoothAdapter.enable()
							}
							*/
							checkBleEnabled()
							eventBus.emit(BluenetEvent.BLE_TURNED_OFF)
							checkScannerReady()
						}
					}
				}
			}
		}
	}

	/**
	 * Broadcast receiver used to handle location events, i.e. turning location services on/off.
	 */
	private val receiverLocation = object : BroadcastReceiver() {
		@Synchronized
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
				// PROVIDERS_CHANGED_ACTION  are also triggered if mode is changed, so only
				// create events if the _locationsServicesReady flag changes
				if (isLocationServiceEnabled()) {
					handler.post {
						Log.i(TAG, "location service on")
						onEnableLocationServiceResult(true)
						eventBus.emit(BluenetEvent.LOCATION_SERVICE_TURNED_ON)
						checkScannerReady()
					}
				}
				else {
					handler.post {
						Log.i(TAG, "location service off")
						eventBus.emit(BluenetEvent.LOCATION_SERVICE_TURNED_OFF)
						checkScannerReady()
					}
				}
			}
		}
	}

	/**
	 * Turn off bluetooth adapter.
	 *
	 * Sometimes the only solution to bluetooth issues is to turn off and on the bluetooth adapter.
	 * But android doesn't allow apps to turn on bluetooth anymore, so all that's left is to turn it off.
	 * This will trigger a bluetooth turned off event, so that the app can show that to the user
	 * and ask for it to be turned on again.
	 *
	 * Use with care! You don't just want to turn off bluetooth, as it also disconnects other devices.
	 */
	@Synchronized
	fun disableBle(): Boolean {
		Log.i(TAG, "disableBle")
		if (!bleInitialized) {
			Log.w(TAG, "BLE not initialized")
			return false
		}
		checkBleEnabled()
		if (!bleEnabled) {
			Log.i(TAG, "Already disabled")
			return false
		}
		val success = bleAdapter.disable()
		if (!success) {
			Log.w(TAG, "Unable to disable BLE")
		}
		return success
	}

	/**
	 * Turn on bluetooth adapter.
	 *
	 *
	 */
	@Synchronized
	fun enableBle(): Boolean {
		Log.i(TAG, "enableBle")
		if (!bleInitialized) {
			Log.w(TAG, "BLE not initialized")
			return false
		}
		checkBleEnabled()
		if (bleEnabled) {
			Log.i(TAG, "Already enabled")
			return false
		}
		val success = bleAdapter.enable()
		if (!success) {
			Log.w(TAG, "Unable to enable BLE")
		}
		return success
	}
}
