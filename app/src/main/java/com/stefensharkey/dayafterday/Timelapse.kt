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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.stefensharkey.dayafterday.Utilities.getNotificationId
import com.stefensharkey.dayafterday.Utilities.getString
import com.stefensharkey.dayafterday.Utilities.getTime
import com.stefensharkey.dayafterday.Utilities.logDebug
import com.stefensharkey.dayafterday.Utilities.logVerbose
import com.stefensharkey.dayafterday.Utilities.pictureDir
import com.stefensharkey.dayafterday.Utilities.removeDirectories
import com.stefensharkey.dayafterday.Utilities.timelapseDir
import org.jcodec.api.android.AndroidSequenceEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import java.io.File
import java.nio.channels.ClosedByInterruptException
import kotlin.concurrent.thread

object Timelapse {

    private val notificationManager = MainActivity.applicationContext()
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var notificationChannel: NotificationChannel
    private val notificationBuilder =
        NotificationCompat.Builder(MainActivity.applicationContext(), "")
    private val notificationIntent =
        PendingIntent.getActivity(
            MainActivity.applicationContext(),
            0,
            Intent(MainActivity.applicationContext(), MainActivity::class.java),
            0
        )
    private var notificationId = 0

    private var progress = 0.0

    private var timelapseThread: Thread? = null

    fun createTimelapse(framesPerSecond: Int, openWhenFinished: Boolean) {
        notificationId = getNotificationId()

        if (timelapseThread == null) {
            timelapseThread = thread(start = true) {
                var canceled = false

                try {
                    timelapseDir.mkdir()

                    // Gets the desired directory and file names.
                    val timelapseFile = File(timelapseDir, "DayAfterDay-${getTime()}.mp4")

                    val files = removeDirectories(pictureDir.listFiles() ?: return@thread)
                    var out: SeekableByteChannel? = null

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
                            logVerbose("Starting encoding of ${file.absolutePath}")

                            encoder.encodeImage(image)
                            logVerbose("Ending encoding of ${file.absolutePath}")

                            reportProgress(
                                counter + 1,
                                files.size,
                                System.currentTimeMillis() - timerStart
                            )
                        }

                        encoder.finish()
                    } catch (e: ClosedByInterruptException) {
                        canceled = true
                        logDebug("Canceled timelapse generation.")
                    } finally {
                        NIOUtils.closeQuietly(out)

                        if (!canceled) {
                            val uri =
                                FileProvider.getUriForFile(
                                    MainActivity.applicationContext(),
                                    "${BuildConfig.APPLICATION_ID}.provider",
                                    timelapseFile
                                )

                            val openIntent = getOpenPendingIntent(uri)

                            // Show the finished notification.
                            notificationBuilder
                                .setContentTitle(getString(R.string.timelapse_finished))
                                .setContentText(getString(R.string.timelapse_tap_to_open))
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setOngoing(false)
                                .setProgress(0, 0, false)
                                .setContentIntent(openIntent)
                                .setSubText(null)
                                .setAutoCancel(true)
                                .addAction(
                                    android.R.drawable.ic_menu_share,
                                    getString(R.string.share),
                                    getSharePendingIntent(uri)
                                )
                            notificationManager.notify(notificationId, notificationBuilder.build())

                            if (openWhenFinished) {
                                openIntent.send()
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    logDebug("Canceled timelapse generation.")
                }

                timelapseThread = null
            }
        } else {
            Toast.makeText(
                MainActivity.applicationContext(),
                R.string.timelapse_already_happening,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopTimelapse(notificationId: Int) {
        if (notificationId == -1) {
            notificationManager.cancelAll()
        } else {
            notificationManager.cancel(notificationId)
        }

        timelapseThread?.interrupt()
        logDebug("Trying to cancel timelapse thread.")
    }

    private fun createNotification(maxProgress: Int) {
        val pendingIntentCancel = PendingIntent.getBroadcast(
            MainActivity.applicationContext(),
            0,
            Intent(MainActivity.applicationContext(), ActionReceiver::class.java).apply {
                action = Intent.ACTION_CLOSE_SYSTEM_DIALOGS
                putExtra("action", "cancel")
                putExtra("notificationId", notificationId)
            },
            0
        )

        notificationBuilder
            .setContentTitle(getString(R.string.timelapse_progress))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(notificationIntent)
            .setShowWhen(false)
            .setOngoing(true)
            .setProgress(maxProgress, 0, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.cancel),
                pendingIntentCancel
            )

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

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun getOpenPendingIntent(uri: Uri): PendingIntent {
        return PendingIntent.getActivity(
            MainActivity.applicationContext(),
            0,
            Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            0
        )
    }

    private fun getSharePendingIntent(uri: Uri): PendingIntent {
        return PendingIntent.getActivity(
            MainActivity.applicationContext(),
            0,
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "video/*"
            },
            0
        )
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

        logVerbose("Progress: $progress")

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
            val timeRemaining: Long = timeElapsed / 1000 * (denominator - numerator)
            val hours: String = if (timeRemaining / (60 * 60) > 0) {
                String.format("%02d", timeRemaining / (60 * 60)) + ":"
            } else {
                ""
            }

            val minutes: String = if (timeRemaining / 60 > 0) {
                String.format("%02d", timeRemaining / 60) + ":"
            } else {
                "00:"
            }

            val seconds: String = String.format("%02d", timeRemaining % 60)

            " $hours$minutes$seconds"
        }
    }

    class ActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            StringBuilder().apply {
//                append("Action: ${intent.action}\n")
//                append("URI: ${intent.toUri(Intent.URI_INTENT_SCHEME)}\n")
//                append("Extras: ${intent.extras}\n")
//                toString().also { log ->
//                    logDebug(log)
//                    Toast.makeText(context, log, Toast.LENGTH_LONG).show()
//                }
//            }
            val action = intent.getStringExtra("action")

            if (action == "cancel") {
                stopTimelapse(intent.getIntExtra("notificationId", -1))
            }
        }
    }
}