/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jul 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.encryption

import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log

/**
 * Class that holds mesh keys.
 */
class MeshKeySet() {
	private val TAG = this.javaClass.simpleName
	var deviceKeyBytes: ByteArray? = null
		private set
	var appKeyBytes: ByteArray? = null
		private set
	var netKeyBytes: ByteArray? = null
		private set
	var initialized = false
		private set

	constructor(deviceKey: ByteArray?, appKey: ByteArray?, netKey: ByteArray?): this() {
		if (
				(deviceKey != null && deviceKey.size != BluenetProtocol.AES_BLOCK_SIZE) ||
				(appKey != null && appKey.size != BluenetProtocol.AES_BLOCK_SIZE) ||
				(netKey != null && netKey.size != BluenetProtocol.AES_BLOCK_SIZE)
		) {
			Log.e(TAG, "Invalid key size")
			return
		}
		deviceKeyBytes = deviceKey
		appKeyBytes = appKey
		netKeyBytes = netKey
		initialized = true
	}

	constructor(deviceKey: String?, appKey: String?, netKey: String?): this() {
		clear()
		try {
			deviceKeyBytes = Conversion.getKeyFromString(deviceKey)
			appKeyBytes = Conversion.getKeyFromString(appKey)
			netKeyBytes = Conversion.getKeyFromString(netKey)
		}
		catch (e: java.lang.NumberFormatException) {
			Log.e(TAG, "Invalid key format")
			clear()
			return
		}
		initialized = true
	}

	fun clear() {
		initialized = false
		deviceKeyBytes = null
		appKeyBytes = null
		netKeyBytes = null
	}

	override fun toString(): String {
		return "MeshKeys: [" +
				"device: ${Conversion.bytesToHexString(deviceKeyBytes)}, " +
				"app: ${Conversion.bytesToHexString(appKeyBytes)}, " +
				"net: ${Conversion.bytesToHexString(netKeyBytes)}, " +
				"]"
	}
}