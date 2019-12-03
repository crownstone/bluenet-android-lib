/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

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

	fun isBitSet(value: UInt, bit: Int): Boolean {
		return isBitSet(value.toLong(), bit)
	}

	fun isBitSet(value: UShort, bit: Int): Boolean {
		return isBitSet(value.toInt(), bit)
	}

	fun isBitSet(value: UByte, bit: Int): Boolean {
		return isBitSet(value.toInt(), bit)
	}

	// Clear the Nth bit in a value
	fun clearBit(value: Long, bit: Int): Long {
		return value and (1L shl bit).inv()
	}

	fun clearBit(value: Int, bit: Int): Int {
		return value and (1 shl bit).inv()
	}

	fun clearBit(value: Short, bit: Int): Short {
		return (value.toInt() and (1 shl bit).inv()).toShort()
	}

	fun clearBit(value: Byte, bit: Int): Byte {
		return (value.toInt() and (1 shl bit).inv()).toByte()
	}

	fun clearBit(value: UInt, bit: Int): UInt {
		return value and (1U shl bit).inv()
	}

	fun clearBit(value: UShort, bit: Int): UShort {
		return clearBit(value.toInt(), bit).toUShort()
	}

	fun clearBit(value: UByte, bit: Int): UByte {
		return clearBit(value.toInt(), bit).toUByte()
	}

	// Set the Nth bit in a value
	fun setBit(value: Long, bit: Int): Long {
		return value or (1L shl bit)
	}

	fun setBit(value: Int, bit: Int): Int {
		return value or (1 shl bit)
	}

	fun setBit(value: Short, bit: Int): Short {
		return (value.toInt() or (1 shl bit)).toShort()
	}

	fun setBit(value: Byte, bit: Int): Byte {
		return (value.toInt() or (1 shl bit)).toByte()
	}

	fun setBit(value: UInt, bit: Int): UInt {
		return value or (1U shl bit)
	}

	fun setBit(value: UShort, bit: Int): UShort {
		return setBit(value.toInt(), bit).toUShort()
	}

	fun setBit(value: UByte, bit: Int): UByte {
		return setBit(value.toInt(), bit).toUByte()
	}

	fun waitPromise(timeMs: Long, handler: Handler): Promise<Unit, Exception> {
		if (timeMs <=0 ) {
			return Promise.ofSuccess(Unit)
		}
		val deferred = deferred<Unit, Exception>()
		handler.postDelayed({deferred.resolve()}, timeMs)
		return deferred.promise
	}

	/**
	 * Makes a unit promise recoverable.
	 *
	 * @param promise The promise to recover.
	 * @param recoverError Function that returns true when promise should be recovered. Example: { error -> error is Exception }
	 * @return Promise that resolves when original promise resolves or is recovered.
	 */
	fun recoverableUnitPromise(promise: Promise<Unit, Exception>, recoverError: (error: Exception) -> Boolean): Promise<Unit, Exception> {
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

	/**
	 * Makes a promise recoverable.
	 *
	 * @param promise The promise to recover.
	 * @param recoverPromise Function that returns a new promise, based on the error. Example: { error ->	return@recoverablePromise Promise.ofSuccess(5) }
	 * @return Promise that resolves when original promise resolves, or the recoverPromise when the original promise is rejected.
	 */
	fun <T>recoverablePromise(promise: Promise<T, Exception>, recoverPromise: (error: Exception) -> Promise<T, Exception>): Promise<T, Exception> {
		val deferred = deferred<T, Exception>()
		promise
				.success {
					deferred.resolve(it)
				}
				.fail {
					recoverPromise(it)
							.success {
								deferred.resolve(it)
							}
							.fail {
								deferred.reject(it)
							}
				}
		return deferred.promise
	}
}