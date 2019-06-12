/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 11, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import rocks.crownstone.bluenet.structs.IbeaconData
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.put
import rocks.crownstone.bluenet.util.putInt
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SetupPacketV2(val stoneId: Uint8,
					val sphereId: Uint8,
					val keySet: KeySet,
					val meshDeviceKeyBytes: ByteArray,
				  val ibeaconData: IbeaconData): PacketInterface {
	companion object {
		const val SIZE = 1+1+7*16+16+2+2
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
				keySet.adminKeyBytes == null || keySet.adminKeyBytes?.size != BluenetProtocol.AES_BLOCK_SIZE ||
				keySet.memberKeyBytes == null || keySet.memberKeyBytes?.size != BluenetProtocol.AES_BLOCK_SIZE ||
				keySet.guestKeyBytes == null || keySet.guestKeyBytes?.size != BluenetProtocol.AES_BLOCK_SIZE ||
				keySet.serviceDataKeyBytes == null || keySet.serviceDataKeyBytes?.size != BluenetProtocol.AES_BLOCK_SIZE ||
				meshDeviceKeyBytes == null || meshDeviceKeyBytes?.size != BluenetProtocol.AES_BLOCK_SIZE ||
				keySet.meshAppKeyBytes == null || keySet.meshAppKeyBytes?.size != BluenetProtocol.AES_BLOCK_SIZE ||
				keySet.meshNetKeyBytes == null || keySet.meshNetKeyBytes?.size != BluenetProtocol.AES_BLOCK_SIZE
		) {
			Log.w(TAG, "wrong key: keySet=$keySet meshDevice=${Conversion.bytesToHexString(meshDeviceKeyBytes)}")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.put(stoneId)
		bb.put(sphereId)
		bb.put(keySet.adminKeyBytes)
		bb.put(keySet.memberKeyBytes)
		bb.put(keySet.guestKeyBytes)
		bb.put(keySet.serviceDataKeyBytes)
		bb.put(meshDeviceKeyBytes)
		bb.put(keySet.meshAppKeyBytes)
		bb.put(keySet.meshNetKeyBytes)
		bb.put(Conversion.uuidToBytes(ibeaconData.uuid))
		bb.putShort(ibeaconData.major)
		bb.putShort(ibeaconData.minor)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}
