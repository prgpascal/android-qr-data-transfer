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

package com.prgpascal.qrdatatransfer.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

import static com.prgpascal.qrdatatransfer.utils.Constants.*;

/**
 * IntentService class used for the transmission of the Acknowledgment messages to
 * the Server peer.
 */
public class ClientAckSender extends IntentService {

    /** Constructors */
    public ClientAckSender(String name) {
        super(name);
    }
    public ClientAckSender() {
        super("ClientAckSender");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Check if the request is correct
        if (intent.getAction().equals(ACTION_SEND_ACK)) {

            // Get intent extras
            Bundle extras = intent.getExtras();
            String ack = extras.getString(ACK);
            String host = extras.getString(HOST);
            int port = extras.getInt(PORT);

            // Create new Socket
            Socket socket = new Socket();

            try {
                // Connect the Socket
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                // Send the Ack message
                OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), Charset.forName("ISO-8859-1"));
                out.write(ack, 0, ack.length());
                out.flush();

                out.close();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}