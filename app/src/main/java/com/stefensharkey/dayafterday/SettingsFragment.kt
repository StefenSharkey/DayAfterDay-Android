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