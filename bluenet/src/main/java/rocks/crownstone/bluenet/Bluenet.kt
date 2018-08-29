package rocks.crownstone.bluenet

import android.content.Context
import nl.komponents.kovenant.Promise

class Bluenet {
	val eventBus = EventBus()
	private lateinit var bleCore: BleCore
	private lateinit var bleScanner: BleScanner
	private lateinit var service: BleServiceManager

	var initialized = false

	fun init (appContext: Context) {
		bleCore = BleCore(appContext, eventBus)
		bleScanner = BleScanner(eventBus, bleCore)
		service = BleServiceManager(appContext, eventBus)
	}

	fun isReady() {

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

	fun setBackgroundScanning(background: Boolean) {

	}


	fun connect() {

	}

	fun disconnect() {

	}



}