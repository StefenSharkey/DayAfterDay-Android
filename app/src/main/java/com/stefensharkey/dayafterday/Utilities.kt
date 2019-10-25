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

import android.graphics.drawable.Drawable
import android.os.Environment
import java.io.File

class Utilities {

    companion object {

        val logTag = "Day After Day"
        val fileDir = File("${Environment.getExternalStorageDirectory()}/DayAfterDay")

        /**
         * Returns the most recent photo taken with this app.
         */
        fun getPreviousPicture(): Drawable? {
            // Check if file list is empty. If yes, do nothing.
            val files = fileDir.listFiles()
            if (files != null) {
                // Sort the files and return the last one.
                return Drawable.createFromPath(files.sortedArray().last().absolutePath)!!
            }

            return null
        }
    }
}