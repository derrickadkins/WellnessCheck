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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PermissionsRequestingActivity extends AppCompatActivity {
    private final String TAG = "PermissionsRequestingActivity";
    private static Map<String, Integer> permissionsToAPI;
    private PermissionsListener permissionsListener;

    public void checkPermissions(String[] permissions, PermissionsListener permissionsListener) {
        this.permissionsListener = permissionsListener;
        if(permissionsToAPI == null) initMap();
        List<String> deniedPermissions = new ArrayList<>();

        for(String permission : permissions) {
            if (!permissionsToAPI.containsKey(permission)) continue;
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
        } else permissionsListener.permissionsGranted();
    }

    private void initMap(){
        permissionsToAPI = new HashMap<>();
        permissionsToAPI.put(Manifest.permission.READ_CONTACTS, Build.VERSION_CODES.BASE);
        permissionsToAPI.put(Manifest.permission.SEND_SMS, Build.VERSION_CODES.BASE);
        permissionsToAPI.put(Manifest.permission.READ_SMS, Build.VERSION_CODES.BASE);
        permissionsToAPI.put(Manifest.permission.RECEIVE_SMS, Build.VERSION_CODES.BASE);
        permissionsToAPI.put(Manifest.permission.RECEIVE_BOOT_COMPLETED, Build.VERSION_CODES.BASE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            permissionsToAPI.put(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Build.VERSION_CODES.M);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            permissionsToAPI.put(Manifest.permission.FOREGROUND_SERVICE, Build.VERSION_CODES.P);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            permissionsToAPI.put(Manifest.permission.SCHEDULE_EXACT_ALARM, Build.VERSION_CODES.S);
    }

    private ActivityResultLauncher<String[]> permissionsResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    for (String permission : result.keySet())
                        if (!result.get(permission)) {
                            permissionsListener.permissionsDenied();
                            return;
                        }
                    permissionsListener.permissionsGranted();
                }
            });
}
