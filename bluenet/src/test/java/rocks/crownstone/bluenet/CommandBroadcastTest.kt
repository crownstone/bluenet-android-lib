package rocks.crownstone.bluenet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
	val sphereId = "mySphere"

	init {
		System.setProperty("runningLocalUnitTest", "true")
		eventBus = EventBus()
		sphereState = SphereStateMap()
		encryptionManager = EncryptionManager(eventBus, sphereState)
		commandBroadcastQueue = CommandBroadcastQueue(sphereState, encryptionManager)
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
		val sphereShortId: Uint8 = 234
		val locationId: Uint8 = 60
		val profileId: Uint8 = 6
		val t2tEnabled = true
		val rssiOffset = -10
		val sphereSettings = SphereSettings(keySet, meshKeySet, UUID.randomUUID(), sphereShortId)
		sphereState[sphereId] = SphereState(sphereSettings, locationId, profileId, t2tEnabled, rssiOffset)
		println("sphereState: $sphereState")
		eventBus.emit(BluenetEvent.SPHERE_SETTINGS_UPDATED)
	}

	@Test
	fun testBackgroundPayload() {
		val payload = commandBroadcastQueue.getBackgroundPayload(sphereId, sphereState[sphereId]!!)
		assertFalse(payload == null)
		if (payload == null) {
			return
		}
		assertTrue(payload contentEquals byteArrayOf(45, -96, 5, 41))
	}

	@Test
	fun testHeader() {
		val keyPair = sphereState[sphereId]!!.settings.keySet.getHighestKey()
		val header = commandBroadcastQueue.getCommandBroadcastHeader(sphereId, sphereState[sphereId]!!, keyPair!!)
		assertFalse(header == null)
		if (header == null) {
			return
		}
		assertTrue(header contentEquals byteArrayOf(80, 7, 2, 64, 22, 164.toByte(), 45, 224.toByte()))
	}

	@Test
	fun testQueue() {
		for (i in 1..7) {
			addSwitchItem(i, 100)
		}

		for (i in 1..10) {
			if (commandBroadcastQueue.hasNext()) {
				val advertiseUuids = commandBroadcastQueue.getNextAdvertisementUuids()
				println("advertiseUUIDs:")
				if (advertiseUuids != null) {
					for (uuid in advertiseUuids) {
						println("   $uuid")
					}
				}
				else {
					println("   empty")
				}

				if (i == 3) {
					// Add new command for stone id 7, while it's being advertised.
					addSwitchItem(7, 100)
					// Add new command for stone id 6, while it's in queue.
					addSwitchItem(6, 100)
				}

				commandBroadcastQueue.advertisementDone(null)
			}
		}

		assertTrue(true)
	}

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
