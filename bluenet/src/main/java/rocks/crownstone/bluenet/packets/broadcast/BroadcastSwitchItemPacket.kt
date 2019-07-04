/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.put
import java.nio.ByteBuffer

class BroadcastSwitchItemPacket(var id: Uint8, var switchValue: Uint8): PacketInterface {
	companion object {
		const val SIZE = 2
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(id)
		bb.put(switchValue)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		// Not implemented yet (no need?)
		return false
	}
}