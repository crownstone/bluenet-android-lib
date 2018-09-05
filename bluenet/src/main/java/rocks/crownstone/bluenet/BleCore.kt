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
	private var scannerInitialezed = false
	private var advertiserInitialized = false

	// Keep up promises
	private var locationPermissionPromise: Deferred<Unit, Exception>? = null
	private var enableBlePromise: Deferred<Unit, Exception>? = null
	private var enableLocationServicePromise: Deferred<Unit, Exception>? = null

	// Keep up if broadcast receivers are registered
	private var receiverRegisteredBle = false
	private var receiverRegisteredLocation = false

	private var scanFilters = ArrayList<ScanFilter>()
	private var scanSettings: ScanSettings
	private var scanning = false

	// The permission request code for requesting location (required for ble scanning).
	private val REQ_CODE_PERMISSIONS_LOCATION = 101

	// The request code to enable bluetooth.
	private val REQ_CODE_ENABLE_BLUETOOOTH = 102

	// The request code to enable location services.
	private val REQ_CODE_ENABLE_LOCATION_SERVICES = 103

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
	 * @return Promise that will be resolved or rejected.
	 */
	fun initBle(): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise
		if (bleInitialized) {
			deferred.resolve()
			return promise
		}

		// Check if phone has bluetooth LE
		if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			deferred.reject(Exception("No BLE hardware"))
			return promise
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
		deferred.resolve()
		return promise
	}

	/**
	 * Initializes scanner.
	 *
	 * Checks if hardware is available and registers broadcast receivers.
	 * Checks if required permissions are given.
	 * Does not check if location service is enabled.
	 *
	 * @param activity If not null, will be used to ask for permissions (if needed).
	 *
	 * @return Promise that will be resolved or rejected.
	 */
	fun initScanner(activity: Activity?): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise
		if (!bleInitialized) {
			deferred.reject(Exception("ble not initialzed"))
			return promise
		}

		if (!isLocationPermissionGranted()) {
			deferred.reject(Exception("location permission not granted"))
			return promise
		}

		// Register the broadcast receiver for location manager changes.
		// Must be done before checkLocationServicesEnabled, but after having permissions.
		if (!receiverRegisteredLocation) {
			context.registerReceiver(receiverLocation, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
			receiverRegisteredLocation = true
		}

		scanner = bleAdapter.bluetoothLeScanner

		deferred.resolve()
		return promise
	}

//	fun initAdvertiser(activity: Activity): Promise<Unit, Exception> {
//		val deferred = deferred<Unit, Exception>()
//		val promise = deferred.promise
//		advertiser = bleAdapter.bluetoothLeAdvertiser
//		deferred.resolve()
//		return promise
//	}

	/**
	 * Checks and gets location permission, required for scanning.
	 *
	 * Does not check if location service is enabled.
	 *
	 * @param activity If not null, will be used to ask for permissions (if needed).
	 *                 Make sure that the activity has Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls BleCore.handlePermissionResult().
	 *
	 * @return Promise that will be resolved or rejected.
	 */
	fun getLocationPermission(activity: Activity?): Promise<Unit, Exception> {
		Log.i(TAG, "getLocationPermission activity=$activity")
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise

		if (isLocationPermissionGranted()) {
			deferred.resolve()
			return promise
		}

		if (!isActivityValid(activity)) {
			deferred.reject(Exception("Unable to request location permission"))
			return promise
		}

		if (locationPermissionPromise != null) {
			deferred.reject(Exception("busy"))
			return promise
		}
		locationPermissionPromise = deferred

		ActivityCompat.requestPermissions(
				activity!!,
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
					locationPermissionPromise = null
				}
				else {
					// Permission not granted.
					locationPermissionPromise?.reject(Exception("location permission denied"))
					locationPermissionPromise = null
				}
				return true
			}
		}
		return false
	}


	fun enableBle(activity: Activity?): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise

		if (isBleEnabled()) {
			deferred.resolve()
			return promise
		}

		if (!isActivityValid(activity)) {
			deferred.reject(Exception("bluetooth not enabled"))
			return promise
		}

		if (enableBlePromise != null) {
			deferred.reject(Exception("busy"))
			return promise
		}
		enableBlePromise = deferred


		val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
		activity!!.startActivityForResult(intent, REQ_CODE_ENABLE_BLUETOOOTH)
		// Can also do it without activity, but that might be confusing?
		// context.startActivity(intent)

		// Wait for result
		return promise
	}

	fun enableLocationServices(activity: Activity?): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise

		if (isLocationServicesEnabled()) {
			deferred.resolve()
			return promise
		}

		if (!isActivityValid(activity)) {
			deferred.reject(Exception("location service not enabled"))
			return promise
		}


		val intent = Intent(context, LocationServiceRequestActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
		activity!!.startActivityForResult(intent, REQ_CODE_ENABLE_BLUETOOOTH)
		// Can also do it without activity, but that might be confusing?
		// context.startActivity(intent)

		// Wait for result
		return promise
	}

	/**
	 * Handles an enable request result.
	 *
	 * @return return true if permission result was handled, false otherwise.
	 */
	fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
		when (requestCode) {
			REQ_CODE_ENABLE_BLUETOOOTH -> {
				Log.d(TAG, "bluetooth enable result: $resultCode")
				if (resultCode == Activity.RESULT_CANCELED) {
					Log.i(TAG, "bluetooth not enabled")
					enableBlePromise?.reject(Exception("bluetooth not enabled"))
					enableBlePromise = null
				}
				return true
			}
			REQ_CODE_ENABLE_LOCATION_SERVICES -> {
				Log.d(TAG, "location services enable result: $resultCode")
				if (resultCode == Activity.RESULT_CANCELED) {
					Log.i(TAG, "location services not enabled")
					enableLocationServicePromise?.reject(Exception("location services not enabled"))
					enableLocationServicePromise = null
				}
				return true
			}
		}
		return false
	}



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
	 * Check if bluetooth is enabled.
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
	 * @return True if location service is enabled.
	 */
	private fun isLocationServicesEnabled(): Boolean {
		Log.i(TAG, "isLocationServicesEnabled")
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

						/*
						// Cancel the bluetooth enable request timeout, if any.
						_timeoutHandler.removeCallbacks(_requestEnableBluetoothTimeout)
						if (_requestEnableBluetoothCallback.isCallbackSet()) {
							_requestEnableBluetoothCallback.resolve()
						}

//						_leScanner = _bluetoothAdapter.getBluetoothLeScanner();
//						_scanSettings = new ScanSettings.Builder()
//								.setScanMode(_scanMode)
//								.build();
//						_scanFilters = new ArrayList<>();

						sendEvent(BleCoreTypes.EVT_BLUETOOTH_ON)

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
						sendEvent(BleCoreTypes.EVT_BLUETOOTH_OFF)

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

				if (isLocationServicesEnabled()) {
					/*
					// Cancel the location services enable request timeout, if any.
					_timeoutHandler.removeCallbacks(_requestEnableLocationServicesTimeout)
					if (_requestEnableLocationServicesCallback.isCallbackSet()) {
						_requestEnableLocationServicesCallback.resolve()
					}
					sendEvent(BleCoreTypes.EVT_LOCATION_SERVICES_ON)
					*/
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
//		if (scanner == null) {
//			return false
//		}
		scanner.startScan(scanFilters, scanSettings, scanCallback)
		scanning = true
//		return true
	}

	fun stopScan() {
//		if (scanner == null) {
//			return false
//		}
		scanner.stopScan(scanCallback)
		scanning = false
//		return true
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