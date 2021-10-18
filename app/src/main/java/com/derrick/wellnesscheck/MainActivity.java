package com.derrick.wellnesscheck;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.room.Room;

import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.Map;

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
    public static ArrayList<Contact> contacts;
    static boolean dbReady = false;
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

            //use to clear db
            //db.contactDao().nukeTable();
            ArrayList<Contact> tmpContacts = new ArrayList<>(db.contactDao().getAll());
            if(tmpContacts != null) contacts = tmpContacts;
            else contacts = new ArrayList<>();

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
