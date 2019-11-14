/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class ByteArrayPacket(data: ByteArray): PacketInterface {
	constructor():                         this(ByteArray(0))
	constructor(byte: Byte):               this(byteArrayOf(byte))
	constructor(short: Short):             this(Conversion.int16ToByteArray(short))
	constructor(int: Int):                 this(Conversion.int32ToByteArray(int))
	constructor(float: Float):             this(Conversion.floatToByteArray(float))

	val TAG = this.javaClass.simpleName
	var data: ByteArray = data
		protected set

	override fun getPacketSize(): Int {
		return data.size
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < ${getPacketSize()}")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.put(data)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		data = ByteArray(bb.remaining())
		bb.get(data)
		return true
	}

	override fun getArray(): ByteArray? {
		return data
	}

	override fun fromArray(array: ByteArray): Boolean {
		data = array
		return true
	}

	fun getPayload(): ByteArray {
		return data
	}

	override fun toString(): String {
		return "dataSize=${data.size} data=${Conversion.bytesToString(data)}"
	}
}
