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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.SeekBar
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.stefensharkey.dayafterday.Utilities.fileDir
import com.stefensharkey.dayafterday.Utilities.getPreviousPicture
import com.stefensharkey.dayafterday.Utilities.logDebug
import com.stefensharkey.dayafterday.Utilities.logError
import com.stefensharkey.dayafterday.Utilities.pictureDir
import com.stefensharkey.dayafterday.Utilities.thumbnailDir
import com.stefensharkey.dayafterday.Utilities.toastLong
import com.stefensharkey.dayafterday.Utilities.toastShort
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.floor

class MainFragment : Fragment(), LifecycleOwner, SeekBar.OnSeekBarChangeListener {

    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT

    // This is an arbitrary number we are using to keep tab of the permission
    // request. Where an app has multiple context for requesting permission,
    // this can help differentiate the different contexts
    private val requestCodePermissions = 10

    // This is an array of all the permission specified in the manifest
    private val requiredPermissions =
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private lateinit var executor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onStart() {
        super.onStart()

        executor = Executors.newSingleThreadExecutor()

        // Create the necessary directories.
        fileDir.mkdir()
        pictureDir.mkdir()
        thumbnailDir.mkdir()

        // If all the permissions required are granted, show the camera; otherwise, request the permissions.
        if (allPermissionsGranted()) {
            viewfinder.post { startCamera() }
        } else {
            requestPermissions(requiredPermissions, requestCodePermissions)
        }

        createListeners()

        prev_picture.alpha = prev_picture_slider.progress.toFloat() / 100.0F
        createPreviousPicture(prev_picture)
    }

    private fun createListeners() {
        // Previous Picture SeekBar
        prev_picture_slider.setOnSeekBarChangeListener(this)

        // Take Picture Floating Action Button
        take_picture.apply {
            setOnClickListener { takePicture() }
            setOnLongClickListener {
                toastShort(R.string.take_picture_desc)
                true
            }
        }

        // Switch Camera Floating Action Button
        switch_camera.apply {
            setOnClickListener { switchCamera() }
            setOnLongClickListener {
                toastShort(R.string.switch_camera_desc)
                true
            }
        }

        // Open Gallery Floating Action Button
        open_gallery.apply {
            setOnClickListener { openGallery() }
            setOnLongClickListener {
                toastShort(R.string.open_gallery_desc)
                true
            }
        }
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == requestCodePermissions) {
            if (allPermissionsGranted()) {
                viewfinder.post { startCamera() }
            } else {
                toastLong(R.string.permissions_failed)
                parentFragmentManager.beginTransaction().remove(this).commit()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(
            requireContext(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Create a shadow of the previous picture controlled by the previous picture SeekBar.
     */
    fun createPreviousPicture(prevPictureView: ImageView) {
        val prevPicture = getPreviousPicture()

        // Check if file list is empty. If yes, do nothing.
        if (prevPicture != null) {
            prevPictureView.setImageDrawable(getPreviousPicture())
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        prev_picture.alpha = progress.toFloat() / 100.0F
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Preview
            preview = Preview.Builder().build()

            val rotation = viewfinder.display.rotation
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            // Preview
            preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            try {
                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(viewfinder.createSurfaceProvider())
            } catch (exc: Exception) {
                logError("Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Unbind camera use cases for safely stopping the camera.
     */
    private fun stopCamera() {
        CameraX.unbindAll()
    }

    /**
     * Upon the switch camera button being pressed, switch the camera.
     */
    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK
        else
            CameraSelector.LENS_FACING_FRONT

        logDebug("Switched camera to $lensFacing")

        stopCamera()
        startCamera()
    }

    /**
     * Upon the open gallery button being pressed, open the gallery.
     */
    private fun openGallery() {
        startActivity(Intent(activity, GalleryActivity::class.java))
    }

    /**
     * Upon the take picture button being pressed, takes a picture.
     */
    private fun takePicture() {
        // Gets the lens facing descriptor.
        val isFacingFront: Boolean = lensFacing == CameraSelector.LENS_FACING_FRONT
        val lensFacingStr = if (isFacingFront) "F" else "B"

        logDebug("Lens facing: $lensFacing")

        // Gets the desired directory and file names.
        val file = File(pictureDir, "DayAfterDay-${Utilities.getTime()}-$lensFacingStr.jpg")

        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = isFacingFront
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file)
            .setMetadata(metadata)
            .build()

        imageCapture?.takePicture(outputOptions, executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(e: ImageCaptureException) {
                    flashScreen()

                    toastLong(R.string.picture_failed)
                    logError(Utilities.getString(R.string.picture_failed), e)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(file)
                    flashScreen()

                    // Implicit broadcasts will be ignored for devices running API
                    // level >= 24, so if you only target 24+ you can remove this statement
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        MainActivity.applicationContext()
                            .sendBroadcast(
                                Intent(
                                    android.hardware.Camera.ACTION_NEW_PICTURE,
                                    savedUri
                                )
                            )
                    }

                    // If the folder selected is an external media directory, this is unnecessary
                    // but otherwise other apps will not be able to access our images unless we
                    // scan them using [MediaScannerConnection]
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(savedUri.toFile().extension)
                    MediaScannerConnection.scanFile(
                        MainActivity.applicationContext(),
                        arrayOf(savedUri.toFile().absolutePath),
                        arrayOf(mimeType)
                    ) { _, uri ->
                        logDebug("Image capture scanned into media store: $uri")
                    }

                    Handler(Looper.getMainLooper()).post {
                        MainFragment().createPreviousPicture(prev_picture)
                    }

                    // Save a thumbnail for the gallery.
                    saveThumbnail(file)

                    logDebug("Photo saved: $savedUri")
                }
            }
        )

        logDebug(file.absolutePath)
    }

    fun saveThumbnail(file: File) {
        val thumbnail =
            getThumbnail(Drawable.createFromPath(file.absolutePath) ?: return).bitmap
        val thumbnailFile =
            File(thumbnailDir, "${file.nameWithoutExtension}-thumb.${file.extension}")

        try {
            val stream: OutputStream = FileOutputStream(thumbnailFile)

            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun getThumbnail(originalImage: Drawable): BitmapDrawable {
        val originalBitmap = (originalImage as BitmapDrawable).bitmap

        var width = originalBitmap.width.toFloat()
        var height = originalBitmap.height.toFloat()
        val maxSize = 100.0F

        if (width > maxSize || height > maxSize) {
            if (width > height) {
                height = floor((height / width) * maxSize)
                width = maxSize
            } else {
                width = floor((width / height) * maxSize)
                height = maxSize
            }
        }

        val scaledBitmap =
            Bitmap.createScaledBitmap(originalBitmap, width.toInt(), height.toInt(), false)

        return BitmapDrawable(MainActivity.applicationContext().resources, scaledBitmap)
    }

    /**
     * Flash the screen to indicate to the user that the picture has been taken.
     */
    internal fun flashScreen() {
        val animation = AlphaAnimation(0.0F, 1.0F)
        animation.duration = 250
        viewfinder.startAnimation(animation)
    }
}