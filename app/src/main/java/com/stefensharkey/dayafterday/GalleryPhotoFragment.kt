package com.stefensharkey.dayafterday

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ScaleDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_photo.*
import java.lang.Math.floor
import java.lang.Math.max
import kotlin.math.floor

class GalleryPhotoFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onStart() {
        super.onStart()

        gallery_photo_layout.setOnClickListener {
            Log.d(Utilities.logTag, "onClick(): ${arguments?.getString("file_addr")}")
            (parentFragment as GalleryFragment).setMainPhotoFromThumbnail(arguments?.getString("file_addr")!!)
        }

        gallery_photo.setImageDrawable(Drawable.createFromPath(arguments?.getString("file_addr")))
    }

//    fun getThumbnail(originalImage: Drawable): BitmapDrawable {
//        val originalBitmap = (originalImage as BitmapDrawable).bitmap
//        originalBitmap.scal
//        val originalWidth = originalBitmap.width
//        val originalHeight = originalBitmap.height
//        val originalRatio = originalWidth.toFloat() / originalHeight.toFloat()
//
//        var finalWidth = 100
//        var finalHeight = 100
//        val finalRatio = 1.0F
//
//        if (finalRatio > originalRatio) {
//            finalWidth = (finalHeight.toFloat() * finalRatio).toInt()
//        } else {
//            finalHeight = (finalWidth.toFloat() / finalRatio).toInt()
//        }
//
//        val resized = Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, false)
//
//        Log.d(Utilities.logTag, "Original Width:  $originalWidth")
//        Log.d(Utilities.logTag, "Final Width:     $finalWidth")
//        Log.d(Utilities.logTag, "Original Height: $originalHeight")
//        Log.d(Utilities.logTag, "Final Height:    $finalHeight")
//
//        return BitmapDrawable(resources, resized)
//    }
}
