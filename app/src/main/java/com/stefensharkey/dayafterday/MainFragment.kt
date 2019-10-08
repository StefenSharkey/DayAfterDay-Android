package com.stefensharkey.dayafterday

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File

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

    val fileDir = File("${Environment.getExternalStorageDirectory()}/DayAfterDay")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onStart() {
        super.onStart()

        // Create the picture directory.
        fileDir.mkdir()

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
            Toast.makeText(context, getString(R.string.take_picture_desc), Toast.LENGTH_SHORT).show()
            true
        }

        // Switch Camera Floating Action Button
        switch_camera.setOnClickListener { switchCamera(switch_camera) }
        switch_camera.setOnLongClickListener {
            Toast.makeText(context, getString(R.string.switch_camera_desc), Toast.LENGTH_SHORT).show()
            true
        }

        // Open Gallery Floating Action Button
        open_gallery.setOnClickListener { openGallery(open_gallery) }
        open_gallery.setOnLongClickListener {
            Toast.makeText(context, getString(R.string.open_gallery_desc), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_LONG).show()
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
    fun createPreviousPicture(prevPicture: ImageView) {
        // Check if file list is empty. If yes, do nothing.
        val files = fileDir.listFiles()
        if (files != null) {
            // Sort the files and obtain the last one.
            val file = files.sortedArray().last()
            prevPicture.setImageDrawable(Drawable.createFromPath(file.absolutePath))

            // If the picture was taken with the front camera, flip the image horizontally to match the viewfinder.
            if (file.nameWithoutExtension.last()== 'F')
                prevPicture.scaleX = -1.0F
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
            setTargetResolution(Size(viewfinder.width, viewfinder.height))
            setTargetAspectRatio(Rational(viewfinder.width, viewfinder.height))
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
            setTargetAspectRatio(previewConfig.targetAspectRatio)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
        }.build()

        // Build the image capture.
        imageCapture = ImageCapture(imageCaptureConfig)

        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(this, viewfinderPreview, imageCapture)

        // Build daily picture object for picture capture.
        dailyPicture = DailyPicture(context, imageCapture, viewfinder, prev_picture)
    }

    /**
     * Unbind camera use cases for safely stopping the camera.
     */
    private fun stopCamera(preview: Preview) {
        CameraX.unbind(preview, imageCapture)
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
    fun takePicture(view: View) {
        dailyPicture.takePicture()
    }

    /**
     * Upon the switch camera button being pressed, switch the camera.
     */
    fun switchCamera(view: View) {
        lensFacing = if (lensFacing == CameraX.LensFacing.FRONT)
            CameraX.LensFacing.BACK
        else
            CameraX.LensFacing.FRONT

        stopCamera(viewfinderPreview)
        startCamera()
    }

    /**
     * Upon the open gallery button being pressed, open the gallery.
     */
    fun openGallery(view: View) {
        notYetImplemented("Gallery")
    }

    /**
     * Warn the console and user that the feature desired is not yet implemented.
     */
    fun notYetImplemented(string: String) {
        val reason = "Not yet implemented: $string"

        Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
        Log.w(MainActivity().logTag, reason)
    }
}