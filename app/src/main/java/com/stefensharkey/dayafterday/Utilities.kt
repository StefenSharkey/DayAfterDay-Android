/*
 * Copyright 2019 Stefen Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stefensharkey.dayafterday

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger

object Utilities {

    lateinit var context: Context

    private const val logTag = "Day After Day"
    val fileDir: File = File("${Environment.getExternalStorageDirectory()}/DayAfterDay")
    val pictureDir: File = File(fileDir, "pictures")
    val timelapseDir: File = File(fileDir, "timelapses")
    val tempDir: File by lazy { File(context.filesDir, "temp") }

    private val notificationId = AtomicInteger(0)

    /**
     * Returns the most recent photo taken with this app.
     */
    fun getPreviousPicture(): Drawable? {
        // Check if file list is empty. If yes, do nothing.
        var files = pictureDir.listFiles()

        if (files != null) {
            files = removeDirectories(files)

            if (files.isNotEmpty()) {
                // Sort the files and return the last one.
                return Drawable.createFromPath(removeDirectories(files.sortedArray()).last().absolutePath)!!
            }
        }

        return null
    }

    /**
     * Remove directories from the given file list.
     */
    fun removeDirectories(files: Array<File>): Array<File> {
        val fileList = files.toMutableList()
        val iterator = fileList.iterator()

        while (iterator.hasNext()) {
            val file = iterator.next()

            if (file.isDirectory) {
                iterator.remove()
            }
        }

        return fileList.toTypedArray()
    }

    fun getNotificationId(): Int {
        return notificationId.incrementAndGet()
    }

    fun getTime(): String {
        // Gets the current locale.
        val locale = Resources.getSystem().configuration.locales.get(0)

        // Formats the date and time as desired.
        return SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", locale).format(Calendar.getInstance().time)
    }

    fun getString(resId: Int): String {
        return context.getString(resId)
    }

    fun getString(resId: Int, vararg args: Any): String {
        return context.getString(resId, args)
    }

    fun logDebug(obj: Any): Int {
        return Log.d(logTag, obj.toString())
    }

    fun logError(obj: Any): Int {
        return Log.e(logTag, obj.toString())
    }

    fun logError(message: String, exception: Exception): Int {
        return Log.e(logTag, message, exception)
    }

    fun logInfo(obj: Any): Int {
        return Log.i(logTag, obj.toString())
    }

    fun logVerbose(obj: Any): Int {
        return Log.v(logTag, obj.toString())
    }

    fun logWarning(obj: Any): Int {
        return Log.w(logTag, obj.toString())
    }

    fun logWtf(obj: Any): Int {
        return Log.wtf(logTag, obj.toString())
    }

    fun toastLong(resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
    }

    fun toastLong(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun toastShort(resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    fun toastShort(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    /**
     * Warn the console and user that the feature desired is not yet implemented.
     */
    fun notYetImplemented(string: String) {
        val reason = "Not yet implemented: $string"

        toastLong(reason)
        logWarning(reason)
    }
}