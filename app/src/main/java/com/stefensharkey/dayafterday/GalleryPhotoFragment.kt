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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_photo.*

class GalleryPhotoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onStart() {
        super.onStart()

        val uri = Uri.parse(arguments?.getString("file_addr"))

        gallery_photo_layout.setOnClickListener {
            (parentFragment as GalleryFragment).setMainPhoto(uri)
        }

        Thread(Runnable {
            gallery_photo.post {
                Glide.with(this)
                    .load(uri)
                    .fitCenter()
                    .into(gallery_photo)

                gallery_photo_loading.visibility = View.GONE
            }
        }).start()
    }
}
