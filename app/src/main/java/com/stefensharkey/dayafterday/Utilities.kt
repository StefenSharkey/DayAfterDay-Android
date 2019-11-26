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

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Utilities {

    companion object {

        val logTag = "Day After Day"
        val fileDir = File("${Environment.getExternalStorageDirectory()}/DayAfterDay")
        val thumbnailDir = File(fileDir, "thumbnails")
        val timelapseDir = File(fileDir, "timelapses")

        lateinit var timelapseRenderId: UUID

        /**
         * Returns the most recent photo taken with this app.
         */
        fun getPreviousPicture(): Drawable? {
            // Check if file list is empty. If yes, do nothing.
            val files = fileDir.listFiles()
            if (files != null) {
                // Sort the files and return the last one.
                return Drawable.createFromPath(Utilities.removeDirectories(files.sortedArray()).last().absolutePath)!!
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

        fun getThumbnail(path: String): String {
            val file = File(path)

            // The new name is created by inserting into the old name.
            val newName = "${file.nameWithoutExtension}-thumb.${file.extension}"
            return File(thumbnailDir, newName).absolutePath
        }

        fun getFullPhoto(path: String): String {
            val file = File(path)

            val oldName = file.nameWithoutExtension

            // The new name is created by removing the length of the thumbnail suffix from the old
            // name.
            val newName = oldName.substring(0, oldName.length - 6) + "." + file.extension

            return File(fileDir, newName).absolutePath
        }

        fun getTime(): String {
            // Gets the current locale.
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Resources.getSystem().configuration.locales.get(0)
            else
                Resources.getSystem().configuration.locale

            // Formats the date and time as desired.
            return SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", locale).format(Calendar.getInstance().time)
        }
    }
}