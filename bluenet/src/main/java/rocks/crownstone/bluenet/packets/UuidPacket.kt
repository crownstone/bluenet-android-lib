/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Mar 20, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

open class UuidPacket(uuid: UUID): PacketInterface {
	constructor():                         this(UUID(0,0))

	val TAG = this.javaClass.simpleName
	var uuid: UUID = uuid
		protected set
	var arr: ByteArray = Conversion.uuidToBytes(uuid)
		protected set

	override fun getPacketSize(): Int {
		return arr.size
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < ${getPacketSize()}")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.put(arr)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		arr = ByteArray(bb.remaining())
		bb.get(arr)
		uuid = Conversion.bytesToUuid16(arr)
		return true
	}

	override fun getArray(): ByteArray? {
		return arr
	}

	override fun fromArray(array: ByteArray): Boolean {
		arr = array
		uuid = Conversion.bytesToUuid16(arr)
		return true
	}

	fun getPayload(): ByteArray {
		return arr
	}

	override fun toString(): String {
		return "uuid=$uuid"
	}
}
