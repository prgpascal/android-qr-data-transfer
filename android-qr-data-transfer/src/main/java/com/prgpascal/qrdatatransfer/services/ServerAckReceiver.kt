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

import android.content.Context
import android.os.AsyncTask
import com.prgpascal.qrdatatransfer.activities.ServerTransferActivity
import com.prgpascal.qrdatatransfer.utils.CHARACTER_SET_EXPANDED
import com.prgpascal.qrdatatransfer.utils.SERVER_PORT
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.nio.charset.Charset

/**
 * AsyncTask that waits for incoming socket connections.
 * (Infinite loop).
 * FOR SERVER!!!
 */
class ServerAckReceiver(context: Context) : AsyncTask<Void?, Void?, Void?>() {
    private val serverCallback: ServerInterface = context as ServerInterface
    private val context = context as ServerTransferActivity
    private val serverSocket = ServerSocket(SERVER_PORT)
    private lateinit var ack: String

    override fun doInBackground(vararg p0: Void?): Void? {
        try {
            // Infinite loop that waits for incoming ACKs
            while (true) {

                // Wait for client connections (THIS IS A BLOCKING CALL!!)
                val client = serverSocket.accept()

                // If this code is reached, a client has connected and transferred data
                // Read the input data
                val inputStreamReader = InputStreamReader(client.getInputStream(), Charset.forName(CHARACTER_SET_EXPANDED))
                ack = ""
                var data = inputStreamReader.read()
                while (data != -1) {
                    ack += data.toChar()
                    data = inputStreamReader.read()
                }
                inputStreamReader.close()

                // Send the received Ack message to the callback
                context.runOnUiThread { serverCallback.ackReceived(ack) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
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