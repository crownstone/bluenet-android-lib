/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.advertising

import nl.komponents.kovenant.Deferred
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.SphereId

enum class CommandAdvertiserItemType {
	SWITCH,
	SET_TIME
}

class CommandAdvertiserItem(
		val promise: Deferred<Unit, Exception>,
		val sphereId: SphereId,
		val type: CommandAdvertiserItemType,
		val stoneId: Int,
		val payload: PacketInterface,
		var timeoutCount: Int
) {

}
