package com.stefensharkey.dayafterday

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.camera.core.ImageCapture
import java.io.File
import android.widget.Toast
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
        val dirName = context.getExternalFilesDir(null)
        val fileName = "DayAfterDay-$date.jpg"

        val file = File(dirName, fileName)

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
                    Log.d(MainActivity().logTag, "Photo saved: ${file.absoluteFile}")
                }
            }
        )
    }
}