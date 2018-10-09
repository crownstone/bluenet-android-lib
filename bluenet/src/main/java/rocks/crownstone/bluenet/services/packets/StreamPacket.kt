package rocks.crownstone.bluenet.services.packets

import rocks.crownstone.bluenet.BluenetProtocol
import rocks.crownstone.bluenet.util.Conversion
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class BasePacket(type: Byte, dataSize: Int, opCode: Byte = BluenetProtocol.OPCODE_WRITE) {
	val TAG = this.javaClass.simpleName
	companion object {
		const val SIZE = 1+1+2
	}
//	protected var type: Byte = -1
//	protected var opCode: Byte  = -1 // Not always used
//	protected var dataSize = 0
	protected var type: Byte = type
	protected var opCode: Byte  = opCode // Not always used
	protected var dataSize: Int = dataSize


//	protected var data: BasePacket? = null

//	constructor(type: Byte, data: BasePacket, opCode: Byte = BluenetProtocol.OPCODE_WRITE): this() {
//		this.type = type
//		this.opCode = opCode
//		this.dataSize = data.getSize()
//		this.data = data
//	}

//	constructor(type: Byte, dataSize: Int, opCode: Byte = BluenetProtocol.OPCODE_WRITE): this() {
//	protected open fun init(type: Byte, dataSize: Int, opCode: Byte = BluenetProtocol.OPCODE_WRITE) {
//		this.type = type
//		this.opCode = opCode
//		this.dataSize = dataSize
//	}

	fun setOpcode(opCode: Byte) {
		this.opCode = opCode
	}

	constructor(): this(-1, -1)

	open fun getSize(): Int {
//		return HEADER_SIZE + dataSize
		return SIZE
	}

	open fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < SIZE) {
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.put(type)
		bb.put(opCode)
		bb.putShort(dataSize.toShort())
		return true
	}

	open fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < SIZE) {
			return false
		}
		type = bb.get()
		opCode = bb.get()
		dataSize = Conversion.toUint16(bb.getShort())
		// TODO: also fill up and parse the payload
		return true
	}

	fun getArray(): ByteArray {
//		val data = data
//		if (data == null) {
//			return ByteArray(0)
//		}
		val arr = ByteArray(getSize())
		val bb = ByteBuffer.wrap(arr)
		val result = toBuffer(bb)
		if (!result) {
			return ByteArray(0)
			// Or just return null?
		}
		return bb.array()
	}
}