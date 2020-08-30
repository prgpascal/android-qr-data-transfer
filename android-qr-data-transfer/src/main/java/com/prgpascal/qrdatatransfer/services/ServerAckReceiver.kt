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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prgpascal.qrdatatransfer.utils.CHARACTER_SET_EXPANDED
import com.prgpascal.qrdatatransfer.utils.T_UUID
import kotlinx.coroutines.*
import java.io.IOException
import java.nio.charset.Charset


class ServerAckReceiver : ViewModel() {
    private val serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("AQRT", T_UUID)
    val lastReceivedAck = MutableLiveData<String>()

    private var isRunning = false
    private var coroutine: Job? = null

    fun start() {
        if (!isRunning) {
            val scope = CoroutineScope(Dispatchers.IO)
            coroutine = scope.launch { receiveFromSocket() }
        }
    }

    fun stop() {
        coroutine?.cancel()
        isRunning = false
        try {
            serverSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun receiveFromSocket() = withContext(Dispatchers.IO) {
        isRunning = true
        var ack: String
        var socket: BluetoothSocket

        // Infinite loop that waits for incoming ACKs
        while (true) {

            if (!isRunning) {
                return@withContext
            }

            try {
                // Wait for client connections (THIS IS A BLOCKING CALL!!)
                socket = serverSocket.accept()
            } catch (e: IOException) {
                e.printStackTrace()
                break
            }

            if (socket != null) {
                // If this code is reached, a client has connected and transferred data
                // Read the input data
                val inputStream = socket.inputStream
                try {
                    val bytes = ByteArray(1024)
                    val length = inputStream.read(bytes)

                    ack = String(bytes, 0, length, Charset.forName(CHARACTER_SET_EXPANDED))

                    if (!TextUtils.isEmpty(ack) && lastReceivedAck.value != ack) {
                        lastReceivedAck.postValue(ack)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    inputStream.close()
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

interface ServerInterface {
    fun ackReceived(ack: String)
}