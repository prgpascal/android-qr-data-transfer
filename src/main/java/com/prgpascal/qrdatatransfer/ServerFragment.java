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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;


/**
 * Fragment that contains the ImageView used for QR transmission.
 * For the QR generation I used the a code found here:
 * http://stackoverflow.com/questions/28827407/generate-designer-2d-qr-code-in-android/30529519#30529519
 */
public class ServerFragment extends Fragment {
    private ImageView qrImageView;          // ImageView for the QR code.
    private TextView qrInfoTextView;        // TextView that counts the QR codes.
    private final int QR_WIDTH = 600;       // Width of the QR-code.
    private final int QR_HEIGTH = 600;      // Height of the QR-code.


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View mContentView = inflater.inflate(R.layout.aqrdt_server_fragment, null);

        return mContentView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // QR ImageView and info TextView
        qrImageView = (ImageView) getView().findViewById(R.id.qr_viewer);
        qrInfoTextView = (TextView) getView().findViewById(R.id.qr_info);
    }



    /**
     * Update the QR ImageView with a new QR containing the given message.
     *
     * @param message the message to be encoded into a new QR code.
     */
    public void updateQR(String message, int currentQrNumber, int totalQrNumber) {
        try {
            // Encode the QR message
            Bitmap bitmap = MyUtils.encodeAsBitmap(QR_WIDTH, QR_HEIGTH, message);

            // Update the QR View
            qrImageView.setImageBitmap(bitmap);

            // Update the QR Infos
            qrInfoTextView.setText("QR "+currentQrNumber+" / "+totalQrNumber);

        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}