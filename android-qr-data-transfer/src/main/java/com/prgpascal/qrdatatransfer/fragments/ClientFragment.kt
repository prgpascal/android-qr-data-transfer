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
package com.prgpascal.qrdatatransfer.fragments

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.prgpascal.qrdatatransfer.R
import com.prgpascal.qrdatatransfer.activities.TransferActivity
import kotlinx.android.synthetic.main.aqrdt_client_fragment.*

/**
 * Fragment that contains the QR code reader.
 * It is instantiated by the Client.
 */
class ClientFragment : Fragment() {
    private lateinit var activity: TransferActivity
    private var previousMessage = ""
    private var canScan = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.aqrdt_client_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity = getActivity() as TransferActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        qr_scanner.decodeContinuous(callback)
    }

    /**
     * Callback for the Barcode reader.
     * It receives BarcodeResult object that contains the incoming message.
     * This callback will read for incoming messages and if canScan is set to TRUE, it passes them to
     * the main Activity, which will parse it.
     * If a message has already been read, or canScan is FALSE, ignore it.
     */
    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (result.text != null) {
                if (canScan) {
                    val message = result.text
                    if (message != previousMessage) {
                        // Woooh, new message arrived !!
                        // Let's vibrate for a while
                        val v = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        v.vibrate(200)

                        // Disable next lectures of the same QR
                        previousMessage = message

                        // Pass it to the activity
                        activity.messageReceived(message)
                    }
                }
            }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    /**
     * Reset the previously read message.
     * Called when an error occurred during the QR scan.
     * The QR code can now be read again.
     */
    fun resetPreviousMessage() {
        previousMessage = ""
    }

    /**
     * Tell the QR code reader if it can read for QR codes or not.
     * It is set to TRUE when the Activity is ready to obtain QR messages from this Fragment.
     */
    fun canScan(canScan: Boolean) {
        this.canScan = canScan
    }

    override fun onResume() {
        super.onResume()
        qr_scanner.resume()
    }

    override fun onPause() {
        super.onPause()
        qr_scanner.pause()
    }
}