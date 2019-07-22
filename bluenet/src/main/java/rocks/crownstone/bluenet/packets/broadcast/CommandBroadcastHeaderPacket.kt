/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 1, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.SphereShortId
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer
import java.util.*

class CommandBroadcastHeaderPacket(
		val protocol: Int,
		val sphereShortId: SphereShortId,
		val accessLevel: Uint8,
		val deviceToken: Uint8,
		val encryptedBackgroundPayload: ByteArray
		): PacketInterface {
	private val encryptedBackgroundPayloadInt: Int
	init {
		encryptedBackgroundPayloadInt = when (encryptedBackgroundPayload.size) {
			4 -> Conversion.byteArrayToInt(encryptedBackgroundPayload)
			else -> 0
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
		val data0: Int = ((sequence0 and 0x03) shl (3+8+3)) +
				((protocol and 0x07) shl (8+3)) +
				((sphereShortId.toInt() and 0xFF) shl (3)) +
				((accessLevel.toInt() and 0x07) shl (0))
//		println("data0 = ${(sequence0 and 0x03) shl (3+8+3)} + ${(protocol and 0x07) shl (8+3)} + ${(sphereShortId.toInt() and 0xFF) shl (3)} + ${(accessLevel.toInt() and 0x07) shl (0)} = $data0")
		bb.putShort(Conversion.toUint16(data0))

		val sequence1: Int = 1
		val data1: Int = ((sequence1 and 0x03) shl (2+8+4)) +
				((0 and 0x03) shl (8+4)) +
				((deviceToken.toInt() and 0xFF) shl (4)) +
				(((encryptedBackgroundPayloadInt shr (32-4)) and 0x0F) shl (0))
//		println("data1 = ${(sequence1 and 0x03) shl (10+4)} + ${(0 and 0x03FF) shl (4)} + ${((encryptedBackgroundPayloadInt shr (32-4)) and 0x0F) shl (0)} = $data1")
		bb.putShort(Conversion.toUint16(data1))

		val sequence2: Int = 2
		val data2: Int = ((sequence2 and 0x03) shl (14)) +
				(((encryptedBackgroundPayloadInt shr (32-4-14)) and 0x3FFF) shl (0))
//		println("data2 = ${(sequence2 and 0x03) shl (14)} + ${((encryptedBackgroundPayloadInt shr (32-4-14)) and 0x3FFF) shl (0)} = $data2")
		bb.putShort(Conversion.toUint16(data2))

		val sequence3: Int = 3
		val data3: Int = ((sequence3 and 0x03) shl (14)) +
				(((encryptedBackgroundPayloadInt shr (32-4-14-14)) and 0x3FFF) shl (0))
//		println("data3 = ${(sequence3 and 0x03) shl (14)} + ${((encryptedBackgroundPayloadInt shr (32-4-14-14)) and 0x3FFF) shl (0)} = $data3")
		bb.putShort(Conversion.toUint16(data3))

		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}

	override fun toString(): String {
		return "CommandBroadcastHeaderPacket(protocol=$protocol, sphereShortId=$sphereShortId, accessLevel=$accessLevel, deviceToken=$deviceToken, encryptedBackgroundPayload=${Arrays.toString(encryptedBackgroundPayload)}, encryptedBackgroundPayloadInt=$encryptedBackgroundPayloadInt)"
	}


}
