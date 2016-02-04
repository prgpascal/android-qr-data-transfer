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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.EnumMap;
import java.util.Map;

import static com.prgpascal.qrdatatransfer.Constants.ACK_LENGTH;
import static com.prgpascal.qrdatatransfer.Constants.DIGEST_LENGTH;

/**
 * Class that provides some useful functions.
 */
public class MyUtils {

    /**
     * Method that encodes a message into a QR bitmap.
     *
     * @param width width of the bitmap
     * @param height height of the bitmap
     * @param message message to be encoded
     * @return the Bitmap of encoded QR code
     */
    public static Bitmap encodeAsBitmap(int width, int height, String message)
            throws WriterException {
        BitMatrix result;

        int WHITE = 0xFFFFFFFF;
        int BLACK = 0xFF000000;

        // Encode Hints
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        //hints.put(EncodeHintType.MARGIN, 2); /* default = 4 */
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // high correction level
        hints.put(EncodeHintType.CHARACTER_SET, "ISO8859_1");

        try {
            result = new MultiFormatWriter().encode(message,
                    BarcodeFormat.QR_CODE, width, height, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }

        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }



    /**
     * Method that calculates the SHA-256 digest of a ISO-8859-1 String.
     *
     * @param message ISO-8859-1 String.
     * @return ISO-8859-1 digest String (32 bytes) */
    public static String calculateDigest(String message) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] messageBytes = message.getBytes("ISO-8859-1");
            byte[] hash = sha256.digest(messageBytes);

            return encodeISO88591(hash);

        } catch (UnsupportedEncodingException ue){
            ue.printStackTrace();
        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }

        return null;
    }



    /**
     * Encode a byte Array into ISO-8859-1 String.
     * Null if encode failed.
     *
     * @param bytes byte Array to be encoded.
     * @return ISO-8859-1 String representation.
     */
    public static String encodeISO88591(byte[] bytes){
        try {
            // byte[] to ISO-8859-1 String
            String encoded = new String(bytes, "ISO-8859-1");

            return encoded;

        } catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }

        return null;
    }



    /**
     * Method that returns the ACK of a given message.
     * (The first 2 bytes)
     *
     * @param message the message that contains the ack.
     * @return the ack of a message (first 2 bytes).
     */
    public static String getAckFromMessage(String message){
        return message.substring(0, ACK_LENGTH);
    }



    /**
     * Method that returns the digest of a given message.
     * (The last 32 bytes)
     *
     * @param message the message that contains content + digest.
     * @return the digest of message (last 32 bytes).
     */
    public static String getDigestFromMessage(String message){
        return message.substring(message.length() - DIGEST_LENGTH);
    }



    /**
     * Method that excludes the ACK and the digest from the message.
     * Return the message content.
     *
     * @param message the message.
     * @return the content of the message.
     */
    public static String getContentFromMessage(String message){
        return message.substring(ACK_LENGTH, message.length() - DIGEST_LENGTH);
    }



    /**
     * Create an ISO-8859-1 String with random bytes.
     * It uses SecureRandom for a strong secure bytes generation.
     *
     * @param numberOfBytes number of output bytes.
     * @return ISO-8859-1 String with random bytes.
     */
    public static String createRandomString(int numberOfBytes){
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[numberOfBytes];
        random.nextBytes(bytes);

        return encodeISO88591(bytes);
    }

}