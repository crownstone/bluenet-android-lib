/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Dec 5, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.behaviour

import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.Bluenet
import rocks.crownstone.bluenet.packets.behaviour.BehaviourIndex
import rocks.crownstone.bluenet.packets.behaviour.BehaviourPacket
import rocks.crownstone.bluenet.packets.behaviour.INDEX_UNKNOWN
import rocks.crownstone.bluenet.packets.behaviour.IndexedBehaviourPacket
import rocks.crownstone.bluenet.util.Log
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Class that synchronizes behaviour with a Crownstone.
 *
 * The strategy from this class is to assume the behaviour on the Crownstone is correct.
 *
 * It will first get a list of behaviour hashes.
 * Then, for each behaviour hash that does not match, it will get the behaviour.
 *
 * Local behaviours with index INDEX_UNKNOWN, will be preserved: it's assumed these should've been set.
 */
class BehaviourSyncerFromCrownstone(val bluenet: Bluenet) {
	val TAG = this.javaClass.simpleName
	private var localBehaviours: List<IndexedBehaviourPacket> = ArrayList()
	private val localBehavioursMap = HashMap<BehaviourIndex, BehaviourPacket>()

	private val indicesToGet = LinkedList<BehaviourIndex>()
	private val remoteBehaviours: ArrayList<IndexedBehaviourPacket> = ArrayList()

	/**
	 * Set behaviours that you think should be on the Crownstone.
	 */
	fun setBehaviours(behaviourList: List<IndexedBehaviourPacket>) {
		Log.i(TAG, "setBehaviours $behaviourList")
		localBehaviours = behaviourList
		localBehavioursMap.clear()
		for (b in localBehaviours) {
			if (b.index == INDEX_UNKNOWN) {
				Log.w(TAG, "behaviour has unknown index: $b")
			}
			localBehavioursMap.put(b.index, b.behaviour)
		}
	}

	/**
	 * Perform synchronization
	 *
	 * @return Promise with behaviours.
	 */
	fun sync(): Promise<List<IndexedBehaviourPacket>, Exception> {
		Log.i(TAG, "sync")
		val deferred = deferred<List<IndexedBehaviourPacket>, Exception>()
		indicesToGet.clear()
		remoteBehaviours.clear()
		bluenet.control.getBehaviourIndices()
				.success {
					for (b in it.indicesWithHash) {
						val index = b.index
						val localBehaviour = localBehavioursMap.get(index)
						Log.d(TAG, "index=$index in local map: ${(localBehaviour == null)}")
						if (localBehaviour != null) {
							val localBehaviourHash = BehaviourHashGen.getHash(localBehaviour)
							Log.d(TAG, "hash: local=$localBehaviourHash remote=${b.hash.hash}")
							if (localBehaviourHash == b.hash.hash) {
								Log.d(TAG, "no need to get behaviour: $localBehaviour")
								remoteBehaviours.add(IndexedBehaviourPacket(index, localBehaviour))
								continue
							}
						}
						indicesToGet.add(b.index)
					}
					for (b in localBehaviours) {
						if (b.index == INDEX_UNKNOWN) {
							Log.w(TAG, "adding behaviour $b")
							remoteBehaviours.add(b)
						}
					}
					getBehaviours()
							.success {
								Log.d(TAG, "remoteBehaviours: $remoteBehaviours")
								deferred.resolve(remoteBehaviours)
							}
							.fail {
								deferred.reject(it)
							}
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	private fun getBehaviours(): Promise<Unit, Exception>{
		if (indicesToGet.isEmpty()) {
			return Promise.ofSuccess(Unit)
		}
		val index = indicesToGet.peek().toUByte()
		Log.d(TAG, "get behaviour $index")
		return bluenet.control.getBehaviour(index)
				.then {
					Log.d(TAG, "got behaviour: $it")
					remoteBehaviours.add(it)
					indicesToGet.remove()
					return@then getBehaviours()
				}.unwrap()

//		val deferred = deferred<Unit, Exception>()
////		val index = indicesToGet.peekFirst()
//		val index = indicesToGet.poll().toUByte()
////		val index = 0.toUByte()
//		Log.d(TAG, "get behaviour $index")
//		bluenet.control.getBehaviour(index)
//				.success {
//					Log.d(TAG, "got behaviour: $it")
//					remoteBehaviours.add(it)
////					indicesToGet.remove()
//					if (indicesToGet.isEmpty()) {
//						deferred.resolve()
//					}
//					else {
//						getBehaviours()
//								.success { deferred.resolve() }
//								.fail { deferred.reject(it) }
//					}
//				}
//		return deferred.promise

//		getBehavioursPromise = deferred()
//		val deferred = getBehavioursPromise ?: return Promise.ofFail(Exception("No promise"))
//		handler.post { getNextBehaviour() }
//		return deferred.promise
	}

//	private fun getNextBehaviour() {
//		val index = indicesToGet.peek()
//		Log.d(TAG, "get behaviour $index")
//		bluenet.control.getBehaviour(index)
//				.success {
//					Log.d(TAG, "got behaviour: $it")
//					remoteBehaviours.add(it)
//					indicesToGet.remove()
//					if (indicesToGet.isEmpty()) {
//						getBehavioursPromise?.resolve()
//					}
//					handler.post { getNextBehaviour() }
//				}
//				.fail {
//					getBehavioursPromise?.reject(it)
//				}
//	}


}