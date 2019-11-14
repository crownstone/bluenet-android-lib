/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers.v3

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.OpcodeType
import rocks.crownstone.bluenet.structs.StateType

open class StatePacket(type: StateType, data: ByteArray?, payload: PacketInterface?): StreamPacket(type.num, data, payload, OpcodeType.READ) {
	constructor(): this(StateType.UNKNOWN, null, null)
	constructor(type: StateType):                           this(type, null, null)
	constructor(type: StateType, data: ByteArray):          this(type, data, null)
	constructor(type: StateType, payload: PacketInterface): this(type, null, payload)
//	constructor(type: StateType, byte: Byte):               this(type, byteArrayOf(byte), null)
//	constructor(type: StateType, short: Short):             this(type, Conversion.int16ToByteArray(short), null)
//	constructor(type: StateType, int: Int):                 this(type, Conversion.int32ToByteArray(int), null)
//	constructor(type: StateType, float: Float):             this(type, Conversion.floatToByteArray(float), null)
}

//class StatePacketUint8 (type: StateType, value: Uint8):  StatePacket(type, value.toByte())
//class StatePacketUint16(type: StateType, value: Uint16): StatePacket(type, value.toShort())
//class StatePacketUint32(type: StateType, value: Uint32): StatePacket(type, value.toInt())