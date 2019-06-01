/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 1, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.advertising

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.put
import rocks.crownstone.bluenet.util.putInt
import java.nio.ByteBuffer

enum class CommandAdvertisementType(val num: Uint8) {
	MULTI_SWITCH(1),
	UNKNOWN(255);
	companion object {
		private val map = CommandAdvertisementType.values().associateBy(CommandAdvertisementType::num)
		fun fromNum(action: Uint8): CommandAdvertisementType {
			return map[action] ?: return UNKNOWN
		}
	}
}

class AdvertiseCommandPacket(val validationTimestamp: Uint32, val type: CommandAdvertisementType, val payload: PacketInterface): PacketInterface {
	companion object {
		const val HEADER_SIZE = 5
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + payload.getPacketSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		val payload = this.payload
		if (payload == null || type == CommandAdvertisementType.UNKNOWN || bb.remaining() < getPacketSize()) {
			return false
		}
		bb.putInt(validationTimestamp)
		bb.put(type.num)
		return payload.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}
