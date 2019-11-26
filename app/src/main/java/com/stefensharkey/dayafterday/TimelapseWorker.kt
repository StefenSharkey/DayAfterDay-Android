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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.jcodec.api.android.AndroidSequenceEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import java.io.File

class TimelapseWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    private companion object {
        const val CHANNEL_ID = "timelapse_progress"
    }

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var notificationChannel: NotificationChannel
    private val notificationBuilder = NotificationCompat.Builder(appContext, "")
    private val notificationIntent = PendingIntent.getActivity(appContext, 0, Intent(appContext, MainActivity::class.java), 0)
    private val notificationImportance = NotificationManager.IMPORTANCE_DEFAULT
    private val notificationId = 1

    private var progress = 0.0

    override fun doWork(): Result {
        createTimelapse()

        return Result.success()
    }

    private fun createTimelapse(): String {
        // Gets the desired directory and file names.
        val timelapseFile = File(Utilities.timelapseDir, "DayAfterDay-${Utilities.getTime()}.mp4")

        var out: SeekableByteChannel? = null

        try {
            out = NIOUtils.writableFileChannel(timelapseFile.absolutePath)
            val encoder = AndroidSequenceEncoder(out, Rational.R(10, 1))
            var files = Utilities.fileDir.listFiles()

            if (files == null) {
                // No pictures were found, so let the user know.
                Toast.makeText(applicationContext, "No pictures available for timelapse.", Toast.LENGTH_LONG).show()
            } else {
                // Remove the directories from the list so that they are not included in iteration.
                files = Utilities.removeDirectories(files)

                // Start the progress notification.
                createNotification(files.size)
                progress = reportProgress(0, files.size, 0)

                // For every image, encode it and update the progress notification.
                for ((counter, file) in files.sortedDescending().withIndex()) {
                    val timerStart = System.currentTimeMillis()
                    val image = (Drawable.createFromPath(file.absolutePath) as BitmapDrawable).bitmap
                    Log.d(Utilities.logTag, "Starting encoding of ${file.absolutePath}")

                    encoder.encodeImage(image)
                    Log.d(Utilities.logTag, "Ending encoding of ${file.absolutePath}")

                    reportProgress(counter + 1, files.size, System.currentTimeMillis() - timerStart)
                }
            }

            encoder.finish()
        } finally {
            NIOUtils.closeQuietly(out)

            // Show the finished notification.
            notificationBuilder.setContentTitle("Timelapse Finished")
                .setContentText("Tap to open video.")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(false)
                .setProgress(0, 0, false)
                .setContentIntent(getFinishedIntent(timelapseFile))
            notificationManager.notify(notificationId, notificationBuilder.build())

            Log.d(Utilities.logTag, "Notification shown.")
        }

        return timelapseFile.absolutePath
    }

    private fun createNotification(maxProgress: Int) {
        notificationBuilder.setContentTitle("Timelapse Rendering Progress")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(notificationIntent)
            .setShowWhen(false)
            .setOngoing(true)
            .setProgress(maxProgress, 0, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(CHANNEL_ID, "Timelapse", notificationImportance)
            notificationChannel.description = applicationContext.getString(R.string.timelapse_title)
            notificationManager.createNotificationChannel(notificationChannel)
            notificationBuilder.setChannelId(CHANNEL_ID)
        } else {
            notificationBuilder.priority = NotificationCompat.PRIORITY_DEFAULT
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun getFinishedIntent(file: File): PendingIntent {
        val uri = FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.provider", file)
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri , "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return PendingIntent.getActivity(applicationContext, 0, intent,0)
    }

    private fun reportProgress(numerator: Int, denominator: Int, timeElapsed: Long): Double {
        val progress = numerator.toDouble() / denominator.toDouble()

        notificationBuilder.setSubText("Time Remaining: ${calculateTimeRemaining(numerator, denominator, timeElapsed)}")
        notificationBuilder.setProgress(denominator, numerator, false)
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(Utilities.logTag, "Progress: $progress")

        return progress
    }

    private fun calculateTimeRemaining(numerator: Int, denominator: Int, timeElapsed: Long): String {
        return if (timeElapsed == 0L) {
            "--:--"
        } else {
            val timeRemaining = timeElapsed / 1000 * (denominator - numerator)
            val hours = if (timeRemaining / (60 * 60) > 0) {
                String.format("%02d", timeRemaining / (60 * 60)) + ":"
            } else {
                ""
            }

            val minutes = if (timeRemaining / 60 > 0) {
                String.format("%02d", timeRemaining / 60) + ":"
            } else {
                "00:"
            }

            val seconds = String.format("%02d", timeRemaining % 60)

            Log.d(Utilities.logTag, hours + minutes + seconds)

            hours + minutes + seconds
        }
    }
}