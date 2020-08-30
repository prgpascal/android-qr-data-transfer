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
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
    private var serverAckReceiverViewModel: ServerAckReceiver? = null
    private var serverFragment: ServerFragment? = null

    private var messages = ArrayList<String>()
    private var messagesIndex = 0
    private var messageAttendedAck: String? = null

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

    private fun makeDiscoverable() {
        if (BluetoothAdapter.getDefaultAdapter().scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val makeDiscoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            makeDiscoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60)
            startActivity(makeDiscoverableIntent)
        }
    }

    override fun createLayout() {
        setContentView(R.layout.aqrdt_transfer_activity)
        serverFragment = ServerFragment(getNextQrMessage())
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, serverFragment!!).commit()

        serverAckReceiverViewModel = ViewModelProvider(this).get(ServerAckReceiver::class.java)
        val ackObserver = Observer<String> { ack ->
            ackReceived(ack)
        }
        serverAckReceiverViewModel?.lastReceivedAck?.observe(this, ackObserver)

        makeDiscoverable()
    }

    override fun onStart() {
        super.onStart()
        serverAckReceiverViewModel?.start()
    }

    public override fun onStop() {
        super.onStop()
        serverAckReceiverViewModel?.stop()
    }

    override fun ackReceived(ack: String) {
        if (ack == messageAttendedAck) {
            if (isFinishingTransmission) {
                finishTransmissionWithSuccess()
            } else {
                sendNextMessageAsQrCode()
            }
        }
    }

    private fun sendNextMessageAsQrCode() {
        val nextMessage = getNextQrMessage()
        if (nextMessage != null) {
            sendMessageAsQR(nextMessage)
        }
    }

    private fun getNextQrMessage(): QrMessage? {
        if (messagesIndex < messages.size) {
            val nextMessage = messages[messagesIndex]
            messagesIndex++
            if (nextMessage == TAG_EOT) {
                // This is the last message. Start finishing the transmission
                isFinishingTransmission = true
            }

            messageAttendedAck = createRandomString(ACK_LENGTH)
            val digest: String? = calculateDigest(nextMessage)

            return QrMessage(messageAttendedAck + nextMessage + digest, messagesIndex, messages.size)
        }
        return null
    }

    private fun sendMessageAsQR(messageToSend: QrMessage) {
        serverFragment!!.updateQR(messageToSend)
    }

    private fun finishTransmissionWithSuccess() {
        val returnIntent = Intent()
        returnIntent.putExtra(I_AM_THE_SERVER, true)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

}