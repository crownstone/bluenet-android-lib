package rocks.crownstone.bluenet

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import nl.komponents.kovenant.*


class BleServiceManager(appContext: Context, evtBus: EventBus) {
	private val TAG = this::class.java.canonicalName
	private val eventBus = evtBus
	private val context = appContext

	private var foreground = false
	private var startServiceDeferred: Deferred<Unit, Exception>? = null

	private var service: BleService? = null
	private var serviceConnection: ServiceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, binder: IBinder) {
			Log.i(TAG, "service connected")
			service = (binder as BleService.ServiceBinder).getService()
			service?.setEventBus(eventBus)
			val deferred = startServiceDeferred
			startServiceDeferred = null
			deferred?.resolve()
		}

		override fun onServiceDisconnected(name: ComponentName) {
			Log.i(TAG, "service disconnected")
			service = null
		}
	}



	fun runInForeground(notificationId: Int, notification: Notification): Promise<Unit, Exception> {
		Log.i(TAG, "runInForeground")
		return startService(true).then {
			startForeground(notificationId, notification)
		}.unwrap()
	}



	fun runInBackground(): Promise<Unit, Exception> {
		Log.i(TAG, "runInBackground")
		return startService(false).then {
			startBackground()
		}.unwrap()
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
		val intent = Intent(context, BleService::class.java)
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

		val bindIntent = Intent(context, BleService::class.java)
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



	private fun startForeground(notificationId: Int, notification: Notification): Promise<Unit, Exception> {
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