/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.core

import android.os.Handler
import android.util.Log
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.resolve
import rocks.crownstone.bluenet.structs.Errors

enum class Action {
	NONE,
	CONNECT,
	DISCONNECT,
	DISCOVER,
	READ,
	WRITE,
	SUBSCRIBE,
	UNSUBSCRIBE,
	REFRESH_CACHE,
}

enum class PromiseType {
	NONE,
	UNIT,
	BYTE_ARRAY,
}

class CorePromises(handler: Handler) {
	private val TAG = this.javaClass.simpleName
	private val handler = handler

	// Keeps up what action is expected to be performed
	private var action = Action.NONE

	// Keep up promises
	private var promiseType = PromiseType.NONE
	private var unitPromise: Deferred<Unit, Exception>? = null
	private var byteArrayPromise: Deferred<ByteArray, Exception>? = null

	@Synchronized fun isBusy(): Boolean {
		Log.d(TAG, "isBusy action=${action.name} promiseType=${promiseType.name}")
		if (action == Action.NONE) {
			// Extra check, for development
			if (promiseType != PromiseType.NONE) {
				Log.e(TAG, "promise type is not none")
			}
			return false
		}
		return true
	}

	@Synchronized fun <V> setBusy(action: Action, deferred: Deferred<V, Exception>, timeoutMs: Long): Boolean {
		if (isBusy()) {
			return false
		}
		Log.d(TAG, "setBusy action=${action.name}")
		when (action) {
			Action.CONNECT, Action.DISCONNECT, Action.REFRESH_CACHE, Action.DISCOVER, Action.WRITE, Action.SUBSCRIBE, Action.UNSUBSCRIBE -> {
				promiseType = PromiseType.UNIT
				unitPromise = deferred as Deferred<Unit, Exception> // Can't check :(
			}
			Action.READ -> {
				promiseType = PromiseType.BYTE_ARRAY
				byteArrayPromise = deferred as Deferred<ByteArray, Exception> // Can't check :(
			}
			else -> {
				Log.e(TAG, "wrong action or promise type")
				deferred.reject(Errors.PromisesWrongType())
				return false
			}
		}
		handler.postDelayed(timeoutRunnable, timeoutMs)
		this.action = action
		return true
	}

	val timeoutRunnable = Runnable {
		timeout()
	}

	@Synchronized fun timeout() {
		reject(Errors.Timeout())
	}

//	@Synchronized fun setBusy(action: Action, deferred: Deferred<Unit, Exception>): Boolean {
//		if (isBusy()) {
//			return false
//		}
//		Log.d(TAG, "setBusy action=${action.name}")
//		when (action) {
//			Action.CONNECT, Action.DISCONNECT, Action.REFRESH_CACHE, Action.DISCOVER, Action.WRITE -> {
//				promiseType = PromiseType.UNIT
//				unitPromise = deferred
//				this.action = action
//			}
//			else -> {
//				Log.e(TAG, "wrong action or promise type")
//				return false
//			}
//		}
//		return true
//	}
//
//	@Synchronized fun setBusyByteArray(action: Action, deferred: Deferred<ByteArray, Exception>): Boolean {
//		if (isBusy()) {
//			return false
//		}
//		Log.d(TAG, "setBusy action=${action.name}")
//		when (action) {
//			Action.READ -> {
//				promiseType = PromiseType.BYTE_ARRAY
//				byteArrayPromise = deferred
//			}
//			else -> {
//				Log.e(TAG, "wrong action or promise type")
//				return false
//			}
//		}
//		return true
//	}

	@Synchronized fun resolve(action: Action) {
		Log.d(TAG, "resolve unit action=${action.name}")
		if (action != this.action) {
			// This shouldn't happen
			Log.e(TAG, "wrong action resolved")
			reject(Errors.PromisesWrongActionType())
			return
		}
		if (promiseType != PromiseType.UNIT) {
			// Reject, cause wrong resolve type
			reject(Errors.PromisesWrongType())
			return
		}

		unitPromise?.resolve()
		cleanupPromises()
	}

	@Synchronized fun resolve(action: Action, byteArray: ByteArray) {
		Log.d(TAG, "resolve byte array action=${action.name}")
		if (action != this.action) {
			// This shouldn't happen
			Log.e(TAG, "wrong action resolved")
			reject(Errors.PromisesWrongActionType())
			return
		}
		if (promiseType != PromiseType.BYTE_ARRAY) {
			// Reject, cause wrong resolve type
			reject(Errors.PromisesWrongType())
			return
		}

		byteArrayPromise?.resolve(byteArray)
		cleanupPromises()
	}

	@Synchronized fun reject(error: Exception) {
		Log.d(TAG, "reject error=${error.message}")
		when (promiseType) {
			PromiseType.UNIT -> {
				unitPromise?.reject(error)
			}
			PromiseType.BYTE_ARRAY -> {
				byteArrayPromise?.reject(error)
			}
			else -> Log.w(TAG, "no promise set")
		}
		cleanupPromises()
	}

	@Synchronized fun cleanupPromises() {
		action = Action.NONE
		promiseType = PromiseType.NONE
		unitPromise = null
		byteArrayPromise = null
		handler.removeCallbacks(timeoutRunnable)
	}
}