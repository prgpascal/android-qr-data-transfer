/*
 * Copyright (C) 2016 Riccardo Leschiutta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.prgpascal.qrdatatransfer.activities

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ChannelListener
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.prgpascal.qrdatatransfer.R
import com.prgpascal.qrdatatransfer.services.WiFiDirectBroadcastReceiver
import com.prgpascal.qrdatatransfer.services.WifiDirectCallbackInterface
import com.prgpascal.qrdatatransfer.utils.DEBUG_TAG
import com.prgpascal.qrdatatransfer.utils.preventScreenRotation
import kotlinx.coroutines.*

abstract class BaseTransferActivity : PermissionsActivity(), ChannelListener, WifiDirectCallbackInterface {
    lateinit var wiFiIntentFilter: IntentFilter
    lateinit var wiFiManager: WifiP2pManager
    lateinit var wiFiChannel: WifiP2pManager.Channel
    lateinit var wiFiReceiver: BroadcastReceiver
    lateinit var peerDiscoveryCoroutineJob: Job
    var serverIPAddress: String? = null

    enum class Status {
        NOT_PAIRED, PAIRING, PAIRED, IN_TRANSMISSION, FINISHING_TRANSMISSION
    }

    var status: Status = Status.NOT_PAIRED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preventScreenRotation(this)

        wiFiIntentFilter = IntentFilter()
        wiFiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        wiFiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        wiFiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        wiFiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        wiFiManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wiFiChannel = wiFiManager.initialize(this, mainLooper, null)
        wiFiReceiver = WiFiDirectBroadcastReceiver(wiFiManager, wiFiChannel, this)

        deletePreviousPersistentGroups()

        checkAppPermissions()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wiFiReceiver, wiFiIntentFilter)
        peerDiscoveryCoroutineJob = CoroutineScope(Dispatchers.Main).launch {
            discoverPeers()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wiFiReceiver)
        peerDiscoveryCoroutineJob.cancel()
    }

    override fun onStop() {
        super.onStop()
        disconnectFromWifiDirect()
    }

    override fun permissionsGranted() {
        createLayout()
    }

    abstract fun createLayout()

    abstract fun isServer(): Boolean

    open fun startTransmission() {
        status = Status.IN_TRANSMISSION
    }

    /**
     * Delete all previous persistent Wifi Direct groups, for privacy reasons and
     * to avoid the reuse of previously created groups.
     * http://stackoverflow.com/questions/15152817/persistent-group-in-wi-fi-direct
     */
    private fun deletePreviousPersistentGroups() {
        try {
            val methods = WifiP2pManager::class.java.methods
            for (i in methods.indices) {
                if (methods[i].name == "deletePersistentGroup") {
                    for (netid in 0..31) {
                        methods[i].invoke(wiFiManager, wiFiChannel, netid, null)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onWifiEnabled() {}
    override fun onWifiDisconnected() {}
    override fun onWifiPeersChanged() {}
    override fun onWifiThisDeviceChanged(thisDevice: WifiP2pDevice) {}

    override fun onWifiConnectionInfoReceived(info: WifiP2pInfo) {
        if (status != Status.PAIRED) {
            if (info.groupFormed) {
                serverIPAddress = info.groupOwnerAddress.getHostAddress();
                status = Status.PAIRED
                startTransmission()
            }
        }
    }

    private fun disconnectFromWifiDirect() {
        status = Status.NOT_PAIRED
        wiFiManager.removeGroup(wiFiChannel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Log.d(DEBUG_TAG, "Disconnect failed. Reason :$reasonCode")
            }

            override fun onSuccess() {
                Log.d(DEBUG_TAG, "Disconnected and group removed")
            }
        })
    }

    override fun onChannelDisconnected() {
        // Try reconnecting
        if (status != Status.PAIRING) {
            status = Status.PAIRING
            Toast.makeText(applicationContext, R.string.aqrdt_error_channel_lost_retry, Toast.LENGTH_LONG).show()
            wiFiManager.initialize(this, mainLooper, this)
        } else {
            Toast.makeText(this, R.string.aqrdt_error_channel_lost, Toast.LENGTH_LONG).show()
        }
    }

    fun finishTransmissionWithError() {
        val returnIntent = Intent()
        setResult(Activity.RESULT_CANCELED, returnIntent)
        finish()
    }

    private suspend fun discoverPeers() = withContext(Dispatchers.IO) {
        while (true) {
            yield()
            if (status == Status.NOT_PAIRED) {
                wiFiManager.discoverPeers(wiFiChannel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(DEBUG_TAG, "Discovery started.")
                    }

                    override fun onFailure(reasonCode: Int) {
                        Log.d(DEBUG_TAG, "Discovery failed. Error: $reasonCode")
                    }
                })
            }
            delay(10000L)
        }
    }

}