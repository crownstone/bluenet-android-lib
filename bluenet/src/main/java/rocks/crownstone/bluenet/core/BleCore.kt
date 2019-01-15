/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet

import android.content.Context
import android.os.Looper
import rocks.crownstone.bluenet.core.CoreScanner
import rocks.crownstone.bluenet.util.EventBus

/**
 * Class that has all the bluetooth LE core functions.
 */
class BleCore(context: Context, eventBus: EventBus, looper: Looper): CoreScanner(context, eventBus, looper) {


}