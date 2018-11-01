package rocks.crownstone.bluenet.scanparsing

import rocks.crownstone.bluenet.Int8

class IbeaconRssiAverager {
	private val list = ArrayList<Int8>()
	var average: Double = -127.0; private set

	fun add(rssi: Int8) {
		list.add(rssi)
	}

	fun calculateAverage(): Double {
		average = calculateAverageIbeaconSpec()
		return average
	}

	internal fun calculateMedian(): Double {
		// Calculate average by using the median
		return when (list.size) {
			0 -> -127.0
			1 -> list[0].toDouble()
			2 -> (list[0] + list[1]) / 2.0
			else -> {
				list.sort()
				list[list.size / 2].toDouble()
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

	fun clear() {
		list.clear()
	}
}