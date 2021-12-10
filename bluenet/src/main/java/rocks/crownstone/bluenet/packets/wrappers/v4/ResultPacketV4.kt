/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers.v4

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.wrappers.PayloadWrapperPacket
import rocks.crownstone.bluenet.packets.wrappers.ResultPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class ResultPacketV4(type: ControlTypeV4, resultCode: ResultType, payload: PacketInterface?): PayloadWrapperPacket(payload), ResultPacket {
	constructor(): this(ControlTypeV4.UNKNOWN, ResultType.UNKNOWN, null)

	override val TAG = this.javaClass.simpleName
	companion object {
		const val SIZE = 2+2+2
	}
	var type = type
		protected set
	var resultCode = resultCode
		protected set

	override fun getCode(): ResultType {
		return resultCode
	}

	protected var dataSize: Uint16 = 0U
	init {
		dataSize = payload?.getPacketSize()?.toUint16() ?: 0U
	}

	override fun getHeaderSize(): Int {
		return SIZE
	}

	override fun getPayloadSize(): Int? {
		return dataSize.toInt()
	}

	override fun headerToBuffer(bb: ByteBuffer): Boolean {
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putShort(type.num)
		bb.putShort(resultCode.num)
		bb.putShort(dataSize)
		return true
	}

	override fun headerFromBuffer(bb: ByteBuffer): Boolean {
		type = ControlTypeV4.fromNum(bb.getUint16())
		resultCode = ResultType.fromNum(bb.getUint16())
		dataSize = bb.getUint16()
		Log.i(TAG, "type=$type resultCode=$resultCode dataSize=$dataSize")
		return true
	}

	override fun toString(): String {
		return "ResultPacketV4(type=$type, resultCode=$resultCode, dataSize=$dataSize, data=${Conversion.bytesToString(getPayload())})"
	}
}
