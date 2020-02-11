package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint64
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.getUint32
import rocks.crownstone.bluenet.util.getUint64
import rocks.crownstone.bluenet.util.getUint8
import java.nio.ByteBuffer

class BehaviourDebugPacket: PacketInterface {
	var time: Uint32 = 0U
		private set
	var sunrise: Uint32 = 0U
		private set
	var sunset: Uint32 = 0U
		private set
	var overrideState: Uint8 = 0U
		private set
	var behaviourState: Uint8 = 0U
		private set
	var aggregatedState: Uint8 = 0U
		private set
	var dimmerPowered: Boolean = false
		private set
	var behaviourEnabled: Boolean = false
		private set
	var storedBehaviours: Uint64 = 0U
		private set
	var activeBehaviours: Uint64 = 0U
		private set
	var activeEndConditions: Uint64 = 0U
		private set
	var activeTimeoutPeriod: Uint64 = 0U
		private set
	var presenceBitmasks: ArrayList<Uint64> = ArrayList(NUM_PROFILES)
//	var presenceBitmasks: ArrayList<Uint64> = ULongArray(NUM_PROFILES, { 0U }).toList()
		private set

	init {
		for (i in 0 until NUM_PROFILES) {
			presenceBitmasks.add(0U)
		}
	}

	companion object {
		const val NUM_PROFILES = 8
		const val SIZE = 3 * Uint32.SIZE_BYTES + 5 * Uint8.SIZE_BYTES + 3 * Uint64.SIZE_BYTES + NUM_PROFILES * Uint64.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		time = bb.getUint32()
		sunrise = bb.getUint32()
		sunset = bb.getUint32()
		overrideState = bb.getUint8()
		behaviourState = bb.getUint8()
		aggregatedState = bb.getUint8()
		dimmerPowered = (bb.getUint8() > 0U)
		behaviourEnabled = (bb.getUint8() > 0U)
		storedBehaviours = bb.getUint64()
		activeBehaviours = bb.getUint64()
		activeEndConditions = bb.getUint64()
		activeTimeoutPeriod = bb.getUint64()
		for (i in 0 until NUM_PROFILES) {
			presenceBitmasks[i] = bb.getUint64()
		}
		return true
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun toString(): String {
		var presenceString = ""
		for (i in 0 until NUM_PROFILES) {
			presenceString += presenceBitmasks[i].toString()
		}
		return "BehaviourDebugPacket(time=$time, sunRise=$sunrise, sunSet=$sunset, overrideState=$overrideState, behaviourState=$behaviourState, aggregatedState=$aggregatedState, dimmerPowered=$dimmerPowered, behaviourEnabled=$behaviourEnabled, storedBehaviours=$storedBehaviours, activeBehaviours=$activeBehaviours, activeEndConditions=$activeEndConditions, activeGracePeriod=$activeTimeoutPeriod, presenceBitmasks=$presenceString)"
	}
}