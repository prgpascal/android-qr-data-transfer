package com.prgpascal.qrdatatransfer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public abstract class BaseActivity extends FragmentActivity {
    private static final int PERMISSIONS_REQUEST = 111;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

    private boolean mIsResolvingPermissionRequests;

    protected void checkAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> requiredPermissions = new ArrayList<>();
            for (String permission : PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    requiredPermissions.add(permission);
                }
            }

            if (requiredPermissions.size() > 0) {
                if (!mIsResolvingPermissionRequests) {
                    mIsResolvingPermissionRequests = true;
                    ActivityCompat.requestPermissions(this, requiredPermissions.toArray(new String[0]), PERMISSIONS_REQUEST);
                }
            } else {
                permissionsGranted();
            }
        } else {
            permissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST) {
            mIsResolvingPermissionRequests = false;
            boolean allPermissionsGranted = false;

            if (grantResults.length > 0) {
                allPermissionsGranted = true;
                for (int permission : grantResults) {
                    if (permission != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
            }

            if (!allPermissionsGranted) {
                Toast.makeText(getApplicationContext(), getString(R.string.aqrdt_error_no_permissions), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                permissionsGranted();
            }
        }
    }

    public abstract void permissionsGranted();

}
