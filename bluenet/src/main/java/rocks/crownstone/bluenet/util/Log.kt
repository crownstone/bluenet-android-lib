/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.util

import android.util.Log

class Log private constructor() {
	private var logLevel =     Log.VERBOSE
	private var logLevelFile = Log.DEBUG
	private var fileLogger: FileLogger? = null

	enum class Level(val num: Int) {
		VERBOSE(Log.VERBOSE),
		DEBUG(Log.DEBUG),
		INFO(Log.INFO),
		WARN(Log.WARN),
		ERROR(Log.ERROR);
	}

	companion object {
		private val levelToChar = charArrayOf(' ', ' ', 'V', 'D', 'I', 'W', 'E') // Based on android.util.Log levels
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

		fun setLogLevel(level: Level) {
			instance.logLevel = level.num
		}

		fun setFileLogLevel(level: Level) {
			instance.logLevelFile = level.num
		}

		fun setFileLogger(fileLogger: FileLogger) {
			instance.fileLogger = fileLogger
		}
	}

	fun error(tag: String, msg: String?) {
		if (msg == null) {
			return
		}
		if (Log.ERROR >= logLevel) {
			log(Log.ERROR, tag, msg)
		}
		if (Log.ERROR >= logLevelFile) {
			logFile(Log.ERROR, tag, msg)
		}
	}

	fun warn(tag: String, msg: String?) {
		if (msg == null) {
			return
		}
		if (Log.WARN >= logLevel) {
			log(Log.WARN, tag, msg)
		}
		if (Log.WARN >= logLevelFile) {
			logFile(Log.WARN, tag, msg)
		}
	}

	fun info(tag: String, msg: String?) {
		if (msg == null) {
			return
		}
		if (Log.INFO >= logLevel) {
			log(Log.INFO, tag, msg)
		}
		if (Log.INFO >= logLevelFile) {
			logFile(Log.INFO, tag, msg)
		}
	}

	fun debug(tag: String, msg: String?) {
//		if (!BuildConfig.DEBUG) {
//			return
//		}
		if (msg == null) {
			return
		}
		if (Log.DEBUG >= logLevel) {
			log(Log.DEBUG, tag, msg)
		}
		if (Log.DEBUG >= logLevelFile) {
			logFile(Log.DEBUG, tag, msg)
		}
	}

	fun verbose(tag: String, msg: String?) {
//		if (!BuildConfig.DEBUG) {
//			return
//		}
		if (msg == null) {
			return
		}
		if (Log.VERBOSE >= logLevel) {
			log(Log.VERBOSE, tag, msg)
		}
		if (Log.VERBOSE >= logLevelFile) {
			logFile(Log.VERBOSE, tag, msg)
		}
	}

	fun log(level: Int, tag: String, msg: String) {
		Log.println(level, tag, msg)
	}

	fun logFile(level: Int, tag: String, msg: String) {
		fileLogger?.logToFile(levelToChar[level], tag, msg)
	}
}