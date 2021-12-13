/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers.v3

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.wrappers.ResultPacket
import rocks.crownstone.bluenet.structs.ControlType
import rocks.crownstone.bluenet.structs.OpcodeType
import rocks.crownstone.bluenet.structs.ResultType
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.getUint16
import rocks.crownstone.bluenet.util.getUint32
import rocks.crownstone.bluenet.util.putUint16
import java.nio.ByteBuffer

class ResultTypePacket: PacketInterface {
	val TAG = this.javaClass.simpleName
	var resultCode = ResultType.UNKNOWN
		private set

	override fun getPacketSize(): Int {
		return 2
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < ${getPacketSize()}")
			return false
		}
		bb.putUint16(resultCode.num)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		resultCode = ResultType.fromNum(bb.getUint16())
//		return resultCode != ResultType.UNKNOWN
		return true
	}
}

open class ResultPacketV3(type: ControlType, resultCode: ResultType): StreamPacket(type.num, null, ResultTypePacket(), OpcodeType.RESULT), ResultPacket {
	constructor(): this(ControlType.UNKNOWN, ResultType.UNKNOWN)

	var resultCode = resultCode
		protected set

	override fun getCode(): ResultType {
		return resultCode
	}

	override fun getPacketSize(): Int {
		return super.getPacketSize() + 2
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (!super.fromBuffer(bb)) {
			return false
		}
		if (opCode != OpcodeType.RESULT) {
			return false
		}

		val payload = super.payload
		if (payload != null && payload is ResultTypePacket) {
			resultCode = payload.resultCode
		}
		return resultCode != ResultType.UNKNOWN
	}
}