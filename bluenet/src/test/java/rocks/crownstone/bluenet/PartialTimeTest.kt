package rocks.crownstone.bluenet

import org.junit.Assert.assertTrue
import org.junit.Test
import rocks.crownstone.bluenet.util.PartialTime
import rocks.crownstone.bluenet.util.toUint16
import rocks.crownstone.bluenet.util.toUint32
import java.util.*

class PartialTimeTest {
	@Test
	fun test() {
		val pt = PartialTime
		val timestamp: Long = 1515426625
		val lsbTimestamp = (timestamp % (0xFFFF + 1))
		val reconstructedTs = pt.reconstructTimestamp(timestamp, lsbTimestamp.toUint16())
		assertTrue(timestamp == reconstructedTs)
	}

	@Test
	fun test2() {
		val pt = PartialTime
		val timestamp: Long = 1516206008
		val lsbTimestamp = 34186
		val reconstructedTs = pt.reconstructTimestamp(timestamp, lsbTimestamp.toUint16())
		assertTrue(reconstructedTs == 1516209546L)
	}

	@Test
	fun testOverflow1() {
		val pt = PartialTime
		val timestamp = (0x5A53FFFF + 1500).toLong()
		val lsbTimestamp = 0xFFFF
		val reconstructedTs = pt.reconstructTimestamp(timestamp, lsbTimestamp.toUint16())
		assertTrue(timestamp == reconstructedTs + 1500)
	}

	@Test
	fun testOverflow2() {
		val pt = PartialTime
		val timestamp: Long = 0x5A53FFFF
		val lsbTimestamp = 0
		val reconstructedTs = pt.reconstructTimestamp(timestamp, lsbTimestamp.toUint16())
		assertTrue(timestamp == reconstructedTs - 1)
	}

	@Test
	fun testOverflow3() {
		val pt = PartialTime
		val timestamp = (0x5A530000 - 1).toLong()
		val lsbTimestamp = 0
		val reconstructedTs = pt.reconstructTimestamp(timestamp, lsbTimestamp.toUint16())
		assertTrue(timestamp == reconstructedTs - 1)
	}

	@Test
	fun testOverflow4() {
		val pt = PartialTime
		val secondsFromGmt = (Calendar.getInstance().timeZone.rawOffset / 1000).toLong()
		val timestamp = 0x5A537FFF - 6 - secondsFromGmt
		val lsbTimestamp = 0x7FFF
		val reconstructedTs = pt.reconstructTimestamp(timestamp, lsbTimestamp.toUint16())
		assertTrue(timestamp == reconstructedTs - 6 - secondsFromGmt)
	}

	@Test
	fun testOverflow5() {
		val pt = PartialTime
		val timestamp: Long = 0x5A537FFF
		val lsbTimestamp = 0x7FFF + 1
		val reconstructedTs = pt.reconstructTimestamp(timestamp, lsbTimestamp.toUint16())
		assertTrue(timestamp == reconstructedTs - 1)
	}
}