/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jul 22, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Util
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer

enum class FlagsBitPos(val num: Int) {
	FLAG_IGNORE_FOR_BEHAVIOUR(1),
	FLAG_TAP_TO_TOGGLE(2)
}

open class RC5BroadcastPayloadPacket(
	val payload: Uint16,
	val locationId: Uint8,
	val profileId: Uint8,
	val rssiOffset: Uint8,
	val flagTapToToggle: Boolean,
	val flagIgnoreForBehaviour: Boolean
): PacketInterface {
	val flags: Int
	init {
		var tempFlags: Int = 0
		if (flagTapToToggle) {
			tempFlags = Util.setBit(tempFlags, FlagsBitPos.FLAG_TAP_TO_TOGGLE.num)
		}
		if (flagIgnoreForBehaviour) {
			tempFlags = Util.setBit(tempFlags, FlagsBitPos.FLAG_IGNORE_FOR_BEHAVIOUR.num)
		}
		flags = tempFlags
	}

	companion object {
		const val SIZE = 4
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		val data = Conversion.toUint16(
				((locationId.toInt() and 0x3F) shl (3+4+3)) +
						((profileId.toInt() and 0x07) shl (4+3)) +
						((rssiOffset.toInt() and 0x0F) shl (3)) +
						((flags and 0x07) shl (0))
		)
		bb.putShort(data)
		bb.putShort(payload)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}

	override fun toString(): String {
		return "payload=$payload, locationId=$locationId, profileId=$profileId, rssiOffset=$rssiOffset, flagTapToToggle=$flagTapToToggle, flagIgnoreForBehaviour=$flagIgnoreForBehaviour, flags=$flags"
	}
}
