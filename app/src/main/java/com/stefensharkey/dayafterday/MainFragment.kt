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
import android.graphics.Matrix
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.stefensharkey.dayafterday.Utilities.fileDir
import com.stefensharkey.dayafterday.Utilities.getPreviousPicture
import com.stefensharkey.dayafterday.Utilities.pictureDir
import com.stefensharkey.dayafterday.Utilities.removeDirectories
import com.stefensharkey.dayafterday.Utilities.thumbnailDir
import com.stefensharkey.dayafterday.Utilities.toastLong
import com.stefensharkey.dayafterday.Utilities.toastShort
import kotlinx.android.synthetic.main.fragment_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainFragment: Fragment(), LifecycleOwner, SeekBar.OnSeekBarChangeListener {

    private lateinit var viewfinderPreview: Preview
    private lateinit var dailyPicture: DailyPicture
    private lateinit var imageCapture: ImageCapture

    var lensFacing = CameraX.LensFacing.FRONT

    // This is an arbitrary number we are using to keep tab of the permission
    // request. Where an app has multiple context for requesting permission,
    // this can help differentiate the different contexts
    private val requestCodePermissions = 10

    // This is an array of all the permission specified in the manifest
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

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
        // Transform the viewfinder as necessary upon every layout change.
        viewfinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        // Previous Picture SeekBar
        prev_picture_slider.setOnSeekBarChangeListener(this)

        // Take Picture Floating Action Button
        take_picture.setOnClickListener { takePicture(take_picture) }
        take_picture.setOnLongClickListener {
            toastShort(R.string.take_picture_desc)
            true
        }

        // Switch Camera Floating Action Button
        switch_camera.setOnClickListener { switchCamera(switch_camera) }
        switch_camera.setOnLongClickListener {
            toastShort(R.string.switch_camera_desc)
            true
        }

        // Open Gallery Floating Action Button
        open_gallery.setOnClickListener { openGallery(open_gallery) }
        open_gallery.setOnLongClickListener {
            toastShort(R.string.open_gallery_desc)
            true
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
                fragmentManager!!.beginTransaction().remove(this).commit()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context!!, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Create a shadow of the previous picture controlled by the previous picture SeekBar.
     */
    fun createPreviousPicture(prevPictureView: ImageView) {
        val prevPicture = getPreviousPicture()

        // Check if file list is empty. If yes, do nothing.
        if (prevPicture != null) {
            val files = removeDirectories(pictureDir.listFiles()!!).sortedArray()

            prevPictureView.setImageDrawable(getPreviousPicture())

            // If the last picture was taken with the front camera, flip the image horizontally to
            // match the viewfinder.
            if (files.last().nameWithoutExtension.last() == 'F') {
                prevPictureView.scaleX = -1.0F
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        prev_picture.alpha = progress.toFloat() / 100.0F
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) { }

    override fun onStopTrackingTouch(seekBar: SeekBar?) { }

    private fun startCamera() {
        // Create configuration object for the viewfinder.
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetResolution(Size(viewfinder.width, (viewfinder.width * (16.0 / 9.0)).toInt()))
        }.build()

        // Build the viewfinder
        viewfinderPreview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout.
        viewfinderPreview.setOnPreviewOutputUpdateListener {
            // To update the SurfaceTexture, we have to remove it and re-add it.
            val parent = viewfinder.parent as ViewGroup
            parent.removeView(viewfinder)
            parent.addView(viewfinder, 0)
            viewfinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture.
        val imageCaptureConfig: ImageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetResolution(previewConfig.targetResolution)
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
        }.build()

        // Build the image capture.
        imageCapture = ImageCapture(imageCaptureConfig)

        // Bind use cases to lifecycle
        CameraX.unbindAll()
        CameraX.bindToLifecycle(this, viewfinderPreview, imageCapture)

        // Build daily picture object for picture capture.
        dailyPicture = DailyPicture(imageCapture, viewfinder, prev_picture, executor)
    }

    /**
     * Unbind camera use cases for safely stopping the camera.
     */
    private fun stopCamera() {
        CameraX.unbindAll()
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewfinder.width / 2f
        val centerY = viewfinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewfinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        viewfinder.setTransform(matrix)
    }

    /**
     * Upon the take picture button being pressed, takes a picture.
     */
    private fun takePicture(view: View) {
        dailyPicture.takePicture()
    }

    /**
     * Upon the switch camera button being pressed, switch the camera.
     */
    private fun switchCamera(view: View) {
        lensFacing = if (lensFacing == CameraX.LensFacing.FRONT)
            CameraX.LensFacing.BACK
        else
            CameraX.LensFacing.FRONT

        stopCamera()
        startCamera()
    }

    /**
     * Upon the open gallery button being pressed, open the gallery.
     */
    private fun openGallery(view: View) {
        startActivity(Intent(activity, GalleryActivity::class.java))
    }
}