/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 1, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion

class BackgroundBroadcastPayloadPacket(
		val timestamp: Uint32,
		locationId: Uint8,
		profileId: Uint8,
		rssiOffset: Uint8,
		flagTapToToggle: Boolean
) : RC5BroadcastPayloadPacket(
		Conversion.toUint16((timestamp shr 7).toInt()),
		locationId,
		profileId,
		rssiOffset,
		flagTapToToggle
) {
	val validationTimestamp: Uint16 = payload
	init {
	}

	override fun toString(): String {
		return "timestamp=$timestamp validationTimestamp=$validationTimestamp ${super.toString()}"
	}
}
