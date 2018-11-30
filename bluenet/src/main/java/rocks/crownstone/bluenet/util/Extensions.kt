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

fun ByteBuffer.getUint8(): Uint8 {
	return Conversion.toUint8(this.get())
}

fun ByteBuffer.getUint16(): Uint16 {
	return Conversion.toUint16(this.getShort())
}

fun ByteBuffer.getUint32(): Uint32 {
	return Conversion.toUint32(this.getInt())
}
