package com.derrick.wellnesscheck.ui.activities;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static com.derrick.wellnesscheck.App.db;
import static com.derrick.wellnesscheck.utils.Utils.getReadableTime;
import static com.derrick.wellnesscheck.utils.Utils.getTime;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NavUtils;
import com.derrick.wellnesscheck.receivers.MonitorReceiver;
import com.derrick.wellnesscheck.App;
import com.derrick.wellnesscheck.data.Contact;
import com.derrick.wellnesscheck.data.Log;
import com.derrick.wellnesscheck.data.Prefs;
import com.derrick.wellnesscheck.data.Task;
import com.derrick.wellnesscheck.utils.*;
import com.derrick.wellnesscheck.R;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

public class SetupSettingsActivity extends PermissionsRequestingActivity {
    static final String TAG = "SetupSettingsActivity";
    boolean returnToMain;
    NumberPicker checkInHours, respondMinutes;
    TextView fromTime, toTime, tvFrom, tvTo, tvFirstCheck/*, tvSensitivity*/;
    AppCompatImageView infoHowOften, infoAllDay, infoResponse, infoLocation, infoAlarm;
    //SeekBar sbSensitivity;
    SwitchCompat allDay, reportLocation, alarm/*, fallDetection*/;
    Button btnStart;
    Timer timer = new Timer();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setNavigationBarColor(getColor(R.color.colorPrimary));

        setContentView(R.layout.activity_settings);

        setSupportActionBar(findViewById(R.id.settings_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        returnToMain = getIntent().getBooleanExtra("returnToMain", true);
        boolean showStart = !returnToMain;
        boolean enable = !Prefs.monitoringOn();

        int riskLvl = 1;
        for(Contact contact : db.contacts.values()){
            if(contact.riskLvl > riskLvl)
                riskLvl = contact.riskLvl;
        }
        String risk = "Low";
        int freqMax = 24;
        int excludedMax = 24;
        int respMax = 59;
        if(riskLvl == 2) {
            risk = "Medium";
            freqMax = 6;
            excludedMax = 12;
            respMax = 59;
        }
        else if(riskLvl == 3) {
            risk = "High";
            freqMax = 3;
            excludedMax = 12;
            respMax = 30;
        }

        btnStart = findViewById(R.id.btnStart);
        btnStart.setVisibility(showStart ? View.VISIBLE : View.GONE);
        btnStart.setOnClickListener(v -> {
            ArrayList permissions = new ArrayList();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM);
            permissions.add(Manifest.permission.SEND_SMS);
            //if(Prefs.fallDetection()) permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            checkPermissions((String[])permissions.toArray(new String[permissions.size()]), new PermissionsListener() {
                @Override
                public void permissionsGranted() {
                    startMonitoring();
                }

                @Override
                public void permissionsDenied() {

                }

                @Override
                public void showRationale(String[] permissions) {
                    for(String permission : permissions)
                        if(permission.equals(Manifest.permission.SEND_SMS)){
                            Snackbar snackbar = Snackbar.make(findViewById(R.id.settings_fragment), "SMS permission required",
                                    Snackbar.LENGTH_LONG);
                            snackbar.setAction("Settings", v -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            });
                            snackbar.show();
                        }
                }
            });
        });

        View.OnTouchListener touchListener = (v, event) -> {
            if(enable) return false;
            Toast.makeText(SetupSettingsActivity.this, "Turn off wellness checks to change settings", Toast.LENGTH_SHORT).show();
            return true;
        };

        checkInHours = findViewById(R.id.numberPickerHours);
        checkInHours.setOnTouchListener(touchListener);
        checkInHours.setMinValue(1);
        checkInHours.setMaxValue(freqMax);
        checkInHours.setValue(Prefs.checkInHours());
        checkInHours.setOnValueChangedListener((picker, oldVal, newVal) -> {
            Prefs.checkInHours(newVal);
            setFirstCheckText();
        });

        respondMinutes = findViewById(R.id.numberPickerMinutes);
        respondMinutes.setOnTouchListener(touchListener);
        respondMinutes.setMinValue(1);
        respondMinutes.setMaxValue(respMax);
        respondMinutes.setValue(Prefs.respondMinutes());
        respondMinutes.setOnValueChangedListener((picker, oldVal, newVal) -> Prefs.respondMinutes(newVal));

        View.OnClickListener onTimeClickListener = v -> {
            int hourOfDay, minute;
            switch (v.getId()){
                case R.id.tvFromTime:
                    hourOfDay = Prefs.fromHour();
                    minute = Prefs.fromMinute();
                    break;
                case R.id.tvToTime:
                    hourOfDay = Prefs.toHour();
                    minute = Prefs.toMinute();
                    break;
                default:
                    hourOfDay = 0;
                    minute = 0;
            }
            new TimePickerDialog(SetupSettingsActivity.this, android.R.style.Theme_DeviceDefault_Dialog,
                    (view, hourOfDay1, minute1) -> {
                        ((TextView) v).setText(getTime(hourOfDay1, minute1));
                        switch (v.getId()) {
                            case R.id.tvFromTime:
                                Prefs.fromHour(hourOfDay1);
                                Prefs.fromMinute(minute1);
                                break;
                            case R.id.tvToTime:
                                Prefs.toHour(hourOfDay1);
                                Prefs.toMinute(minute1);
                        }
                        checkInterval();
                        setFirstCheckText();
                    }, hourOfDay, minute, DateFormat.is24HourFormat(this)).show();
        };

        CompoundButton.OnCheckedChangeListener checkedChangeListener = (buttonView, isChecked) -> {
            switch (buttonView.getId()) {
                case R.id.switchReportLocation:
                    Prefs.reportLocation(isChecked);
                    if(isChecked)
                        checkPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET},
                                new PermissionsListener() {
                                    @Override
                                    public void permissionsGranted() { if(Prefs.reportLocation()) reportLocation.setChecked(true); }

                                    @Override
                                    public void permissionsDenied() { reportLocation.setChecked(false); Prefs.reportLocation(false); }

                                    @Override
                                    public void showRationale(String[] permissions) {
                                        Snackbar snackbar = Snackbar.make(findViewById(R.id.settings_fragment), "Location permission required",
                                                Snackbar.LENGTH_LONG);
                                        snackbar.setAction("Settings", v -> {
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                                            intent.setData(uri);
                                            startActivity(intent);
                                        });
                                        snackbar.show();
                                        reportLocation.setChecked(false); Prefs.reportLocation(false);
                                    }
                                });
                    break;
                /*case R.id.switchFallDetection:
                    Prefs.fallDetection(isChecked);
                    tvSensitivity.setVisibility(isChecked?View.VISIBLE:View.GONE);
                    sbSensitivity.setVisibility(isChecked?View.VISIBLE:View.GONE);
                    break;*/
                case R.id.switchAllDay:
                    Prefs.allDay(isChecked);
                    int visibility = View.VISIBLE;
                    if (isChecked) {
                        visibility = View.GONE;
                    }
                    tvFrom.setVisibility(visibility);
                    fromTime.setVisibility(visibility);
                    tvTo.setVisibility(visibility);
                    toTime.setVisibility(visibility);

                    checkInterval();
                    setFirstCheckText();
                    break;
                case R.id.switchAlarm:
                    Prefs.alarm(isChecked);
                    break;
            }
        };

        allDay = findViewById(R.id.switchAllDay);
        allDay.setOnTouchListener(touchListener);
        allDay.setChecked(Prefs.allDay());
        allDay.setOnCheckedChangeListener(checkedChangeListener);

        tvFrom = findViewById(R.id.tvFrom);
        tvFrom.setOnTouchListener(touchListener);
        tvFrom.setVisibility(Prefs.allDay() ? View.GONE : View.VISIBLE);

        tvTo = findViewById(R.id.tvTo);
        tvTo.setOnTouchListener(touchListener);
        tvTo.setVisibility(Prefs.allDay() ? View.GONE : View.VISIBLE);

        fromTime = findViewById(R.id.tvFromTime);
        fromTime.setOnTouchListener(touchListener);
        fromTime.setOnClickListener(onTimeClickListener);
        fromTime.setText(getTime(Prefs.fromHour(), Prefs.fromMinute()));
        fromTime.setVisibility(Prefs.allDay() ? View.GONE : View.VISIBLE);

        toTime = findViewById(R.id.tvToTime);
        toTime.setOnTouchListener(touchListener);
        toTime.setOnClickListener(onTimeClickListener);
        toTime.setText(getTime(Prefs.toHour(), Prefs.toMinute()));
        toTime.setVisibility(Prefs.allDay() ? View.GONE : View.VISIBLE);

        tvFirstCheck = findViewById(R.id.tvFirstCheck);
        if(returnToMain) tvFirstCheck.setVisibility(View.GONE);
        else setFirstCheckText();

        /*fallDetection = findViewById(R.id.switchFallDetection);
        fallDetection.setEnabled(enable);
        fallDetection.setChecked(Prefs.fallDetection());
        fallDetection.setOnCheckedChangeListener(checkedChangeListener);

        tvSensitivity = findViewById(R.id.tvSensitivity);
        tvSensitivity.setEnabled(enable);
        sbSensitivity = findViewById(R.id.seekBar_fallSensitivity);
        sbSensitivity.setEnabled(enable);
        sbSensitivity.setKeyProgressIncrement(5);
        sbSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Prefs.fallSensitivity(seekBar.getProgress());
            }
        });
        tvSensitivity.setVisibility(Prefs.fallDetection()?View.VISIBLE:View.GONE);
        sbSensitivity.setVisibility(Prefs.fallDetection()?View.VISIBLE:View.GONE);*/

        reportLocation = findViewById(R.id.switchReportLocation);
        reportLocation.setOnTouchListener(touchListener);
        reportLocation.setChecked(Prefs.reportLocation() && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        reportLocation.setOnCheckedChangeListener(checkedChangeListener);

        alarm = findViewById(R.id.switchAlarm);
        alarm.setOnTouchListener(touchListener);
        alarm.setChecked(Prefs.alarm());
        alarm.setOnCheckedChangeListener(checkedChangeListener);

        infoHowOften = findViewById(R.id.info_how_often);
        infoAllDay = findViewById(R.id.info_all_day);
        infoResponse = findViewById(R.id.info_response);
        infoLocation = findViewById(R.id.info_location);
        infoAlarm = findViewById(R.id.info_alarm);

        AlertDialog.Builder builder = new AlertDialog.Builder(SetupSettingsActivity.this, R.style.AppTheme_Dialog_Alert);
        infoHowOften.setOnClickListener(v -> builder.setTitle("Check-In Frequency")
                .setMessage("Check-in frequency determines the rate at which you receive wellness check notifications. " +
                        "If a check-in is not completed within the response time, your emergency contacts will be notified via SMS.")
                .show());
        infoAllDay.setOnClickListener(v -> builder.setTitle("All-Day")
                .setMessage("Turning this off will allow you to exclude a period of time each day to limit when you receive wellness check notifications. " +
                        "The 'From' time minute value determines the minute during the hour each wellness check will occur.")
                .show());
        infoResponse.setOnClickListener(v -> builder.setTitle("Response Time")
                .setMessage("Response time determines how much time you have to respond to a wellness check notification before your emergency contacts are alerted via SMS.")
                .show());
        infoLocation.setOnClickListener(v -> builder.setTitle("Report Location")
                .setMessage("Turn this on to include a link to your location in the SMS sent to your emergency contacts when a wellness check is not completed within the response time.")
                .show());
        infoAlarm.setOnClickListener(v -> builder.setTitle("Alarm")
                .setMessage("Turn this on to include an alarm with your wellness check notifications.")
                .show());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Override BOTH getSupportParentActivityIntent() AND getParentActivityIntent() because
    // if your device is running on API 11+ it will call the native
    // getParentActivityIntent() method instead of the support version.
    // The docs do **NOT** make this part clear and it is important!
    @Override
    public Intent getSupportParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    @Override
    public Intent getParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    private Intent getParentActivityIntentImpl() {
        return new Intent(this, returnToMain ? MainActivity.class : SetupContactsActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFirstCheckText();
    }

    void checkInterval(){
        //todo: is from - to > 1 ?
        //if interval is larger than non-excluded hours, set to 24
        if(!Prefs.allDay()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.set(Calendar.HOUR_OF_DAY, Prefs.fromHour());
            calendar.set(Calendar.MINUTE, Prefs.fromMinute());
            long fromTime = calendar.getTimeInMillis();
            calendar.set(Calendar.HOUR_OF_DAY, Prefs.toHour());
            calendar.set(Calendar.MINUTE, Prefs.toMinute());
            long toTime = calendar.getTimeInMillis();
            long interval = Prefs.checkInHours() * HOUR_IN_MILLIS;
            if (fromTime < toTime) {
                if (toTime - fromTime < interval)
                    checkInHours.setValue(24);
            } else if ((24 * HOUR_IN_MILLIS) - (fromTime - toTime) < interval) {
                checkInHours.setValue(24);
            }
        }
    }

    void setFirstCheckText(){
        long firstCheckIn = App.getNextCheckIn();
        tvFirstCheck.setText(getString(R.string.wellness_check_will_be_at) + getFirstCheckInString(firstCheckIn));
        timer.cancel();
        timer = new Timer();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                timer = new Timer();
                timer.schedule(new Task(this), new Date(App.getNextCheckIn()));
            }
        };
        timer.schedule(new Task(runnable), new Date(firstCheckIn));
    }

    void startMonitoring(){
        timer.cancel();

        Prefs.monitoringOn(true);
        Prefs.checkedIn(true);
        Prefs.nextCheckIn(App.getNextCheckIn());
        Prefs.prevCheckIn(System.currentTimeMillis());
        Log.d(TAG, "triggering first alarm at " + getReadableTime(Prefs.nextCheckIn()));

        App.setAlarm(this, Prefs.nextCheckIn(), MonitorReceiver.ACTION_NOTIFY);

        //if(Prefs.fallDetection()) startService(new Intent(this, FallDetectionService.class));

        PowerManager powerManager = getSystemService(PowerManager.class);
        if(!powerManager.isIgnoringBatteryOptimizations(getPackageName()))
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+getPackageName()))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

        startActivity(new Intent(SetupSettingsActivity.this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

        SetupSettingsActivity.this.finish();
    }

    private String getFirstCheckInString(long firstCheckIn){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(firstCheckIn);
        String time = getTime(calendar);
        if(firstCheckIn > App.getMidnight(calendar)) time += getString(R.string.tomorrow);
        return time;
    }
}