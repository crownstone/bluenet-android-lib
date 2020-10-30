package rocks.crownstone.bluenet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rocks.crownstone.bluenet.encryption.RC5
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.toUint16

class RC5Test {
	private val TAG = this.javaClass.simpleName

	@Test
	fun testEncryption() {
		val keyString = "localizationKeyX"
		val key = Conversion.getKeyFromString(keyString)
		assertFalse(key == null)
		if (key == null) {
			return
		}
		println("key: $keyString = ${Conversion.bytesToString(key)}")
		assertTrue(key contentEquals byteArrayOf(108, 111, 99, 97, 108, 105, 122, 97, 116, 105, 111, 110, 75, 101, 121, 88))

		val expandedKey = RC5.expandKey(key)
		assertFalse(expandedKey == null)
		if (expandedKey == null) {
			return
		}
		for (i in 0 until expandedKey.size) {
			println("expandedKey $i: ${expandedKey[i]}")
		}
		val expandedKeyIntList = arrayListOf(17188, 48365, 24340, 1800, 42461, 5147, 20677, 25885, 6492, 7594, 37111, 45723, 17422, 17628, 2782, 13305, 17628, 42281, 16228, 35216, 16588, 9898, 26128, 2533, 52784, 17181)
		val expandedUint16List = ArrayList<Uint16>()
		for (i in 0 until expandedKeyIntList.size) {
			expandedUint16List.add(expandedKeyIntList[i].toUint16())
		}
		assertTrue(expandedKey == expandedUint16List)

		val data = Conversion.toUint32(123456789)
		val dataList = Conversion.uint32ToUint16ListReversed(data)
		println("data: $data = $dataList")
		assertTrue(dataList == arrayListOf(1883.toUint16(), 52501.toUint16()))

		val encrypted = RC5.encrypt(dataList, expandedKey)
		assertFalse(encrypted == null)
		if (encrypted == null) {
			return
		}
		println("encrypted: ${Conversion.uint16ListToUint32Reversed(encrypted)} = $encrypted")
		assertTrue(encrypted == arrayListOf(54517.toUint16(), 58512.toUint16()))

		val decryptedList = RC5.decrypt(encrypted, expandedKey)
		assertFalse(decryptedList == null)
		if (decryptedList == null) {
			return
		}
		val decrypted = Conversion.uint16ListToUint32Reversed(decryptedList)
		println("decrypted: $decrypted = $decryptedList")
		assertTrue(decryptedList == arrayListOf(1883.toUint16(), 52501.toUint16()))

		assertTrue(decrypted == data)
	}



	@Test
	fun testEncryption2() {
		val key = byteArrayOf(208.toByte(), 211.toByte(), 199.toByte(), 197.toByte(), 208.toByte(), 205.toByte(), 222.toByte(), 197.toByte(), 216.toByte(), 205.toByte(), 211.toByte(), 210.toByte(), 175.toByte(), 201.toByte(), 221.toByte(), 188.toByte())
		println("key: ${Conversion.bytesToString(key)}")

		val expandedKey = RC5.expandKey(key)
		assertFalse(expandedKey == null)
		if (expandedKey == null) {
			return
		}
		for (i in 0 until expandedKey.size) {
			println("expandedKey $i: ${expandedKey[i]}")
		}
		val expandedKeyIntList = arrayListOf(63394, 22665, 9267, 62171, 47672, 50449, 41322, 41686, 39063, 43392, 10913, 14132, 3935, 57443, 22653, 22250, 9717, 32252, 43610, 38221, 30911, 33677, 57943, 3511, 29883, 17815)
		val expandedUint16List = ArrayList<Uint16>()
		for (i in 0 until expandedKeyIntList.size) {
			expandedUint16List.add(expandedKeyIntList[i].toUint16())
		}
		assertTrue(expandedKey == expandedUint16List)

		val data = Conversion.toUint32(123456789)
		val dataList = Conversion.uint32ToUint16ListReversed(data)
		println("data: $data = $dataList")
		assertTrue(dataList == arrayListOf(1883.toUint16(), 52501.toUint16()))

		val encrypted = RC5.encrypt(dataList, expandedKey)
		assertFalse(encrypted == null)
		if (encrypted == null) {
			return
		}
		println("encrypted: ${Conversion.uint16ListToUint32Reversed(encrypted)} = $encrypted")
		assertTrue(encrypted == arrayListOf(61699.toUint16(), 17598.toUint16()))

		val decryptedList = RC5.decrypt(encrypted, expandedKey)
		assertFalse(decryptedList == null)
		if (decryptedList == null) {
			return
		}
		val decrypted = Conversion.uint16ListToUint32Reversed(decryptedList)
		println("decrypted: $decrypted = $decryptedList")
		assertTrue(decryptedList == arrayListOf(1883.toUint16(), 52501.toUint16()))

		assertTrue(decrypted == data)
	}
}