/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class ControlPacketV2(type: ControlV2Type, payload: PacketInterface?): PayloadWrapperPacket(payload) {
	constructor():                                         this(ControlV2Type.UNKNOWN, null)
	constructor(type: ControlV2Type):                      this(type, null)
	constructor(type: ControlV2Type, data: ByteArray):     this(type, ByteArrayPacket(data))
	constructor(type: ControlV2Type, byte: Byte):          this(type, byteArrayOf(byte))
	constructor(type: ControlV2Type, short: Short):        this(type, Conversion.int16ToByteArray(short))
	constructor(type: ControlV2Type, int: Int):            this(type, Conversion.int32ToByteArray(int))
	constructor(type: ControlV2Type, float: Float):        this(type, Conversion.floatToByteArray(float))

	override val TAG = this.javaClass.simpleName
	companion object {
		const val SIZE = 2+2
	}
	var type: Uint16 = type.num
		protected set
	protected var dataSize: Uint16 = 0
	init {
		dataSize = payload?.getPacketSize() ?: 0
	}

	override fun getHeaderSize(): Int {
		return SIZE
	}

	override fun getPayloadSize(): Int? {
		return dataSize
	}

	override fun headerToBuffer(bb: ByteBuffer): Boolean {
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putShort(type)
		bb.putShort(dataSize)
		return true
	}

	override fun headerFromBuffer(bb: ByteBuffer): Boolean {
		type = bb.getUint16()
		dataSize = bb.getUint16()
		return true
	}

	override fun toString(): String {
		return "type=$type dataSize=$dataSize data=${Conversion.bytesToString(getPayload())}"
	}
}
