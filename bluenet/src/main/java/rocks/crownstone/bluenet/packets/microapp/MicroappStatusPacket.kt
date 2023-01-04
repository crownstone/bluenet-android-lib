/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 23, 2022
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.microapp

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.behaviour.DaysOfWeekPacket
import rocks.crownstone.bluenet.packets.behaviour.TimeOfDayPacket
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MicroappStatusPacket(): PacketInterface {
	val TAG = this.javaClass.simpleName

	var buildVersion: Uint32 = 0U
		private set
	var sdkVersion = MicroappSdkVersionPacket()
		private set
	var checksum: Uint16 = 0U
		private set
	var checksumHeader: Uint16 = 0U
		private set
	var tests = MicroappTestsPacket()
		private set
	var functionTrying: Uint8 = 0U
		private set
	var functionFailed: Uint8 = 0U
		private set
	var functionsPassed: Uint32 = 0U
		private set

	companion object {
		const val SIZE = Uint32.SIZE_BYTES +
				MicroappSdkVersionPacket.SIZE +
				Uint16.SIZE_BYTES +
				Uint16.SIZE_BYTES +
				MicroappTestsPacket.SIZE +
				Uint8.SIZE_BYTES +
				Uint8.SIZE_BYTES +
				Uint32.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.i(TAG, "size=${bb.remaining()} expected=${getPacketSize()}")
			return false
		}
		buildVersion = bb.getUint32()
		if (!sdkVersion.fromBuffer(bb)) {
			return false
		}
		checksum = bb.getUint16()
		checksumHeader = bb.getUint16()
		if (!tests.fromBuffer(bb)) {
			return false
		}
		functionTrying = bb.getUint8()
		functionFailed = bb.getUint8()
		functionsPassed = bb.getUint32()
		return true
	}

	override fun toString(): String {
		return "MicroappStatusPacket(buildVersion=$buildVersion, sdkVersion=$sdkVersion, checksum=$checksum, checksumHeader=$checksumHeader, tests=$tests, functionTrying=$functionTrying, functionFailed=$functionFailed, functionsPassed=$functionsPassed)"
	}


}
