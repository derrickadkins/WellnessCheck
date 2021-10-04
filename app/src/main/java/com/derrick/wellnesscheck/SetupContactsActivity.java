package com.derrick.wellnesscheck;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class SetupContactsActivity extends AppCompatActivity implements EmergencyContactsFragment.FragmentListener, SmsReceiver.SmsListener {
    EmergencyContactsFragment contactsFragment;
    Button setupNext;
    SmsReceiver smsReceiver;
    AlertDialog alertDialog;
    Contact contact;
    final String TAG = "SetupContactActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");

        contactsFragment = new EmergencyContactsFragment(this);
        setContentView(R.layout.activity_setup);
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_setup_layout, contactsFragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onViewCreated(View v) {
        setupNext = v.findViewById(R.id.btnSetupNext);
        setupNext.setVisibility(View.VISIBLE);
        setupNext.setEnabled(false);
        setupNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SetupContactsActivity.this, SetupSettingsActivity.class));
            }
        });
    }

    @Override
    public void onContactListSizeChange(int size) {
        setupNext.setEnabled(size > 0);
    }

    @Override
    public void onTryAddContact(Contact contact) {
        List<String> permissions = new ArrayList<>();
        this.contact = contact;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
        }
        if(permissions.size() > 0){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), 0);
            }
        }else {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, SmsReceiver.class), 0);
            SmsManager smsManager = getSystemService(SmsManager.class);
            if(smsManager == null) smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(contact.number, null, getString(R.string.contact_request), null, null);

            alertDialog = new AlertDialog.Builder(this)
                    .setMessage("Waiting for response ...")
                    .setView(new ProgressBar(this))
                    .create();
            alertDialog.show();

            smsReceiver = new SmsReceiver(this);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
            registerReceiver(smsReceiver, intentFilter);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 0:
                if(grantResults.length > 1 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    onTryAddContact(contact);
            break;
        }
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

    @Override
    public void onSmsReceived(String number, String message) {
        if(normalizeNumber(number).equalsIgnoreCase(normalizeNumber(contact.number))) {
            if(message.equalsIgnoreCase("yes")){
                contactsFragment.addContact(contact);
            }
            alertDialog.cancel();
            unregisterReceiver(smsReceiver);
        }
    }

    String normalizeNumber(String number){
        return number.replaceAll("\\D+", "");
    }
}
