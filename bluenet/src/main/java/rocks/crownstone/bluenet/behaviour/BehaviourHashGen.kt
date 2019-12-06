/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Dec 5, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.behaviour

import rocks.crownstone.bluenet.packets.behaviour.BehaviourHash
import rocks.crownstone.bluenet.packets.behaviour.BehaviourPacket
import rocks.crownstone.bluenet.packets.behaviour.IndexedBehaviourPacket
import rocks.crownstone.bluenet.util.*

object BehaviourHashGen {
	val TAG = this.javaClass.simpleName

	/**
	 * Calculate hash of a single behaviour.
	 *
	 * @param behaviour      Single behaviour packet.
	 * @return Hash of the behaviour.
	 */
	fun getHash(behaviour: BehaviourPacket): BehaviourHash {
		val arr = behaviour.getArray() ?: return 0U
		return Hash.fletcher32(arr)
	}

	/**
	 * Calculate hash of all behaviours.
	 *
	 * @param behaviourList  List of behaviours, with their index.
	 * @return Hash of all behaviours.
	 */
	fun getHash(behaviourList: List<IndexedBehaviourPacket>): BehaviourHash {
		val sortedList = behaviourList.sorted()
		Log.i(TAG, "sorted: $sortedList")

		var hash: BehaviourHash = 0U
		for (p in sortedList) {
			val indexArr = byteArrayOf(p.index.toByte())
			Hash.fletcher32(indexArr, hash)
			val behaviourArr = p.behaviour.getArray() ?: return 0U
			Hash.fletcher32(behaviourArr, hash)
		}
		return hash
	}
}