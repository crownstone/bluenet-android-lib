/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers.v3

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.ConfigType
import rocks.crownstone.bluenet.structs.OpcodeType

open class ConfigPacket(type: ConfigType, data: ByteArray?, payload: PacketInterface?, opCode: OpcodeType = OpcodeType.READ): StreamPacket(type.num, data, payload, opCode) {
	constructor(): this(ConfigType.UNKNOWN, null, null)
	constructor(type: ConfigType, opCode: OpcodeType = OpcodeType.READ):  this(type, null, null, opCode)
	constructor(type: ConfigType, data: ByteArray, opCode: OpcodeType = OpcodeType.WRITE): this(type, data, null, opCode)
	constructor(type: ConfigType, payload: PacketInterface, opCode: OpcodeType = OpcodeType.WRITE): this(type, null, payload, opCode)
//	constructor(type: ConfigType, byte: Byte,               opCode: OpcodeType = OpcodeType.WRITE): this(type, byteArrayOf(byte), null, opCode)
//	constructor(type: ConfigType, short: Short,             opCode: OpcodeType = OpcodeType.WRITE): this(type, Conversion.int16ToByteArray(short), null, opCode)
//	constructor(type: ConfigType, int: Int,                 opCode: OpcodeType = OpcodeType.WRITE): this(type, Conversion.int32ToByteArray(int), null, opCode)
//	constructor(type: ConfigType, float: Float,             opCode: OpcodeType = OpcodeType.WRITE): this(type, Conversion.floatToByteArray(float), null, opCode)
}

//class ConfigPacketUint8 (type: ConfigType, value: Uint8,  opCode: OpcodeType = OpcodeType.WRITE): ConfigPacket(type, value.toByte(), opCode)
//class ConfigPacketUint16(type: ConfigType, value: Uint16, opCode: OpcodeType = OpcodeType.WRITE): ConfigPacket(type, value.toShort(), opCode)
//class ConfigPacketUint32(type: ConfigType, value: Uint32, opCode: OpcodeType = OpcodeType.WRITE): ConfigPacket(type, value.toInt(), opCode)