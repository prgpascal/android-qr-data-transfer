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

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.zxing.WriterException
import com.prgpascal.qrdatatransfer.R
import com.prgpascal.qrdatatransfer.utils.QrMessage
import com.prgpascal.qrdatatransfer.utils.encodeAsBitmap
import kotlinx.android.synthetic.main.aqrdt_server_fragment.*

class ServerFragment(private val firstMessage: QrMessage?) : Fragment() {
    private val qrWidth = 600
    private val qrHeight = 600

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.aqrdt_server_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (firstMessage != null) {
            updateQR(firstMessage)
        }
    }

    fun updateQR(qrMessage: QrMessage) {
        try {
            val bitmap: Bitmap = encodeAsBitmap(qrWidth, qrHeight, qrMessage.message)
            qrViewer.setImageBitmap(bitmap)
            qrInfo.text = getString(R.string.aqrt_qr_counter, qrMessage.currentQrNumber, qrMessage.totalQrNumber)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}