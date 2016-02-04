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

package com.prgpascal.qrdatatransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.widget.Toast;

/**
 * BroadcastReceiver that receives Wifi events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private TransferActivity activity;      // Main Activity
    private WifiP2pManager manager;         // Wifi manager
    private Channel channel;                // Wifi channel



    /** Constructor */
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager,
                                       Channel channel,
                                       TransferActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the action identifier
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // The Wifi state has changed.
            // Check if the WiFi is enabled or not.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi enabled!, start the peers discovery.
                activity.discoverPeers();

            } else {
                // Error: Wifi Direct disabled, enable it.
                Toast.makeText(activity.getApplicationContext(), R.string.aqrdt_operation_enabling_wifi, Toast.LENGTH_SHORT).show();
                WifiManager wifi = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
                wifi.setWifiEnabled(true);
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Wifi P2P peers changed.
            // Maybe Discovery of peers has finished and the client could be able to scan for QR codes now.
            activity.peersChanged();

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Wifi P2P connection has changed.
            // Check if the device is connected or not.
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                // Connected with the other device, request more info.
                manager.requestConnectionInfo(channel, activity);

            } else {
                // Disconnected
                if (activity.isConnected && !activity.isFinishingTransmission){
                    // Error: the other peer has disconnected during transmission.
                    // It's not legal because I'm not in "finishing" state.
                    // Finish the transmission with error.
                    activity.finishTransmissionWithError();
                }
            }


        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // This device's wifi p2p connection state changed.
            // Update the info about this device.
            WifiP2pDevice thisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            activity.updateThisDevice(thisDevice.deviceAddress);
        }
    }
}