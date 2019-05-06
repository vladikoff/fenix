/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.feature.qr.QrFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.requireComponents
import kotlin.coroutines.CoroutineContext

class SyncFragment : PreferenceFragmentCompat(), CoroutineScope {

    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).title = getString(R.string.preferences_sync)
        (activity as AppCompatActivity).supportActionBar?.show()
        job = Job()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sync_preferences, rootKey)

        val preferenceSignIn =
            findPreference<Preference>(context!!.getPreferenceKey(R.string.pref_key_sync_sign_in))
        val preferenceNewAccount =
            findPreference<Preference>(context!!.getPreferenceKey(R.string.pref_key_sync_create_account))
        val preferencePairSignIn =
            findPreference<Preference>(context!!.getPreferenceKey(R.string.pref_key_sync_pair_sign_in))
        preferenceSignIn?.onPreferenceClickListener = getClickListenerForSignIn()
        preferenceNewAccount?.onPreferenceClickListener = getClickListenerForSignIn()
        preferencePairSignIn?.onPreferenceClickListener = getClickListenerForPairSignIn()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun getClickListenerForSignIn(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            requireComponents.services.accountsAuthFeature.beginAuthentication()
            // TODO The sign-in web content populates session history,
            // so pressing "back" after signing in won't take us back into the settings screen, but rather up the
            // session history stack.
            // We could auto-close this tab once we get to the end of the authentication process?
            // Via an interceptor, perhaps.
            view?.let {
                (activity as HomeActivity).openToBrowser(null, BrowserDirection.FromSettings)
            }
            true
        }
    }

    private fun getClickListenerForPairSignIn(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            showQuickSettingsDialog()

            true
        }
    }

    private fun showQuickSettingsDialog() {
        launch {
            launch(Dispatchers.Main) {
                val quickSettingsSheet = SyncPairDialogFragment.newInstance()
                quickSettingsSheet.show(
                    requireFragmentManager(),
                    SyncPairDialogFragment.FRAGMENT_TAG
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSIONS -> qrFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1
    }
}
