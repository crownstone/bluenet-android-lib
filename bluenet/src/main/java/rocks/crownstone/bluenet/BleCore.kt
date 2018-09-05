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

	companion object {
		// The permission request code for requesting location (required for ble scanning).
		val REQ_CODE_PERMISSIONS_LOCATION = 101

		// The request code to enable bluetooth.
		val REQ_CODE_ENABLE_BLUETOOOTH = 102

		// The request code to enable location services.
		val REQ_CODE_ENABLE_LOCATION_SERVICE = 103
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
			return true
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
		if (scannerInitialezed) {
			return true
		}
		if (!bleInitialized) {
			Log.e(TAG, "ble not initialzed")
			return false
		}

		if (!isLocationPermissionGranted()) {
			Log.w(TAG, "location permission not granted")
			return false
		}

		// Register the broadcast receiver for location manager changes.
		// Must be done before checking if location service is enabled, but after having location permissions.
		if (!receiverRegisteredLocation) {
			context.registerReceiver(receiverLocation, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
			receiverRegisteredLocation = true
		}

		scanner = bleAdapter.bluetoothLeScanner
		scannerInitialezed = true
		return true
	}

//	fun initAdvertiser(activity: Activity): Promise<Unit, Exception> {
//		val deferred = deferred<Unit, Exception>()
//		val promise = deferred.promise
//		advertiser = bleAdapter.bluetoothLeAdvertiser
//		deferred.resolve()
//		return promise
//	}

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
	 * @param activity If not null, will be used to ask for permissions (if needed).
	 *                 The activity must has Activity.onRequestPermissionsResult() implemented,
	 *                 and from there calls BleCore.handlePermissionResult().
	 *
	 * @return Promise that will be resolved when permissions are granted.
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
	 * Requests location service to be enabled.
	 *
	 * @param activity Optional activity to be used to ask for location service to be enabled.
	 *                 The activity can implement Activity.onActivityResult() to see if the user canceled the request.
	 *                 The request code will be BleCore.REQ_CODE_ENABLE_LOCATION_SERVICE
	 * @return False when unable to make the request
	 */
	fun requestEnableLocationServices(activity: Activity?): Boolean {
		if (isLocationServicesEnabled()) {
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

//	/**
//	 * Handles an enable request result.
//	 *
//	 * @return return true if permission result was handled, false otherwise.
//	 */
//	fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
//		when (requestCode) {
//			REQ_CODE_ENABLE_BLUETOOOTH -> {
//				Log.d(TAG, "bluetooth enable result: $resultCode")
//				if (resultCode == Activity.RESULT_CANCELED) {
//					Log.i(TAG, "bluetooth not enabled")
//					enableBlePromise?.reject(Exception("bluetooth not enabled"))
//					enableBlePromise = null
//				}
//				return true
//			}
//			REQ_CODE_ENABLE_LOCATION_SERVICE -> {
//				Log.d(TAG, "location services enable result: $resultCode")
//				if (resultCode == Activity.RESULT_CANCELED) {
//					Log.i(TAG, "location services not enabled")
//					enableLocationServicePromise?.reject(Exception("location services not enabled"))
//					enableLocationServicePromise = null
//				}
//				return true
//			}
//		}
//		return false
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
	fun isBleEnabled(): Boolean {
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
	fun isLocationServicesEnabled(): Boolean {
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
				if (isLocationServicesEnabled()) {
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