/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jul 22, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion

open class CommandBroadcastRC5Packet(
		val commandCount: Uint8,
		locationId: Uint8,
		profileId: Uint8,
		rssiOffset: Uint8,
		flagTapToToggle: Boolean,
		flagIgnoreForBehaviour: Boolean
) : RC5BroadcastPayloadPacket(
		Conversion.toUint16((commandCount.toInt() shl 8)),
		locationId,
		profileId,
		rssiOffset,
		flagTapToToggle,
		flagIgnoreForBehaviour
) {
	init {
	}

	override fun toString(): String {
		return "commandCount=$commandCount ${super.toString()}"
	}
}
