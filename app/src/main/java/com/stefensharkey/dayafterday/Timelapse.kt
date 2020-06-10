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
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.stefensharkey.dayafterday.Utilities.getString
import com.stefensharkey.dayafterday.Utilities.getTime
import com.stefensharkey.dayafterday.Utilities.logDebug
import com.stefensharkey.dayafterday.Utilities.pictureDir
import com.stefensharkey.dayafterday.Utilities.removeDirectories
import com.stefensharkey.dayafterday.Utilities.timelapseDir
import org.jcodec.api.android.AndroidSequenceEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import java.io.File

class Timelapse(private val framesPerSecond: Int, private val openWhenFinished: Boolean) :
    Runnable {

    private var isRendering = false

    private val channelId = "timelapse_progress"

    private val notificationManager = MainActivity.applicationContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var notificationChannel: NotificationChannel
    private val notificationBuilder = NotificationCompat.Builder(MainActivity.applicationContext(), "")
    private val notificationIntent =
        PendingIntent.getActivity(MainActivity.applicationContext(),
            0,
            Intent(MainActivity.applicationContext(), MainActivity::class.java),
            0)
    private val notificationId = 1

    private var progress = 0.0

    override fun run() {
        createTimelapse()
    }

    private fun createTimelapse() {
        if (!isRendering) {
            isRendering = true
            timelapseDir.mkdir()

            // Gets the desired directory and file names.
            val timelapseFile =
                File(timelapseDir, "DayAfterDay-${getTime()}.mp4")

            var out: SeekableByteChannel? = null

            val files = removeDirectories(pictureDir.listFiles() ?: return)

            try {
                out = NIOUtils.writableFileChannel(timelapseFile.absolutePath)
                val encoder = AndroidSequenceEncoder(out, Rational.R(framesPerSecond, 1))

                // Start the progress notification.
                createNotification(files.size)
                progress = reportProgress(0, files.size, 0)

                // For every image, encode it and update the progress notification.
                for ((counter, file) in files.sortedDescending().withIndex()) {
                    val timerStart = System.currentTimeMillis()
                    val image =
                        (Drawable.createFromPath(file.absolutePath) as BitmapDrawable).bitmap
                    logDebug("Starting encoding of ${file.absolutePath}")

                    encoder.encodeImage(image)
                    logDebug("Ending encoding of ${file.absolutePath}")

                    reportProgress(
                        counter + 1,
                        files.size,
                        System.currentTimeMillis() - timerStart
                    )
                }

                encoder.finish()
            } finally {
                NIOUtils.closeQuietly(out)

                val videoIntent = getFinishedIntent(timelapseFile)

                // Show the finished notification.
                notificationBuilder
                    .setContentTitle(getString(R.string.timelapse_finished))
                    .setContentText(getString(R.string.timelapse_tap_to_open))
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .setProgress(0, 0, false)
                    .setContentIntent(videoIntent)
                    .setSubText(null)
                    .setAutoCancel(true)
                notificationManager.notify(notificationId, notificationBuilder.build())

                if (openWhenFinished) {
                    videoIntent.send()
                }

                logDebug("Notification shown.")
            }

            isRendering = false
        }
    }

    private fun createNotification(maxProgress: Int) {
        notificationBuilder
            .setContentTitle(getString(R.string.timelapse_progress))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(notificationIntent)
            .setShowWhen(false)
            .setOngoing(true)
            .setProgress(maxProgress, 0, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(
                    channelId,
                    getString(R.string.timelapse_settings),
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.description = getString(R.string.timelapse_title)
            notificationManager.createNotificationChannel(notificationChannel)
            notificationBuilder.setChannelId(channelId)
        } else {
            notificationBuilder.priority = NotificationCompat.PRIORITY_DEFAULT
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun getFinishedIntent(file: File): PendingIntent {
        val uri =
            FileProvider.getUriForFile(MainActivity.applicationContext(),
                "${BuildConfig.APPLICATION_ID}.provider",
                file)
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri , "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return PendingIntent.getActivity(MainActivity.applicationContext(), 0, intent,0)
    }

    private fun reportProgress(numerator: Int, denominator: Int, timeElapsed: Long): Double {
        val progress = numerator.toDouble() / denominator.toDouble()
        val timeRemaining = getString(R.string.timelapse_time_remaining) +
                calculateTimeRemaining(numerator, denominator, timeElapsed)

        notificationBuilder.setSubText(timeRemaining)

        if (timeElapsed == 0L) {
            notificationBuilder.setProgress(denominator, numerator, true)
        } else {
            notificationBuilder.setProgress(denominator, numerator, false)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())

        logDebug("Progress: $progress")

        return progress
    }

    private fun calculateTimeRemaining(
        numerator: Int,
        denominator: Int,
        timeElapsed: Long
    ): String {
        return if (timeElapsed == 0L) {
            " --:--"
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

            " $hours$minutes$seconds"
        }
    }
}