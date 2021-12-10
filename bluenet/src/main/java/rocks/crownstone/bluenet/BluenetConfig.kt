/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet

object BluenetConfig {
	// Timeouts in ms
	val TIMEOUT_CONNECT: Long =             12000 // 8000 wasn't enough for the lenovo P2.
	val TIMEOUT_DISCONNECT: Long =          3000
	val TIMEOUT_WAIT_FOR_DISCONNECT: Long = 10000
	val TIMEOUT_DISCOVER: Long =            8000
	val TIMEOUT_READ: Long =                3000
	val TIMEOUT_WRITE: Long =               4000
	val TIMEOUT_SUBSCRIBE: Long =           4000
	val TIMEOUT_UNSUBSCRIBE: Long =         4000
	val TIMEOUT_REFRESH_CACHE: Long =       3000
	val TIMEOUT_CONNECT_RETRY: Long =       3000 // Only retry when connect failed in less than this time.
	val DELAY_AFTER_DISCONNECT: Long =      200 // Time to wait after disconnect. The lenovo P2 seemed to need this.

	val DELAY_READ_AFTER_COMMAND: Long =    500 // Time to wait before reading the result of a control command for old versions.

	val TIMEOUT_SET_CONFIG: Long = 5000
	val TIMEOUT_GET_CONFIG: Long = 5000
	val TIMEOUT_GET_STATE: Long = 5000

	val TIMEOUT_CONTROL_RESULT: Long = 5000

	val DELAY_REFRESH_CACHE: Long = 1000

	// See https://android.googlesource.com/platform/packages/apps/Bluetooth/+/b26e4a3f58192cf3c33883982b2ba37c2589fd68/src/com/android/bluetooth/gatt/AppScanStats.java#84
	val SCAN_CHECK_NUM_PER_PERIOD = 4
	val SCAN_CHECK_PERIOD = 30 * 1000

	val CONNECT_RETRIES = 3

	val SETUP_WAIT_FOR_SUCCESS_TIME: Long = 4000

	val WAIT_FOR_DISCONNECT_ATTEMPTS = 15
	val WAIT_FOR_DISCONNECT_ATTEMPT_WAIT: Long = 200

	val COMMAND_BROADCAST_INTERVAL_MS = 250 // Time for each packet to be advertised.
	val COMMAND_BROADCAST_TIME_MS = 1500 // Time for each command to be advertised. Should be a multiple of interval.
	val COMMAND_BROADCAST_RELIABLE_TIME_MS = 5000 // Time for a command that needs high reliability to be advertised. Should be a multiple of interval.
}