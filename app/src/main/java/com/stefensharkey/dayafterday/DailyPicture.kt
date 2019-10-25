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
import android.content.Intent
import android.content.res.Resources
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.camera.core.ImageCapture
import android.widget.Toast
import androidx.camera.core.CameraX
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

class DailyPicture(
    private val context: Context?,
    private val imageCapture: ImageCapture,
    private val viewfinder: View,
    private val prev_picture: ImageView
) {

    fun takePicture() {
        // Gets the current locale.
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Resources.getSystem().configuration.locales.get(0)
        else
            Resources.getSystem().configuration.locale

        // Formats the date and time as desired..
        val date = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", locale).format(Calendar.getInstance().time)

        // Gets the lens facing descriptor.
        val lensFacing = if (MainFragment().lensFacing == CameraX.LensFacing.FRONT) "F" else "B"

        // Gets the desired directory and file names.
        val fileName = "DayAfterDay-$date-$lensFacing.jpg"
        val file = File(Utilities.fileDir, fileName)

        imageCapture.takePicture(file,
            object: ImageCapture.OnImageSavedListener {
                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    flashScreen()

                    val msg = "Photo failed to save!"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    Log.e(Utilities.logTag, msg)
                    cause?.printStackTrace()
                }

                override fun onImageSaved(file: File) {
                    flashScreen()

                    // Implicit broadcasts will be ignored for devices running API
                    // level >= 24, so if you only target 24+ you can remove this statement
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        context!!.sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(file)))
                    }

                    // If the folder selected is an external media directory, this is unnecessary
                    // but otherwise other apps will not be able to access our images unless we
                    // scan them using [MediaScannerConnection]
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)

                    MainFragment().createPreviousPicture(prev_picture)

                    Log.d(Utilities.logTag, "Photo saved: ${file.absoluteFile}")
                }
            }
        )

        Log.d(Utilities.logTag, file.absolutePath)
    }

    /**
     * Flash the screen to indicate to the user that the picture has been taken.
     */
    private fun flashScreen() {
        val animation = AlphaAnimation(0.0F, 1.0F)
        animation.duration = 250
        viewfinder.startAnimation(animation)
    }
}