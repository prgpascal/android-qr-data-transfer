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

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import java.util.List;

/**
 * Fragment that contains the QR code reader.
 * It is instantiated by the Client.
 */
public class ClientFragment extends Fragment {
    private CompoundBarcodeView barcodeView;    // BarcodeView
    private TransferActivity activity;          // Main Activity of this Fragment
    private String previousMessage = "";        // Used to check if a message has already been read
    private boolean canScan = false;            // When true the camera can read QR codes.

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View mContentView = inflater.inflate(R.layout.aqrdt_client_fragment, null);

        return mContentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Get the Activity instance
        activity = (TransferActivity)getActivity();

        // Set the BarcodeView
        barcodeView = (CompoundBarcodeView) getView().findViewById(R.id.qr_scanner);
        barcodeView.decodeContinuous(callback);
    }

    /**
     * Callback for the Barcode reader.
     * It receives BarcodeResult object that contains the incoming message.
     * This callback will read for incoming messages and if canScan is set to TRUE, it passes them to
     * the main Activity, which will parse it.
     * If a message has already been read, or canScan is FALSE, ignore it.
     */
    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            // Message read from QR
            if (result.getText() != null) {

                if (canScan) {
                    // Get the message and check if it is a new message
                    String message = result.getText();

                    if (!message.equals(previousMessage)) {
                        // Woooh, new message arrived !!
                        // Let's vibrate for a while
                        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(200);

                        // Disable next lectures of the same QR
                        previousMessage = message;

                        // Pass it to the activity
                        activity.messageReceived(message);

                    } else {
                        // Message already read... do nothing...
                    }
                }
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    /**
     * Reset the previously read message.
     * Called when an error occured during the QR scan.
     * The QR code can now be read again.
     */
    public void resetPreviousMessage(){
        previousMessage = "";
    }

    /**
     * Tell the QR code reader if it can read for QR codes or not.
     * It is set to TRUE when the Activity is ready to obtain QR messages from this Fragment.
     */
    public void canScan(boolean canScan){
        this.canScan = canScan;
    }

    @Override
    public void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}