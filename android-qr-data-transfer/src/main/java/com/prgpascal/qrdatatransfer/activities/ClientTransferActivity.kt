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
    private var previousMessageAck: String? = null

    override fun isServer(): Boolean {
        return false
    }

    override fun createLayout() {
        setContentView(R.layout.aqrdt_transfer_activity)
        clientFragment = ClientFragment()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, clientFragment!!).commit()
    }

    override fun messageReceived(message: String) {
        // Messaged received via QR code scan
        try {
            val ack: String = getAckFromMessage(message)
            val content: String = getContentFromMessage(message)
            val digest: String = getDigestFromMessage(message)

            if (digest == calculateDigest(content)) {
                when {
                    content.startsWith(TAG_MAC) -> {
                        // MAC message, the First QR code of the transmission.
                        // It contains the Server MAC address.
                        // Start the connection with the Server.
                        // DISABLE further QR codes scan, until connection is not established.
                        if (status != Status.PAIRED && status != Status.PAIRING) {
                            val mac: String = content.substring(TAG_MAC.length)
                            connect(mac)
                        }
                    }

                    content == TAG_EOT -> {
                        // EOT message, End of Transmission reached
                        if (ack != previousMessageAck) {
                            status = Status.FINISHING_TRANSMISSION
                            finishTransmissionWithSuccess()
                        }
                        sendAckToServer(ack)
                    }

                    else -> {
                        // Regular message
                        if (ack != previousMessageAck) {
                            messages.add(content)
                        }
                        sendAckToServer(ack)
                    }
                }
            } else {
                // Digest error
                throw StringIndexOutOfBoundsException()
            }
        } catch (e: StringIndexOutOfBoundsException) {
            // The message received is smaller than expected or error on digest.
            e.printStackTrace()
        }
    }

    private fun connect(serverMacAddress: String) {
        status = Status.PAIRING

        val otherDeviceConfig = WifiP2pConfig()
        otherDeviceConfig.deviceAddress = serverMacAddress
        otherDeviceConfig.wps.setup = WpsInfo.PBC
        otherDeviceConfig.groupOwnerIntent = 0 // I want the other device (the Server) to be the Group Owner !!

        wiFiManager.connect(wiFiChannel, otherDeviceConfig, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us.
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(applicationContext, R.string.aqrdt_error_connection_failed, Toast.LENGTH_SHORT).show()
                finishTransmissionWithError()
            }
        })
    }

    private fun sendAckToServer(ack: String) {
        val sendMessageIntent = Intent(this, ClientAckSender::class.java)
        sendMessageIntent.action = ACTION_SEND_ACK
        sendMessageIntent.putExtra(ACK, ack)
        sendMessageIntent.putExtra(HOST, serverIPAddress)
        sendMessageIntent.putExtra(PORT, SERVER_PORT)
        startService(sendMessageIntent)
    }

    private fun finishTransmissionWithSuccess() {
        val returnIntent = Intent()
        returnIntent.putExtra(I_AM_THE_SERVER, false)
        returnIntent.putStringArrayListExtra(MESSAGES, messages)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

}