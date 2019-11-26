/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.util

import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import java.nio.ByteBuffer

fun ByteBuffer.put(value: Uint8) {
	this.put(value.toByte())
}

fun ByteBuffer.putShort(value: Uint16) {
	this.putShort(value.toShort())
}

fun ByteBuffer.putInt(value: Uint32) {
	this.putInt(value.toInt())
}

fun ByteBuffer.putUint8(value: Uint8) {
	this.put(value.toByte())
}

fun ByteBuffer.putUint16(value: Uint16) {
	this.putShort(value.toShort())
}

fun ByteBuffer.putUInt32(value: Uint32) {
	this.putInt(value.toInt())
}

fun ByteBuffer.getUint8(): Uint8 {
	return Conversion.toUint8(this.get())
}

fun ByteBuffer.getUint16(): Uint16 {
	return Conversion.toUint16(this.getShort())
}

fun ByteBuffer.getUint32(): Uint32 {
	return Conversion.toUint32(this.getInt())
}

fun ByteBuffer.getUint8(offset: Int): Uint8 {
	return Conversion.toUint8(this.get(offset))
}

fun ByteBuffer.getUint16(offset: Int): Uint16 {
	return Conversion.toUint16(this.getShort(offset))
}

fun ByteBuffer.getUint32(offset: Int): Uint32 {
	return Conversion.toUint32(this.getInt(offset))
}

fun Boolean.toInt() = if (this) 1 else 0
