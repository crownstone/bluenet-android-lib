package rocks.crownstone.bluenet.util

import kotlin.experimental.and

object Util {
	// Check if Nth bit in a value is set
	fun isBitSet(value: Long, bit: Int): Boolean {
		return value and (1L shl bit) != 0L
	}

	fun isBitSet(value: Int, bit: Int): Boolean {
		return value and (1 shl bit) != 0
	}

	fun isBitSet(value: Byte, bit: Int): Boolean {
		return isBitSet(value.toInt(), bit)
	}

	// Clear the Nth bit in a value
	fun clearBit(value: Int, bit: Int): Int {
		return value and (1 shl bit).inv()
	}
}