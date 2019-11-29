/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers.v4

import rocks.crownstone.bluenet.packets.ByteArrayPacket
import rocks.crownstone.bluenet.packets.EmptyPacket
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.ControlTypeV4

object ResultPayloadPacketV4 {
	fun getPacket(type: ControlTypeV4): PacketInterface {
		return when (type) {
			ControlTypeV4.SETUP -> EmptyPacket()
			ControlTypeV4.FACTORY_RESET -> EmptyPacket()
			ControlTypeV4.GET_STATE -> EmptyPacket()
			ControlTypeV4.SET_STATE -> EmptyPacket()
			ControlTypeV4.RESET -> EmptyPacket()
			ControlTypeV4.GOTO_DFU -> EmptyPacket()
			ControlTypeV4.NOOP -> EmptyPacket()
			ControlTypeV4.DISCONNECT -> EmptyPacket()
			ControlTypeV4.SWITCH -> EmptyPacket()
			ControlTypeV4.MULTI_SWITCH -> EmptyPacket()
			ControlTypeV4.DIMMER -> EmptyPacket()
			ControlTypeV4.RELAY -> EmptyPacket()
			ControlTypeV4.SET_TIME -> EmptyPacket()
			ControlTypeV4.INCREASE_TX -> EmptyPacket()
			ControlTypeV4.RESET_STATE_ERRORS -> EmptyPacket()
			ControlTypeV4.MESH_COMMAND -> EmptyPacket()
			ControlTypeV4.ALLOW_DIMMING -> EmptyPacket()
			ControlTypeV4.LOCK_SWITCH -> EmptyPacket()
			ControlTypeV4.UART_MSG -> EmptyPacket()
			ControlTypeV4.BEHAVIOUR_ADD -> EmptyPacket()
			ControlTypeV4.BEHAVIOUR_REPLACE -> EmptyPacket()
			ControlTypeV4.BEHAVIOUR_REMOVE -> EmptyPacket()
			ControlTypeV4.BEHAVIOUR_GET -> EmptyPacket()
			ControlTypeV4.BEHAVIOUR_GET_INDICES -> EmptyPacket()
			ControlTypeV4.UNKNOWN -> EmptyPacket()
		}
	}
}