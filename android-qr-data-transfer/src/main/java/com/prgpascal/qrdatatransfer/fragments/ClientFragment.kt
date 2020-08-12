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

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.prgpascal.qrdatatransfer.R
import kotlinx.android.synthetic.main.aqrdt_client_fragment.*

/**
 * Fragment that contains the QR code reader.
 * It is instantiated by the Client.
 */
class ClientFragment : Fragment() {
    private lateinit var clientCallback: ClientInterface
    private var canScan = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.aqrdt_client_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        clientCallback = activity as ClientInterface
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        qrScanner.decodeContinuous(barcodeCallback)
    }

    /**
     * Callback for the Barcode reader.
     * It receives BarcodeResult object that contains the incoming message.
     * This callback will read for incoming messages and if canScan is set to TRUE, it passes them to
     * the Activity, which will parse it.
     * If a message has already been read, or canScan is FALSE, it will be ignored.
     */
    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (!TextUtils.isEmpty(result.text) && canScan) {
                clientCallback.messageReceived(result.text)
            }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    fun canScan(canScan: Boolean) {
        this.canScan = canScan
    }

    override fun onResume() {
        super.onResume()
        qrScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        qrScanner.pause()
    }
}

interface ClientInterface {
    fun messageReceived(message: String)
}