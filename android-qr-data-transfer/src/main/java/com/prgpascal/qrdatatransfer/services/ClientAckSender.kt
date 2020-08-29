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

import android.app.IntentService
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import com.prgpascal.qrdatatransfer.utils.*
import java.io.IOException
import java.nio.charset.Charset

class ClientAckSender : IntentService("ClientAckSender") {

    override fun onHandleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SEND_ACK) {
            val extras = intent.extras
            if (extras != null) {
                val ack = extras.getString(ACK)
                val serverMacAddress: String? = extras.getString(MAC_ADDRESS)

                if (ack != null && serverMacAddress != null) {
                    val serverDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(serverMacAddress)
                    val socket = serverDevice.createRfcommSocketToServiceRecord(T_UUID)

                    socket.connect()

                    // Send the Ack message
                    val outputStream = socket.outputStream
                    try {
                        outputStream.write(ack.toByteArray(Charset.forName(CHARACTER_SET_EXPANDED)))
                        outputStream.flush()
                    } catch (e: Exception) {
                        Toast.makeText(applicationContext, "eeeeeee client", Toast.LENGTH_SHORT).show()
                    } finally {
                        outputStream.close()
                        if (socket.isConnected) {
                            try {
                                socket.close()
                            } catch (ioe: IOException) {
                                ioe.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}