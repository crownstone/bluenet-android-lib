/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 1, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer

class BackgroundBroadcastPayloadPacket(
		val timestamp: Uint32,
		val locationId: Uint8,
		val profileId: Uint8,
		val rssiOffset: Uint8,
		val flagTapToToggle: Boolean
		): PacketInterface {
	val validationTimestamp: Uint16
	val flags: Int
	init {
		validationTimestamp = Conversion.toUint16((timestamp shr 7).toInt())
		var tempFlags: Int = 0
		if (flagTapToToggle) {
			tempFlags += 1 shl 2
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
		bb.putShort(validationTimestamp)
		val data: Int = (locationId.toInt() and 0x3F) shl (3+4+3)
		+ (profileId.toInt() and 0x07) shl (4+3)
		+ (rssiOffset.toInt() and 0x0F) shl (3)
		+ (flags and 0x07) shl (0)
		val data1 = (data shr 8) and 0xFF
		val data2 = data and 0xFF
		bb.put(data1.toByte())
		bb.put(data2.toByte())
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}
