/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.util

import rocks.crownstone.bluenet.structs.*
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

fun ByteBuffer.putUint32(value: Uint32) {
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

fun ByteBuffer.getUint64(): Uint64 {
	return Conversion.toUint64(this.getLong())
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


fun ByteBuffer.getInt8(): Int8 {
	return this.get()
}

fun ByteBuffer.getInt16(): Int16 {
	return this.getShort()
}

fun ByteBuffer.getInt32(): Int32 {
	return this.getInt()
}

fun ByteBuffer.getInt8(offset: Int): Int8 {
	return this.get(offset)
}

fun ByteBuffer.getInt16(offset: Int): Int16 {
	return this.getShort(offset)
}

fun ByteBuffer.getInt32(offset: Int): Int32 {
	return this.getInt(offset)
}


fun Boolean.toInt() = if (this) 1 else 0

fun Byte.toUint8() = this.toUByte()
fun Short.toUint8() = this.toUByte()
fun UShort.toUint8() = this.toUByte()
fun Int.toUint8() = this.toUByte()
fun UInt.toUint8() = this.toUByte()

fun UByte.toInt8() = this.toByte()
fun UShort.toInt8() = this.toByte()
fun UInt.toInt8() = this.toByte()


fun Byte.toUint16() = this.toUShort()
fun UByte.toUint16() = this.toUShort()
fun Short.toUint16() = this.toUShort()
fun Int.toUint16() = this.toUShort()
fun UInt.toUint16() = this.toUShort()
fun Long.toUint16() = this.toUShort()

fun UByte.toInt16() = this.toShort()
fun UShort.toInt16() = this.toShort()
fun UInt.toInt16() = this.toShort()


fun Byte.toUint32() = this.toUInt()
fun UByte.toUint32() = this.toUInt()
fun Short.toUint32() = this.toUInt()
fun UShort.toUint32() = this.toUInt()
fun Int.toUint32() = this.toUInt()
fun Double.toUint32() = this.toLong().toUInt()

fun UByte.toInt32() = this.toInt()
fun UShort.toInt32() = this.toInt()
fun UInt.toInt32() = this.toInt()

fun Long.toUint32() = this.toUInt()
//fun UByte.toDouble() = this.toShort().toDouble()
//fun UShort.toDouble() = this.toInt().toDouble()

//fun UInt.toDouble() = this.toLong().toDouble()
