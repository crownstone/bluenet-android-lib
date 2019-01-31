/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class FileLogger(context: Context) {
	private val TAG = this.javaClass.simpleName
	private val logDir = context.getExternalFilesDir(null)
	private var enabled = false
	private var logFile: File? = null
	private var logFileDate: Date? = null
	private var logFileStream: DataOutputStream? = null

	init {
		checkPermissions(context)
	}

	companion object {
		const val permissionRequired = false // Set to true when the logDir is at a place that requires write permissions.
		private val logTimestampFormat = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS", Locale.ENGLISH)
		private val fileNameTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)
		const val filenamePrefix = "log_"
		const val filenamePostfix = ".txt"
		const val minFreeSpace = (10 * 1024 * 1024L) // 1 MB, checked when before writing a line.
		const val writePermissionRequestCode = 3491
		private var hasWritePermissions = true


		fun checkPermissions(context: Context): Boolean {
			if (permissionRequired && Build.VERSION.SDK_INT >= 23) {
				val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				hasWritePermissions = (permission == PackageManager.PERMISSION_GRANTED)
			}
			else {
				hasWritePermissions = true
			}
			return hasWritePermissions
		}

		fun requestPermissions(context: Activity) {
			if (hasWritePermissions) {
				return
			}
			ActivityCompat.requestPermissions(context,
					arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
					writePermissionRequestCode)
		}

		fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
			when (requestCode) {
				writePermissionRequestCode -> {
					if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
						hasWritePermissions = true
					}
					return true
				}
			}
			return false
		}
	}

	@Synchronized
	fun enable(enable: Boolean) {
		enabled = enable
	}

	@Synchronized
	fun logToFile(level: Char, tag: String, msg: String) {
		if (enabled && checkFile()) {
			try {
				val log = "${logTimestampFormat.format(Date())} $level $tag $msg\r\n"
				logFileStream?.write(log.toByteArray())
			} catch (e: IOException) {
				Log.e(TAG, "Failed to write to log file")
				e.printStackTrace()
			}

		}
	}

	@Synchronized
	fun checkFile(): Boolean {
		if (!hasWritePermissions) {
			return false
		}
		if (logFileStream == null) {
			return createLogFile()
		}
		val freeSpace = logFile?.freeSpace ?: return false
		if (freeSpace < minFreeSpace) {
			stop()
			return false
		}
		try {
			if (logFileDate?.day != Date().day) {
				logFileStream?.close()
				return createLogFile()
			}
		} catch (e: IOException) {
			Log.e("BleLog", "Error closing logfile", e)
			return false
		}
		return true
	}

	@Synchronized
	private fun createLogFile(): Boolean {
		logFileDate = Date()
		val fileName = filenamePrefix + fileNameTimestampFormat.format(logFileDate) + filenamePostfix

		//		File path = new File(Environment.getExternalStorageDirectory().getPath() + "/" + _logDir);
		logFile = File(logDir, fileName)

		//		path.mkdirs();
		try {
			logFileStream = DataOutputStream(FileOutputStream(logFile))
		} catch (e: FileNotFoundException) {
			Log.e(TAG, "Error creating $fileName", e)
			return false
		}

		return true
	}

	@Synchronized
	private fun stop() {
		enabled = false
		if (logFileStream != null) {
			try {
				logFileStream?.close()
			} catch (e: IOException) {
				Log.e("BleLog", "Error closing logfile", e)
			}
		}
		logFileStream = null
		logFile = null
		logFileDate = null
	}

	private fun getLogFiles(): Array<File>? {
		return logDir.listFiles(FilenameFilter { dir, filename ->
			return@FilenameFilter (!filename.startsWith(filenamePrefix) || !filename.endsWith(filenamePostfix))
		})
	}

	fun clearLogFiles(): Boolean {
		if (!hasWritePermissions) {
			return false
		}
		try {
			logFileStream?.close()
		} catch (e: IOException) {
			Log.e(TAG, "Error closing logfile", e)
			return false
		}

		val files = getLogFiles()
		if (files == null) {
			return true
		}
		for (file in files) {
			if (!file.delete()) {
				return false
			}
		}
		return true
	}
}