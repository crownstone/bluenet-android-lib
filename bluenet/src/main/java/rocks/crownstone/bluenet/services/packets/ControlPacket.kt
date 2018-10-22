package rocks.crownstone.bluenet.services.packets

import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.util.Conversion

//typealias ControlPacket = StreamPacket
open class ControlPacket(type: ControlType, data: ByteArray?, payload: PacketInterface?): StreamPacket(type.num, data, payload, OpcodeType.WRITE) {


	constructor(): this(ControlType.UNKNOWN, null, null)
	constructor(type: ControlType): this(type, null, null)
	constructor(type: ControlType, data: ByteArray): this(type, data, null)
	constructor(type: ControlType, payload: PacketInterface): this(type, null, payload)
	constructor(type: ControlType, byte: Byte):               this(type, byteArrayOf(byte), null)
	constructor(type: ControlType, short: Short):             this(type, Conversion.int16ToByteArray(short), null)
	constructor(type: ControlType, int: Int):                 this(type, Conversion.int32ToByteArray(int), null)
	constructor(type: ControlType, float: Float):             this(type, Conversion.floatToByteArray(float), null)
}

class ControlPacketUint8(type: ControlType, value: Uint8): ControlPacket(type, value.toByte())
class ControlPacketUint16(type: ControlType, value: Uint16): ControlPacket(type, value.toShort())
class ControlPacketUint32(type: ControlType, value: Uint32): ControlPacket(type, value.toInt())