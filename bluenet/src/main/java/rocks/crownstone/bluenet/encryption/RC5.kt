/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jul 9, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.encryption

import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.structs.BluenetProtocol.RC5_KEYLEN
import rocks.crownstone.bluenet.structs.BluenetProtocol.RC5_NUM_SUBKEYS
import rocks.crownstone.bluenet.structs.BluenetProtocol.RC5_ROUNDS
import rocks.crownstone.bluenet.structs.BluenetProtocol.RC5_WORD_SIZE
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.toUint16
import rocks.crownstone.bluenet.util.toUint8

/**
 * Static class to perform RC5 encryption.
 *
 * Assumes 16 bit word size, so 32 bit data.
 *
 * See https://en.wikipedia.org/wiki/RC5
 */
object RC5 {
	private val TAG = this.javaClass.simpleName

	// Magic numbers for RC5 with 16 bit words.
	const val RC5_16BIT_P: Uint16 = 0xB7E1U
	const val RC5_16BIT_Q: Uint16 = 0x9E37U

	fun expandKey(key: ByteArray?): List<Uint16>? {
		if (key == null || key.size != RC5_KEYLEN) {
			return null
		}

		// c - The length of the key in words (or 1, if keyLength = 0).
		val keyLenWords = ((RC5_KEYLEN - 1) / RC5_WORD_SIZE) + 1
		val loops = when (RC5_NUM_SUBKEYS > keyLenWords) {
			true ->  3 * RC5_NUM_SUBKEYS
			false -> 3 * keyLenWords
		}

		// L[] - A temporary working array used during key scheduling. initialized to the key in words.
		val L = ArrayList<Uint16>(keyLenWords)
		for (i in 0 until keyLenWords) {
			L.add(0U)
		}
		for (i in 0 until keyLenWords) {
			L[i] = ((key[2*i+1].toUint8().toUInt() shl 8) + key[2*i].toUint8().toUInt()).toUint16()
//			Log.i(TAG, "${(key[2*i+1].toUint8().toUInt() shl 8)} + ${key[2*i].toUint8().toUInt()}: uint=${((key[2*i+1].toUint8().toUInt() shl 8) + key[2*i].toUint8().toUInt())} uint16=${((key[2*i+1].toUint8().toUInt() shl 8) + key[2*i].toUint8().toUInt()).toUint16()}")
//			Log.i(TAG, "RC5 L[i]=${L[i]}: (${Conversion.toUint8(key[2*i+1]).toInt()} << 8 = ${(Conversion.toUint8(key[2*i+1]).toInt() shl 8)}) + ${Conversion.toUint8(key[2*i])} = ${(Conversion.toUint8(key[2*i+1]).toInt() shl 8) + Conversion.toUint8(key[2*i]).toInt()}")
		}

		val subKeys = ArrayList<Uint16>(RC5_NUM_SUBKEYS)
		subKeys.add(RC5_16BIT_P)
		for (i in 1 until RC5_NUM_SUBKEYS) {
			subKeys.add(Conversion.toUint16(subKeys[i-1] + RC5_16BIT_Q))
		}

		var i: Int = 0
		var j: Int = 0
		var a: Uint16 = 0U
		var b: Uint16 = 0U
		for (k in 0 until loops) {
//			Log.i(TAG, "RC5 i=$i j=$j a=$a b=$b L[j]=${L[j]} subKeys[i]]${subKeys[i]}")
			a = rotateLeft((subKeys[i] + a + b).toUint16(), 3)
			subKeys[i] = a
			b = rotateLeft((L[j] + a + b).toUint16(), (a+b).toInt() % 16)
			L[j] = b
//			Log.i(TAG, "  RC5 i=$i j=$j a=$a b=$b L[j]=${L[j]} subKeys[i]]${subKeys[i]}")
			i = (i+1) % RC5_NUM_SUBKEYS
			j = (j+1) % keyLenWords
		}
//		Log.i(TAG, "RC5 16B key=${Conversion.bytesToString(key)}")
		for (i in 0 until RC5_NUM_SUBKEYS) {
//			Log.i(TAG, "RC5 key[$i] = ${subKeys[i]}")
		}
		return subKeys
	}

	fun encrypt(data: List<Uint16>, expandedKey: List<Uint16>): List<Uint16>? {
		if (data.size != 2) {
			return null
		}
		var a = Conversion.toUint16(data[0] + expandedKey[0])
		var b = Conversion.toUint16(data[1] + expandedKey[1])
		for (i in 1..RC5_ROUNDS) {
			a = Conversion.toUint16(rotateLeft(a xor b, b.toInt() % 16) + expandedKey[2*i])
			b = Conversion.toUint16(rotateLeft(b xor a, a.toInt() % 16) + expandedKey[2*i + 1])
		}
		val encrypted = ArrayList<Uint16>(2)
		encrypted.add(a)
		encrypted.add(b)
		return encrypted
	}

	fun decrypt(data: List<Uint16>, expandedKey: List<Uint16>): List<Uint16>? {
		if (data.size != 2) {
			return null
		}
		var a = data[0]
		var b = data[1]
		for (i in RC5_ROUNDS downTo 1) {
			b = rotateRight((b - expandedKey[2*i + 1]).toUint16(), a.toInt() % 16) xor a
			a = rotateRight((a - expandedKey[2*i]).toUint16()    , b.toInt() % 16)
		}
		val decrypted = ArrayList<Uint16>(2)
		decrypted.add(Conversion.toUint16(a - expandedKey[0]))
		decrypted.add(Conversion.toUint16(b - expandedKey[1]))
		return decrypted
	}

	fun encrypt(data: Uint32, preparedKey: List<Uint16>): Uint32? {
		val result = encrypt(Conversion.uint32ToUint16ListReversed(data), preparedKey) ?: return null
		return Conversion.uint16ListToUint32Reversed(result)
	}

	fun decrypt(data: Uint32, preparedKey: List<Uint16>): Uint32? {
		val result = decrypt(Conversion.uint32ToUint16ListReversed(data), preparedKey) ?: return null
		return Conversion.uint16ListToUint32Reversed(result)
	}

	private fun rotateLeft(x: Uint16, shift: Int): Uint16 {
		val u = x.toUInt()
		return Conversion.toUint16((u shl shift) or (u shr (16 - shift)))
	}

	private fun rotateRight(x: Uint16, shift: Int): Uint16 {
		val u = x.toUInt()
		return Conversion.toUint16((u shr shift) or (u shl (16 - shift)))
	}
}