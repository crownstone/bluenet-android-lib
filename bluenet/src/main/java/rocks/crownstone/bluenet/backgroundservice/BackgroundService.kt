/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import rocks.crownstone.bluenet.scanning.BleScanner
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log


class BackgroundService: Service() {
	private val TAG = this.javaClass.simpleName
//	lateinit var instance: BackgroundService
	private val binder = ServiceBinder()

	private lateinit var eventBus: EventBus
	private lateinit var scanner: BleScanner
//	private lateinit var advertiser:

	inner class ServiceBinder : Binder() {
		fun getService(): BackgroundService {
//			return instance
			return this@BackgroundService
		}
	}


	override fun onCreate() {
		super.onCreate()
		Log.i(TAG, "onCreate")
//		instance = this
	}


	// The system calls your Service’s onBind() only once when the first client binds to retrieve the IBinder.
	// The system won’t call onBind() again when any additional clients bind. Instead it’ll deliver the same IBinder.
	override fun onBind(intent: Intent?): IBinder {
		Log.i(TAG, "onBind")
		parseIntent(intent)
		return binder
	}

	// Never used?
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.i(TAG, "onStartCommand")
		parseIntent(intent)
//		return Service.START_STICKY
		return Service.START_REDELIVER_INTENT
	}

	private fun parseIntent(intent: Intent?) {
		if (intent == null) {
			return
		}
		val bla = intent.getIntExtra("bla", 0)
	}

	override fun onDestroy() {
		Log.i(TAG, "onDestroy")
		super.onDestroy()
	}

	fun setEventBus(eventBus: EventBus) {
		Log.i(TAG, "setEventBus")
		this.eventBus = eventBus
	}

	fun setBleScanner(scanner: BleScanner) {
		Log.i(TAG, "setBleScanner")
		this.scanner = scanner
	}
}