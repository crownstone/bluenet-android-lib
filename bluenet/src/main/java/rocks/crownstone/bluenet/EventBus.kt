package rocks.crownstone.bluenet

import android.util.Log
import java.util.*
import java.util.UUID.randomUUID
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

typealias SubscriptionId = UUID
typealias EventType = String
typealias EventCallback = (Any) -> Unit

class EventBus {
	private val TAG = this::class.java.canonicalName

	private data class EventCallbackWithId(val id: SubscriptionId, val callback: EventCallback)
	private data class PendingSubscription(val id: SubscriptionId, val callback: EventCallback, val eventType: EventType)

	private val subscribers = HashMap<SubscriptionId, EventType>()
//	val eventSubscriptions = HashMap<EventType, HashMap<UUID, EventCallback>>()
	private val eventSubscriptions = HashMap<EventType, ArrayList<EventCallbackWithId>>()

	private val pendingSubscriptions = ArrayList<PendingSubscription>()
	private val pendingUnsubscriptions = ArrayList<SubscriptionId>()

	private var emitting = false

	init {
	}

	@Synchronized fun emit(eventType: BluenetEvent, data: Any = Unit) {
		emit(eventType.name, data)
	}

	@Synchronized fun emit(eventType: EventType, data: Any = Unit) {
		when (eventType) {
			BluenetEvent.SCAN_RESULT.name, BluenetEvent.SCAN_RESULT_RAW.name -> Log.v(TAG, "emit $eventType data: $data")
			else -> Log.i(TAG, "emit $eventType data: $data")
		}
		if (!eventSubscriptions.containsKey(eventType)) {
			return
		}
//		val callbacks = eventSubscriptions.get(event)!
//		for (it in callbacks) {
//
//		}

		emitting = true
		for (it in eventSubscriptions.getValue(eventType)) {
//		for (it in eventSubscriptions.get(event)!) {
			it.callback(data)
		}
		emitting = false

		// Handle pending subscriptions / unsubscriptions
		for (it in pendingSubscriptions) {
			subscribe(it.eventType, it.callback, it.id)
		}
		pendingSubscriptions.clear()
		for (it in pendingUnsubscriptions) {
			_unsubscribe(it)
		}
		pendingUnsubscriptions.clear()
	}

	@Synchronized fun subscribe(eventType: BluenetEvent, callback: EventCallback) : SubscriptionId {
		return subscribe(eventType.name, callback)
	}

	@Synchronized fun subscribe(eventType: EventType, callback: EventCallback) : SubscriptionId {
		val id : SubscriptionId = randomUUID()
		if (emitting) {
			Log.i(TAG, "add pending subscription $eventType $id")
			pendingSubscriptions.add(PendingSubscription(id, callback, eventType))
		}
		else {
			subscribe(eventType, callback, id)
		}
		return id
	}

	@Synchronized private fun subscribe(eventType: EventType, callback: EventCallback, id: SubscriptionId) {
		Log.i(TAG, "subscribe $eventType $id")
		if (!eventSubscriptions.containsKey(eventType)) {
			eventSubscriptions[eventType] = ArrayList()
		}
		subscribers[id] = eventType
		eventSubscriptions[eventType]?.add(EventCallbackWithId(id, callback))
	}

	@Synchronized fun unsubscribe(id: SubscriptionId) {
		if (emitting) {
			Log.i(TAG, "add pending unsubscription $id")
			pendingUnsubscriptions.add(id)
		}
		else {
			_unsubscribe(id)
		}


	}

	@Synchronized private fun _unsubscribe(id: SubscriptionId) {
		Log.i(TAG, "unsubscribe $id")
		val eventType = subscribers[id]
		if (eventType == null) {
			return
		}
		Log.i(TAG, "    eventType=$eventType")
		subscribers.remove(id)

		val list = eventSubscriptions.getValue(eventType)
		for (i in 0 until list.size) {
			if (list[i].id.equals(id)) {
				list.removeAt(i)
				break
			}
		}
//		for (it in eventSubscriptions.getValue(eventType)) {
//			if (it.id.equals(id)) {
//				eventSubscriptions[eventType]?.remove(it)
//				break
//			}
//		}
	}
}