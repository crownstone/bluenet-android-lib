/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Apr 6, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.other

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

/**
 * Ibeacon config ID packet.
 *
 * @param id            The ibeacon config ID to use for advertising.
 * @param timestamp     When the ID should be set.
 * @param interval      Interval in seconds when the ID should be set again.
 *
 * Set the interval to 0 if you to set the ID only once.
 * Set the interval to 0, and the timestamp to 0 if you want to set the ID only once, and now.
 */
class IbeaconConfigIdPacket(id: Uint8, timestamp: Uint32, interval: Uint16): PacketInterface {
	var id = id; private set
	var timestamp = timestamp; private set
	var interval = interval; private set

	constructor(): this(0U, 0U, 0U)

	companion object {
		const val SIZE = Uint8.SIZE_BYTES + Uint32.SIZE_BYTES + Uint16.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.putUint8(id)
		bb.putUint32(timestamp)
		bb.putUint16(interval)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		id = bb.getUint8()
		timestamp = bb.getUint32()
		interval = bb.getUint16()
		return true
	}

	override fun toString(): String {
		return "IbeaconConfigIdPacket(id=$id, timestamp=$timestamp, interval=$interval)"
	}
}