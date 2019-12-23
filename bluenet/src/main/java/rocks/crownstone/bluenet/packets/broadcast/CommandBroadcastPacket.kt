/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 1, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.CommandBroadcastType
import rocks.crownstone.bluenet.structs.SphereId
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.util.put
import rocks.crownstone.bluenet.util.putInt
import java.nio.ByteBuffer

/**
 * Packet to sent command broadcasts.
 * The size is fixed by zero padding.
 */
class CommandBroadcastPacket(val validationTimestamp: Uint32, val sphereId: SphereId, val type: CommandBroadcastType, val payload: CommandBroadcastPayloadInterface): PacketInterface {
	companion object {
		const val HEADER_SIZE = 5
		const val PAYLOAD_SIZE = 11
	}

	override fun getPacketSize(): Int {
//		return HEADER_SIZE + payload.getPacketSize()
		return HEADER_SIZE + PAYLOAD_SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		val payload = this.payload
		if (type == CommandBroadcastType.UNKNOWN || payload.getPacketSize() > PAYLOAD_SIZE || bb.remaining() < getPacketSize()) {
			return false
		}
		bb.putInt(validationTimestamp)
		bb.put(type.num)
		if (!payload.toBuffer(bb)) {
			return false
		}
		for (i in 0 until (PAYLOAD_SIZE - payload.getPacketSize())) {
			bb.put(0)
		}
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}

	override fun toString(): String {
		return "CommandBroadcastPacket(validationTimestamp=$validationTimestamp, sphereId='$sphereId', type=$type, payload=$payload)"
	}
}
