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
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.prgpascal.qrdatatransfer.R
import com.prgpascal.qrdatatransfer.fragments.ClientFragment
import com.prgpascal.qrdatatransfer.fragments.ClientInterface
import com.prgpascal.qrdatatransfer.utils.*
import com.prgpascal.qrdatatransfer.utils.TransferParams.Companion.ERROR_NO_DEVICE_SELECTED
import com.prgpascal.qrdatatransfer.utils.TransferParams.Companion.I_AM_THE_SERVER
import com.prgpascal.qrdatatransfer.utils.TransferParams.Companion.PARAM_MESSAGES
import com.prgpascal.qrdatatransfer.viewmodels.ClientAckSenderViewModel
import java.util.*
import kotlin.collections.HashMap

const val TAG_SELECT_DEVICE = "aqrt_select_device"

class ClientTransferActivity : BaseTransferActivity(), ClientInterface {
    private var serverMacAddress: String? = null

    private val receivedMessages = ArrayList<String>()
    private var previousMessageAck: String? = null

    private val btIntentFilter = IntentFilter()
    private val btDevicesMap = HashMap<String, BluetoothDevice>()
    private val btDevicesList = ArrayList<BluetoothDevice>()
    private lateinit var btDevicesAdapter: ArrayAdapter<String>

    private var viewModel: ClientAckSenderViewModel? = null

    override fun createLayout() {
        setContentView(R.layout.aqrdt_transfer_activity)
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, ClientFragment()).commit()

        btDevicesAdapter = ArrayAdapter(this, R.layout.aqrt_dialog_select_device)

        // Add bonded (already paired) devices
        for (device in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
            btDevicesMap[device.address] = device
            btDevicesList.add(device)
            btDevicesAdapter.add(device.name
                    ?: getString(R.string.aqrdt_unknown) + "\n" + device.address)
        }

        btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        if (BluetoothAdapter.getDefaultAdapter().isDiscovering) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
        }
        BluetoothAdapter.getDefaultAdapter().startDiscovery()

        SelectDeviceDialog().show(supportFragmentManager, TAG_SELECT_DEVICE)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(btDevicesReceiver, btIntentFilter)
    }

    public override fun onStop() {
        super.onStop()
        unregisterReceiver(btDevicesReceiver)
        viewModel?.stop()
    }

    fun onBtDeviceSelected(selectedDevice: BluetoothDevice) {
        serverMacAddress = selectedDevice.address
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        viewModel = ViewModelProvider(this).get(ClientAckSenderViewModel::class.java)
        viewModel!!.start(selectedDevice.address)
        val ackObserver = Observer<String> { ackSent ->
            previousMessageAck = ackSent
            if (isFinishingTransmission) {
                // The last ACK was sent. Finish.
                finishTransmissionWithSuccess()
            }
        }
        viewModel!!.lastSentAckLiveData.observe(this, ackObserver)
    }

    override fun qrMessageReceived(message: String) {
        try {
            val ack: String = getAckFromMessage(message)
            val content: String = getContentFromMessage(message)
            val digest: String = getDigestFromMessage(message)

            if (digest == calculateDigest(content)) {
                when (content) {
                    TAG_EOT -> {
                        // End of Transmission reached
                        if (ack != previousMessageAck) {
                            isFinishingTransmission = true
                        }
                        sendAckToServer(ack)
                    }
                    else -> {
                        // Regular message
                        if (ack != previousMessageAck) {
                            receivedMessages.add(content)
                        }
                        sendAckToServer(ack)
                    }
                }
            } else {
                throw StringIndexOutOfBoundsException()
            }
        } catch (e: StringIndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    private fun sendAckToServer(ack: String) {
        viewModel?.sendAck(ack)
    }

    private fun finishTransmissionWithSuccess() {
        val returnIntent = Intent()
        returnIntent.putExtra(I_AM_THE_SERVER, false)
        returnIntent.putStringArrayListExtra(PARAM_MESSAGES, receivedMessages)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    class SelectDeviceDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val callback = activity as ClientTransferActivity
            val builder = AlertDialog.Builder(activity as Context)
            builder.setTitle(getString(R.string.aqrdt_select_device))
            builder.setAdapter(callback.btDevicesAdapter) { _, index: Int ->
                callback.onBtDeviceSelected(callback.btDevicesList[index])
            }
            return builder.create()
        }

        override fun onCancel(dialog: DialogInterface) {
            super.onCancel(dialog)
            val callback = activity as ClientTransferActivity
            callback.finishTransmissionWithError(error = ERROR_NO_DEVICE_SELECTED)
        }
    }

    private val btDevicesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    val deviceAddress = device.address
                    if (!btDevicesMap.contains(deviceAddress)) {
                        btDevicesMap[deviceAddress] = device
                        btDevicesList.add(device)
                        btDevicesAdapter.add(device.name
                                ?: getString(R.string.aqrdt_unknown) + "\n" + device.address)
                        btDevicesAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

}