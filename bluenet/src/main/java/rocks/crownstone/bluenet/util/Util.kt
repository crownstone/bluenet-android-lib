package rocks.crownstone.bluenet.util

import android.os.Handler
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.resolve

object Util {
	// Check if Nth bit in a value is set
	fun isBitSet(value: Long, bit: Int): Boolean {
		return value and (1L shl bit) != 0L
	}

	fun isBitSet(value: Int, bit: Int): Boolean {
		return value and (1 shl bit) != 0
	}

	fun isBitSet(value: Short, bit: Int): Boolean {
		return isBitSet(value.toInt(), bit)
	}

	fun isBitSet(value: Byte, bit: Int): Boolean {
		return isBitSet(value.toInt(), bit)
	}

	// Clear the Nth bit in a value
	fun clearBit(value: Int, bit: Int): Int {
		return value and (1 shl bit).inv()
	}

	fun waitPromise(timeMs: Long, handler: Handler): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		handler.postDelayed({deferred.resolve()}, timeMs)
		return deferred.promise
	}

	fun recoverablePromise(promise: Promise<Unit, Exception>, recoverError: (error: Exception) -> Boolean): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		promise
				.success {
					deferred.resolve(it)
				}
				.fail {
					if (recoverError(it)) {
						deferred.resolve()
					}
					else {
						deferred.reject(it)
					}
				}
		return deferred.promise
	}
}