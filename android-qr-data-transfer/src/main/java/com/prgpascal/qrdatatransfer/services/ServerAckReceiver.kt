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
import android.content.Context
import android.os.AsyncTask
import android.text.TextUtils
import com.prgpascal.qrdatatransfer.activities.ServerTransferActivity
import com.prgpascal.qrdatatransfer.utils.CHARACTER_SET_EXPANDED
import com.prgpascal.qrdatatransfer.utils.T_UUID
import java.io.IOException
import java.nio.charset.Charset

class ServerAckReceiver(context: Context) : AsyncTask<Void?, Void?, Void?>() {
    private val serverCallback: ServerInterface = context as ServerInterface
    private val context = context as ServerTransferActivity
    private val serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("TODO", T_UUID)

    override fun doInBackground(vararg p0: Void?): Void? {
        var ack: String
        var socket: BluetoothSocket
        var lastReceivedAck = ""

        // Infinite loop that waits for incoming ACKs
        while (true) {

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
                    val bytes = ByteArray(2)
                    inputStream.read(bytes, 0, 2)
                    ack = String(bytes, Charset.forName(CHARACTER_SET_EXPANDED))

                    if (!TextUtils.isEmpty(ack) && lastReceivedAck != ack) {
                        lastReceivedAck = ack
                        context.runOnUiThread { serverCallback.ackReceived(ack) }
                    }

                } catch (e: Exception) {

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
        return null
    }

    fun stopSocket() {
        try {
            serverSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}

interface ServerInterface {
    fun ackReceived(ack: String)
}