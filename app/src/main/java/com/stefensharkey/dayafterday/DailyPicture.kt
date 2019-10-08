package com.stefensharkey.dayafterday

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.camera.core.ImageCapture
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

class DailyPicture(private val context: Context, private val imageCapture: ImageCapture) {

    fun takePicture() {
        // Gets the current locale.
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Resources.getSystem().configuration.locales.get(0)
        else
            Resources.getSystem().configuration.locale

        // Formats the date and time as desired..
        val date = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", locale).format(Calendar.getInstance().time)

        // Gets the desired directory and file names.
        // TODO: Use MediaStore or another framework so that other applications are able to view the files.
        val dirName = "${Environment.getExternalStorageDirectory()}/DayAfterDay"
        val fileName = "DayAfterDay-$date.jpg"
        val file = File(dirName, fileName)

        Log.d(MainActivity().logTag, File(dirName).mkdir().toString())

        imageCapture.takePicture(file,
            object: ImageCapture.OnImageSavedListener {
                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    val msg = "Photo failed to save!"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    Log.e(MainActivity().logTag, msg)
                    cause?.printStackTrace()
                }

                override fun onImageSaved(file: File) {
                    // Implicit broadcasts will be ignored for devices running API
                    // level >= 24, so if you only target 24+ you can remove this statement
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        context.sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(file)))
                    }

                    // If the folder selected is an external media directory, this is unnecessary
                    // but otherwise other apps will not be able to access our images unless we
                    // scan them using [MediaScannerConnection]
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)

                    Log.d(MainActivity().logTag, "Photo saved: ${file.absoluteFile}")
                }
            }
        )

        Log.d(MainActivity().logTag, file.absolutePath)
    }
}