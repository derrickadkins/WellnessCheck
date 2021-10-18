package com.derrick.wellnesscheck;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

public abstract class PermissionsRequestingActivity extends AppCompatActivity {
    PermissionsListener permissionsListener;
    ActivityResultLauncher<String[]> smsPermissionsResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    for (String permission : result.keySet())
                        if (!result.get(permission)) {
                            if(permissionsListener != null) permissionsListener.permissionGranted(false);
                            return;
                        }
                    if(permissionsListener != null) permissionsListener.permissionGranted(true);
                }
            });
}
