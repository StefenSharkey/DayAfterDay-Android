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

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.stefensharkey.dayafterday.Utilities.getPreviousPicture
import com.stefensharkey.dayafterday.Utilities.logDebug
import com.stefensharkey.dayafterday.Utilities.pictureDir
import kotlinx.android.synthetic.main.fragment_gallery.*

class GalleryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onStart() {
        super.onStart()

        setMainPhoto(getPreviousPicture())

        val files = pictureDir.listFiles()

        // Checks if any files exist.
        if (files != null) {
            // For every thumbnail that exists, list them in newest to oldest order.
            for (file in files.sortedArrayDescending()) {
                val fragmentTransaction = childFragmentManager.beginTransaction()
                val fragment = GalleryPhotoFragment()

                // Pass the path of the fragment's photo as an argument.
                val arguments = Bundle()
                arguments.putString("file_addr", file.toUri().toString())

                logDebug(file.absolutePath)

                fragment.arguments = arguments
                fragmentTransaction.add(gallery_photos.id, fragment, tag).commit()
            }
        }
    }

    /**
     * Sets the main photo to the drawable at the given path.
     */
    fun setMainPhoto(uri: Uri) {
        setMainPhoto(Drawable.createFromPath(uri.path))
    }

    /**
     * Sets the main photo to the given drawable.
     */
    private fun setMainPhoto(drawable: Drawable?) {
        gallery_main_photo.setImageDrawable(drawable)
    }
}