/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.broadcast

import nl.komponents.kovenant.Deferred
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.SphereId

enum class CommandBroadcastItemType {
	SWITCH,
	SET_TIME
}

class CommandBroadcastItem(
		val promise: Deferred<Unit, Exception>,
		val sphereId: SphereId,
		val type: CommandBroadcastItemType,
		val stoneId: Int,
		val payload: PacketInterface,
		var timeoutCount: Int
) {

}
