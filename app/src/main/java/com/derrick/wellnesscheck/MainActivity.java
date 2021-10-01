package com.derrick.wellnesscheck;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.room.Room;

import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    HomeFragment homeFragment = new HomeFragment();
    EmergencyContactsFragment emergencyContactsFragment = new EmergencyContactsFragment();
    MentalHealthResourcesFragment mentalHealthResourcesFragment = new MentalHealthResourcesFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    ArrayList<Fragment> fragments = new ArrayList<>();
    int currentFragmentIndex = 0;
    BottomNavigationView bottomNavigationView;
    public static DB db;
    public static AppSettings settings;
    static boolean dbReady = false;

    static void InitDB(Context context){
        new Thread(() -> {
            db = Room.databaseBuilder(context,
                    DB.class, "database-name")
                    .fallbackToDestructiveMigration()
                    .build();

            AppSettings tmpSettings = db.settingsDao().getSettings();
            if(tmpSettings != null) settings = tmpSettings;
            else{
                settings = new AppSettings();
                db.settingsDao().insert(settings);
            }


            dbReady = true;
        }).start();
    }

    static void updateSettings(){
        new Thread(() -> db.settingsDao().update(settings)).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InitDB(this);
        while(!dbReady);

        setContentView(R.layout.activity_main);

        fragments.add(homeFragment);
        fragments.add(emergencyContactsFragment);
        fragments.add(mentalHealthResourcesFragment);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.nav);
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
            setFragment(fragments.indexOf(mentalHealthResourcesFragment));
        }else if (id == R.id.action_emergency_contacts) {
            setFragment(fragments.indexOf(emergencyContactsFragment));
        }

        return true;
    }
}
