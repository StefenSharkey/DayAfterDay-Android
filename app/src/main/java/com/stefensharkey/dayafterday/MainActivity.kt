package com.stefensharkey.dayafterday

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), LifecycleOwner {

    val logTag = "Day After Day"

    lateinit var viewfinderPreview: Preview
    lateinit var dailyPicture: DailyPicture
    lateinit var imageCapture: ImageCapture

    private var lensFacing = CameraX.LensFacing.FRONT

    // This is an arbitrary number we are using to keep tab of the permission
    // request. Where an app has multiple context for requesting permission,
    // this can help differentiate the different contexts
    private val requestCodePermissions = 10

    // This is an array of all the permission specified in the manifest
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // If all the permissions required are granted, show the camera; otherwise, request the permissions.
        if (allPermissionsGranted()) {
            viewfinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, requestCodePermissions)
        }

        // Transform the viewfinder as necessary upon every layout change.
        viewfinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
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
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

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
        dailyPicture = DailyPicture(this, imageCapture)
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

        Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
        Log.w(logTag, reason)
    }
}
