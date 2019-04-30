/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mozilla.components.feature.qr.QrFeature
import mozilla.components.feature.sitepermissions.SitePermissions
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import kotlin.coroutines.CoroutineContext
import androidx.fragment.app.Fragment

@SuppressWarnings("TooManyFunctions")
class SyncPairFragment : Fragment(), BackHandler {
    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()
    private lateinit var job: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).title = getString(R.string.preferences_sync)
        job = Job()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sync_pair, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        qrFeature.set(
            QrFeature(
                requireContext(),
                fragmentManager = requireFragmentManager(),
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_CAMERA_PERMISSIONS)
                },
                onScanResult = { pairingUrl ->
                    requireComponents.services.accountsAuthFeature.beginPairingAuthentication(pairingUrl)
                    view?.let {
                        (activity as HomeActivity).openToBrowser(BrowserDirection.FromSettings)
                    }
                }),
            owner = this,
            view = view
        )

        qrFeature.withFeature {
            it.scan(R.id.pair_layout)
        }

    }

    override fun onBackPressed(): Boolean {
        qrFeature.onBackPressed()
        fragmentManager?.popBackStack()
        return true
    }

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSIONS -> qrFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
            }
        }
    }
}
