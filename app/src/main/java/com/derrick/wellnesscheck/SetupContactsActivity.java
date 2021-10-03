package com.derrick.wellnesscheck;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SetupContactsActivity extends AppCompatActivity implements EmergencyContactsFragment.FragmentListener {
    EmergencyContactsFragment contactsFragment;
    Button setupNext;
    String destinationAddress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contactsFragment = new EmergencyContactsFragment(this);
        setContentView(R.layout.activity_setup);
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_setup_layout, contactsFragment).commit();
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
    public void onTryAddContact(String destinationAddress) {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            this.destinationAddress = destinationAddress;
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        0);
            }
        }else {
            SmsManager smsManager = getSystemService(SmsManager.class);
            if(smsManager == null) smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(destinationAddress, null, "Will you be my Emergency Contact? Reply with 'Yes' or 'No'", null, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 0:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    onTryAddContact(destinationAddress);
            break;
        }
    }
}
