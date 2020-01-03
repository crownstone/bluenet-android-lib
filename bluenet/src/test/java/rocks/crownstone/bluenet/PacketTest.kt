package rocks.crownstone.bluenet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rocks.crownstone.bluenet.packets.wrappers.v3.StatePacket
import rocks.crownstone.bluenet.packets.schedule.ScheduleListPacket
import rocks.crownstone.bluenet.structs.StateType
import rocks.crownstone.bluenet.util.Conversion

class PacketTest {
	@Test
	fun testScheduleList() {
		//	Schedule list:
		//		0 repeatType=0 actionType=0 override=0 timestamp=0 repeat=every 0 minutes action=switchVal: 0
		//		1 repeatType=0 actionType=0 override=0 timestamp=0 repeat=every 0 minutes action=switchVal: 0
		//		2 repeatType=0 actionType=0 override=0 timestamp=0 repeat=every 0 minutes action=switchVal: 0
		//		3 repeatType=1 actionType=0 override=1 timestamp=1540995487 repeat=weekdays mask: 01110000 action=switchVal: 99
		//		4 repeatType=0 actionType=0 override=0 timestamp=0 repeat=every 0 minutes action=switchVal: 0
		//		5 repeatType=0 actionType=2 override=0 timestamp=1540995487 repeat=every 60 minutes action=toggle
		//		6 repeatType=0 actionType=0 override=0 timestamp=0 repeat=every 0 minutes action=switchVal: 0
		//		7 repeatType=1 actionType=1 override=127 timestamp=1540995487 repeat=weekdays mask: 11111111 action=switchVal: 50 duration:15
		//		8 repeatType=0 actionType=0 override=0 timestamp=0 repeat=every 0 minutes action=switchVal: 0
		//		9 repeatType=0 actionType=0 override=0 timestamp=0 repeat=every 0 minutes action=switchVal: 0
		val intArray = intArrayOf(133, 0, 121, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 159, 185, 217, 91, 112, 0, 99, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 159, 185, 217, 91, 60, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 127, 159, 185, 217, 91, 255, 0, 50, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
		val arr = ByteArray(intArray.size)
		for (i in 0 until intArray.size) {
			arr[i] = Conversion.toUint8(intArray[i]).toByte()
		}
		val schedulePacket = ScheduleListPacket()
		val statePacket = StatePacket(StateType.SCHEDULE, schedulePacket)
		assertTrue(statePacket.fromArray(arr))
		assertTrue(statePacket.type == StateType.SCHEDULE.num)
		assertFalse(schedulePacket.list[0].isActive())
		assertFalse(schedulePacket.list[1].isActive())
		assertFalse(schedulePacket.list[2].isActive())
		assertFalse(schedulePacket.list[4].isActive())
		assertFalse(schedulePacket.list[6].isActive())
		assertFalse(schedulePacket.list[8].isActive())
		assertFalse(schedulePacket.list[9].isActive())

		assertTrue(schedulePacket.list[3].isActive())
		assertTrue(schedulePacket.list[3].repeatType.num == 1)
		assertTrue(schedulePacket.list[3].overrideMask.bitmask == 1.toUByte())
		assertTrue(schedulePacket.list[3].timestamp == 1540995487U)
		assertTrue(schedulePacket.list[3].dayOfWeekMask.bitmask == 112.toUByte())
		assertTrue(schedulePacket.list[3].switchVal == 99.toUByte())

		assertTrue(schedulePacket.list[5].isActive())
		assertTrue(schedulePacket.list[5].repeatType.num == 0)
		assertTrue(schedulePacket.list[5].actionType.num == 2)
		assertTrue(schedulePacket.list[5].timestamp == 1540995487U)
		assertTrue(schedulePacket.list[5].minutes == 60.toUShort())

		assertTrue(schedulePacket.list[7].isActive())
		assertTrue(schedulePacket.list[7].repeatType.num == 1)
		assertTrue(schedulePacket.list[7].actionType.num == 1)
		assertTrue(schedulePacket.list[7].overrideMask.bitmask == 127.toUByte())
		assertTrue(schedulePacket.list[7].timestamp == 1540995487U)
		assertTrue(schedulePacket.list[7].dayOfWeekMask.bitmask == 255.toUByte())
		assertTrue(schedulePacket.list[7].switchVal == 50.toUByte())
		assertTrue(schedulePacket.list[7].fadeDuration == 15.toUShort())
	}
}