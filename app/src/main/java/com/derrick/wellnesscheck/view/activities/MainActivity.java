package com.derrick.wellnesscheck.view.activities;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.derrick.wellnesscheck.WellnessCheck;
import com.derrick.wellnesscheck.model.DB;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Settings;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;
import com.derrick.wellnesscheck.view.fragments.*;
import com.derrick.wellnesscheck.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.ramotion.paperonboarding.PaperOnboardingPage;

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
        //todo: find another solution
        //null prevents crash after return from permission being revoked
        super.onCreate(null);

        if(getPreferences(Context.MODE_PRIVATE).getBoolean("onboardingComplete", false)) startNormal();
        else startOnboarding();
    }

    private void startNormal(){
        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.nav);
        bottomNavigationView.setOnItemSelectedListener(this);
        DB.InitDB(this);
        setSupportActionBar(findViewById(R.id.main_activity_toolbar));
    }

    private void startOnboarding(){
        setContentView(R.layout.onboarding);
        ArrayList<PaperOnboardingPage> onboardingPages = new ArrayList<>();
        onboardingPages.add(new PaperOnboardingPage("Wellness Checks",
                "Clicking on a wellness check notification prevents notifying emergency contacts",
                getColor(R.color.colorPrimaryDark),
                R.drawable.wellness_check_icon_64_transparent_background,
                R.drawable.wellness_check_icon_64_transparent_background));
        onboardingPages.add(new PaperOnboardingPage("Emergency Contacts",
                "Ask people you trust to support you through your times of need",
                getColor(R.color.colorPrimary),
                R.drawable.ic_contact_default,
                R.drawable.ic_contacts));
        onboardingPages.add(new PaperOnboardingPage("Resources",
                "A wide range of resources are available for quick use in an emergency",
                getColor(R.color.colorAccent),
                android.R.drawable.ic_dialog_alert,
                R.drawable.ic_resources));
        OnboardingFragment onboardingFragment = OnboardingFragment.newInstance(onboardingPages);
        TextView onboardingButtonLeft = findViewById(R.id.onboarding_button_left);
        TextView onboardingButtonRight = findViewById(R.id.onboarding_button_right);
        onboardingFragment.setOnChangeListener((i, i1) -> {
            i = onboardingFragment.onboardingEngine.getActiveElementIndex();
            onboardingButtonLeft.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
            onboardingButtonRight.setText(i == onboardingPages.size() - 1 ? "Let's Go" : "Next >");
            if(i == onboardingPages.size() - 1){
                checkPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS}, null);
            }
        });
        fragmentManager.beginTransaction().add(R.id.onboarding_frame_layout, onboardingFragment).commit();
        onboardingButtonRight.setOnClickListener(v -> {
            if(onboardingFragment.onboardingEngine.getActiveElementIndex() == onboardingPages.size() - 1) {
                getPreferences(Context.MODE_PRIVATE).edit().putBoolean("onboardingComplete", true).apply();
                startNormal();
            }else onboardingFragment.onboardingEngine.toggleContent(false);
        });
        onboardingButtonLeft.setOnClickListener(v -> onboardingFragment.onboardingEngine.toggleContent(true));
    }

    @Override
    public void onDbReady(DB db) {
        Settings settings = db.settings;
        Log.d(TAG, "settings:" + settings.toString());
        WellnessCheck.applySettings(this, settings);

        fragments.add(homeFragment);
        fragments.add(contactsFragment);
        fragments.add(resourcesFragment);

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
