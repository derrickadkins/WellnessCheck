package com.derrick.wellnesscheck.view.activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.MenuItem;

import com.derrick.wellnesscheck.WellnessCheck;
import com.derrick.wellnesscheck.model.DB;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Settings;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;
import com.derrick.wellnesscheck.view.fragments.*;
import com.derrick.wellnesscheck.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;

public class MainActivity extends PermissionsRequestingActivity implements NavigationBarView.OnItemSelectedListener, DB.DbListener {
    static final String TAG = "MainActivity";
    HomeFragment homeFragment = new HomeFragment();
    ContactsFragment contactsFragment = new ContactsFragment();
    ResourcesFragment resourcesFragment = new ResourcesFragment();
    LogFragment logFragment = new LogFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    ArrayList<Fragment> fragments = new ArrayList<>();
    int currentFragmentIndex = 0;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DB.InitDB(this);
    }

    @Override
    public void onDbReady(DB db) {
        setContentView(R.layout.activity_main);
        Settings settings = db.settings;
        Log.d(TAG, "settings:" + settings.toString());
        WellnessCheck.applySettings(this, settings);

        fragments.add(homeFragment);
        fragments.add(contactsFragment);
        fragments.add(resourcesFragment);

        bottomNavigationView = findViewById(R.id.nav);
        bottomNavigationView.setOnItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.action_home);
    }

    @Override
    public void onBackPressed() {
        if(currentFragmentIndex > 0) {
            bottomNavigationView.setSelectedItemId(R.id.action_home);
        }else{
            super.onBackPressed();
        }
    }

    //todo: Can not perform this action after onSaveInstanceState
    public void setFragment(int fragmentIndex){
        currentFragmentIndex = fragmentIndex;
        fragmentManager.beginTransaction().replace(R.id.main_content_fragment, fragments.get(fragmentIndex)).commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_home) {
            setFragment(fragments.indexOf(homeFragment));
        }else if (id == R.id.action_mental_health_resources) {
            setFragment(fragments.indexOf(resourcesFragment));
        }else if (id == R.id.action_emergency_contacts) {
            setFragment(fragments.indexOf(contactsFragment));
        }

        return true;
    }
}
