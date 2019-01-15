/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanhandling

class IbeaconRssiAverager {
	private val list = ArrayList<Int>()
	private var average: Int = -127

	fun add(rssi: Int) {
		list.add(rssi)
	}

	fun getAverage(): Int {
		if (average == -127) {
			calculateAverage()
		}
		return average
	}

	fun clear() {
		list.clear()
	}

	internal fun calculateAverage() {
		average = calculateAverageIbeaconSpec().toInt()
	}

	internal fun calculateMedian(): Double {
		// Calculate average by using the median
		return when (list.size) {
			0 -> -127.0
			1 -> list[0].toDouble()
			2 -> (list[0] + list[1]) / 2.0
			else -> {
				list.sort()
				val center = list.size / 2
				if (list.size % 2 == 0) {
					(list[center-1] + list[center]) / 2.0
				}
				else {
					list[center].toDouble()
				}
			}
		}
	}

	internal fun calculateAverageIbeaconSpec(): Double {
		// Calculate average as iBeacon spec calibrates the rssi at one meter:
		// Remove the top 10%, remove the bottom 20%, and average the remaining values
		list.sort()
		val size = list.size
		var startIndex = 0
		var endIndex = size
		if (size > 2) {
			startIndex = size / 5
			endIndex = size - size / 10
		}
		var sum = 0.0
		for (i in startIndex until endIndex) {
			sum += list[i]
		}
		return (sum / (endIndex - startIndex))
	}
}