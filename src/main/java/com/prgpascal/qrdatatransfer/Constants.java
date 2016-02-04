package com.prgpascal.qrdatatransfer;

/**
 * Class that provides constants used by more than an Activity.
 * I use a final class because it is considered a better approach instead of using interfaces.
 *
 * http://stackoverflow.com/questions/2659593/what-is-the-use-of-interface-constants
 * https://en.wikipedia.org/wiki/Constant_interface
 */
public final class Constants {

    /** Private constructor */
    private Constants() {
        // restrict instantiation
    }

    /** Intent params */
    public static final String I_AM_THE_SERVER = "i_am_the_server";     // Indicates if the current device may act as Client or Server
    public static final String MESSAGES = "messages";                   // Used for messages ArrayList exchange


    /** Client Ack Sender params */
    public static final String ACTION_SEND_ACK = "send_ack";        // Used for the ClientAckSender
    public static final String ACK = "ack";                         /* ... */
    public static final String HOST = "host";                       /* ... */
    public static final String PORT = "port";                       /* ... */


    /** Socket params */
    public static final int SOCKET_TIMEOUT = 10000;          // Timeout for the socket
    public static final int SERVER_PORT = 8988;              // Incoming connection port for the Server


    /** Messages params */
    public static final String TAG_EOT = "OTP_EOT";         // End Of Transmission message
    public static final String TAG_MAC = "OTP_MAC:";        // MAC address message
    public static final int DIGEST_LENGTH = 32;             // Number of bytes (characters) of the digest
    public static final int ACK_LENGTH = 2;                 // Number of bytes of the ACK

    /** Debug */
    public static final String DEBUG_TAG = "qr-data-transfer";         // Used for debug

}