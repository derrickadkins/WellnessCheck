package com.derrick.wellnesscheck;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.room.Room;

import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    EmergencyContactsFragment emergencyContactsFragment = new EmergencyContactsFragment();
    MentalHealthResourcesFragment mentalHealthResourcesFragment = new MentalHealthResourcesFragment();
    SettingsFragment settingsFragment = new SettingsFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    ArrayList<Fragment> fragments = new ArrayList<>();
    int currentFragmentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fragments.add(emergencyContactsFragment);
        fragments.add(mentalHealthResourcesFragment);
        fragments.add(settingsFragment);
        setFragment(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            setFragment(fragments.indexOf(settingsFragment));
            return true;
        }else if (id == R.id.action_mental_health_resources) {
            setFragment(fragments.indexOf(mentalHealthResourcesFragment));
            return true;
        }else if (id == android.R.id.home){
            setFragment(0);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(currentFragmentIndex > 0) {
            setFragment(0);
        }else{
            super.onBackPressed();
        }
    }

    public void setFragment(int fragmentIndex){
        currentFragmentIndex = fragmentIndex;
        getSupportActionBar().setDisplayHomeAsUpEnabled(fragmentIndex > 0);
        fragmentManager.beginTransaction().replace(R.id.main_content_fragment, fragments.get(fragmentIndex)).commit();
    }
}
