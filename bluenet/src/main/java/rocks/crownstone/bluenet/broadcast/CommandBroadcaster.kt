/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.broadcast

import android.bluetooth.le.AdvertiseData
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.BluenetConfig
import rocks.crownstone.bluenet.encryption.Encryption
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.broadcast.*
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.util.*
import kotlin.collections.ArrayList

class CommandBroadcaster(evtBus: EventBus, state: SphereStateMap, bleCore: BleCore, encryptionManager: EncryptionManager, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val libState = state
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val handler = Handler(looper)
	private val queue = CommandBroadcastQueue(libState, encryptionManager)
	private var broadcasting = false

	/**
	 * Add an item to the queue.
	 */
	@Synchronized
	fun add(item: CommandBroadcastItem) {
		queue.add(item)
		broadcastNext()
	}

	@Synchronized
	private fun broadcastNext() {
		Log.d(TAG, "broadcastNext")
		if (!queue.hasNext()) {
			return
		}
		val advertiseData = queue.getNextAdvertisement()
		if (advertiseData == null) {
			broadcastNext()
			return
		}
		broadcasting = true
		bleCore.advertise(advertiseData, BluenetConfig.COMMAND_BROADCAST_INTERVAL_MS)
		handler.postDelayed(onBroadcastDoneRunnable, BluenetConfig.COMMAND_BROADCAST_INTERVAL_MS.toLong())
	}

	private val onBroadcastDoneRunnable = Runnable {
		onBroadcastDone()
	}

	@Synchronized
	private fun onBroadcastDone() {
		Log.i(TAG, "onBroadcastDone")
		broadcasting = false
		broadcastNext()
	}
}