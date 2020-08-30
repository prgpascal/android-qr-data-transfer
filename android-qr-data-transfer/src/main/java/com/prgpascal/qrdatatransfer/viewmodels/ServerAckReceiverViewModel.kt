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
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prgpascal.qrdatatransfer.utils.CHARACTER_SET_EXPANDED
import com.prgpascal.qrdatatransfer.utils.T_UUID
import kotlinx.coroutines.*
import java.io.IOException
import java.nio.charset.Charset


class ServerAckReceiverViewModel : ViewModel() {
    private var isRunning = false

    val lastReceivedAckLiveData = MutableLiveData<String>()

    fun start() {
        if (!isRunning) {
            CoroutineScope(Dispatchers.IO).launch { receiveFromSocket() }
        }
    }

    fun stop() {
        isRunning = false
    }

    private suspend fun receiveFromSocket() = withContext(Dispatchers.IO) {
        isRunning = true
        val serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("AQRT", T_UUID)
        var socket: BluetoothSocket? = null

        while (isRunning) {

            try {
                socket = serverSocket.accept()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (socket != null) {

                while (isRunning && socket.isConnected) {
                    val inputStream = socket.inputStream
                    val outputStream = socket.outputStream
                    try {
                        val bytes = ByteArray(1024)
                        val length = inputStream.read(bytes)
                        val ack = String(bytes, 0, length, Charset.forName(CHARACTER_SET_EXPANDED))

                        outputStream.write(ack.toByteArray(Charset.forName(CHARACTER_SET_EXPANDED)))
                        outputStream.flush()

                        if (!TextUtils.isEmpty(ack) && lastReceivedAckLiveData.value != ack) {
                            lastReceivedAckLiveData.postValue(ack)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        inputStream.close()
                        outputStream.close()
                    }
                }

                if (socket.isConnected) {
                    try {
                        socket.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }
                    try {
                        serverSocket.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }
                }
            }
        }
    }

}

interface ServerInterface {
    fun ackReceived(ack: String)
}