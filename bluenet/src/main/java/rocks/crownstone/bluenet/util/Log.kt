/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.util

import android.util.Log

class Log private constructor() {
	private var logLevel = Log.INFO
	private var logLevelFile = Log.ERROR

	enum class Level(val num: Int) {
		VERBOSE(Log.VERBOSE),
		DEBUG(Log.DEBUG),
		INFO(Log.INFO),
		WARN(Log.WARN),
		ERROR(Log.ERROR);

	}

	companion object {
		val instance = Log()
		fun e(tag: String, msg: String?) {
			instance.error(tag, msg)
		}

		fun w(tag: String, msg: String?) {
			instance.warn(tag, msg)
		}

		fun i(tag: String, msg: String?) {
			instance.info(tag, msg)
		}

		fun d(tag: String, msg: String?) {
			instance.debug(tag, msg)
		}

		fun v(tag: String, msg: String?) {
			instance.verbose(tag, msg)
		}

		fun setMinLevel(level: Level) {
			instance.logLevel = level.num
		}
	}

	fun error(tag: String, msg: String?) {
		if (Log.ERROR >= logLevel) {
			log(Log.ERROR, tag, msg)
		}
	}

	fun warn(tag: String, msg: String?) {
		if (Log.WARN >= logLevel) {
			log(Log.WARN, tag, msg)
		}
	}

	fun info(tag: String, msg: String?) {
		if (Log.INFO >= logLevel) {
			log(Log.INFO, tag, msg)
		}
	}

	fun debug(tag: String, msg: String?) {
		if (Log.DEBUG >= logLevel) {
			log(Log.DEBUG, tag, msg)
		}
	}

	fun verbose(tag: String, msg: String?) {
		if (Log.VERBOSE >= logLevel) {
			log(Log.VERBOSE, tag, msg)
		}
	}


	fun log(level: Int, tag: String, msg: String?) {
		Log.println(level, tag, msg)
	}
}