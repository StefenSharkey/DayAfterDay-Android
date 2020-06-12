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

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialogFragment
import com.stefensharkey.dayafterday.Utilities.pictureDir
import com.stefensharkey.dayafterday.Utilities.removeDirectories
import com.stefensharkey.dayafterday.Utilities.toastLong

class GalleryActivity : AppCompatActivity(), TimelapseDialogFragment.NoticeDialogListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, GalleryFragment())
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.gallery_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_timelapse -> {
                if (!Timelapse.isRendering) {
                    if (removeDirectories(pictureDir.listFiles()!!).isNotEmpty()) {
                        val fragmentTransaction = supportFragmentManager.beginTransaction()
                        val prev = supportFragmentManager.findFragmentByTag("dialog")

                        if (prev != null) {
                            fragmentTransaction.remove(prev)
                        }

                        fragmentTransaction.addToBackStack(null)
                        val dialogFragment = TimelapseDialogFragment()
                        dialogFragment.show(fragmentTransaction, "dialog")
                    } else {
                        toastLong(R.string.timelapse_no_pictures)
                    }
                } else {
                    Toast.makeText(
                        applicationContext,
                        R.string.timelapse_already_happening,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDialogPositiveClick(dialogFragment: AppCompatDialogFragment) {
        val timelapseDialogFragment = dialogFragment as TimelapseDialogFragment

        Timelapse.createTimelapse(
            timelapseDialogFragment.properties,
            applicationContext
        )
        dialogFragment.dismiss()
    }

    override fun onDialogNegativeClick(dialogFragment: AppCompatDialogFragment) {
        dialogFragment.dismiss()
    }
}