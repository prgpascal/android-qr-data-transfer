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
import android.content.Intent
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.widget.Toast
import com.prgpascal.qrdatatransfer.R
import com.prgpascal.qrdatatransfer.fragments.ClientFragment
import com.prgpascal.qrdatatransfer.fragments.ClientInterface
import com.prgpascal.qrdatatransfer.services.ClientAckSender
import com.prgpascal.qrdatatransfer.utils.*
import java.util.*

class ClientTransferActivity : BaseTransferActivity(), ClientInterface {
    private var clientFragment: ClientFragment? = null

    private var messages = ArrayList<String>()

    private var peerDiscoveryFinished = false

    private var serverIPAddress: String? = null

    override fun createLayout() {
        setContentView(R.layout.aqrdt_transfer_activity)
        clientFragment = ClientFragment()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, clientFragment!!).commit()
    }

    override fun onWifiConnectionInfoReceived(info: WifiP2pInfo) {
        if (info.groupFormed) {
            isConnected = true
            if (!info.isGroupOwner) {
                serverIPAddress = info.groupOwnerAddress.hostAddress
                makeQrScanAvailable(true)
            } else {
                // Error, the Client is the Group Owner. Devices must be inverted!
                Toast.makeText(applicationContext, R.string.aqrdt_error_invert_devices, Toast.LENGTH_SHORT).show()
                finishTransmissionWithError()
            }
        }
    }

    override fun onWifiDisconnected() {
        if (isConnected && !isFinishingTransmission) {
            // Error: the other peer have disconnected during transmission.
            // It's not legal because I'm not in "finishing" state.
            // Finish the transmission with an error.
            finishTransmissionWithError()
        }
    }

    override fun onWifiPeersChanged() {
        if (!peerDiscoveryFinished) {
            peerDiscoveryFinished = true
            makeQrScanAvailable(true)
        }
    }

    override fun onWifiThisDeviceChanged(thisDevice: WifiP2pDevice) {}

    private fun connect(serverMacAddress: String) {
        val otherDeviceConfig = WifiP2pConfig()
        otherDeviceConfig.deviceAddress = serverMacAddress
        otherDeviceConfig.wps.setup = WpsInfo.PBC
        otherDeviceConfig.groupOwnerIntent = 0 // I want the other device (the Server) to be the Group Owner !!

        manager.connect(channel, otherDeviceConfig, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us.
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(applicationContext, R.string.aqrdt_error_connection_failed, Toast.LENGTH_SHORT).show()
                finishTransmissionWithError()
            }
        })
    }

    override fun messageReceived(message: String) {
        // Messaged received via QR code scan
        try {
            val ack: String = getAckFromMessage(message)
            val content: String = getContentFromMessage(message)
            val digest: String = getDigestFromMessage(message)

            if (digest == calculateDigest(content)) {
                // Digest OK.
                when {
                    content.startsWith(TAG_MAC) -> {
                        // MAC message, the First QR code of the transmission.
                        // It contains the Server MAC address.
                        // Start the connection with the Server.
                        // DISABLE further QR codes scan, until connection is not established.
                        makeQrScanAvailable(false)
                        val mac: String = content.substring(TAG_MAC.length)
                        connect(mac)
                    }

                    content == TAG_EOT -> {
                        // EOT message, End of Transmission reached
                        isFinishingTransmission = true
                        sendAckToTheServer(ack)
                        finishTransmissionWithSuccess()
                    }

                    else -> {
                        // Regular message
                        messages.add(content)
                        sendAckToTheServer(ack)
                    }
                }
            } else {
                // Digest error
                throw StringIndexOutOfBoundsException()
            }
        } catch (e: StringIndexOutOfBoundsException) {
            // The message received is smaller than expected or error on digest.
            e.printStackTrace()

            // Allow the QR to be read again
            clientFragment!!.resetPreviousMessage()
        }
    }

    private fun sendAckToTheServer(ack: String) {
        val sendMessageIntent = Intent(this, ClientAckSender::class.java)
        sendMessageIntent.action = ACTION_SEND_ACK
        sendMessageIntent.putExtra(ACK, ack)
        sendMessageIntent.putExtra(HOST, serverIPAddress)
        sendMessageIntent.putExtra(PORT, SERVER_PORT)
        startService(sendMessageIntent)
    }

    private fun makeQrScanAvailable(value: Boolean) {
        clientFragment!!.canScan(value)
    }

    private fun finishTransmissionWithSuccess() {
        val returnIntent = Intent()
        returnIntent.putExtra(I_AM_THE_SERVER, false)
        returnIntent.putStringArrayListExtra(MESSAGES, messages)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

}