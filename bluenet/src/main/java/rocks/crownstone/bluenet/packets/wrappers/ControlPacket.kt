/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers

import rocks.crownstone.bluenet.connection.ExtConnection
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.wrappers.v3.ControlPacketV3
import rocks.crownstone.bluenet.packets.wrappers.v4.ControlPacketV4
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.structs.ControlType
import rocks.crownstone.bluenet.structs.ControlTypeV4
import java.nio.ByteBuffer

/**
 * Class that determines what wrapper packet to use based on discovered characteristics.
 *
 * Must be connected for this to work.
 */
class ControlPacket(connection: ExtConnection, type: ControlType, type4: ControlTypeV4, payload: PacketInterface?): PacketInterface {

	val packet: PacketInterface

	init {
		packet = if (connection.hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID)) {
			ControlPacketV3(type, null, payload)
		}
		else {
			ControlPacketV4(type4, payload)
		}
	}

	override fun getPacketSize(): Int {
		return packet.getPacketSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return packet.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return packet.fromBuffer(bb)
	}
}