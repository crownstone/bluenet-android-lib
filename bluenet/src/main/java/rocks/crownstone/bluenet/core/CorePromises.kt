package rocks.crownstone.bluenet.core

import android.util.Log
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.resolve

enum class Action {
	NONE,
	CONNECT,
	DISCONNECT,
	DISCOVER,
	READ,
	WRITE,
	SUBSCRIBE,
	UNSUBSCRIBE,
	REFRESH,
}

enum class PromiseType {
	NONE,
	UNIT,
	BYTE_ARRAY,
}

class CorePromises {
	private val TAG = "CorePromises"

	// Keeps up what action is expected to be performed
	private var action = Action.NONE

	// Keep up promises
	private var promiseType = PromiseType.NONE
	//	private var rejectFun:
	private var unitPromise: Deferred<Unit, Exception>? = null

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

	@Synchronized fun setBusy(action: Action, deferred: Deferred<Unit, Exception>): Boolean {
		if (isBusy()) {
			return false
		}
		Log.d(TAG, "setBusy action=${action.name}")
		when (action) {
			Action.CONNECT, Action.DISCONNECT, Action.REFRESH -> {
				promiseType = PromiseType.UNIT
				unitPromise = deferred
				this.action = action
			}
			else -> {
				Log.e(TAG, "wrong action or promise type")
				return false
			}
		}
		return true
	}

	@Synchronized fun resolve(action: Action) {
		Log.d(TAG, "resolve unit action=${action.name}")
		if (action != this.action) {
			// This shouldn't happen
			Log.e(TAG, "wrong action resolved")
			reject(Exception("wrong action resolved"))
			return
		}
		if (promiseType != PromiseType.UNIT) {
			// Reject, cause wrong resolve type
			reject(Exception("wrong promise type"))
			return
		}

		unitPromise?.resolve()
		cleanupPromises()
	}

	@Synchronized fun reject(error: Exception) {
		Log.d(TAG, "reject error=${error.message}")
		when (promiseType) {
			PromiseType.UNIT -> {
				unitPromise?.reject(error)
				unitPromise = null
			}
			else -> Log.w(TAG, "no promise set")
		}
		cleanupPromises()
	}

	@Synchronized fun cleanupPromises() {
		action = Action.NONE
		promiseType = PromiseType.NONE
		unitPromise = null
	}
}