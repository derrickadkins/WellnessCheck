package com.derrick.wellnesscheck.ui.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.derrick.wellnesscheck.App;
import com.derrick.wellnesscheck.data.DB;
import com.derrick.wellnesscheck.data.Log;
import com.derrick.wellnesscheck.data.Prefs;
import com.derrick.wellnesscheck.ui.fragments.contacts.ContactsFragment;
import com.derrick.wellnesscheck.ui.fragments.home.HomeFragment;
import com.derrick.wellnesscheck.ui.fragments.onboarding.OnboardingFragment;
import com.derrick.wellnesscheck.ui.fragments.resources.ResourcesFragment;
import com.derrick.wellnesscheck.utils.FragmentReadyListener;
import com.derrick.wellnesscheck.ui.fragments.*;
import com.derrick.wellnesscheck.R;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.ramotion.paperonboarding.PaperOnboardingPage;

import java.util.ArrayList;

public class MainActivity extends PermissionsRequestingActivity implements NavigationBarView.OnItemSelectedListener, FragmentReadyListener {
    static final String TAG = "MainActivity";
    HomeFragment homeFragment = new HomeFragment();
    ContactsFragment contactsFragment = new ContactsFragment();
    ResourcesFragment resourcesFragment = new ResourcesFragment();
    LogFragment logFragment = new LogFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    ArrayList<Fragment> fragments = new ArrayList<>();
    int currentFragmentIndex = 0, showcaseStep = 0;
    BottomNavigationView bottomNavigationView;
    ConstraintLayout warningBanner;
    TextView learnMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //todo: find another solution?
        //null prevents crash after return from permission being revoked
        super.onCreate(null);

        DB.InitDB(this);

        getWindow().setNavigationBarColor(getColor(R.color.colorPrimary));

        if(Prefs.onboardingComplete()) startNormal();
        else startOnboarding();
    }

    private void startNormal(){
        Log.d(TAG, new Prefs().toString());
        App.applyPrefs(this);

        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.main_activity_toolbar));

        fragments.add(homeFragment);
        fragments.add(contactsFragment);
        fragments.add(resourcesFragment);

        bottomNavigationView = findViewById(R.id.nav);
        bottomNavigationView.setOnItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.action_home);

        warningBanner = findViewById(R.id.powerOptimizationWarningBanner_main);
        if(Prefs.monitoringOn()) {
            learnMore = findViewById(R.id.learn_more_main);
            learnMore.setOnClickListener(view -> {
                new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Alert)
                        .setTitle(R.string.battery_optimization)
                        .setMessage(R.string.battery_optimization_msg)
                        .setPositiveButton(R.string.settings, (dialogInterface, i) -> startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)))
                        .show();
            });
            PowerManager powerManager = getSystemService(PowerManager.class);
            warningBanner.setVisibility(powerManager.isIgnoringBatteryOptimizations(getPackageName()) ? View.GONE : View.VISIBLE);
        }else warningBanner.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(warningBanner == null) return;
        if(Prefs.monitoringOn()) {
            PowerManager powerManager = getSystemService(PowerManager.class);
            warningBanner.setVisibility(powerManager.isIgnoringBatteryOptimizations(getPackageName()) ? View.GONE : View.VISIBLE);
        }else warningBanner.setVisibility(View.GONE);
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
            if(i == onboardingPages.size() - 1)
                checkPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS}, null);
        });
        fragmentManager.beginTransaction().add(R.id.onboarding_frame_layout, onboardingFragment).commit();
        onboardingButtonRight.setOnClickListener(v -> {
            if(onboardingFragment.onboardingEngine.getActiveElementIndex() == onboardingPages.size() - 1) {
                Prefs.onboardingComplete(true);
                startNormal();
            }else onboardingFragment.onboardingEngine.toggleContent(false);
        });
        onboardingButtonLeft.setOnClickListener(v -> onboardingFragment.onboardingEngine.toggleContent(true));
    }

    public void showCase(){
        if(Prefs.walkthroughComplete()) return;
        switch (showcaseStep++){
            case 0:
                TapTargetView.showFor(this,                 // `this` is an Activity
                        TapTarget.forView(findViewById(R.id.action_contacts),
                                "Emergency Contacts",
                                "Click here to add, call, SMS, or delete emergency contacts")
                                .outerCircleColor(R.color.colorPrimary)      // Specify a color for the outer circle
                                .outerCircleAlpha(0.96f)            // Specify the alpha amount for the outer circle
                                .titleTextSize(20)                  // Specify the size (in sp) of the title text
                                .descriptionTextSize(14)            // Specify the size (in sp) of the description text
                                .textColor(android.R.color.white)            // Specify a color for both the title and description text
                                .dimColor(android.R.color.black)            // If set, will dim behind the view with 30% opacity of the given color
                                .drawShadow(true)                   // Whether to draw a drop shadow or not
                                .cancelable(false)                  // Whether tapping outside the outer circle dismisses the view
                                .transparentTarget(true)           // Specify whether the target is transparent (displays the content underneath)
                                .targetRadius(40),                  // Specify the target radius (in dp)
                        new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                bottomNavigationView.setSelectedItemId(R.id.action_contacts);
                            }
                        });
                break;
            case 1:
                TapTargetView.showFor(this,
                        TapTarget.forView(findViewById(R.id.fab),
                                "Add Emergency Contacts",
                                "Click here to add people you can trust to support you")
                                .outerCircleColor(R.color.colorPrimaryDark)
                                .outerCircleAlpha(0.96f)
                                .titleTextSize(20)
                                .descriptionTextSize(14)
                                .textColor(android.R.color.white)
                                .dimColor(android.R.color.black)
                                .drawShadow(true)
                                .cancelable(false)
                                .transparentTarget(true)
                                .targetRadius(40),
                        new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                showCase();
                            }
                        });
                break;
            case 2:
                TapTargetView.showFor(this,
                        TapTarget.forView(findViewById(R.id.action_resources),
                                "Resources",
                                "Here you will find a variety of emergency resources, just swipe to use one")
                                .outerCircleColor(R.color.colorPrimary)
                                .outerCircleAlpha(0.96f)
                                .titleTextSize(20)
                                .descriptionTextSize(14)
                                .textColor(android.R.color.white)
                                .dimColor(android.R.color.black)
                                .drawShadow(true)
                                .cancelable(false)
                                .transparentTarget(true)
                                .targetRadius(40),
                        new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                bottomNavigationView.setSelectedItemId(R.id.action_resources);
                            }
                        });
                break;
            case 3:
                TapTargetView.showFor(this,
                        TapTarget.forView(findViewById(R.id.action_home),
                                "Home",
                                "Click here to access your wellness check status and settings")
                                .outerCircleColor(R.color.colorPrimaryDark)
                                .outerCircleAlpha(0.96f)
                                .titleTextSize(20)
                                .descriptionTextSize(14)
                                .textColor(android.R.color.white)
                                .dimColor(android.R.color.black)
                                .drawShadow(true)
                                .cancelable(false)
                                .transparentTarget(true)
                                .targetRadius(40),
                        new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                bottomNavigationView.setSelectedItemId(R.id.action_home);
                            }
                        });
                break;
            case 4:
                TapTargetView.showFor(this,
                        TapTarget.forView(findViewById(R.id.imgSettingsIcon),
                                "Settings",
                                "Click here to see your settings, they can only be changed while your wellness checks are turned off")
                                .outerCircleColor(R.color.colorPrimary)
                                .outerCircleAlpha(0.96f)
                                .titleTextSize(20)
                                .descriptionTextSize(14)
                                .textColor(android.R.color.white)
                                .dimColor(android.R.color.black)
                                .drawShadow(true)
                                .cancelable(false)
                                .transparentTarget(true)
                                .targetRadius(40),
                        new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                showCase();
                            }
                        });
                break;
            case 5:
                DisplayMetrics dM = getResources().getDisplayMetrics();
                float dpWidth = dM.widthPixels / dM.density;
                int targetRadius = (int)dpWidth / 2;
                TapTargetView.showFor(this,
                        TapTarget.forView(findViewById(R.id.progressBar),
                                "Wellness Checks",
                                "Click here to start the setup process for wellness checks, check in, or see when your next wellness check is here")
                                .outerCircleColor(R.color.colorPrimaryDark)
                                .outerCircleAlpha(0.96f)
                                .titleTextSize(20)
                                .descriptionTextSize(14)
                                .textColor(android.R.color.white)
                                .dimColor(android.R.color.black)
                                .drawShadow(true)
                                .cancelable(false)
                                .transparentTarget(true)
                                .targetRadius(targetRadius),
                        new TapTargetView.Listener(){
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                Prefs.walkthroughComplete(true);
                            }
                        });
                break;
        }
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
        }else if (id == R.id.action_resources) {
            setFragment(fragments.indexOf(resourcesFragment));
        }else if (id == R.id.action_contacts) {
            setFragment(fragments.indexOf(contactsFragment));
        }

        return true;
    }

    @Override
    public void onFragmentReady() {
        showCase();
    }
}
