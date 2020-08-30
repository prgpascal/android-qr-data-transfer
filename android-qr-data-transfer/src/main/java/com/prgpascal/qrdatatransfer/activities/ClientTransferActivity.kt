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
import androidx.lifecycle.ViewModelProvider
import com.prgpascal.qrdatatransfer.R
import com.prgpascal.qrdatatransfer.fragments.ClientFragment
import com.prgpascal.qrdatatransfer.fragments.ClientInterface
import com.prgpascal.qrdatatransfer.services.ClientAckSender
import com.prgpascal.qrdatatransfer.services.ServerAckReceiver
import com.prgpascal.qrdatatransfer.utils.*
import java.util.*
import kotlin.collections.HashMap

class ClientTransferActivity : BaseTransferActivity(), ClientInterface {
    private var clientFragment: ClientFragment? = null
    private var serverMacAddress: String? = null

    private var messages = ArrayList<String>()
    private var previousMessageAck: String? = null

    var btDevicesMap = HashMap<String, BluetoothDevice>()
    var btDevicesList = ArrayList<BluetoothDevice>()
    var btDevicesAdapter: ArrayAdapter<String>? = null

    var viewModel: ClientAckSender? = null

    override fun createLayout() {
        setContentView(R.layout.aqrdt_transfer_activity)
        clientFragment = ClientFragment()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, clientFragment!!).commit()

        btDevicesAdapter = ArrayAdapter(this, R.layout.aqrt_dialog_select_device)

        // Add bonded (paired) devices
        for (device in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
            btDevicesMap[device.address] = device
            btDevicesList.add(device)
            btDevicesAdapter?.add(device.name ?: "Unknown" + "\n" + device.address)
        }

        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

        registerReceiver(btDevicesReceiver, filter)

        // Start the discovery
        if (BluetoothAdapter.getDefaultAdapter().isDiscovering){
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
        }
        BluetoothAdapter.getDefaultAdapter().startDiscovery()

        SelectDeviceDialog().show(supportFragmentManager, "aqrt_select_device")
    }

    public override fun onStop() {
        super.onStop()
        unregisterReceiver(btDevicesReceiver)
        viewModel?.stop()
    }

    fun onBtDeviceSelected(selectedDevice: BluetoothDevice) {
        serverMacAddress = selectedDevice.address
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        viewModel = ViewModelProvider(this).get(ClientAckSender::class.java)
        viewModel?.start(selectedDevice.address)
    }

    override fun messageReceived(message: String) {
        // Messaged received via QR code scan
        try {
            val ack: String = getAckFromMessage(message)
            val content: String = getContentFromMessage(message)
            val digest: String = getDigestFromMessage(message)

            if (digest == calculateDigest(content)) {
                when (content) {
                    TAG_EOT -> {
                        // EOT message, End of Transmission reached
                        sendAckToServer(ack)
                        if (ack != previousMessageAck) {
                            isFinishingTransmission = true
                            Toast.makeText(applicationContext, "FINITO", Toast.LENGTH_SHORT).show()
                            // finishTransmissionWithSuccess()
                        }
                    }
                    else -> {
                        // Regular message
                        if (ack != previousMessageAck) {
                            messages.add(content)
                            previousMessageAck = ack
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
        returnIntent.putStringArrayListExtra(MESSAGES, messages)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    class SelectDeviceDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val callback = activity as ClientTransferActivity
            val builder = AlertDialog.Builder(activity as Context)
            builder.setTitle("TODO")
            builder.setAdapter(callback.btDevicesAdapter) { _, index: Int ->
                callback.onBtDeviceSelected(callback.btDevicesList[index])
            }

            val dialog = builder.create()
            dialog.setCancelable(false)
            return dialog
        }

        override fun onCancel(dialog: DialogInterface) {
            super.onCancel(dialog)
            val callback = activity as ClientTransferActivity
            callback.finishTransmissionWithError()
            // TODO: toast di errore
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
                        btDevicesAdapter?.add(device.name ?: "Unknown" + "\n" + device.address) // TODO
                        btDevicesAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

}