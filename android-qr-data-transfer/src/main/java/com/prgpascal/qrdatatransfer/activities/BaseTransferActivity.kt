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
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ChannelListener
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.prgpascal.qrdatatransfer.R
import com.prgpascal.qrdatatransfer.services.WiFiDirectBroadcastReceiver
import com.prgpascal.qrdatatransfer.services.WifiDirectCallbackInterface
import com.prgpascal.qrdatatransfer.utils.DEBUG_TAG
import com.prgpascal.qrdatatransfer.utils.I_AM_THE_SERVER
import com.prgpascal.qrdatatransfer.utils.MESSAGES
import com.prgpascal.qrdatatransfer.utils.preventScreenRotation

/**
 * Activity that performs the transmission of messages between the Client and the Server.
 */
abstract class BaseTransferActivity : PermissionsActivity(), ChannelListener, WifiDirectCallbackInterface {
    lateinit var intentFilter: IntentFilter
    lateinit var manager: WifiP2pManager
    lateinit var channel: WifiP2pManager.Channel
    lateinit var receiver: BroadcastReceiver

    var retryConnection = false                 // TRUE when I'm trying to re-connect after an error occurred.
    var isConnected = false                     // TRUE when the Client and Server are connected each other via Wifi Direct.
    var isFinishingTransmission = false         // TRUE when the Server sends the EOT message, so it's in "finishing" state.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preventScreenRotation(this)

        intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        receiver = WiFiDirectBroadcastReceiver(manager, channel, this)

        deletePreviousPersistentGroups()

        checkAppPermissions()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun onStop() {
        super.onStop();
        disconnectFromWifiDirect();
    }

    override fun permissionsGranted() {
        createLayout()
    }

    abstract fun createLayout()

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
                        methods[i].invoke(manager, channel, netid, null)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Peers discovery. This must be called before peers connection. */
    private fun discoverPeers() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(DEBUG_TAG, "Discovery started.")
            }

            override fun onFailure(reasonCode: Int) {
                Log.d(DEBUG_TAG, "Discovery failed. Error: $reasonCode")
            }
        })
    }

    private fun disconnectFromWifiDirect() {
        isConnected = false
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Log.d(DEBUG_TAG, "Disconnect failed. Reason :$reasonCode")
            }

            override fun onSuccess() {
                Log.d(DEBUG_TAG, "Disconnected and group removed")
            }
        })
    }

    override fun onChannelDisconnected() {
        if (!retryConnection) {
            // Retry connection
            Toast.makeText(applicationContext, R.string.aqrdt_error_channel_lost_retry, Toast.LENGTH_LONG).show()
            retryConnection = true
            manager.initialize(this, mainLooper, this)
        } else {
            Toast.makeText(this, R.string.aqrdt_error_channel_lost, Toast.LENGTH_LONG).show()
        }
    }

    override fun onWifiEnabled() {
        discoverPeers()
    }

    fun finishTransmissionWithError() {
        val returnIntent = Intent()
        setResult(Activity.RESULT_CANCELED, returnIntent)
        finish()
    }

}