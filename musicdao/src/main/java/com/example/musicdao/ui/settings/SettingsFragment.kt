package com.example.musicdao.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.musicdao.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)
    }
}
