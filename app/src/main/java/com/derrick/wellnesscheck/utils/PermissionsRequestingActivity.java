package com.derrick.wellnesscheck.utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.derrick.wellnesscheck.model.data.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PermissionsRequestingActivity extends AppCompatActivity {
    private final String TAG = "PermissionsRequestingActivity";
    private static List<String> permissions;
    private PermissionsListener permissionsListener;

    public void checkPermissions(String[] permissions, PermissionsListener permissionsListener) {
        this.permissionsListener = permissionsListener;
        if(PermissionsRequestingActivity.permissions == null) initPermissions();
        List<String> deniedPermissions = new ArrayList<>();

        for(String permission : permissions) {
            if (!PermissionsRequestingActivity.permissions.contains(permission)) continue;
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, permission + ": GRANTED");
            } else {
                Log.d(TAG, permission + ": DENIED");
                deniedPermissions.add(permission);
            }
        }

        if (deniedPermissions.size() > 0) {
            boolean showRationale = false;
            List<String> permissionsToRationalize = new ArrayList<>();
            for(String permission : deniedPermissions){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    Log.d(TAG, permission + ": should show rationale");
                    permissionsToRationalize.add(permission);
                    showRationale = true;
                }
            }
            if (showRationale) {
                if(permissionsListener != null)
                    permissionsListener.showRationale(permissionsToRationalize.toArray(new String[permissionsToRationalize.size()]));
            } else {
                Log.d(TAG, "requesting permission: " + permissions.toString());
                permissionsResult.launch(deniedPermissions.toArray(new String[deniedPermissions.size()]));
            }
        } else if(permissionsListener != null) permissionsListener.permissionsGranted();
    }

    private void initPermissions(){
        permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        permissions.add(Manifest.permission.INTERNET);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.CALL_PHONE);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.RECEIVE_SMS);
        permissions.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            permissions.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM);
    }

    private ActivityResultLauncher<String[]> permissionsResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    for (String permission : result.keySet())
                        if (!result.get(permission)) {
                            if(permissionsListener != null) permissionsListener.permissionsDenied();
                            return;
                        }
                    if(permissionsListener != null) permissionsListener.permissionsGranted();
                }
            });
}
