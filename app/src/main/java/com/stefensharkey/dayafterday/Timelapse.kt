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
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.arthenica.mobileffmpeg.FFmpeg
import com.stefensharkey.dayafterday.Utilities.getNotificationId
import com.stefensharkey.dayafterday.Utilities.getString
import com.stefensharkey.dayafterday.Utilities.getTime
import com.stefensharkey.dayafterday.Utilities.logDebug
import com.stefensharkey.dayafterday.Utilities.logError
import com.stefensharkey.dayafterday.Utilities.pictureDir
import com.stefensharkey.dayafterday.Utilities.removeDirectories
import com.stefensharkey.dayafterday.Utilities.tempDir
import com.stefensharkey.dayafterday.Utilities.timelapseDir
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter

object Timelapse {

    private lateinit var context: Context

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var notificationBuilder: NotificationCompat.Builder

    var isRendering: Boolean = false
        private set

    fun createTimelapse(properties: HashMap<String, Any>, context: Context) {
        isRendering = true
        this.context = context

        val notificationId = getNotificationId()

        try {
            tempDir.mkdir()
            timelapseDir.mkdir()

            // Gets the desired directory and file names.
            val tempTimelapseFile = File(tempDir, "DayAfterDay-${getTime()}.mp4")
            val timelapseFile = File(timelapseDir, "DayAfterDay-${getTime()}.mp4")

            val files = removeDirectories(pictureDir.listFiles() ?: return).sorted()

            // Create file containing list of files.
            writeToFile("ffmpeg_list.txt", files.joinToString("\n file ", "file "))

            // Construct the FFmpeg command.
            val ffmpegMakeTemp = "-f concat " +
                    "-r ${properties[getString(R.string.frames_per_second)]} " +
                    "-safe 0 " +
                    "-i ${context.getFileStreamPath("ffmpeg_list.txt").absolutePath} " +
                    "-c:v libx264 " +
                    tempTimelapseFile.absolutePath
            val ffmpegMakeFinal = "-i ${tempTimelapseFile.absolutePath} " +
                    "-c copy " +
                    "-metadata:s:v:0 rotate=90 " +
                    timelapseFile.absolutePath

            initNotification()

            val applicationIntent = PendingIntent.getActivity(
                Timelapse.context,
                0,
                Intent(Timelapse.context, MainActivity::class.java),
                0
            )

            showStartNotification(notificationId, applicationIntent)

            // Execute the command and observe its return code.
            if (FFmpeg.execute(ffmpegMakeTemp) == 0) {
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${BuildConfig.APPLICATION_ID}.provider",
                        timelapseFile
                    )
                val openIntent = getOpenTimelapsePendingIntent(uri)
                val shareIntent = getShareTimelapsePendingIntent(uri)

                if (FFmpeg.execute(ffmpegMakeFinal) == 0) {
                    showSuccessNotification(notificationId, openIntent, shareIntent)

                    if (properties[getString(R.string.open_when_finished)] as Boolean) {
                        openIntent.send()
                    }
                } else {
                    showFailedNotification(notificationId, applicationIntent)
                }
            }
        } catch (e: InterruptedException) {
            logDebug("Canceled timelapse generation.")
        }

        isRendering = false
    }

    private fun initNotification() {
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = NotificationCompat.Builder(context, "")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(
                    getString(R.string.timelapse_notification_channel_id),
                    getString(R.string.timelapse_settings),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            notificationChannel.description = getString(R.string.timelapse_title)
            notificationManager.createNotificationChannel(notificationChannel)
            notificationBuilder.setChannelId(getString(R.string.timelapse_notification_channel_id))
        } else {
            notificationBuilder.priority = NotificationCompat.PRIORITY_DEFAULT
        }
    }

    private fun showStartNotification(notificationId: Int, openIntent: PendingIntent) {
        notificationBuilder
            .setContentTitle(getString(R.string.timelapse_rendering))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .setShowWhen(false)
            .setOngoing(true)
            .setProgress(0, 0, true)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun showSuccessNotification(
        notificationId: Int,
        openIntent: PendingIntent,
        shareIntent: PendingIntent
    ) {
        notificationBuilder
            .setContentTitle(getString(R.string.timelapse_finished))
            .setContentText(getString(R.string.timelapse_tap_to_open))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .setContentIntent(openIntent)
            .setSubText(null)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_share,
                getString(R.string.share),
                shareIntent
            )

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun showFailedNotification(notificationId: Int, openIntent: PendingIntent) {
        notificationBuilder
            .setContentTitle(getString(R.string.timelapsed_failed))
            .setContentText(null)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .setContentIntent(openIntent)
            .setSubText(null)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun getOpenTimelapsePendingIntent(uri: Uri): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            0
        )
    }

    private fun getShareTimelapsePendingIntent(uri: Uri): PendingIntent {
        return PendingIntent.getActivity(
            context,
            1,
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "video/*"
            },
            0
        )
    }

    private fun writeToFile(fileName: String, data: String) {
        try {
            OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE)).apply {
                write(data)
                close()
            }
        } catch (e: IOException) {
            logError("File write failed.", e)
        }
    }
}