/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log

/**
 * Service that can run in foreground.
 */
class BackgroundServiceManager(appContext: Context, eventBus: EventBus, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val context = appContext
	private val handler = Handler(looper)

	private var foreground = false
	private var startServiceDeferred: Deferred<Unit, Exception>? = null

	private var service: BackgroundService? = null
	private var serviceConnection: ServiceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, binder: IBinder) {
			// This is called from the service thread.
			Log.i(TAG, "service connected")
			handler.post {
				// Decouple from service thread.
				onBackgroundServiceConnected(name, binder)
			}
		}

		override fun onServiceDisconnected(name: ComponentName) {
			Log.i(TAG, "service disconnected")
			handler.post {
				// Decouple from service thread.
				onBackgroundServiceDisconnected(name)
			}
		}
	}

	@Synchronized
	fun destroy() {
		Log.i(TAG, "destroy")
		context.unbindService(serviceConnection)
		val intent = Intent(context, BackgroundService::class.java)
		context.stopService(intent)
		foreground = false
		startServiceDeferred = null
		service = null
	}


	@Synchronized
	fun runInForeground(notificationId: Int, notification: Notification): Promise<Unit, Exception> {
		Log.i(TAG, "runInForeground")
		return startService(true).then {
			startForeground(notificationId, notification)
		}.unwrap()
	}



	@Synchronized
	fun runInBackground(): Promise<Unit, Exception> {
		Log.i(TAG, "runInBackground")
		return startService(false).then {
			// This is called from a new thread.. why?
			startBackground()
		}.unwrap()

//		val deferred = deferred<Unit, Exception>()
//		startService(false)
//				.success {
//					Log.d(TAG, "runInBackground startservice success")
//					handler.post {
//						Log.d(TAG, "runInBackground startbackground")
//						startBackground()
//								.success {
//									Log.d(TAG, "runInBackground success")
//									handler.post { deferred.resolve() }
//								}
//								.fail {
//									handler.post { deferred.reject(it) }
//								}
//					}
//				}
//				.fail {
//					handler.post { deferred.reject(it) }
//				}
//		return deferred.promise
	}



	private fun startService(foreground: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "startService foreground=$foreground")
		val deferred = deferred<Unit, Exception>()
		if (startServiceDeferred != null) {
			Log.e(TAG, "busy")
			deferred.reject(IllegalStateException("busy"))
			return deferred.promise
		}

		// Start service, regardless whether it was started, to make sure startForegroundService() is called.
		val intent = Intent(context, BackgroundService::class.java)
		if (foreground && Build.VERSION.SDK_INT >= 26) {
			context.startForegroundService(intent)
		}
		else {
			context.startService(intent)
		}

		// Bind to service
		if (service != null) {
			Log.i(TAG, "already bound to service")
			deferred.resolve()
			return deferred.promise
		}

		val bindIntent = Intent(context, BackgroundService::class.java)
		val success = context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
		Log.i(TAG, "bind to service: $success")
		if (!success) {
			deferred.reject(Exception("failed to bind to service"))
			return deferred.promise
		}

		// Resolve when service is connected
		startServiceDeferred = deferred
		return deferred.promise
	}

	@Synchronized
	private fun onBackgroundServiceConnected(name: ComponentName, binder: IBinder) {
		Log.i(TAG, "onBackgroundServiceConnected")
		service = (binder as BackgroundService.ServiceBinder).getService()
		service?.setEventBus(eventBus)
		val deferred = startServiceDeferred
		startServiceDeferred = null
		deferred?.resolve()
	}

	@Synchronized
	private fun onBackgroundServiceDisconnected(name: ComponentName) {
		Log.i(TAG, "onBackgroundServiceDisconnected")
		service = null
	}

	private fun startForeground(notificationId: Int, notification: Notification): Promise<Unit, Exception> {
		Log.i(TAG, "startForeground")
		val deferred = deferred<Unit, Exception>()
		val service = service // Else kotlin complains about mutable object
		if (service == null) {
			deferred.reject(Exception("start service first"))
			return deferred.promise
		}
		if (foreground) {
			Log.i(TAG, "already there")
			deferred.resolve()
			return deferred.promise
		}

		foreground = true
		service.startForeground(notificationId, notification)
		deferred.resolve()
		return deferred.promise
	}



	private fun startBackground(): Promise<Unit, Exception> {
		Log.i(TAG, "startBackground")
		val deferred = deferred<Unit, Exception>()
		val service = service // Else kotlin complains about mutable object
		if (service == null) {
			deferred.reject(Exception("start service first"))
			return deferred.promise
		}
		if (!foreground) {
			Log.i(TAG, "already there")
			deferred.resolve()
			return deferred.promise
		}

		foreground = false
		service.stopForeground(true)
		deferred.resolve()
		return deferred.promise
	}
}
