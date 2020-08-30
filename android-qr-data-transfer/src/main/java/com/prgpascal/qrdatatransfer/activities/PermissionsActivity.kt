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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.prgpascal.qrdatatransfer.R
import java.util.*

abstract class PermissionsActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSIONS_REQUEST = 111
        private val PERMISSIONS = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private var mIsResolvingPermissionRequests = false

    protected fun checkAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requiredPermissions = ArrayList<String>()
            for (permission in PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    requiredPermissions.add(permission)
                }
            }
            if (requiredPermissions.size > 0) {
                if (!mIsResolvingPermissionRequests) {
                    mIsResolvingPermissionRequests = true
                    ActivityCompat.requestPermissions(this, requiredPermissions.toTypedArray(), PERMISSIONS_REQUEST)
                }
            } else {
                permissionsGranted()
            }
        } else {
            permissionsGranted()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            mIsResolvingPermissionRequests = false
            var allPermissionsGranted = false
            if (grantResults.isNotEmpty()) {
                allPermissionsGranted = true
                for (permission in grantResults) {
                    if (permission != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false
                        break
                    }
                }
            }
            if (!allPermissionsGranted) {
                Toast.makeText(applicationContext, getString(R.string.aqrdt_error_no_permissions), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                permissionsGranted()
            }
        }
    }

    abstract fun permissionsGranted()

}