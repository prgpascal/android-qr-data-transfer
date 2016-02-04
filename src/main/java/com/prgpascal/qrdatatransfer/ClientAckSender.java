package com.prgpascal.qrdatatransfer;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

import static com.prgpascal.qrdatatransfer.Constants.*;

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