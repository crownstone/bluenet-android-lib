/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet

object BluenetConfig {
	// Timeouts in ms
	val TIMEOUT_CONNECT: Long =       10000
	val TIMEOUT_DISCONNECT: Long =    3000
	val TIMEOUT_DISCOVER: Long =      8000
	val TIMEOUT_READ: Long =          3000
	val TIMEOUT_WRITE: Long =         4000
	val TIMEOUT_SUBSCRIBE: Long =     4000
	val TIMEOUT_UNSUBSCRIBE: Long =   4000
	val TIMEOUT_REFRESH_CACHE: Long = 3000

	val TIMEOUT_GET_CONFIG: Long = 5000
	val TIMEOUT_GET_STATE: Long = 5000

	val DELAY_REFRESH_CACHE: Long = 1000

	// See https://android.googlesource.com/platform/packages/apps/Bluetooth/+/b26e4a3f58192cf3c33883982b2ba37c2589fd68/src/com/android/bluetooth/gatt/AppScanStats.java#84
	val SCAN_CHECK_NUM_PER_PERIOD = 4
	val SCAN_CHECK_PERIOD = 30 * 1000
}