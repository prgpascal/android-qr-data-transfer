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
import android.content.Intent
import com.prgpascal.qrdatatransfer.utils.*
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset

/**
 * IntentService class used for the transmission of the Ack messages to
 * the Server peer.
 */
class ClientAckSender : IntentService(TAG) {
    companion object {
        const val TAG = "ClientAckSender"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SEND_ACK) {
            val extras = intent.extras
            if (extras != null) {
                val ack = extras.getString(ACK)
                val host = extras.getString(HOST)
                val port = extras.getInt(PORT)

                if (ack != null && host != null && port != -1) {
                    val socket = Socket()
                    try {
                        // Connect the Socket
                        socket.bind(null)
                        socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT)

                        // Send the Ack message
                        val out = OutputStreamWriter(socket.getOutputStream(), Charset.forName(CHARACTER_SET_EXPANDED))
                        out.write(ack, 0, ack.length)
                        out.flush()
                        out.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    } finally {
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