/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mozilla.components.feature.qr.QrFeature
import mozilla.components.feature.sitepermissions.SitePermissions
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import kotlin.coroutines.CoroutineContext

private const val KEY_URL = "KEY_URL"
private const val KEY_IS_SECURED = "KEY_IS_SECURED"
private const val KEY_SITE_PERMISSIONS = "KEY_SITE_PERMISSIONS"
private const val KEY_IS_TP_ON = "KEY_IS_TP_ON"
private const val REQUEST_CODE_QUICK_SETTINGS_PERMISSIONS = 4

@SuppressWarnings("TooManyFunctions")
class SyncPairDialogFragment : BottomSheetDialogFragment(), CoroutineScope {

    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()
    private val safeArguments get() = requireNotNull(arguments)
    private val url: String by lazy { safeArguments.getString(KEY_URL) }
    private val isSecured: Boolean by lazy { safeArguments.getBoolean(KEY_IS_SECURED) }
    private val isTrackingProtectionOn: Boolean by lazy { safeArguments.getBoolean(KEY_IS_TP_ON) }
    private lateinit var job: Job

    var sitePermissions: SitePermissions?
        get() = safeArguments.getParcelable(KEY_SITE_PERMISSIONS)
        set(value) {
            safeArguments.putParcelable(KEY_SITE_PERMISSIONS, value)
        }

    override val coroutineContext: CoroutineContext get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        (activity as HomeActivity).openToBrowser(null, BrowserDirection.FromSettings)
                    }
                }),
            owner = this,
            view = view
        )

        val openCamera = view.findViewById(R.id.pair_open_camera) as Button
        openCamera.setOnClickListener(View.OnClickListener {
            qrFeature.get()?.scan(R.id.container)
            dismiss();
        })

        val cancelCamera = view.findViewById(R.id.pair_cancel) as Button
        cancelCamera.setOnClickListener(View.OnClickListener {
            dismiss();
        })

        view.setOnKeyListener(object: View.OnKeyListener {
            // TODO: this doesn't work :(
            override fun onKey(v:View, keyCode:Int, event: KeyEvent):Boolean {
                if (event.getAction() === KeyEvent.ACTION_DOWN)
                {
                    if (keyCode == KeyEvent.KEYCODE_BACK)
                    {
                        qrFeature.onBackPressed()
                        return true
                    }
                }
                return false
            }
        })
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
