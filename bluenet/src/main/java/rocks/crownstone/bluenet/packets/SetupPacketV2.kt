/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 11, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import rocks.crownstone.bluenet.structs.IbeaconData
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.encryption.MeshKeySet
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.put
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SetupPacketV2(val stoneId: Uint8,
					val sphereId: Uint8,
					val keySet: KeySet,
					val meshKeys: MeshKeySet,
				  val ibeaconData: IbeaconData): PacketInterface {
	companion object {
		const val SIZE = 1+1+8*16+16+2+2
	}
	private val TAG = this.javaClass.simpleName

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < SIZE) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < $SIZE")
			return false
		}
		if (
				keySet.adminKeyBytes == null ||
				keySet.memberKeyBytes == null ||
				keySet.guestKeyBytes == null ||
				keySet.serviceDataKeyBytes == null ||
				keySet.localizationKeyBytes == null ||
				meshKeys.deviceKeyBytes == null ||
				meshKeys.appKeyBytes == null ||
				meshKeys.netKeyBytes == null
		) {
			Log.w(TAG, "wrong key: keySet=$keySet meshKeys=$meshKeys")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.put(stoneId)
		bb.put(sphereId)
		bb.put(keySet.adminKeyBytes)
		bb.put(keySet.memberKeyBytes)
		bb.put(keySet.guestKeyBytes)
		bb.put(keySet.serviceDataKeyBytes)
		bb.put(keySet.localizationKeyBytes)
		bb.put(meshKeys.deviceKeyBytes)
		bb.put(meshKeys.appKeyBytes)
		bb.put(meshKeys.netKeyBytes)
		bb.put(Conversion.uuidToBytes(ibeaconData.uuid))
		bb.putShort(ibeaconData.major)
		bb.putShort(ibeaconData.minor)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}
