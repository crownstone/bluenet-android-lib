package rocks.crownstone.bluenet.util

import java.util.Calendar
import java.util.TimeZone

object PartialTime {
	private const val HALF_UINT16: Long = 0x7FFF
	private const val MAX_UINT16: Long = 0xFFFF

	/**
	 * Reconstructs a full timestamp from the current time and a partial timestamp.
	 *
	 * Works as long as the source of the partial timestamp is not more than 9 hours off.
	 *
	 * @param lsbTimestamp   Partial timestamp.
	 * @return               The reconstructed timestamp.
	 */
	fun reconstructTimestamp(lsbTimestamp: Int): Long {
		val timeZone = TimeZone.getTimeZone("GMT")
		val calendar = Calendar.getInstance(timeZone)
		val gmtTimestamp = calendar.time.time / 1000
		return reconstructTimestamp(gmtTimestamp, lsbTimestamp)
	}


	/**
	 * Reconstructs a full timestamp from the current unix timestamp and a partial timestamp.
	 *
	 * Works as long as the source of the partial timestamp is not more than 9 hours off.
	 *
	 * @param unixTimestamp  Seconds since epoch (gmt).
	 * @param lsbTimestamp   Partial timestamp.
	 * @return               The reconstructed timestamp.
	 */
	fun reconstructTimestamp(unixTimestamp: Long, lsbTimestamp: Int): Long {
		val calendar = Calendar.getInstance()
		val timeZone = calendar.timeZone
		val secondsFromGmt = (timeZone.rawOffset / 1000).toLong()
		val correctedTimestamp = unixTimestamp + secondsFromGmt

		var reconstructedTimestamp = combineTimestamp(correctedTimestamp, lsbTimestamp)

		val delta = correctedTimestamp - reconstructedTimestamp
		if (delta > -HALF_UINT16 && delta < HALF_UINT16) {
			return reconstructedTimestamp
		} else if (delta < -HALF_UINT16) {
			reconstructedTimestamp = combineTimestamp(correctedTimestamp - MAX_UINT16, lsbTimestamp)
		} else if (delta > HALF_UINT16) {
			reconstructedTimestamp = combineTimestamp(correctedTimestamp + MAX_UINT16, lsbTimestamp)
		}
		return reconstructedTimestamp
	}


	/**
	 * Replaces the least significant bytes of a timestamp by the partial timestamp.
	 *
	 * @param timestamp      Timestamp in seconds.
	 * @param lsbTimestamp   Partial timestamp.
	 * @return               Timestamp with the least significant bytes replaced by the partial timestamp.
	 */
	private fun combineTimestamp(timestamp: Long, lsbTimestamp: Int): Long {
		val arr = Conversion.uint32ToByteArray(timestamp)
		val arrLsb = Conversion.shortToByteArray(lsbTimestamp)
		arr[0] = arrLsb[0]
		arr[1] = arrLsb[1]
		return Conversion.toUint32(Conversion.byteArrayToInt(arr))
	}

// // Other method:
//	modulo = timestamp % (1 << 16);
//
//	if (modulo >= lsbTimestamp) {
//		if (modulo - lsbTimestamp > (1<<15)) {
//			// Assume partial timestamp is older
//			timeDiff = modulo - (lsbTimestamp + (1<<16));
//		}
//		else {
//			timeDiff = modulo - lsbTimestamp;
//		}
//	}
//			else {
//		if (lsbTimestamp - modulo > (1<<15)) {
//			timeDiff = (modulo + (1<<16)) - lsbTimestamp;
//		}
//		else {
//			timeDiff = modulo - lsbTimestamp;
//		}
//	}
//	// timestamp is current mesh time
//	// timeDiff is current mesh time - service data time
//	reconstructedTimestamp = timestamp - timeDiff;

}