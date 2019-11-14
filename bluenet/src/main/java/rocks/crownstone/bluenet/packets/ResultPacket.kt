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

open class ResultPacket(type: ControlV2Type, resultCode: ResultType, payload: PacketInterface?): PayloadWrapperPacket(payload) {
	constructor():                                                                      this(ControlV2Type.UNKNOWN, ResultType.UNKNOWN, null)
	constructor(type: ControlV2Type, resultCode: ResultType):                           this(type, resultCode, null)
	constructor(type: ControlV2Type, resultCode: ResultType, data: ByteArray):          this(type, resultCode, ByteArrayPacket(data))
	constructor(type: ControlV2Type, resultCode: ResultType, byte: Byte):               this(type, resultCode, byteArrayOf(byte))
	constructor(type: ControlV2Type, resultCode: ResultType, short: Short):             this(type, resultCode, Conversion.int16ToByteArray(short))
	constructor(type: ControlV2Type, resultCode: ResultType, int: Int):                 this(type, resultCode, Conversion.int32ToByteArray(int))
	constructor(type: ControlV2Type, resultCode: ResultType, float: Float):             this(type, resultCode, Conversion.floatToByteArray(float))

	override val TAG = this.javaClass.simpleName
	companion object {
		const val SIZE = 2+2+2
	}
	var type: Uint16 = type.num
		protected set
	var resultCode: Uint16 = resultCode.num
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
		bb.putShort(resultCode)
		bb.putShort(dataSize)
		return true
	}

	override fun headerFromBuffer(bb: ByteBuffer): Boolean {
		type = bb.getUint16()
		resultCode = bb.getUint16()
		dataSize = bb.getUint16()
		return true
	}

	override fun toString(): String {
		return "type=$type result=$resultCode dataSize=$dataSize data=${Conversion.bytesToString(getPayload())}"
	}
}
