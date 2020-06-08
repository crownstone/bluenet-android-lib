/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: May 29, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.commandSource

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class CommandSourcePacket: PacketInterface {
	var viaMesh: Boolean = false
		private set
	var reserved: Uint8 = 0U
		private set
	var type: CommandSourceType = CommandSourceType.ENUM
		private set
	var id: Uint8 = CommandSourceId.UNKNOWN.num
		private set

	companion object {
		val SIZE = 2 // 5b flags, 3b type, 8b id
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < SIZE) {
			return false
		}
		val b1 = bb.getUint8()
		viaMesh = Util.isBitSet(b1, 0)
		val reservedMask: Uint8 = 15U // 4 bits
		reserved = (b1.toUInt() shr 1).toUint8() and reservedMask
//		val typeMask: Uint8 = 224U
//		val typeNum = ((b1 and typeMask).toUInt() shr 5).toUint8()
		val typeMask: Uint8 = 7U // 3 bits
		val typeNum = (b1.toUInt() shr 5).toUint8() and typeMask
		type = CommandSourceType.fromNum(typeNum)
		id = bb.getUint8()
		return true
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun toString(): String {
		return when (type) {
			CommandSourceType.ENUM -> "CommandSourcePacket(viaMesh=$viaMesh, reserved=$reserved, type=$type, id=${CommandSourceId.fromNum(id)})"
			else -> "CommandSourcePacket(viaMesh=$viaMesh, reserved=$reserved, type=$type, id=$id)"
		}
	}
}