package com.derrick.wellnesscheck.view.activities;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.MenuItem;

import com.derrick.wellnesscheck.MonitorReceiver;
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
    EmergencyContactsFragment emergencyContactsFragment = new EmergencyContactsFragment();
    MentalHealthResourcesFragment mentalHealthResourcesFragment = new MentalHealthResourcesFragment();
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
        if(settings.monitoringOn && !WellnessCheck.isMonitoring()){
            long now = System.currentTimeMillis();
            long responseInterval = settings.respondMinutes * MINUTE_IN_MILLIS;
            if(settings.nextCheckIn < now) {
                settings.nextCheckIn = WellnessCheck.getNextCheckIn();
                settings.update();
            }
            long time = settings.nextCheckIn;
            if(!settings.checkedIn && settings.prevCheckIn + responseInterval > now)
                time = settings.prevCheckIn + responseInterval - now;

            WellnessCheck.setAlarm(this, time, MonitorReceiver.ACTION_ALARM, settings.toBundle());
        }

        fragments.add(homeFragment);
        fragments.add(emergencyContactsFragment);
        fragments.add(logFragment);

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
            setFragment(fragments.indexOf(logFragment));
        }else if (id == R.id.action_emergency_contacts) {
            setFragment(fragments.indexOf(emergencyContactsFragment));
        }

        return true;
    }
}
