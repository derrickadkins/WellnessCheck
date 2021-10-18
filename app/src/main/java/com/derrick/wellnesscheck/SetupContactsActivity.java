package com.derrick.wellnesscheck;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

public class SetupContactsActivity extends AppCompatActivity {
    EmergencyContactsFragment contactsFragment;
    final String TAG = "SetupContactActivity";
    public PermissionsListener permissionsListener;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contactsFragment = new EmergencyContactsFragment();
        setContentView(R.layout.activity_setup);
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_setup_layout, contactsFragment).commit();
    }

    void readSMS(){
        // public static final String INBOX = "content://sms/inbox";
        // public static final String SENT = "content://sms/sent";
        // public static final String DRAFT = "content://sms/draft";
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                String msgData = "";
                for(int idx=0;idx<cursor.getColumnCount();idx++)
                {
                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                }
                // use msgData
            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
        }
    }
}
