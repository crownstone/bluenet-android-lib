/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 1, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.advertising

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer

class CommandAdvertisementHeaderPacket(
		val protocol: Int,
		val sphereId: Int,
		val accessLevel: Int,
		val backgroundPayload: BackgroundAdvertisementPayloadPacket
		): PacketInterface {
	val payload: Int
	init {
		val arr = backgroundPayload.getArray()
		payload = when (arr) {
			null -> 0
			else -> Conversion.byteArrayToInt(arr)
		}
	}

	companion object {
		const val SIZE = 8
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		val sequence0: Int = 0
		val data0: Int = (sequence0 and 0x03) shl (3+8+3)
		+ (protocol and 0x07) shl (8+3)
		+ (sphereId and 0xFF) shl (3)
		+ (accessLevel and 0x07) shl (0)
		bb.putShort(Conversion.toUint16(data0))

		val sequence1: Int = 1
		val data1: Int = (sequence1 and 0x03) shl (10+4)
		+ (0 and 0x03FF) shl (4)
		+ ((payload shr (32-4)) and 0x0F) shl (0)
		bb.putShort(Conversion.toUint16(data1))

		val sequence2: Int = 2
		val data2: Int = (sequence2 and 0x03) shl (14)
		+ ((payload shr (32-4-14)) and 0x3FFF) shl (0)
		bb.putShort(Conversion.toUint16(data2))

		val sequence3: Int = 3
		val data3: Int = (sequence3 and 0x03) shl (14)
		+ ((payload shr (32-4-14-14)) and 0x3FFF) shl (0)
		bb.putShort(Conversion.toUint16(data3))

		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}
