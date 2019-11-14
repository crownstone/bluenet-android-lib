/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Base class for a packet that has a generic payload.
 */
abstract class PayloadWrapperPacket(payload: PacketInterface?): PacketInterface {
	open val TAG = this.javaClass.simpleName

	protected var payload: PacketInterface? = payload

	/**
	 * Get size of the header.
	 */
	abstract fun getHeaderSize(): Int

	/**
	 * Get size of the payload, or null if unknown.
	 */
	abstract fun getPayloadSize(): Int?

	/**
	 * Serialize header to buffer.
	 *
	 * Size check is already performed.
	 */
	abstract fun headerToBuffer(bb: ByteBuffer): Boolean

	/**
	 * Deserialize header from buffer.
	 *
	 * Size check is already performed.
	 */
	abstract fun headerFromBuffer(bb: ByteBuffer): Boolean

	final override fun getPacketSize(): Int {
		val payload = this.payload
		if (payload == null) {
			return getHeaderSize()
		}
		return getHeaderSize() + payload.getPacketSize()
	}

	final override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < ${getPacketSize()}")
			return false
		}
		val payloadSize = getPayloadSize()
		if (payloadSize != null && bb.remaining() < payloadSize) {
			Log.w(TAG, "buffer too small for payload: ${bb.remaining()} < $payloadSize")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		headerToBuffer(bb)
		val payload = this.payload
		if (payload != null) {
			return payload.toBuffer(bb)
		}
		return true
	}

	final override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getHeaderSize()) {
			Log.w(TAG, "buffer too small for header: ${bb.remaining()} < ${getHeaderSize()}")
			return false
		}
		headerFromBuffer(bb)
		val payload = this.payload
		if (payload != null) {
			return payload.fromBuffer(bb)
		}
		val dataSize = getPayloadSize() ?: bb.remaining()
		val data = ByteArray(dataSize)
		bb.get(data)
		this.payload = ByteArrayPacket(data)
		return true
	}

	fun getPayload(): ByteArray? {
		return payload?.getArray()
	}

	override fun toString(): String {
		return "payloadSize=${payload?.getPacketSize() ?: 0} data=${Conversion.bytesToString(getPayload())}"
	}
}