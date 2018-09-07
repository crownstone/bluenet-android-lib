package rocks.crownstone.bluenet

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import nl.komponents.kovenant.*

class BleCore(appContext: Context, evtBus: EventBus) {
	private val TAG = this::class.java.canonicalName
	private val eventBus = evtBus
	private val context = appContext
	private lateinit var bleManager: BluetoothManager
	private lateinit var bleAdapter: BluetoothAdapter
	private lateinit var scanner: BluetoothLeScanner
	private lateinit var advertiser: BluetoothLeAdvertiser

	private var bleInitialized = false
	private var scannerInitialized = false
	private var advertiserInitialized = false

	// Keep up promises
	private var locationPermissionPromise: Deferred<Unit, Exception>? = null
	private var enableBlePromise: Deferred<Unit, Exception>? = null
	private var enableLocationServicePromise: Deferred<Unit, Exception>? = null

	private val handler = Handler() // Same thread

	// Keep up if broadcast receivers are registered
	private var receiverRegisteredBle = false
	private var receiverRegisteredLocation = false

	private var scanFilters = ArrayList<ScanFilter>()
	private var scanSettings: ScanSettings
	private var scanning = false

	companion object {
		// The permission request code for requesting location (required for ble scanning).
		val REQ_CODE_PERMISSIONS_LOCATION = 101

		// The request code to enable bluetooth.
		val REQ_CODE_ENABLE_BLUETOOOTH = 102

		// The request code to enable location services.
		val REQ_CODE_ENABLE_LOCATION_SERVICE = 103

		// Timeout for a bluetooth enable request. If timeout expires, promise is rejected.
		private val BLUETOOTH_ENABLE_TIMEOUT: Long = 5000

		// Timeout for a location service enable request. If timeout expires, promise is rejected.
		private val LOCATION_SERVICE_ENABLE_TIMEOUT: Long = 10000
	}


	init {
		// Init scan settings
		val builder = ScanSettings.Builder()
		builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
		//builder.setScanResultType(SCAN_RESULT_TYPE_FULL)
		builder.setReportDelay(0)
		if (Build.VERSION.SDK_INT >= 23) {
			builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
			builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
			builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
		}
		if (Build.VERSION.SDK_INT >= 26) {
			builder.setLegacy(true)
		}
		scanSettings = builder.build()
	}

	/**
	 * Initializes BLE
	 *
	 * Checks if hardware is available and registers broadcast receiver.
	 * Does not check if BLE is enabled.
	 *
	 * @return True on success.
	 */
	fun initBle(): Boolean {
		if (bleInitialized) {
			return true
		}

		// Check if phone has bluetooth LE
		if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Log.e(TAG,"No BLE hardware")
			return false
		}

		bleManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		bleAdapter = bleManager.adapter

		// Register the broadcast receiver for bluetooth action state changes
		// Must be done before attempting to enable bluetooth
		if (!receiverRegisteredBle) {
			context.registerReceiver(receiverBle, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
			receiverRegisteredBle = true
		}

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
	fun initScanner(): Boolean {
		if (scannerInitialized) {
			return true
		}
		if (!bleInitialized) {
			Log.e(TAG, "ble not initialzed")
			return false
		}

		if (!isLocationPermissionGranted()) {
			Log.w(TAG, "location permission not granted")
//			handler.post { eventBus.emit(BluenetEvent.NO_LOCATION_SERVICE_PERMISSION) } // Emit event when this function is done.
			eventBus.emit(BluenetEvent.NO_LOCATION_SERVICE_PERMISSION)
			return false
		}

		// Register the broadcast receiver for location manager changes.
		// Must be done before checking if location service is enabled, but after having location permissions.
		if (!receiverRegisteredLocation) {
			context.registerReceiver(receiverLocation, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
			receiverRegisteredLocation = true
		}

		scanner = bleAdapter.bluetoothLeScanner
		scannerInitialized = true
		return true
	}

	/**
	 * Try to make BLE ready to connect.
	 * @param activity Optional activity to be used for requests.
	 */
	fun tryMakeBleReady(activity: Activity?) {
		initBle()
		requestEnableBle(activity)
	}

	/**
	 * Make BLE ready to connect.
	 * @param activity Optional activity to be used for requests.
	 * @return Promise that resolves when ready to connect.
	 */
	fun makeBleReady(activity: Activity?): Promise<Unit, Exception> {
		initBle()
		return enableBle(activity)
	}

	/**
	 * Try to make the scanner ready to scan.
	 * @param activity Activity to be used to ask for requests.
	 */
	fun tryMakeScannerReady(activity: Activity) {
		initBle()
		getLocationPermission(activity)
				.success {
					initScanner()
					requestEnableBle(activity)
					requestEnableLocationService(activity)
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
	fun makeScannerReady(activity: Activity): Promise<Unit, Exception> {
		initBle()
		return getLocationPermission(activity)
				.then {
					initScanner()
				}
				.then {
					enableBle(activity)
				}.unwrap()
				.then {
					enableLocationServices(activity)
				}.unwrap()
	}

	/**
	 * Checks and requests location permission, required for scanning.
	 *
	 * @param activity Activity to be used to ask for permissions.
	 *                 The activity can implement Activity.onRequestPermissionsResult() to see if the user canceled.
	 *                 The request code will be BleCore.REQ_CODE_PERMISSIONS_LOCATION
	 * @return False when unable to make the request
	 */
	fun requestLocationPermission(activity: Activity): Boolean {
		Log.i(TAG, "requestLocationPermission activity=$activity")

		if (isLocationPermissionGranted()) {
			Log.i(TAG, "no need to request")
			return true
		}

		if (!isActivityValid(activity)) {
			Log.w(TAG,"Invalid activity")
			return false
		}

		ActivityCompat.requestPermissions(
				activity,
				arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
				REQ_CODE_PERMISSIONS_LOCATION)

		return true
	}

	/**
	 * Checks and gets location permission, required for scanning.
	 *
	 * Does not check if location service is enabled.
	 *
	 * @param activity Activity that will be used to ask for permissions (if needed).
	 *                 The activity must has Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls BleCore.handlePermissionResult().
	 * @return Promise that will be resolved when permissions are granted.
	 */
	fun getLocationPermission(activity: Activity): Promise<Unit, Exception> {
		Log.i(TAG, "getLocationPermission activity=$activity")
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise

		if (isLocationPermissionGranted()) {
			deferred.resolve()
			return promise
		}

		if (!isActivityValid(activity)) {
			deferred.reject(Exception("Invalid activity"))
			return promise
		}

		if (locationPermissionPromise != null) {
			deferred.reject(Exception("busy"))
			return promise
		}
		locationPermissionPromise = deferred

		ActivityCompat.requestPermissions(
				activity,
				arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
				REQ_CODE_PERMISSIONS_LOCATION)

		// Wait for result
		return promise
	}

	/**
	 * Handles a permission request result, simply passed on from Activity.onRequestPermissionsResult().
	 *
	 * @return return true if permission result was handled, false otherwise.
	 */
	fun handlePermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
		when (requestCode) {
			REQ_CODE_PERMISSIONS_LOCATION -> {
				if (permissions.isNotEmpty() && permissions[0] == Manifest.permission.ACCESS_COARSE_LOCATION &&
						grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// Permission granted.
					locationPermissionPromise?.resolve()
				}
				else {
					// Permission not granted.
					locationPermissionPromise?.reject(Exception("location permission denied"))
				}
				locationPermissionPromise = null
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
	fun requestEnableBle(activity: Activity?): Boolean {
		Log.i(TAG, "requestEnableBle activity=$activity")
		if (isBleEnabled()) {
			Log.i(TAG, "no need to request")
			return true
		}

		val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
		if (isActivityValid(activity)) {
			activity!!.startActivityForResult(intent, REQ_CODE_ENABLE_BLUETOOOTH)
		}
		else {
			context.startActivity(intent)
		}
		return true
	}

	/**
	 * Requests BLE to be enabled.
	 *
	 * @param activity Optional activity to be used to ask for bluetooth to be enabled.
	 *                 The activity should implement Activity.onActivityResult(),
	 *                 and from there call BleCore.handleActivityResult().
	 * @return Promise that will resolve when BLE was enabled, or rejected when user cancels or on timeout.
	 */
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
		enableBleResult(isBleEnabled(), "timeout")
	}

	private fun enableBleResult(enabled: Boolean, reason: String? = null) {
		Log.i(TAG, "enableBleResult $enabled ($reason)")
		handler.removeCallbacks(enableBleTimeout)
		if (enabled) {
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
	 * @param activity Optional activity to be used to ask for location service to be enabled.
	 *                 The activity can implement Activity.onActivityResult() to see if the user canceled the request.
	 *                 The request code will be BleCore.REQ_CODE_ENABLE_LOCATION_SERVICE
	 * @return False when unable to make the request
	 */
	fun requestEnableLocationService(activity: Activity?): Boolean {
		if (isLocationServiceEnabled()) {
			Log.i(TAG, "no need to request")
			return true
		}

		val intent = Intent(context, LocationServiceRequestActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
		if (isActivityValid(activity)) {
			activity!!.startActivityForResult(intent, REQ_CODE_ENABLE_LOCATION_SERVICE)
		}
		else {
			context.startActivity(intent)
		}
		return true
	}

	/**
	 * Requests location service to be enabled.
	 *
	 * @param activity Optional activity to be used to ask for location service to be enabled.
	 *                 The activity should implement Activity.onActivityResult(),
	 *                 and from there call BleCore.handleActivityResult().
	 * @return Promise that will resolve when location service was enabled, or rejected when user cancels or on timeout.
	 */
	fun enableLocationServices(activity: Activity?): Promise<Unit, Exception> {
		Log.i(TAG, "enableLocationServices")
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
		enableLocationServiceResult(isLocationServiceEnabled(), "timeout")
	}

	private fun enableLocationServiceResult(enabled: Boolean, reason: String? = null) {
		Log.i(TAG, "enableLocationServiceResult $enabled ($reason)")
		handler.removeCallbacks(enableLocationServiceTimeout)
		if (enabled) {
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
	fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
		when (requestCode) {
			REQ_CODE_ENABLE_BLUETOOOTH -> {
				Log.i(TAG, "bluetooth enable result: $resultCode")
				if (resultCode == Activity.RESULT_CANCELED) {
					enableBleResult(false, "canceled")
				}
				return true
			}
			REQ_CODE_ENABLE_LOCATION_SERVICE -> {
				Log.i(TAG, "location services enable result: $resultCode")
				if (resultCode == Activity.RESULT_CANCELED) {
					enableLocationServiceResult(false, "canceled")
				}
				return true
			}
		}
		return false
	}


	fun isBleReady(): Boolean {
		return (bleInitialized && isBleEnabled())
	}

	fun isScannerReady(): Boolean {
//		return (scannerInitialized && isBleReady() && isLocationPermissionGranted() && isLocationServiceEnabled())
		return (scannerInitialized && isBleReady() && isLocationServiceEnabled())
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
//			eventBus.emit(BluenetEvent.SCANNER_READY)
//		}
//		return true
//	}

	/**
	 * Check if activity is valid.
	 *
	 * @param activity The activity to check
	 * @return True when valid.
	 */
	private fun isActivityValid(activity: Activity?): Boolean {
		return (activity != null && !activity.isDestroyed)
	}

	/**
	 * Check if bluetooth is enabled. Can only be called after BLE is initialized
	 *
	 * @return True if bluetooth is enabled.
	 */
	private fun isBleEnabled(): Boolean {
		if (bleAdapter.isEnabled && bleAdapter.state == BluetoothAdapter.STATE_ON) {
			return true
		}
		Log.d(TAG, "bluetooth enabled=${bleAdapter.isEnabled} state=${bleAdapter.state} + STATE_ON=${BluetoothAdapter.STATE_ON}")
		return false
	}

	/**
	 * Check if location service is enabled.
	 *
	 * @return True when location service is enabled.
	 */
	private fun isLocationServiceEnabled(): Boolean {
		Log.i(TAG, "isLocationServiceEnabled")
		if (Build.VERSION.SDK_INT < 23) {
			return true
		}
		val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
		val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
		return isGpsEnabled || isNetworkEnabled
	}

	/**
	 * Check if location permissions are granted.
	 *
	 * @return True when permissions are granted.
	 */
	private fun isLocationPermissionGranted(): Boolean {
		Log.i(TAG, "isLocationPermissionGranted")
		if (Build.VERSION.SDK_INT < 23) {
			return true
		}
		val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
		return permissionCheck == PackageManager.PERMISSION_GRANTED
	}

	/**
	 * Broadcast receiver used to handle bluetooth events, i.e. turning bluetooth on/off
	 */
	private val receiverBle = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
				when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
					BluetoothAdapter.STATE_ON -> {
						Log.i(TAG, "bluetooth on")

						enableBleResult(true)
						/*
						// if bluetooth state turns on because of a reset, then reset was completed
						if (_resettingBle) {
							_resettingBle = false
						}
						*/
						eventBus.emit(BluenetEvent.BLE_TURNED_ON)
					}
					BluetoothAdapter.STATE_OFF -> {
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
						eventBus.emit(BluenetEvent.BLE_TURNED_OFF)
					}
				}
			}
		}
	}

	/**
	 * Broadcast receiver used to handle location events, i.e. turning location services on/off.
	 */
	private val receiverLocation = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
				// PROVIDERS_CHANGED_ACTION  are also triggered if mode is changed, so only
				// create events if the _locationsServicesReady flag changes
				if (isLocationServiceEnabled()) {
					enableLocationServiceResult(true)
					eventBus.emit(BluenetEvent.LOCATION_SERVICE_TURNED_ON)
				}
				else {
					/*
					// TODO: _scannerInitialized = false;
					sendEvent(BleCoreTypes.EVT_LOCATION_SERVICES_OFF)
					*/
					eventBus.emit(BluenetEvent.LOCATION_SERVICE_TURNED_OFF)
				}
			}
		}
	}

	fun startScan() {
		if (!initScanner()) {
			return
		}
		scanner.startScan(scanFilters, scanSettings, scanCallback)
		scanning = true
	}

	fun stopScan() {
		if (!initScanner()) {
			return
		}
		scanner.stopScan(scanCallback)
		scanning = false
	}


	private val scanCallback = object: ScanCallback() {
		override fun onScanResult(callbackType: Int, result: ScanResult?) {

			// Sometimes a scan result is still received after scanning has been stopped.
			// Sometimes a scan with invalid rssi is received, ignore this result.
			if (!scanning || result == null || result.rssi >= 0) {
				return
			}

			eventBus.emit(BluenetEvent.SCAN_RESULT_RAW, result)
		}

		override fun onBatchScanResults(results: MutableList<ScanResult>?) {
			if (results != null) {
				for (result in results) {
					onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
				}
			}
		}

		override fun onScanFailed(errorCode: Int) {
			Log.e(TAG, "onScanFailed: $errorCode")
			if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
				// No problem!
				return
			}
			scanning = false
			eventBus.emit(BluenetEvent.SCAN_FAILURE)
		}
	}



}