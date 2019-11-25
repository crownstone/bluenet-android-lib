/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 25, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.*
import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.math.min

enum class PresenceType(val num: Uint8) {
	ALWAYS_TRUE(0),
	ANYONE_IN_ROOM(1),
	NO_ONE_IN_ROOM(2),
	ANYONE_IN_SPHERE(3),
	NO_ONE_IN_SPHERE(4),
	UNKNOWN(255);
	companion object {
		private val map = values().associateBy(PresenceType::num)
		fun fromNum(action: Uint8): PresenceType {
			return map[action] ?: return UNKNOWN
		}
	}
}

class Presence(type: PresenceType, rooms: ArrayList<Uint8>, timeoutSeconds: Uint32): PacketInterface {
	var type = type
	val rooms = rooms
	var timeoutSeconds = timeoutSeconds
	constructor(): this(PresenceType.UNKNOWN, ArrayList<Uint8>(), 0)

	companion object {
		const val SIZE = 1+8+4
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(type.num)
		var bitmask1: Uint32 = 0
		var bitmask2: Uint32 = 0
		for (room in rooms) {
			when {
				room < 32 -> bitmask1 += (1 shl room.toInt())
				room < 64 -> bitmask2 += (1 shl room.toInt())
				else -> return false
			}
		}
		bb.putInt(bitmask2)
		bb.putInt(bitmask1)
		bb.putInt(timeoutSeconds)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}

		type = PresenceType.fromNum(bb.getUint8())
		val bitmask2 = bb.getUint32()
		val bitmask1 = bb.getUint32()
		rooms.clear()
		for (i in 0 until 32) {
			if ((bitmask1 and (1L shl i)) == 1L) {
				rooms.add(Conversion.toUint8(i))
			}
		}
		for (i in 0 until 32) {
			if ((bitmask2 and (1L shl i)) == 1L) {
				rooms.add(Conversion.toUint8(i + 32))
			}
		}
		timeoutSeconds = bb.getUint32()
		return true
	}
}