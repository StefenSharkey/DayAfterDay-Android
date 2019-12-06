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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment: PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference!!.key) {
            "preference_send_feedback" -> {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.author_email)))
                    putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} Feedback")
                }

                if (intent.resolveActivity(activity!!.packageManager) != null) {
                    startActivity(intent)
                }

                true
            }
            "preference_about" -> {
                // TODO: Allow for reverse traversal of preference screens.
                setPreferencesFromResource(R.xml.preferences_about, preferenceScreen.key)
                preferenceManager.findPreference<Preference>("preference_version")?.summary = BuildConfig.VERSION_NAME
                true
            }
            "preference_author" -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/StefenSharkey")))
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}