/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.advertising

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.Log
import java.nio.ByteBuffer

class AdvertiseSingleItemPacket: CommandAdvertisementPayloadInterface {
	private val TAG = this.javaClass.simpleName
	private lateinit var itemPayload: PacketInterface

	companion object {
		const val MAX_PAYLOAD_SIZE = 11
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
}
