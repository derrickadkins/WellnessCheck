package com.derrick.wellnesscheck;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SetupContactsActivity extends AppCompatActivity {
    EmergencyContactsFragment contactsFragment;
    final String TAG = "SetupContactActivity";

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
