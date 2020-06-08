/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: May 25, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.packets.EmptyPacket
import rocks.crownstone.bluenet.packets.other.AdcRestartsPacket
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesIndices
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesPacket
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesRequestPacket
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesType
import rocks.crownstone.bluenet.packets.switchHistory.SwitchHistoryListPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log

/**
 * Class to get debug information.
 *
 * Most commands assume you are already connected to the crownstone.
 */
class DebugData(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
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
		return getNextPowerSamples(type, indices, powerSamples)
	}

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
}