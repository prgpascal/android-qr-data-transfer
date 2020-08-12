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
package com.prgpascal.qrdatatransfer.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Parcelable
import android.widget.Toast
import com.prgpascal.qrdatatransfer.R

/**
 * BroadcastReceiver for Wifi events.
 */
class WiFiDirectBroadcastReceiver(private val wifiManager: WifiP2pManager,
                                  private val wifiChannel: WifiP2pManager.Channel,
                                  private val wifiCallback: WifiDirectCallbackInterface) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    wifiCallback.onWifiEnabled()
                } else {
                    // Error: Wifi Direct disabled, enable it.
                    Toast.makeText(context.applicationContext, R.string.aqrdt_operation_enabling_wifi, Toast.LENGTH_SHORT).show()
                    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifi.isWifiEnabled = true
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                wifiCallback.onWifiPeersChanged()
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo = intent.getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo
                if (networkInfo.isConnected) {
                    // Connected with the other device, request more info.
                    wifiManager.requestConnectionInfo(wifiChannel) { info -> wifiCallback.onWifiConnectionInfoReceived(info) }
                } else {
                    wifiCallback.onWifiDisconnected()
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // This device's wifi p2p connection state changed.
                // Update this device's info
                val thisDevice = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                if (thisDevice != null) {
                    wifiCallback.onWifiThisDeviceChanged(thisDevice)
                }
            }

        }
    }
}

interface WifiDirectCallbackInterface {
    fun onWifiEnabled()
    fun onWifiConnectionInfoReceived(info: WifiP2pInfo)
    fun onWifiDisconnected()
    fun onWifiPeersChanged()
    fun onWifiThisDeviceChanged(thisDevice: WifiP2pDevice)
}