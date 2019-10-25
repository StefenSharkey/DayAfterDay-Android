package com.stefensharkey.dayafterday

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_photo.*

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
            (parentFragment as GalleryFragment).setMainPhoto(arguments?.getString("file_addr"))
        }

        gallery_photo.setImageDrawable(Drawable.createFromPath(arguments?.getString("file_addr")))
    }
}