/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: May 25, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.packets.ByteArrayPacket
import rocks.crownstone.bluenet.packets.EmptyPacket
import rocks.crownstone.bluenet.packets.other.AdcChannelSwapsPacket
import rocks.crownstone.bluenet.packets.other.AdcRestartsPacket
import rocks.crownstone.bluenet.packets.other.GpregretPacket
import rocks.crownstone.bluenet.packets.other.RamStatsPacket
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesIndices
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesPacket
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesRequestPacket
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesType
import rocks.crownstone.bluenet.packets.switchHistory.SwitchHistoryListPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*

/**
 * Class to get debug information.
 *
 * Most commands assume you are already connected to the crownstone.
 */
class DebugData(eventBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val connection = connection

	/**
	 * Get the uptime.
	 *
	 * @return Promise with uptime in seconds as value.
	 */
	@Synchronized
	fun getUptime(): Promise<Uint32, Exception> {
		Log.i(TAG, "getUptime")
		val controlClass = Control(eventBus, connection)
		return controlClass.writeCommandAndGetResult(ControlTypeV4.GET_UPTIME, EmptyPacket())
	}

	/**
	 * Get the number of ADC restarts.
	 *
	 * @return Promise with ADC restarts packet as value.
	 */
	@Synchronized
	fun getAdcRestarts(): Promise<AdcRestartsPacket, Exception> {
		Log.i(TAG, "getAdcRestarts")
		val controlClass = Control(eventBus, connection)
		val resultPacket = AdcRestartsPacket()
		return controlClass.writeCommandAndGetResult(ControlTypeV4.GET_ADC_RESTARTS, EmptyPacket(), resultPacket)
	}

	/**
	 * Get the number of ADC channel swaps.
	 *
	 * @return Promise with ADC channel swaps packet as value.
	 */
	@Synchronized
	fun getAdcChannelSwaps(): Promise<AdcChannelSwapsPacket, Exception> {
		Log.i(TAG, "getAdcChannelSwaps")
		val controlClass = Control(eventBus, connection)
		val resultPacket = AdcChannelSwapsPacket()
		return controlClass.writeCommandAndGetResult(ControlTypeV4.GET_ADC_CHANNEL_SWAPS, EmptyPacket(), resultPacket)
	}

	/**
	 * Get power samples of an interesting event.
	 *
	 * Gets all indices of given type.
	 *
	 * @return Promise with list of power samples as value.
	 */
	@Synchronized
	fun getPowerSamples(type: PowerSamplesType): Promise<List<PowerSamplesPacket>, Exception> {
		val indices = PowerSamplesIndices(type)
		Log.i(TAG, "getPowerSamples type=$type indices=$indices")
		val powerSamples = ArrayList<PowerSamplesPacket>()
//		return getNextPowerSamples(type, indices, powerSamples)
		return getNextPowerSamples(type, 0U, powerSamples)
	}

	/**
	 * Gets power samples of given list of indices.
	 */
	private fun getNextPowerSamples(type: PowerSamplesType, indices: MutableList<Uint8>, powerSamples: MutableList<PowerSamplesPacket>):
			Promise<List<PowerSamplesPacket>, Exception> {
		if (indices.isEmpty()) {
			Log.d(TAG, "Power samples: $powerSamples")
			return Promise.ofSuccess(powerSamples)
		}
		val index: Uint8 = indices.removeAt(0)
		return getPowerSamples(type, index)
				.then {
					powerSamples.add(it)
					getNextPowerSamples(type, indices, powerSamples)
				}.unwrap()
	}

	/**
	 * Gets power samples at given index, then continues with next indices, until the result code is WRONG_PARAMETER.
	 */
	private fun getNextPowerSamples(type: PowerSamplesType, index: Uint8, powerSamples: MutableList<PowerSamplesPacket>):
			Promise<List<PowerSamplesPacket>, Exception> {
		val deferred = deferred<List<PowerSamplesPacket>, Exception>()
		getPowerSamples(type, index)
				.success { samples ->
					powerSamples.add(samples)
					val nextIndex: Uint8 = (index + 1U).toUint8()
					getNextPowerSamples(type, nextIndex, powerSamples)
							.success {
								deferred.resolve(powerSamples)
							}
							.fail {
								deferred.reject(it)
							}
				}
				.fail {
					if (it is Errors.Result && it.result == ResultType.WRONG_PARAMETER) {
						deferred.resolve(powerSamples)
					}
					else {
						deferred.reject(it)
					}
				}
		return deferred.promise
	}

	/**
	 * Get power samples of an interesting event.
	 *
	 * @return Promise with power samples as value.
	 */
	@Synchronized
	fun getPowerSamples(type: PowerSamplesType, index: Uint8): Promise<PowerSamplesPacket, Exception> {
		Log.i(TAG, "getPowerSamples type=$type index=$index")
		val controlClass = Control(eventBus, connection)
		val writePacket = PowerSamplesRequestPacket(type, index)
		val resultPacket = PowerSamplesPacket()
		return controlClass.writeCommandAndGetResult(ControlTypeV4.GET_POWER_SAMPLES, writePacket, resultPacket)
	}

	/**
	 * Get a history of switch commands.
	 *
	 * @return Promise with switch history as value.
	 */
	@Synchronized
	fun getSwitchHistory(): Promise<SwitchHistoryListPacket, Exception> {
		Log.i(TAG, "getSwitchHistory")
		val controlClass = Control(eventBus, connection)
		val resultPacket = SwitchHistoryListPacket()
		return controlClass.writeCommandAndGetResult(ControlTypeV4.GET_SWITCH_HISTORY, EmptyPacket(), resultPacket)
	}

	/**
	 * Get minimum queue space left of app scheduler observed so far.
	 *
	 * @return Promise with minimum queue space left as value.
	 */
	@Synchronized
	fun getSchedulerMinFree(): Promise<Uint16, Exception> {
		Log.i(TAG, "getSchedulerMinFree")
		val controlClass = Control(eventBus, connection)
		return controlClass.writeCommandAndGetResult(ControlTypeV4.GET_SCHEDULER_MIN_FREE, EmptyPacket())
	}

	/**
	 * Get the reset reason.
	 *
	 * @return Promise with reset reason bitmask as value.
	 */
	@Synchronized
	fun getResetReason(): Promise<Uint32, Exception> {
		Log.i(TAG, "getResetReason")
		val controlClass = Control(eventBus, connection)
		return controlClass.writeCommandAndGetResult(ControlTypeV4.GET_RESET_REASON, EmptyPacket())
	}

	/**
	 * Get all GPREGRET.
	 *
	 * @return Promise with list of GPREGRET packets as value.
	 */
	@Synchronized
	fun getGpregret(): Promise<List<GpregretPacket>, Exception> {
		Log.i(TAG, "getGpregret")
		val indices: MutableList<Uint8> = arrayListOf(0U, 1U)
		val gpregrets = ArrayList<GpregretPacket>()
		return getNextGpregret(indices, gpregrets)
	}

	private fun getNextGpregret(indices: MutableList<Uint8>, gpregrets: MutableList<GpregretPacket>):
			Promise<List<GpregretPacket>, Exception> {
		if (indices.isEmpty()) {
			Log.d(TAG, "GPREGRET: $gpregrets")
			return Promise.ofSuccess(gpregrets)
		}
		val index: Uint8 = indices.removeAt(0)
		return getGpregret(index)
				.then {
					gpregrets.add(it)
					getNextGpregret(indices, gpregrets)
				}.unwrap()
	}

	/**
	 * Get the Nth GPREGRET.
	 *
	 * @return Promise with GPREGRET packet as value.
	 */
	@Synchronized
	fun getGpregret(index: Uint8): Promise<GpregretPacket, Exception> {
		Log.i(TAG, "getGpregret $index")
		val controlClass = Control(eventBus, connection)
		val writePacket = ByteArrayPacket(Conversion.toByteArray(index))
		val resultPacket = GpregretPacket()
		return controlClass.writeCommandAndGetResult(ControlTypeV4.GET_GPREGRET, writePacket, resultPacket)
	}

	/**
	 * Get RAM statistics.
	 *
	 * @return Promise with RAM statistics packet as value.
	 */
	@Synchronized
	fun getRamStats(): Promise<RamStatsPacket, Exception> {
		Log.i(TAG, "getRamStats")
		val controlClass = Control(eventBus, connection)
		val resultPacket = RamStatsPacket()
		return controlClass.writeCommandAndGetResult(ControlTypeV4.GET_RAM_STATS, EmptyPacket(), resultPacket)
	}

	/**
	 * Clean up flash
	 */
	@Synchronized
	fun cleanFlash(): Promise<Unit, Exception> {
		Log.i(TAG, "cleanFlash")
		val controlClass = Control(eventBus, connection)
		return controlClass.writeCommandAndGetResult(ControlTypeV4.CLEAN_FLASH, EmptyPacket())
	}
}