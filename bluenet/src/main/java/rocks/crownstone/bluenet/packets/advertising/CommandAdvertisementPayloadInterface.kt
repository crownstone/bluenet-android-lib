/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.advertising

import rocks.crownstone.bluenet.packets.PacketInterface

interface CommandAdvertisementPayloadInterface : PacketInterface{
	fun add(packet: PacketInterface): Boolean
	fun isFull(): Boolean
}