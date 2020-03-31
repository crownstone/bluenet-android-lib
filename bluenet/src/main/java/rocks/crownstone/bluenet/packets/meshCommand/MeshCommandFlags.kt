/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Mar 31, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */


package rocks.crownstone.bluenet.packets.meshCommand

import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Util

class MeshCommandFlags(val broadcast: Boolean, val acked: Boolean, val useKnownIds: Boolean) {
	companion object {
		val BIT_POS_BROADCAST =        0
		val BIT_POS_ACKED =            1
		val BIT_POS_USE_KNOWN_IDS =    2
	}
	var flags: Uint8; private set

	init {
		flags = 0U
		if (broadcast)       flags = Util.setBit(flags, BIT_POS_BROADCAST)
		if (acked)           flags = Util.setBit(flags, BIT_POS_ACKED)
		if (useKnownIds)     flags = Util.setBit(flags, BIT_POS_USE_KNOWN_IDS)
	}

	override fun toString(): String {
		return "MeshCommandFlags(broadcast=$broadcast, acked=$acked, useKnownIds=$useKnownIds, flags=$flags)"
	}
}