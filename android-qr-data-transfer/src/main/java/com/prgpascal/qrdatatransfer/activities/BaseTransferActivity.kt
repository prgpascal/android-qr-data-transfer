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
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import com.prgpascal.qrdatatransfer.utils.TransferParams.Companion.ERROR_BT_DISABLED
import com.prgpascal.qrdatatransfer.utils.TransferParams.Companion.ERROR_BT_NOT_AVAILABLE
import com.prgpascal.qrdatatransfer.utils.TransferParams.Companion.ERROR_PERMISSIONS_NOT_GRANTED
import com.prgpascal.qrdatatransfer.utils.TransferParams.Companion.PARAM_ERROR
import com.prgpascal.qrdatatransfer.utils.preventScreenRotation

abstract class BaseTransferActivity : PermissionsActivity() {
    var isFinishingTransmission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preventScreenRotation(this)
        checkAppPermissions()
    }

    override fun permissionsGranted() {
        checkBluetooth()
    }

    override fun permissionsNotGranted() {
        finishTransmissionWithError(error = ERROR_PERMISSIONS_NOT_GRANTED)
    }

    private fun checkBluetooth() {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            finishTransmissionWithError(error = ERROR_BT_NOT_AVAILABLE)
        } else if (!btAdapter.isEnabled) {
            finishTransmissionWithError(error = ERROR_BT_DISABLED)
        } else {
            createLayout()
        }
    }

    abstract fun createLayout()

    fun finishTransmissionWithError(error: String? = null) {
        val returnIntent = Intent()
        if (error != null) {
            returnIntent.putExtra(PARAM_ERROR, error)
        }
        setResult(Activity.RESULT_CANCELED, returnIntent)
        finish()
    }

}