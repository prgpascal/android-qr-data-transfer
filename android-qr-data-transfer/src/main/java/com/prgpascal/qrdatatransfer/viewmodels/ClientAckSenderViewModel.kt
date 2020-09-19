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
package com.prgpascal.qrdatatransfer.viewmodels

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prgpascal.qrdatatransfer.utils.CHARACTER_SET_EXPANDED
import com.prgpascal.qrdatatransfer.utils.T_UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset

class ClientAckSenderViewModel : ViewModel() {
    private var isRunning = false

    val lastSentAckLiveData = MutableLiveData<String>()

    private var serverMacAddress: String? = null
    private var nextAckToSend: String? = null

    fun start(serverMacAddress: String) {
        this.serverMacAddress = serverMacAddress
        if (!isRunning) {
            CoroutineScope(Dispatchers.IO).launch { sendAckToServer() }
        }
    }

    fun stop() {
        isRunning = false
    }

    fun sendAck(ack: String) {
        this.nextAckToSend = ack
    }

    private suspend fun sendAckToServer() = withContext(Dispatchers.IO) {
        isRunning = true
        val serverDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(serverMacAddress)
        var socket: BluetoothSocket? = null

        while (isRunning) {

            try {
                socket = serverDevice.createRfcommSocketToServiceRecord(T_UUID)
                socket.connect()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (socket != null) {

                while (isRunning && socket.isConnected) {

                    if (nextAckToSend != null && nextAckToSend != lastSentAckLiveData.value) {
                        val ack = nextAckToSend
                        val outputStream = socket.outputStream
                        val inputStream = socket.inputStream
                        try {
                            outputStream.write(ack?.toByteArray(Charset.forName(CHARACTER_SET_EXPANDED)))
                            outputStream.flush()

                            val bytes = ByteArray(1024)
                            val length = inputStream.read(bytes)
                            val returnedAck = String(bytes, 0, length, Charset.forName(CHARACTER_SET_EXPANDED))

                            if (returnedAck == ack) {
                                lastSentAckLiveData.postValue(returnedAck)
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            outputStream.close()
                        }
                    }
                }

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