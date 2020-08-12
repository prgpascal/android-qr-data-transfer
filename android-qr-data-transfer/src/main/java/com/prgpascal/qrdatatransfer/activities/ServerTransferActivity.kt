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
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Bundle
import android.widget.Toast
import com.prgpascal.qrdatatransfer.R
import com.prgpascal.qrdatatransfer.fragments.ServerFragment
import com.prgpascal.qrdatatransfer.services.ServerAckReceiver
import com.prgpascal.qrdatatransfer.services.ServerInterface
import com.prgpascal.qrdatatransfer.utils.*
import java.util.*

class ServerTransferActivity : BaseTransferActivity(), ServerInterface {
    companion object {
        const val PARAM_MESSAGES = MESSAGES
    }

    private var serverAckReceiver: ServerAckReceiver? = null
    private var serverFragment: ServerFragment? = null
    private var serverMacAddress: String? = null

    private var messages = ArrayList<String>()
    private var messagesIndex = 0
    private var messageAttendedAck: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        getIntentExtras()
        super.onCreate(savedInstanceState)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun getIntentExtras() {
        val extras = intent.extras
        if (extras != null && extras.containsKey(MESSAGES)) {
            messages = intent.getStringArrayListExtra(MESSAGES)
            messages.add(TAG_EOT) // Append the End Of Transmission (EOT) as the last message.
        } else {
            finishTransmissionWithError()
        }
    }

    override fun createLayout() {
        setContentView(R.layout.aqrdt_transfer_activity)
        serverFragment = ServerFragment()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, serverFragment!!).commit()
    }

    public override fun onStop() {
        super.onStop()
        stopServerReceiver()
    }

    private fun stopServerReceiver() {
        serverAckReceiver?.stopSocket()
        serverAckReceiver?.cancel(true)
    }

    override fun onWifiConnectionInfoReceived(info: WifiP2pInfo) {
        if (info.groupFormed) {
            isConnected = true
            if (info.isGroupOwner) {
                // Instantiate the ServerAckReceiver
                if (serverAckReceiver == null) {
                    serverAckReceiver = ServerAckReceiver(this@ServerTransferActivity)
                    serverAckReceiver!!.execute()
                }
            } else {
                // Error, the Server is not the Group Owner. Devices must be inverted!
                Toast.makeText(applicationContext, R.string.aqrdt_error_invert_devices, Toast.LENGTH_SHORT).show()
                finishTransmissionWithError()
            }
        }
    }

    override fun onWifiDisconnected() {
        if (isConnected && !isFinishingTransmission) {
            finishTransmissionWithError()
        }
    }

    override fun onWifiPeersChanged() {}

    override fun onWifiThisDeviceChanged(thisDevice: WifiP2pDevice) {
        if (messageAttendedAck == null) {
            serverMacAddress = thisDevice.deviceAddress
            sendFirstMessageAsQR()
        }
    }

    private fun sendFirstMessageAsQR() {
        sendMessageAsQR(TAG_MAC + serverMacAddress)
    }

    override fun ackReceived(ack: String) {
        // ACK received via Wifi Direct
        if (ack == messageAttendedAck) {
            // The Ack is correct.
            // Now, check if I've reached the EOT.
            if (isFinishingTransmission) {
                // EOT ack received, End of Transmission reached.
                // Finish the transmission with success.
                finishTransmissionWithSuccess()
            } else {
                sendNextMessageAsQrCode()
            }
        } else {
            // The Ack is incorrect (irreversible error).
            // Finish the Activity.
            finishTransmissionWithError()
        }
    }

    private fun sendNextMessageAsQrCode() {
        if (messagesIndex < messages.size) {
            val nextMessage = messages[messagesIndex]
            messagesIndex++
            if (nextMessage == TAG_EOT) {
                // This is the last message. Start finishing the transmission
                isFinishingTransmission = true
            }

            sendMessageAsQR(nextMessage)
        }
    }

    private fun sendMessageAsQR(messageToSend: String) {
        messageAttendedAck = createRandomString(ACK_LENGTH)
        val digest: String? = calculateDigest(messageToSend)

        // Update the Server Fragment
        // +1 because the first QR is the Mac address QR
        serverFragment!!.updateQR(messageAttendedAck + messageToSend + digest, messagesIndex + 1, messages.size + 1)
    }

    private fun finishTransmissionWithSuccess() {
        val returnIntent = Intent()
        returnIntent.putExtra(I_AM_THE_SERVER, true)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

}