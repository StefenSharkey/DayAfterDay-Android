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

    lateinit var preview: Preview
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

        if (allPermissionsGranted()) {
            viewfinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, requestCodePermissions)
        }

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
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        // Create configuration object for the viewfinder
         val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)

            setTargetResolution(Size(viewfinder.width, viewfinder.height))
            setTargetAspectRatio(Rational(viewfinder.width, viewfinder.height))
        }.build()

        // Build the viewfinder
        preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout.
        preview.setOnPreviewOutputUpdateListener {
            // To update the SurfaceTexture, we have to remove it and re-add it.
            val parent = viewfinder.parent as ViewGroup

            parent.removeView(viewfinder)
            parent.addView(viewfinder, 0)

            viewfinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig: ImageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetAspectRatio(previewConfig.targetAspectRatio)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.getCameraWithLensFacing(lensFacing)
        CameraX.bindToLifecycle(this, preview, imageCapture)

        dailyPicture = DailyPicture(this, imageCapture)
    }

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

    fun takePicture(view: View) {
        dailyPicture.takePicture()
    }

    fun switchCamera(view: View) {
        if (lensFacing == CameraX.LensFacing.FRONT)
            lensFacing = CameraX.LensFacing.BACK
        else
            lensFacing = CameraX.LensFacing.FRONT

        stopCamera(preview)
        startCamera()
    }

    fun openGallery(view: View) {
        notYetImplemented("Gallery")
    }

    fun notYetImplemented(string: String) {
        val reason = "Not yet implemented: $string"

        Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
        Log.w(logTag, reason)
    }
}
