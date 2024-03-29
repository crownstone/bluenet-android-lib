/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.broadcast

import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.resolve
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Errors
import rocks.crownstone.bluenet.structs.SphereId
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Log

enum class CommandBroadcastItemType {
	NO_OP,
	SWITCH,
	SET_TIME,
	BEHAVIOUR_SETTINGS,
}

class CommandBroadcastItem(
		var promise: Deferred<Unit, Exception>?,
		val sphereId: SphereId,
		val type: CommandBroadcastItemType,
		val stoneId: Uint8?,
		val payload: PacketInterface,
		var timeLeftMs: Int,
		val validationTimestamp: Uint32? = null    // Set to null to use the default.
) {
	private val TAG = this.javaClass.simpleName

	@Synchronized
	fun startedAdvertising() {
		Log.v(TAG, "startedAdvertising ${toString()}")
	}

	@Synchronized
	fun stoppedAdvertising(broadcastedTimeMs: Int, error: java.lang.Exception?) {
		Log.v(TAG, "stoppedAdvertising ${toString()}")
		if (error != null) {
			reject(error)
			return
		}
		if (timeLeftMs > 0) {
			timeLeftMs -= broadcastedTimeMs
		}
		if (timeLeftMs <= 0) {
			Log.v(TAG, "resolve ${toString()}")
			promise?.resolve()
			promise = null
			timeLeftMs = 0
		}
	}

	@Synchronized
	fun reject(error: java.lang.Exception) {
		Log.v(TAG, "reject ${toString()}")
		promise?.reject(error)
		promise = null
		timeLeftMs = 0
	}

	@Synchronized
	fun isDone(): Boolean {
		return (timeLeftMs <= 0)
	}

	override fun toString(): String {
		return "CommandBroadcastItem(promise=$promise, sphereId='$sphereId', type=$type, stoneId=$stoneId, timeLeftMs=$timeLeftMs, payload=$payload)"
	}


}
