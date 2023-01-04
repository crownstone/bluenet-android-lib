/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 23, 2022
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.microapp

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.getUint16
import rocks.crownstone.bluenet.util.getUint8
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MicroappInfoPacket(): PacketInterface {
	val TAG = this.javaClass.simpleName

	var protocol: Uint8 = 0U
		private set
	var maxApps: Uint8 = 0U
		private set
	var maxAppSize: Uint16 = 0U
		private set
	var maxChunkSize: Uint16 = 0U
		private set
	var maxRamUsage: Uint16 = 0U
		private set
	var sdkVersion = MicroappSdkVersionPacket()
		private set
	var appsStatus = ArrayList<MicroappStatusPacket>()
		private set

	override fun getPacketSize(): Int {
		return Uint8.SIZE_BYTES +
				Uint8.SIZE_BYTES +
				Uint16.SIZE_BYTES +
				Uint16.SIZE_BYTES +
				Uint16.SIZE_BYTES +
				MicroappSdkVersionPacket.SIZE +
				MicroappStatusPacket.SIZE * maxApps.toInt()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		val size = bb.remaining()
		if (size < getPacketSize()) {
			Log.i(TAG, "size=$size expected=${getPacketSize()}")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		protocol = bb.getUint8()
		maxApps = bb.getUint8()

		// Recheck size, because maxApps is set now.
		if (size < getPacketSize()) {
			Log.i(TAG, "size=$size expected=${getPacketSize()} maxApps=$maxApps")
			return false
		}

		maxAppSize = bb.getUint16()
		maxChunkSize = bb.getUint16()
		maxRamUsage = bb.getUint16()

		if (!sdkVersion.fromBuffer(bb)) {
			return false
		}

		appsStatus.clear()
		for (i in 0 until maxApps.toInt()) {
			val status = MicroappStatusPacket()
			if (!status.fromBuffer(bb)) {
				return false
			}
			appsStatus.add(status)
		}
		return true
	}

	override fun toString(): String {
		return "MicroappInfoPacket(protocol=$protocol, maxApps=$maxApps, maxAppSize=$maxAppSize, maxChunkSize=$maxChunkSize, maxRamUsage=$maxRamUsage, sdkVersion=$sdkVersion, appsStatus=$appsStatus)"
	}
}
