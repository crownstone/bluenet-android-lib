/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.keepAlive

import rocks.crownstone.bluenet.structs.MultiKeepAliveType
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.put
import java.nio.ByteBuffer

class MultiKeepAlivePacket(val payload: PacketInterface): PacketInterface {
	var type: MultiKeepAliveType
	init {
		type = when (payload::class) {
			KeepAliveSameTimeout::class -> MultiKeepAliveType.SAME_TIMEOUT
			else -> MultiKeepAliveType.UNKNOWN
		}
	}

	companion object {
		const val HEADER_SIZE = 1
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + payload.getPacketSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (type == MultiKeepAliveType.UNKNOWN || bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(type.num)
		return payload.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}