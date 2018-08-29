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

	private val subscribers = HashMap<SubscriptionId, EventType>()
//	val topics = HashMap<EventType, HashMap<UUID, EventCallback>>()
	private val eventSubscriptions = HashMap<EventType, ArrayList<EventCallbackWithId>>()

	init {
	}

	fun emit(eventType: BluenetEvent, data: Any = Unit) {
		emit(eventType.name, data)
	}

	fun emit(eventType: EventType, data: Any = Unit) {
		if (!eventSubscriptions.containsKey(eventType)) {
			return
		}
//		val callbacks = topics.get(event)!
//		for (it in callbacks) {
//
//		}

		for (it in eventSubscriptions.getValue(eventType)) {
//		for (it in topics.get(event)!) {
			it.callback(data)
		}
	}

	fun subscribe(eventType: BluenetEvent, callback: EventCallback) : SubscriptionId {
		return subscribe(eventType.name, callback)
	}

	fun subscribe(eventType: EventType, callback: EventCallback) : SubscriptionId {
		Log.i(TAG, "subscribe $eventType")
		if (!eventSubscriptions.containsKey(eventType)) {
			eventSubscriptions[eventType] = ArrayList()
		}
		val id : SubscriptionId = randomUUID()
		Log.i(TAG, "    id=$id")
		subscribers[id] = eventType
		eventSubscriptions[eventType]?.add(EventCallbackWithId(id, callback))
		return id
	}

	fun unsubscribe(id: SubscriptionId) {
		Log.i(TAG, "unsubscribe $id")
		val eventType = subscribers[id] ?: return
//		if (eventType == null) {
//			return
//		}
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