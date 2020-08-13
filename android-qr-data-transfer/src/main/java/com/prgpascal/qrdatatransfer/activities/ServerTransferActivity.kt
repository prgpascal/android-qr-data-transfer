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
import android.os.Bundle
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

    override fun isServer(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        getIntentExtras()
        super.onCreate(savedInstanceState)
    }

    private fun getIntentExtras() {
        val extras = intent.extras
        if (extras != null && extras.containsKey(MESSAGES)) {
            messages = intent.getStringArrayListExtra(MESSAGES) ?: arrayListOf()
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
        stopServerAckReceiver()
    }

    private fun stopServerAckReceiver() {
        serverAckReceiver?.stopSocket()
        serverAckReceiver?.cancel(true)
    }

    override fun startTransmission() {
        super.startTransmission()
        if (serverAckReceiver == null) {
            serverAckReceiver = ServerAckReceiver(this@ServerTransferActivity)
            serverAckReceiver!!.execute()
        }
        sendNextMessageAsQrCode()
    }

    override fun onWifiThisDeviceChanged(thisDevice: WifiP2pDevice) {
        if (status != Status.PAIRING && status != Status.PAIRED) {
            serverMacAddress = thisDevice.deviceAddress
            sendConfigurationMessageAsQr()
        }
    }

    private fun sendConfigurationMessageAsQr() {
        status = Status.PAIRING
        sendMessageAsQR(TAG_MAC + serverMacAddress)
    }

    override fun ackReceived(ack: String) {
        if (ack == messageAttendedAck) {
            if (status == Status.FINISHING_TRANSMISSION) {
                finishTransmissionWithSuccess()
            } else {
                sendNextMessageAsQrCode()
            }
        }
    }

    private fun sendNextMessageAsQrCode() {
        if (status == Status.IN_TRANSMISSION) {
            if (messagesIndex < messages.size) {
                val nextMessage = messages[messagesIndex]
                messagesIndex++
                if (nextMessage == TAG_EOT) {
                    // This is the last message. Start finishing the transmission
                    status = Status.FINISHING_TRANSMISSION
                }

                sendMessageAsQR(nextMessage)
            }
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