package rocks.crownstone.bluenet.services.packets

import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.util.Conversion
import java.nio.ByteBuffer

open class CommandResultPacket(type: ControlType, resultCode: ResultType): StreamPacket(type.num, null, null, OpcodeType.RESULT) {
	constructor(): this(ControlType.UNKNOWN, ResultType.UNKNOWN)

	var resultCode = resultCode
		private set

	override fun getSize(): Int {
		return super.getSize() + 2
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (!super.fromBuffer(bb)) {
			return false
		}
		if (opCode != OpcodeType.RESULT) {
			return false
		}
		resultCode = ResultType.fromNum(Conversion.toUint16(bb.getShort()))
		return resultCode != ResultType.UNKNOWN
	}
}