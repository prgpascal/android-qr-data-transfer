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

package com.prgpascal.qrdatatransfer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static com.prgpascal.qrdatatransfer.Constants.*;

/**
 * Activity that performs the transmission of messages between the Client and the Server.
 */
public class TransferActivity extends FragmentActivity implements
        ChannelListener,
        WifiP2pManager.ConnectionInfoListener {

    // Wifi Direct objects
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private IntentFilter intentFilter;
    private BroadcastReceiver receiver = null;

    // ServerAckReceiver
    private ServerAckReceiver mServerReceiver;
    private boolean iAmTheServer = false;
    private String serverIPAddress;             // Required for Socket connection.

    // Client/Server fragments (only one of these will be instantiated)
    private ServerFragment serverFragment;
    private ClientFragment clientFragment;

    // Messages to be exchanged
    private ArrayList<String> messages = new ArrayList<>();         // Strings that contain the messages to be transferred.
    private int messagesIndex = 0;                                  // Used to scan the messages ArrayList.
    private String attendedAck;                                     // Ack attended by the Server.

    // About the transmission
    public boolean isFinishingTransmission = false;     // TRUE if the Server sends the EOT message, so it's in "finishing" state.
    public boolean isConnected = false;                 // TRUE if Client and Server are connected each other via Wifi Direct.
    private boolean retryConnection = false;            // TRUE if I'm trying to re-connect after an error occured.
    private boolean peerDiscoveryFinished = false;      // TRUE if the first peer discovery has finished.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent from Screen rotation
        MyCustomScreenOrientationManager.lockScreen(TransferActivity.this);

        // First, get the Intent Extras...
        checkAndGetIntentExtras(getIntent().getExtras());

        // Add the required intent filter values for the WIFI Direct connection
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Initialize the WiFiP2PManager and get the Channel
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        // Delete previous persistent Wifi Direct groups
        deletePreviousPersistentGroups();

        // Create the layout
        createLayout();
    }

    /**
     * Read and check all the Intext extra parameters.
     * If one or more required parameter are missing, finish the Activity with an error.
     */
    private void checkAndGetIntentExtras(Bundle extras){

        // I_AM_THE_SERVER param
        if (extras.containsKey(I_AM_THE_SERVER)){
            iAmTheServer = extras.getBoolean(I_AM_THE_SERVER);

            // MESSAGES param
            if (iAmTheServer){
                if (extras.containsKey(MESSAGES)) {
                    messages = getIntent().getStringArrayListExtra(MESSAGES);

                    // Append the End Of Transmission (EOT) message at the end.
                    messages.add(TAG_EOT);

                } else {
                    // Error: missing param!!
                    finishTransmissionWithError();
                }
            }

        } else {
            // Error: missing param!!
            finishTransmissionWithError();
        }
    }

    /**
     * Delete all previous persistent Wifi Direct groups, for privacy reasons and
     * to avoid the reuse of previously created groups.
     * http://stackoverflow.com/questions/15152817/persistent-group-in-wi-fi-direct
     */
    private void deletePreviousPersistentGroups(){
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(manager, channel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register the broadcast receiver
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister the broadcast receiver
        unregisterReceiver(receiver);
    }

    @Override
    public void onStop(){
        super.onStop();

        // Disconnect the WiFi Direct connection
        disconnect();

        // Stop the Server Receiver
        if (iAmTheServer) {
            stopServerReceiver();
        }
    }

    /** Stop the ServerReceiver AsyncTask. */
    private void stopServerReceiver(){
        try {
            mServerReceiver.stopSocket();
            mServerReceiver.cancel(true);
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    /** Create the layout */
    private void createLayout() {
        // Set the layout
        setContentView(R.layout.aqrdt_transfer_activity);

        // Client/Server Fragment
        if (iAmTheServer){
            // Create the Server fragment
            serverFragment = new ServerFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, serverFragment).commit();

        } else {
            // Create the Client fragment
            clientFragment = new ClientFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, clientFragment).commit();
        }
    }

    /**
     *  Called when info about this device are available.
     *  If this device is the Server, create the QR code with the MAC address.
     *
     *  @param thisDeviceMAC the MAC address of this device */
    public void updateThisDevice(String thisDeviceMAC){
        if (iAmTheServer){
            // Create the first message to send as QR code.
            // At this point Client and Server ar not connected via Wifi Direct yet.
            // This QR code will contain the Server MAC address.
            sendMessageAsQR(TAG_MAC + thisDeviceMAC);
        }
    }

    /**
     * Called when the peers list has changed.
     * This method is called the first time when the peer discovery has finished.
     * Set the value of peerDiscoveryFinished as true.
     * Now that the peer discovery has finished we can enable the QR scan.
     */
    public void peersChanged(){
        if ((!iAmTheServer)&&(!peerDiscoveryFinished)){
            peerDiscoveryFinished = true;
            makeQRscanAvailable(true);
        }
    }

    /**
     * Manipulate the incoming message.
     * The message can be a regular message or an ACK message.
     * If the Server receives an incorrect ACK, finish the Activity with an error.
     * If the Client receives an incorrect message, retry reading the same QR.
     *
     * @param message the incoming message.
     */
    public void messageReceived(String message){

        if (iAmTheServer){
            // I'm the Server and a ACK has received (via Wifi Direct)

            if (message.equals(attendedAck)){
                // The Ack is correct.
                // If available, next QR will be created by ServerAckReceiver.

                // Now, check if I've reached the EOT.
                if (isFinishingTransmission){
                    // EOT ack received, End of Transmission reached.
                    // Finish the transmission with success.
                    finishTransmissionWithSuccess();
                }

            } else {
                // The Ack is incorrect (irreversible error).
                // Finish the Activity.
                finishTransmissionWithError();
            }

        } else {
            // I'm the client and a message has received (via QR code scan)
            try {
                // Message received
                String ack = MyUtils.getAckFromMessage(message);
                String content = MyUtils.getContentFromMessage(message);
                String digest = MyUtils.getDigestFromMessage(message);

                // Check the digest
                if (digest.equals(MyUtils.calculateDigest(content))) {
                    // Digest OK.
                    // Check the content.
                    if (content.startsWith(TAG_MAC)){
                        // MAC message, the First QR code of the transmission.
                        // It contains the Server MAC address.
                        // Start the connection with the Server.
                        // DISABLE further QR codes scan, until connection is not established.
                        makeQRscanAvailable(false);
                        String mac = content.substring(TAG_MAC.length());
                        connect(mac);

                    } else if (content.equals(TAG_EOT)) {
                        // EOT message, End of Transmission reached
                        // Send the last ACK message and finish the Activity with success.
                        isFinishingTransmission = true;

                        sendAck(ack);
                        finishTransmissionWithSuccess();

                    } else {
                        // Regular message, add it to the messages ArrayList
                        messages.add(content);
                        sendAck(ack);
                    }

                } else {
                    // Digest error.
                    throw new StringIndexOutOfBoundsException();
                }

            } catch (StringIndexOutOfBoundsException e){
                // The message received is smaller than expected or error on digest.
                e.printStackTrace();

                // Allow the QR to be read again
                clientFragment.resetPreviousMessage();
            }
        }
    }

    /**
     * Allow the Client camera to read QR codes.
     * - Initially is set to FALSE.
     * - Set TRUE when the Client finished discovery.
     * - Set FALSE when the Client has read the MAC QR code and started the connection with
     *   the Server.
     * - Set TRUE when Client and Server are successfully connected via Wifi Direct.
     * - It will remain TRUE until transmission end.
     */
    public void makeQRscanAvailable(boolean value){
        if (!iAmTheServer) {
            clientFragment.canScan(value);
        }
    }

    /**
     * Method called when the transmission ended:
     * - The Client read the QR with EOT and sent the ack.
     * - The Server received the ACK for EOT.
     *
     * This method will not stop the ServerReceiver neither will disconnect Wifi Direct,
     * because that will be done in onStop() method.
     */
    public void finishTransmissionWithSuccess(){
        // Return to the calling Activity
        Intent returnIntent = new Intent();
        returnIntent.putExtra(I_AM_THE_SERVER, iAmTheServer);
        if (!iAmTheServer){
            returnIntent.putStringArrayListExtra(MESSAGES, messages);
        }

        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    /**
     * Method called when an irreversible error occured:
     * - The Client can't connect to the Server via Wifi Direct.
     * - The Server receives a wrong ACK from the Client.
     * - Server or Client disconnected before the transmission ended.
     *
     * This method will not stop the ServerReceiver neither will disconnect Wifi Direct,
     * because that will be done in onStop() method.
     */
    public void finishTransmissionWithError(){
        // Return to the calling Activity
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, returnIntent);
        finish();
    }

    /** Request for a new message to be sent as QR code. */
    public void sendNextMessage(){
        if (messagesIndex < messages.size()) {
            // Get the new message to send as QR code
            String newMessage = messages.get(messagesIndex);
            messagesIndex++;

            if (newMessage.equals(TAG_EOT)){
                // This is the last message, so I'm finishing the transmission
                isFinishingTransmission = true;
            }

            // Send it
            sendMessageAsQR(newMessage);
        }
    }

    /**
     * Send the message to the Client, so the QR ImageView of the Server must be updated.
     * Create a random ACK.
     * Calculate and append the digest to the message.
     *
     * @param messageToSend message to be encoded as a QR code.
     */
    private void sendMessageAsQR(String messageToSend){
        // Create the random ACK
        attendedAck = MyUtils.createRandomString(ACK_LENGTH);

        // Calculate and append the Digest to the message
        String digest = MyUtils.calculateDigest(messageToSend);

        // Update the Server Fragment
        // +1 because the first QR is the Mac address QR
        serverFragment.updateQR(attendedAck + messageToSend + digest, messagesIndex+1, messages.size()+1);
    }

    /**
     * Send a new ACK to the Server.
     *
     * @param ack the Ack to be sent.
     */
    private void sendAck(String ack){
        // Send a new ack message to the Server
        Intent sendMessageIntent = new Intent(TransferActivity.this, ClientAckSender.class);
        sendMessageIntent.setAction(ACTION_SEND_ACK);
        sendMessageIntent.putExtra(ACK, ack);
        sendMessageIntent.putExtra(HOST, serverIPAddress);
        sendMessageIntent.putExtra(PORT, SERVER_PORT);
        startService(sendMessageIntent);
    }

    /** Discovers for peers. Must call this method before the peers connection. */
    public void discoverPeers() {
        manager.discoverPeers(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(DEBUG_TAG, "Discovery initiated");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(DEBUG_TAG, "Discovery failed. Error: " + reasonCode);
            }
        });
    }

    /**
     * Connect to the desired peer.
     *
     * @param deviceMacAddress the MAC address of the Server peer to connect with.
     */
    private void connect(String deviceMacAddress) {
        // Create other device config
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceMacAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0; // I want the other device to be the Group Owner !!

        // Perform connection
        manager.connect(channel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(TransferActivity.this, R.string.aqrdt_error_connection_failed, Toast.LENGTH_SHORT).show();

                // Error during connection to the peer. Force the Activity to be finished.
                finishTransmissionWithError();
            }
        });
    }

    /**
     * Connected with the other peer.
     * This method is called after connection success.
     * GROUP OWNER = Server (receives the ack messages).
     * Update isConnected value and get the Server IP address.
     * If I'm the Server, instantiate the ServerAckReceiver.
     * If I'm the Client, now I'm ready to receive new incoming messages, make QR scan available.
     *
     * @param info the info about this connection.
     */
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        // Now I'm connected
        isConnected = true;

        if (info.groupFormed){
            // Connection established.
            // Get the serverAddress (used for socket connection).
            serverIPAddress = info.groupOwnerAddress.getHostAddress();

            if ((iAmTheServer && !info.isGroupOwner)||(!iAmTheServer && info.isGroupOwner)){
                // Error, the Group Owner is not also the Server, devices must be inverted!
                Toast.makeText(getApplicationContext(), R.string.aqrdt_error_invert_devices, Toast.LENGTH_SHORT).show();

            } else if (iAmTheServer) {
                // I'm the Server, instantiate the ServerAckReceiver
                if (mServerReceiver == null) {
                    mServerReceiver = new ServerAckReceiver(TransferActivity.this);
                    mServerReceiver.execute();
                }

            } else {
                // I'm the Client, make QR code scan available.
                makeQRscanAvailable(true);
            }
        }
    }

    /** Disconnect from Wifi Direct. */
    private void disconnect() {
        // Now I'm disconnected.
        isConnected = false;

        // Remove this Wifi Direct group.
        manager.removeGroup(channel, new ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d(DEBUG_TAG, "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                Log.d(DEBUG_TAG, "Disconnected and group removed");
            }
        });
    }

    @Override
    public void onChannelDisconnected() {
        // Try connecting again
        if (manager != null && !retryConnection) {
            Toast.makeText(TransferActivity.this, R.string.aqrdt_error_channel_lost_retry, Toast.LENGTH_LONG).show();
            retryConnection = true;
            manager.initialize(this, getMainLooper(), this);

        } else {
            Toast.makeText(this, R.string.aqrdt_error_channel_lost, Toast.LENGTH_LONG).show();
        }
    }
}