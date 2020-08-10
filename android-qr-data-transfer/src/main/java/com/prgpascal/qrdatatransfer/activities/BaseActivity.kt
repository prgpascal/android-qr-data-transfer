package com.prgpascal.qrdatatransfer.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.prgpascal.qrdatatransfer.R
import java.util.*

abstract class BaseActivity : FragmentActivity() {
    companion object {
        private const val PERMISSIONS_REQUEST = 111
        private val PERMISSIONS = arrayOf(
                Manifest.permission.CAMERA
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