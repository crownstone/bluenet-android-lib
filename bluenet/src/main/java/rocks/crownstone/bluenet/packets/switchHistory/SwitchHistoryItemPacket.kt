/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: May 29, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.switchHistory

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.commandSource.CommandSourcePacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class SwitchHistoryItemPacket: PacketInterface {
	var timestamp: Uint32 = 0U
		private set
	var command: Uint8 = 0U
		private set
	var state: SwitchState = SwitchState(0U)
		private set
	val source = CommandSourcePacket()

	companion object {
		val SIZE = Uint32.SIZE_BYTES + Uint8.SIZE_BYTES + Uint8.SIZE_BYTES + CommandSourcePacket.SIZE
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		timestamp = bb.getUint32()
		command = bb.getUint8()
		state = SwitchState(bb.getUint8())
		return source.fromBuffer(bb)
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun toString(): String {
		return "SwitchHistoryItemPacket(timestamp=$timestamp, command=$command, state=$state, source=$source)"
	}
}