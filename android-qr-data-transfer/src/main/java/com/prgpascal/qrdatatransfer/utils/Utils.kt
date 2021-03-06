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
package com.prgpascal.qrdatatransfer.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.Surface
import android.view.WindowManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*

const val WHITE = -0x1
const val BLACK = -0x1000000
const val CHARACTER_SET_EXPANDED = "ISO-8859-1"
const val CHARACTER_SET = "ISO8859_1"
const val SHA_ALGORITHM = "SHA-256"


val T_UUID: UUID = UUID.fromString("974e8deb-2232-40fd-b56c-7a4c9b298248")

const val TAG_EOT = "OTP_EOT"   // End Of Transmission message
const val DIGEST_LENGTH = 32    // Number of bytes (characters) of the digest
const val ACK_LENGTH = 2        // Number of bytes of the ACK


fun encodeAsBitmap(width: Int, height: Int, message: String): Bitmap {
    try {
        val hints: MutableMap<EncodeHintType, Any?> = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H // high correction level
        hints[EncodeHintType.CHARACTER_SET] = CHARACTER_SET

        val result = MultiFormatWriter().encode(message, BarcodeFormat.QR_CODE, width, height, hints)
        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result[x, y]) BLACK else WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    } catch (e: Exception) {
        throw EncodeException(e)
    }
}

fun calculateDigest(message: String): String? {
    try {
        val sha256 = MessageDigest.getInstance(SHA_ALGORITHM)
        val messageBytes = message.toByteArray(charset(CHARACTER_SET_EXPANDED))
        val hash = sha256.digest(messageBytes)
        return encodeISO88591(hash)
    } catch (ue: UnsupportedEncodingException) {
        ue.printStackTrace()
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }
    return null
}

fun encodeISO88591(bytes: ByteArray?): String? {
    try {
        return String(bytes!!, charset(CHARACTER_SET_EXPANDED))
    } catch (e: UnsupportedEncodingException) {
        e.printStackTrace()
    }
    return null
}

fun getAckFromMessage(message: String): String {
    return message.substring(0, ACK_LENGTH)
}

fun getDigestFromMessage(message: String): String {
    return message.substring(message.length - DIGEST_LENGTH)
}

fun getContentFromMessage(message: String): String {
    return message.substring(ACK_LENGTH, message.length - DIGEST_LENGTH)
}

fun createRandomString(numberOfBytes: Int): String? {
    val random = SecureRandom()
    val bytes = ByteArray(numberOfBytes)
    random.nextBytes(bytes)
    return encodeISO88591(bytes)
}

fun preventScreenRotation(context: Activity) {
    context.requestedOrientation = currentScreenOrientation(context)
}

private fun currentScreenOrientation(context: Context): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return when (context.resources.configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            val rotation = windowManager.defaultDisplay.rotation
            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_180) {
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        Configuration.ORIENTATION_LANDSCAPE -> {
            val rotation = windowManager.defaultDisplay.rotation
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
        }
        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}