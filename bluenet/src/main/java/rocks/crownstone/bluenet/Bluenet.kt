package rocks.crownstone.bluenet

import android.app.Activity
import android.app.Notification
import android.content.Context
import android.util.Log
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred

class Bluenet {
	private val TAG = this::class.java.canonicalName
	private val eventBus = EventBus()
	private lateinit var context: Context
	private lateinit var bleCore: BleCore
	private lateinit var bleScanner: BleScanner
	private lateinit var service: BleServiceManager

	var initialized = false

	fun init (appContext: Context) : Promise<Unit, Exception> {
		context = appContext
		bleCore = BleCore(context, eventBus)
//		bleCore.init()
		bleScanner = BleScanner(eventBus, bleCore)
		service = BleServiceManager(appContext, eventBus)
		return service.runInBackground()
	}

	fun makeBleReady(activity: Activity) : Promise<Unit, Exception> {
		return bleCore.enableBle(activity)
	}

	fun makeScannerReady() {

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

	fun setBackgroundScanning(background: Boolean, notificationId: Int?, notification: Notification?) : Promise<Unit, Exception> {
		Log.i(TAG, "setBackgroundScanning $background")
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


	fun connect() {

	}

	fun disconnect() {

	}

}