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
import androidx.navigation.Navigation
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

private const val KEY_SITE_PERMISSIONS = "KEY_SITE_PERMISSIONS"

@SuppressWarnings("TooManyFunctions")
class SyncPairDialogFragment : BottomSheetDialogFragment(), CoroutineScope, BackHandler {

    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()
    private val safeArguments get() = requireNotNull(arguments)
    private lateinit var job: Job

    var sitePermissions: SitePermissions?
        get() = safeArguments.getParcelable(KEY_SITE_PERMISSIONS)
        set(value) {
            safeArguments.putParcelable(KEY_SITE_PERMISSIONS, value)
        }

    override val coroutineContext: CoroutineContext get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).title = getString(R.string.preferences_sync)
        job = Job()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sync_pair_dialog_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        qrFeature.set(
            QrFeature(
                requireContext(),
                fragmentManager = requireFragmentManager(),
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, SyncPairDialogFragment.REQUEST_CODE_CAMERA_PERMISSIONS)
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

        val openCamera = view.findViewById(R.id.pair_open_camera) as Button
        openCamera.setOnClickListener(View.OnClickListener {
            val directions = SyncPairDialogFragmentDirections.actionSyncPairDialogFragmentToSyncPairFragment()
            Navigation.findNavController(view!!).navigate(directions)

            //dismiss();
        })

        val cancelCamera = view.findViewById(R.id.pair_cancel) as Button
        cancelCamera.setOnClickListener(View.OnClickListener {
            onBackPressed();
            //dismiss();
        })
    }

    override fun onBackPressed(): Boolean {
        fragmentManager?.popBackStack()
        return true
//        return when {
//            qrFeature.onBackPressed() -> true
//            else -> false
//        }
    }

    companion object {
        const val FRAGMENT_TAG = "SYNC_PAIR_DIALOG_FRAGMENT_TAG"
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1

        fun newInstance(): SyncPairDialogFragment {
            val fragment = SyncPairDialogFragment()
            return fragment
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSIONS -> qrFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
            }
        }
    }
}
