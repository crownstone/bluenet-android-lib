/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.EmptyPacket
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.getUint8
import rocks.crownstone.bluenet.util.putUint8
import java.nio.ByteBuffer

enum class BehaviourType(val num: Uint8) {
	SWITCH(0),
	TWILIGHT(1),
	SMART_TIMER(2),
	UNKNOWN(255);
	companion object {
		private val map = values().associateBy(BehaviourType::num)
		fun fromNum(action: Uint8): BehaviourType {
			return map[action] ?: return UNKNOWN
		}
	}
}

typealias BehaviourPayloadInterface = PacketInterface

class BehaviourPacket(payload: BehaviourPayloadInterface) : PacketInterface {
	var type: BehaviourType
	var payload = payload
	init {
		type = when (payload::class) {
			SwitchBehaviourPacket::class -> BehaviourType.SWITCH
			TwilightBehaviourPacket::class -> BehaviourType.TWILIGHT
			SmartTimerBehaviourPacket::class -> BehaviourType.SMART_TIMER
			else -> BehaviourType.UNKNOWN
		}
	}
	constructor(): this(EmptyPacket())

	companion object {
		const val HEADER_SIZE = 1
	}

	override fun getPacketSize(): Int {
//		val payloadSize = payload?.getPacketSize() ?: 0
		return HEADER_SIZE + payload.getPacketSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
//		if (payload == null) {
//			return false
//		}
		if (type == BehaviourType.UNKNOWN) {
			return false
		}
		bb.putUint8(type.num)
		return payload.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < HEADER_SIZE) {
			return false
		}
		type = BehaviourType.fromNum(bb.getUint8())
		when (type) {
			BehaviourType.SWITCH -> payload = SwitchBehaviourPacket()
			BehaviourType.TWILIGHT -> payload = TwilightBehaviourPacket()
			BehaviourType.SMART_TIMER -> payload = SmartTimerBehaviourPacket()
			else -> return false
		}
		return payload.fromBuffer(bb)
	}
}