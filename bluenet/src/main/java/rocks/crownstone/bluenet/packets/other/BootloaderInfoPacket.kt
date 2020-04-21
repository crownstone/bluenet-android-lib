/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Apr 21, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.other

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.getUint16
import rocks.crownstone.bluenet.util.getUint8
import rocks.crownstone.bluenet.util.toUint8
import java.nio.ByteBuffer

class BootloaderInfoPacket: PacketInterface {
	enum class BuildType(val num: Uint8) {
		DEBUG(1U),
		RELEASE(2U),
		RELEASE_WITH_DEBUG_INFO(3U),
		MIN_SIZE_RELEASE(4U),
		UNKNOWN(255U);
		companion object {
			private val map = values().associateBy(BuildType::num)
			fun fromNum(action: Uint8): BuildType {
				return map[action] ?: return UNKNOWN
			}
		}
	}

	var protocol:    Uint8 = 0U; private set
	var dfuVersion:  Uint16 = 0U; private set
	var major:       Uint8 = 0U; private set
	var minor:       Uint8 = 0U; private set
	var patch:       Uint8 = 0U; private set
	var prerelease:  Uint8 = 0U; private set
	var buildType:   BuildType = BuildType.UNKNOWN; private set

	companion object {
		const val SIZE = 6 * Uint8.SIZE_BYTES + Uint16.SIZE_BYTES
		const val NON_RC: Uint8 = 255U
	}

	/**
	 * Get the version as string.
	 *
	 * Format: 1.2.3 or 1.2.3-RC4
	 */
	fun getVersionString(): String {
		val rcStr = if (prerelease == NON_RC) {
			""
		}
		else {
			"-RC$prerelease"
		}
		return "$major.$minor.$patch$rcStr"
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		protocol = bb.getUint8()
		dfuVersion = bb.getUint16()
		major = bb.getUint8()
		minor = bb.getUint8()
		patch = bb.getUint8()
		prerelease = bb.getUint8()
		buildType = BuildType.fromNum(bb.getUint8())

		if (protocol != 1U.toUint8()) {
			return false
		}
		return true
	}

	override fun toString(): String {
		return "BootloaderInfoPacket(protocol=$protocol, dfuVersion=$dfuVersion, major=$major, minor=$minor, patch=$patch, prerelease=$prerelease, buildType=$buildType)"
	}
}