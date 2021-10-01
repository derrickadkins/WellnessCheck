package com.derrick.wellnesscheck;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SetupContactsActivity extends AppCompatActivity implements EmergencyContactsFragment.FragmentListener {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EmergencyContactsFragment contactsFragment = new EmergencyContactsFragment(this);
        setContentView(R.layout.activity_setup);
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_setup_layout, contactsFragment).commit();
    }

    @Override
    public void onViewCreated(View v) {
        Button setupNext = v.findViewById(R.id.btnSetupNext);
        setupNext.setVisibility(View.VISIBLE);
        setupNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SetupContactsActivity.this, SetupSettingsActivity.class));
            }
        });
    }
}
