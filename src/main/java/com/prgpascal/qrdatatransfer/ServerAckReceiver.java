package com.prgpascal.qrdatatransfer;

import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

import static com.prgpascal.qrdatatransfer.Constants.SERVER_PORT;


/**
 * AsyncTask that contains waits for incoming socket connections.
 * (Infinite loop).
 */
public class ServerAckReceiver extends AsyncTask<Void, Void, Void> {
    private TransferActivity activity;      // Main Activity
    private String ack;                     // Ack received
    private ServerSocket serverSocket;      // Server Socket


    /** Constructor */
    public ServerAckReceiver(Context context) {
        this.activity = (TransferActivity)context;
    }


    @Override
    protected Void doInBackground(Void... params) {
        try {
            // Create the Server Socket
            serverSocket  = new ServerSocket(SERVER_PORT);

            // Infinite loop that waits for incoming acks
            while(true) {

                // Request next QR code generation
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.sendNextMessage();
                    }
                });


                // Wait for client connections (BLOCKING CALL!!)
                Socket client = serverSocket.accept();


                // If this code is reached, a client has connected and transferred data
                // Read the input data
                InputStreamReader in = new InputStreamReader(client.getInputStream(), Charset.forName("ISO-8859-1"));
                ack = "";
                int data = in.read();
                while(data != -1) {
                    ack = ack + ((char)data);
                    data = in.read();
                }

                in.close();


                // Send the received Ack message to the Activity
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.messageReceived(ack);
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    /** Close the Server Socket */
    public void stopSocket() {
        try {
            // Close the socket
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
