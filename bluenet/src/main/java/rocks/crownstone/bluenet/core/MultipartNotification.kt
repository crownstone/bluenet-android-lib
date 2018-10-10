package rocks.crownstone.bluenet.core

import android.util.Log
import rocks.crownstone.bluenet.BluenetProtocol
import rocks.crownstone.bluenet.util.Conversion
import java.nio.ByteBuffer

class MultipartNotification() {
	private val TAG = this.javaClass.simpleName

	private var nextMsgNr = 0
	private var buffer = ByteBuffer.allocate(BluenetProtocol.MULTIPART_NOTIFICATION_MAX_SIZE)

	fun onData(data: ByteArray) {
		if (data.isEmpty()) {
			Log.e(TAG, "empty data")
			return
		}
		val msgNr = Conversion.toUint8(data[0]).toInt()
		if (msgNr != 255 && nextMsgNr != msgNr) {
			Log.e(TAG, "unexpected msg nr $msgNr, expected $nextMsgNr")
			reset()
			return
		}
		Log.d(TAG, "received notification part $msgNr")
		buffer.put(data, 1, data.size-1)
		nextMsgNr += 1

		if (msgNr == BluenetProtocol.MULTIPART_NOTIFICATION_LAST_NR) {
			// Last notification
			// Copy data from buffer to byte array of correct size.
			val mergedData = ByteArray(buffer.position())
			buffer.rewind()
			buffer.get(mergedData)

			// TODO: emit or use callback for merged data
			reset()
		}
	}

	private fun reset() {
		nextMsgNr = 0
		buffer.clear()
	}
}