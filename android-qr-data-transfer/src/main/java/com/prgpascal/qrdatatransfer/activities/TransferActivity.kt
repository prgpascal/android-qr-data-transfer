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
package com.prgpascal.qrdatatransfer.activities

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ChannelListener
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.prgpascal.qrdatatransfer.R
import com.prgpascal.qrdatatransfer.fragments.ClientFragment
import com.prgpascal.qrdatatransfer.fragments.ServerFragment
import com.prgpascal.qrdatatransfer.services.ClientAckSender
import com.prgpascal.qrdatatransfer.services.ServerAckReceiver
import com.prgpascal.qrdatatransfer.services.WiFiDirectBroadcastReceiver
import com.prgpascal.qrdatatransfer.utils.*
import java.util.*

/**
 * Activity that performs the transmission of messages between the Client and the Server.
 */
class TransferActivity : BaseActivity(), ChannelListener, ConnectionInfoListener {
    companion object {
        const val PARAM_I_AM_THE_SERVER = I_AM_THE_SERVER
        const val PARAM_MESSAGES = MESSAGES
    }

    // Wifi Direct objects
    private lateinit var intentFilter: IntentFilter
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver

    // ServerAckReceiver
    private var mServerReceiver: ServerAckReceiver? = null
    private var iAmTheServer = false
    private var serverIPAddress: String? = null

    // Client/Server fragments (only one of these will be instantiated)
    private var serverFragment: ServerFragment? = null
    private var clientFragment: ClientFragment? = null

    // Messages to be exchanged
    private var messages = ArrayList<String>()
    private var messagesIndex = 0
    private var attendedAck: String? = null

    // About the transmission
    private var retryConnection = false         // TRUE when I'm trying to re-connect after an error occurred.
    private var peerDiscoveryFinished = false   // TRUE when the peer discovery has finished.
    var isFinishingTransmission = false         // TRUE when the Server sends the EOT message, so it's in "finishing" state.
    var isConnected = false                     // TRUE when the Client and Server are connected each other via Wifi Direct.

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent from Screen rotation
        lockScreen(this)

        // First, get the Intent Extras...
        getIntentExtras(intent.extras)

        intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        receiver = WiFiDirectBroadcastReceiver(manager, channel, this)

        deletePreviousPersistentGroups()
        checkAppPermissions()
    }

    override fun permissionsGranted() {
        createLayout()
    }

    /**
     * If one or more required parameter are missing, finish the Activity with an error.
     */
    private fun getIntentExtras(extras: Bundle?) {
        if (extras != null && extras.containsKey(I_AM_THE_SERVER)) {
            iAmTheServer = extras.getBoolean(I_AM_THE_SERVER)
            if (iAmTheServer) {
                if (extras.containsKey(MESSAGES)) {
                    messages = intent.getStringArrayListExtra(MESSAGES)

                    // Append the End Of Transmission (EOT) as the last message.
                    messages.add(TAG_EOT)
                } else {
                    finishTransmissionWithError()
                }
            }
        } else {
            finishTransmissionWithError()
        }
    }

    /**
     * Delete all previous persistent Wifi Direct groups, for privacy reasons and
     * to avoid the reuse of previously created groups.
     * http://stackoverflow.com/questions/15152817/persistent-group-in-wi-fi-direct
     */
    private fun deletePreviousPersistentGroups() {
        try {
            val methods = WifiP2pManager::class.java.methods
            for (i in methods.indices) {
                if (methods[i].name == "deletePersistentGroup") {
                    for (netid in 0..31) {
                        methods[i].invoke(manager, channel, netid, null)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    public override fun onStop() {
        super.onStop()
        disconnect()
        if (iAmTheServer) {
            stopServerReceiver()
        }
    }

    private fun stopServerReceiver() {
        try {
            mServerReceiver?.stopSocket()
            mServerReceiver?.cancel(true)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    private fun createLayout() {
        setContentView(R.layout.aqrdt_transfer_activity)
        if (iAmTheServer) {
            serverFragment = ServerFragment()
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, serverFragment!!).commit()
        } else {
            clientFragment = ClientFragment()
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, clientFragment!!).commit()
        }
    }

    /**
     * Called when info about this device are available.
     * If this device is the Server, create the QR code with the MAC address.
     *
     * @param thisDeviceMAC the MAC address of this device
     */
    fun updateThisDevice(thisDeviceMAC: String) {
        if (iAmTheServer) {
            // Create the first message to send as QR code.
            // At this point Client and Server ar not connected via Wifi Direct yet.
            // This QR code will contain the Server MAC address.
            sendMessageAsQR(TAG_MAC.toString() + thisDeviceMAC)
        }
    }

    /**
     * Called when the peers list has changed.
     * This method is called the first time when the peer discovery has finished.
     * Set the value of peerDiscoveryFinished as true.
     * Now that the peer discovery has finished we can enable the QR scan.
     */
    fun peersChanged() {
        if (!iAmTheServer && !peerDiscoveryFinished) {
            peerDiscoveryFinished = true
            makeQRscanAvailable(true)
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
    fun messageReceived(message: String) {
        if (iAmTheServer) {
            // I'm the Server and a ACK has received (via Wifi Direct)
            if (message == attendedAck) {
                // The Ack is correct.
                // If available, next QR will be created by ServerAckReceiver.

                // Now, check if I've reached the EOT.
                if (isFinishingTransmission) {
                    // EOT ack received, End of Transmission reached.
                    // Finish the transmission with success.
                    finishTransmissionWithSuccess()
                }
            } else {
                // The Ack is incorrect (irreversible error).
                // Finish the Activity.
                finishTransmissionWithError()
            }
        } else {
            // I'm the client and a message has received (via QR code scan)
            try {
                // Message received
                val ack: String = getAckFromMessage(message)
                val content: String = getContentFromMessage(message)
                val digest: String = getDigestFromMessage(message)

                // Check the digest
                if (digest == calculateDigest(content)) {
                    // Digest OK.
                    // Check the content.
                    when {
                        content.startsWith(TAG_MAC) -> {
                            // MAC message, the First QR code of the transmission.
                            // It contains the Server MAC address.
                            // Start the connection with the Server.
                            // DISABLE further QR codes scan, until connection is not established.
                            makeQRscanAvailable(false)
                            val mac: String = content.substring(TAG_MAC.length)
                            connect(mac)
                        }
                        content == TAG_EOT -> {
                            // EOT message, End of Transmission reached
                            // Send the last ACK message and finish the Activity with success.
                            isFinishingTransmission = true
                            sendAckToTheServer(ack)
                            finishTransmissionWithSuccess()
                        }
                        else -> {
                            // Regular message, add it to the messages ArrayList
                            messages.add(content)
                            sendAckToTheServer(ack)
                        }
                    }
                } else {
                    // Digest error
                    throw StringIndexOutOfBoundsException()
                }
            } catch (e: StringIndexOutOfBoundsException) {
                // The message received is smaller than expected or error on digest.
                e.printStackTrace()

                // Allow the QR to be read again
                clientFragment!!.resetPreviousMessage()
            }
        }
    }

    /**
     * Allow the Client camera to read QR codes.
     * - Initially is set to FALSE.
     * - Set TRUE when the Client finished discovery.
     * - Set FALSE when the Client has read the MAC QR code and started the connection with
     * the Server.
     * - Set TRUE when Client and Server are successfully connected via Wifi Direct.
     * - It will remain TRUE until transmission end.
     */
   private fun makeQRscanAvailable(value: Boolean) {
        if (!iAmTheServer) {
            clientFragment!!.canScan(value)
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
   private fun finishTransmissionWithSuccess() {
        val returnIntent = Intent()
        returnIntent.putExtra(I_AM_THE_SERVER, iAmTheServer)
        if (!iAmTheServer) {
            returnIntent.putStringArrayListExtra(MESSAGES, messages)
        }
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
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
    fun finishTransmissionWithError() {
        val returnIntent = Intent()
        setResult(Activity.RESULT_CANCELED, returnIntent)
        finish()
    }

    /** Request for a new message to be sent as QR code. */
    fun sendNextMessage() {
        if (messagesIndex < messages.size) {
            // Get the new message to send as QR code
            val newMessage = messages[messagesIndex]
            messagesIndex++
            if (newMessage == TAG_EOT) {
                // This is the last message, so I'm finishing the transmission
                isFinishingTransmission = true
            }

            sendMessageAsQR(newMessage)
        }
    }

    /**
     * Send the message to the Client, so the QR ImageView of the Server must be updated.
     * Create a random ACK.
     * Calculate and append the digest to the message.
     *
     * @param messageToSend message to be encoded as a QR code.
     */
    private fun sendMessageAsQR(messageToSend: String) {
        attendedAck = createRandomString(ACK_LENGTH)

        val digest: String? = calculateDigest(messageToSend)

        // Update the Server Fragment
        // +1 because the first QR is the Mac address QR
        serverFragment!!.updateQR(attendedAck + messageToSend + digest, messagesIndex + 1, messages.size + 1)
    }

    private fun sendAckToTheServer(ack: String) {
        val sendMessageIntent = Intent(this, ClientAckSender::class.java)
        sendMessageIntent.action = ACTION_SEND_ACK
        sendMessageIntent.putExtra(ACK, ack)
        sendMessageIntent.putExtra(HOST, serverIPAddress)
        sendMessageIntent.putExtra(PORT, SERVER_PORT)
        startService(sendMessageIntent)
    }

    /** Discovers for peers. Must call this method before the peers connection.  */
    fun discoverPeers() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(DEBUG_TAG, "Discovery initiated")
            }

            override fun onFailure(reasonCode: Int) {
                Log.d(DEBUG_TAG, "Discovery failed. Error: $reasonCode")
            }
        })
    }

    /**
     * Connect to the desired peer.
     *
     * @param deviceMacAddress the MAC address of the Server peer to connect with.
     */
    private fun connect(deviceMacAddress: String) {
        val otherDeviceConfig = WifiP2pConfig()
        otherDeviceConfig.deviceAddress = deviceMacAddress
        otherDeviceConfig.wps.setup = WpsInfo.PBC
        otherDeviceConfig.groupOwnerIntent = 0 // I want the other device to be the Group Owner !!

        manager.connect(channel, otherDeviceConfig, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(applicationContext, R.string.aqrdt_error_connection_failed, Toast.LENGTH_SHORT).show()
                finishTransmissionWithError()
            }
        })
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
    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        isConnected = true
        if (info.groupFormed) {
            // Get the serverAddress (used for socket connection).
            serverIPAddress = info.groupOwnerAddress.hostAddress
            if (iAmTheServer && !info.isGroupOwner || !iAmTheServer && info.isGroupOwner) {
                // Error, the Group Owner is not also the Server, devices must be inverted!
                Toast.makeText(applicationContext, R.string.aqrdt_error_invert_devices, Toast.LENGTH_SHORT).show()
            } else if (iAmTheServer) {
                // I'm the Server, instantiate the ServerAckReceiver
                if (mServerReceiver == null) {
                    mServerReceiver = ServerAckReceiver(this@TransferActivity)
                    mServerReceiver!!.execute()
                }
            } else {
                // I'm the Client, make QR code scan available.
                makeQRscanAvailable(true)
            }
        }
    }

    /** Disconnect from Wifi Direct.  */
    private fun disconnect() {
        isConnected = false

        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Log.d(DEBUG_TAG, "Disconnect failed. Reason :$reasonCode")
            }

            override fun onSuccess() {
                Log.d(DEBUG_TAG, "Disconnected and group removed")
            }
        })
    }

    override fun onChannelDisconnected() {
        if (!retryConnection) {
            Toast.makeText(this@TransferActivity, R.string.aqrdt_error_channel_lost_retry, Toast.LENGTH_LONG).show()
            retryConnection = true
            manager.initialize(this, mainLooper, this)
        } else {
            Toast.makeText(this, R.string.aqrdt_error_channel_lost, Toast.LENGTH_LONG).show()
        }
    }
}