/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.Log
import java.nio.ByteBuffer
import kotlin.reflect.KClass

class BroadcastItemListPacket: CommandBroadcastPayloadInterface {
	private val TAG = this.javaClass.simpleName
	private val list = ArrayList<PacketInterface>()
	private var size = HEADER_SIZE
	private var itemClass: KClass<out PacketInterface>? = null

	companion object {
		const val HEADER_SIZE = 1
		const val MAX_PAYLOAD_SIZE = 10
		const val MAX_PACKET_SIZE = HEADER_SIZE + MAX_PAYLOAD_SIZE
	}

	override fun add(item: PacketInterface): Boolean {
		// Only allow items of the same type
		if (itemClass == null) {
			itemClass = item::class
		}
		else if (item::class != itemClass) {
			Log.e(TAG, "Wrong class: ${item::class.simpleName} instead of ${itemClass?.simpleName}")
			return false
		}

		val itemSize = item.getPacketSize()
		if (size + itemSize > MAX_PACKET_SIZE) {
			return false
		}
		size += itemSize
		list.add(item)
		return true
	}

	override fun isFull(): Boolean {
		if (list.isEmpty()) {
			return false
		}
		val itemSize = list[0].getPacketSize()
		return (size + itemSize > MAX_PACKET_SIZE)
	}

	override fun getPacketSize(): Int {
//		var size: Int = HEADER_SIZE
//		for (it in list) {
//			size += it.getPacketSize()
//		}
		return size
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(list.size.toByte())
		for (it in list) {
			if (!it.toBuffer(bb)) {
				return false
			}
		}
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		// Not implemented yet (no need?)
		return false
	}
}
