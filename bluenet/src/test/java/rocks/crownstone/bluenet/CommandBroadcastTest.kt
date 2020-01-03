package rocks.crownstone.bluenet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rocks.crownstone.bluenet.broadcast.BroadcastPacketBuilder
import rocks.crownstone.bluenet.broadcast.CommandBroadcastItem
import rocks.crownstone.bluenet.broadcast.CommandBroadcastItemType
import rocks.crownstone.bluenet.broadcast.CommandBroadcastQueue
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.encryption.MeshKeySet
import rocks.crownstone.bluenet.packets.broadcast.BroadcastSwitchItemPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import java.util.*

class CommandBroadcastTest {
	val eventBus: EventBus
	val sphereState: SphereStateMap
	val encryptionManager: EncryptionManager
	val commandBroadcastQueue: CommandBroadcastQueue
	val broadcastPacketBuilder: BroadcastPacketBuilder
	val sphereId = "mySphere"

	init {
		System.setProperty("runningLocalUnitTest", "true")
		eventBus = EventBus()
		sphereState = SphereStateMap()
		val bluenetState = BluenetState(sphereState, null)

		encryptionManager = EncryptionManager(eventBus, bluenetState)
		commandBroadcastQueue = CommandBroadcastQueue()
		broadcastPacketBuilder = BroadcastPacketBuilder(bluenetState, encryptionManager)
		val keySet = KeySet(
				"adminKeyForCrown",
				"memberKeyForHome",
				"guestKeyForOther",
				"myServiceDataKey",
				"localizationKeyX"
		)
		println("$keySet")
		val meshKeySet = MeshKeySet(
				null,
				null,
				"meshKeyForStones"
		)
		val sphereShortId: Uint8 = 234U
		val commandCount: Uint8 = 253U
		val locationId: Uint8 = 60U
		val profileId: Uint8 = 6U
		val t2tEnabled = true
		val rssiOffset = -10
		val deviceToken: Uint8 = 254U
		val sphereSettings = SphereSettings(keySet, meshKeySet, UUID.randomUUID(), sphereShortId, deviceToken)
		sphereState[sphereId] = SphereState(sphereSettings, commandCount, locationId, profileId, t2tEnabled, rssiOffset)
		println("sphereState: $sphereState")
		eventBus.emit(BluenetEvent.SPHERE_SETTINGS_UPDATED)
	}

	@Test
	fun testBackgroundPayload() {
		val payload = broadcastPacketBuilder.getBackgroundPacket(sphereId)
		assertFalse(payload == null)
		if (payload == null) {
			return
		}
//		assertTrue(payload contentEquals byteArrayOf(45, -96, 5, 41))
	}

	@Test
	fun testHeader() {
		val keyPair = sphereState[sphereId]!!.settings.keySet.getHighestKey()
		val header = broadcastPacketBuilder.getCommandBroadcastHeader(sphereId, keyPair!!.accessLevel)
		assertFalse(header == null)
		if (header == null) {
			return
		}
		assertTrue(header contentEquals byteArrayOf(80, 7, 2, 64, 22, 164.toByte(), 45, 224.toByte()))
	}

//	@Test
//	fun testQueue() {
//		for (i in 1..7) {
//			addSwitchItem(i, 100)
//		}
//
//		for (i in 1..10) {
//			if (commandBroadcastQueue.hasNext()) {
//				val advertiseUuids = broadcastPacketBuilder.getCommandBroadcastServiceUuids()
//				println("advertiseUUIDs:")
//				if (advertiseUuids != null) {
//					for (uuid in advertiseUuids) {
//						println("   $uuid")
//					}
//				}
//				else {
//					println("   empty")
//				}
//
//				if (i == 3) {
//					// Add new command for stone id 7, while it's being advertised.
//					addSwitchItem(7, 100)
//					// Add new command for stone id 6, while it's in queue.
//					addSwitchItem(6, 100)
//				}
//
//				commandBroadcastQueue.advertisementDone(250, null)
//			}
//		}
//
//		assertTrue(true)
//	}

	private fun addSwitchItem(stoneId: Int, switchValue: Int) {
		val switchItem = BroadcastSwitchItemPacket(Conversion.toUint8(stoneId), Conversion.toUint8(switchValue))
		val item = CommandBroadcastItem(
				null,
				sphereId,
				CommandBroadcastItemType.SWITCH,
				Conversion.toUint8(stoneId),
				switchItem,
				3
		)
		commandBroadcastQueue.add(item)
	}

}
