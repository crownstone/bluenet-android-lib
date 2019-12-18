/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import java.nio.ByteBuffer

class BroadcastSingleItemPacket: CommandBroadcastPayloadInterface {
	private val TAG = this.javaClass.simpleName
	private lateinit var itemPayload: PacketInterface

	companion object {
		const val MAX_PAYLOAD_SIZE = CommandBroadcastPacket.PAYLOAD_SIZE
	}

	override fun add(item: PacketInterface): Boolean {
		// Only allow 1 item.
		if (::itemPayload.isInitialized) {
			return false
		}
		if (item.getPacketSize() > MAX_PAYLOAD_SIZE) {
			return false
		}
		itemPayload = item
		return true
	}

	override fun isFull(): Boolean {
		return (::itemPayload.isInitialized)
	}

	override fun getPacketSize(): Int {
		if (::itemPayload.isInitialized) {
			return itemPayload.getPacketSize()
		}
		return 0
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		if (::itemPayload.isInitialized) {
			return itemPayload.toBuffer(bb)
		}
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		// Not implemented yet (no need?)
		return false
	}

	override fun toString(): String {
		if (::itemPayload.isInitialized) {
			return itemPayload.toString()
		}
		return "<empty>"
	}
}
