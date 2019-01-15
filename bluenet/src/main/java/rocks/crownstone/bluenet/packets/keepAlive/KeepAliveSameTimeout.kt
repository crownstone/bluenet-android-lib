/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.keepAlive

import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.KeepAliveActionSwitch
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer

class KeepAliveSameTimeout(val timeout: Uint16): PacketInterface {
	val list = ArrayList<KeepAliveSameTimeoutItem>()

	companion object {
		const val HEADER_SIZE = 4
	}

	fun add(item: KeepAliveSameTimeoutItem) {
		list.add(item)
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + list.size * KeepAliveSameTimeoutItem.SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (list.isEmpty() || bb.remaining() < getPacketSize()) {
			return false
		}
		bb.putShort(timeout)
		bb.put(list.size.toByte())
		bb.put(0) // reserved
		for (it in list) {
			if (!it.toBuffer(bb)) {
				return false
			}
		}
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}


class KeepAliveSameTimeoutItem(val id: Uint8, val actionSwitchValue: KeepAliveActionSwitch): PacketInterface {
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
		bb.put(id.toByte())
		bb.put(actionSwitchValue.actionSwitchValue.toByte())
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}