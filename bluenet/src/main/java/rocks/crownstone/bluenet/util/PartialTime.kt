/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.util

import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint32
import java.util.Calendar
import java.util.TimeZone

object PartialTime {
	private val TAG = this.javaClass.simpleName
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
	fun reconstructTimestamp(lsbTimestamp: Uint16): Long {
		val gmtTimestamp = Util.getGmtTimestamp()
		val reconstructTimestamp = reconstructTimestamp(gmtTimestamp, lsbTimestamp)
		Log.v(TAG, "gmtTimestamp=$gmtTimestamp reconstructTimestamp=$reconstructTimestamp")
		return reconstructTimestamp.toLong()
	}


	/**
	 * Reconstructs a full timestamp from the given unix timestamp and a partial timestamp.
	 *
	 * Works as long as the source of the partial timestamp is not more than 9 hours off.
	 *
	 * @param unixTimestamp  Seconds since epoch (gmt).
	 * @param lsbTimestamp   Partial timestamp.
	 * @return               The reconstructed timestamp.
	 */
	fun reconstructTimestamp(unixTimestamp: Long, lsbTimestamp: Uint16): Uint32 {
//		val calendar = Calendar.getInstance()
//		val timeZone = calendar.timeZone
//		val secondsFromGmt = ((timeZone.rawOffset + timeZone.dstSavings) / 1000).toLong()
//		val correctedTimestamp = unixTimestamp + secondsFromGmt

		val correctedTimestamp = Util.getLocalTimestamp(unixTimestamp).toLong()
		Log.v(TAG, "unixTimestamp=$unixTimestamp correctedTimestamp=$correctedTimestamp lsbTimestamp=$lsbTimestamp")

		var reconstructedTimestamp = combineTimestamp(correctedTimestamp.toUint32(), lsbTimestamp)

		val delta = correctedTimestamp - reconstructedTimestamp.toLong()
		if (delta > -HALF_UINT16 && delta < HALF_UINT16) {
			return reconstructedTimestamp
		} else if (delta < -HALF_UINT16) {
			reconstructedTimestamp = combineTimestamp((correctedTimestamp - MAX_UINT16).toUint32(), lsbTimestamp)
		} else if (delta > HALF_UINT16) {
			reconstructedTimestamp = combineTimestamp((correctedTimestamp + MAX_UINT16).toUint32(), lsbTimestamp)
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
	private fun combineTimestamp(timestamp: Uint32, lsbTimestamp: Uint16): Uint32 {
		return (timestamp and 0xFFFF0000.toUint32()) + (lsbTimestamp.toUint32() and 0x0000FFFF.toUint32())
//		val arr = Conversion.uint32ToByteArray(timestamp)
//		val arrLsb = Conversion.uint16ToByteArray(lsbTimestamp)
//		arr[0] = arrLsb[0]
//		arr[1] = arrLsb[1]
//		return Conversion.byteArrayToInt(arr)
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