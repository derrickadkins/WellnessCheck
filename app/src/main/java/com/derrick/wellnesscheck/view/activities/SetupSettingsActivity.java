package com.derrick.wellnesscheck.view.activities;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static com.derrick.wellnesscheck.WellnessCheck.db;
import static com.derrick.wellnesscheck.utils.Utils.getReadableTime;
import static com.derrick.wellnesscheck.utils.Utils.getTime;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import com.derrick.wellnesscheck.MonitorReceiver;
import com.derrick.wellnesscheck.WellnessCheck;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Settings;
import com.derrick.wellnesscheck.model.data.Task;
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
    AppCompatImageView infoHowOften, infoAllDay, infoResponse, infoLocation;
    //SeekBar sbSensitivity;
    SwitchCompat allDay, reportLocation/*, fallDetection*/;
    Button btnStart;
    Timer timer = new Timer();
    Settings settings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setNavigationBarColor(getColor(R.color.colorPrimary));

        setContentView(R.layout.activity_settings);

        setSupportActionBar(findViewById(R.id.settings_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        settings = db.settings;

        returnToMain = getIntent().getBooleanExtra("returnToMain", true);
        boolean showStart = !returnToMain;
        boolean enable = !settings.monitoringOn;

        btnStart = findViewById(R.id.btnStart);
        btnStart.setVisibility(showStart ? View.VISIBLE : View.GONE);
        btnStart.setOnClickListener(v -> {
            ArrayList permissions = new ArrayList();
            permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM);
            permissions.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            permissions.add(Manifest.permission.SEND_SMS);
            //if(settings.fallDetection) permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
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
                        if(permission == Manifest.permission.SEND_SMS){
                            Snackbar snackbar = Snackbar.make(findViewById(R.id.settings_fragment), "SMS permission required",
                                    Snackbar.LENGTH_LONG);
                            snackbar.setAction("Settings", v -> {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
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
        checkInHours.setMaxValue(24);
        checkInHours.setValue(settings.checkInHours);
        checkInHours.setOnValueChangedListener((picker, oldVal, newVal) -> {
            settings.checkInHours = newVal;
            settings.update();
            setFirstCheckText();
        });

        respondMinutes = findViewById(R.id.numberPickerMinutes);
        respondMinutes.setOnTouchListener(touchListener);
        respondMinutes.setMinValue(1);
        respondMinutes.setMaxValue(59);
        respondMinutes.setValue(settings.respondMinutes);
        respondMinutes.setOnValueChangedListener((picker, oldVal, newVal) -> {
            settings.respondMinutes = newVal;
            settings.update();
        });

        View.OnClickListener onTimeClickListener = v -> {
            int hourOfDay, minute;
            switch (v.getId()){
                case R.id.tvFromTime:
                    hourOfDay = settings.fromHour;
                    minute = settings.fromMinute;
                    break;
                case R.id.tvToTime:
                    hourOfDay = settings.toHour;
                    minute = settings.toMinute;
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
                                settings.fromHour = hourOfDay1;
                                settings.fromMinute = minute1;
                                break;
                            case R.id.tvToTime:
                                settings.toHour = hourOfDay1;
                                settings.toMinute = minute1;
                        }
                        checkInterval();
                        setFirstCheckText();
                        settings.update();
                    }, hourOfDay, minute, DateFormat.is24HourFormat(this)).show();
        };

        CompoundButton.OnCheckedChangeListener checkedChangeListener = (buttonView, isChecked) -> {
            switch (buttonView.getId()) {
                case R.id.switchReportLocation:
                    settings.reportLocation = isChecked;
                    if(isChecked)
                        checkPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET},
                                new PermissionsListener() {
                                    @Override
                                    public void permissionsGranted() { if(settings.reportLocation) reportLocation.setChecked(true); }

                                    @Override
                                    public void permissionsDenied() { reportLocation.setChecked(false); settings.reportLocation = false; }

                                    @Override
                                    public void showRationale(String[] permissions) {
                                        Snackbar snackbar = Snackbar.make(findViewById(R.id.settings_fragment), "Location permission required",
                                                Snackbar.LENGTH_LONG);
                                        snackbar.setAction("Settings", v -> {
                                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                                            intent.setData(uri);
                                            startActivity(intent);
                                        });
                                        snackbar.show();
                                        reportLocation.setChecked(false); settings.reportLocation = false;
                                    }
                                });
                    break;
                /*case R.id.switchFallDetection:
                    settings.fallDetection = isChecked;
                    tvSensitivity.setVisibility(isChecked?View.VISIBLE:View.GONE);
                    sbSensitivity.setVisibility(isChecked?View.VISIBLE:View.GONE);
                    break;*/
                case R.id.switchAllDay:
                    settings.allDay = isChecked;
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
            }

            settings.update();
        };

        allDay = findViewById(R.id.switchAllDay);
        allDay.setOnTouchListener(touchListener);
        allDay.setChecked(settings.allDay);
        allDay.setOnCheckedChangeListener(checkedChangeListener);

        tvFrom = findViewById(R.id.tvFrom);
        tvFrom.setOnTouchListener(touchListener);
        tvFrom.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        tvTo = findViewById(R.id.tvTo);
        tvTo.setOnTouchListener(touchListener);
        tvTo.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        fromTime = findViewById(R.id.tvFromTime);
        fromTime.setOnTouchListener(touchListener);
        fromTime.setOnClickListener(onTimeClickListener);
        fromTime.setText(getTime(settings.fromHour, settings.fromMinute));
        fromTime.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        toTime = findViewById(R.id.tvToTime);
        toTime.setOnTouchListener(touchListener);
        toTime.setOnClickListener(onTimeClickListener);
        toTime.setText(getTime(settings.toHour, settings.toMinute));
        toTime.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        tvFirstCheck = findViewById(R.id.tvFirstCheck);
        if(returnToMain) tvFirstCheck.setVisibility(View.GONE);
        else setFirstCheckText();

        /*fallDetection = findViewById(R.id.switchFallDetection);
        fallDetection.setEnabled(enable);
        fallDetection.setChecked(settings.fallDetection);
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
                settings.fallSensitivity = seekBar.getProgress();
                settings.update();
            }
        });
        tvSensitivity.setVisibility(settings.fallDetection?View.VISIBLE:View.GONE);
        sbSensitivity.setVisibility(settings.fallDetection?View.VISIBLE:View.GONE);*/

        reportLocation = findViewById(R.id.switchReportLocation);
        reportLocation.setOnTouchListener(touchListener);
        reportLocation.setChecked(settings.reportLocation && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        reportLocation.setOnCheckedChangeListener(checkedChangeListener);

        infoHowOften = findViewById(R.id.info_how_often);
        infoAllDay = findViewById(R.id.info_all_day);
        infoResponse = findViewById(R.id.info_response);
        infoLocation = findViewById(R.id.info_location);

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
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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
        if(!settings.allDay) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.set(Calendar.HOUR_OF_DAY, settings.fromHour);
            calendar.set(Calendar.MINUTE, settings.fromMinute);
            long fromTime = calendar.getTimeInMillis();
            calendar.set(Calendar.HOUR_OF_DAY, settings.toHour);
            calendar.set(Calendar.MINUTE, settings.toMinute);
            long toTime = calendar.getTimeInMillis();
            long interval = settings.checkInHours * HOUR_IN_MILLIS;
            if (fromTime < toTime) {
                if (toTime - fromTime < interval)
                    checkInHours.setValue(24);
            } else if ((24 * HOUR_IN_MILLIS) - (fromTime - toTime) < interval) {
                checkInHours.setValue(24);
            }
        }
    }

    void setFirstCheckText(){
        long firstCheckIn = WellnessCheck.getNextCheckIn();
        tvFirstCheck.setText(getString(R.string.first_wellness_check_will_be_at) + getFirstCheckInString(firstCheckIn));
        timer.cancel();
        timer = new Timer();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                timer = new Timer();
                timer.schedule(new Task(this), new Date(WellnessCheck.getNextCheckIn()));
            }
        };
        timer.schedule(new Task(runnable), new Date(firstCheckIn));
    }

    void startMonitoring(){
        timer.cancel();

        settings.monitoringOn = true;
        settings.checkedIn = true;
        settings.nextCheckIn = WellnessCheck.getNextCheckIn();
        settings.prevCheckIn = System.currentTimeMillis();
        settings.update();
        Log.d(TAG, "triggering first alarm at " + getReadableTime(settings.nextCheckIn));

        WellnessCheck.setAlarm(this, settings.nextCheckIn, MonitorReceiver.ACTION_ALARM, settings.toBundle());

        //if(settings.fallDetection) startService(new Intent(this, FallDetectionService.class));

        startActivity(new Intent(SetupSettingsActivity.this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

        SetupSettingsActivity.this.finish();
    }

    private String getFirstCheckInString(long firstCheckIn){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(firstCheckIn);
        String time = getTime(calendar);
        if(firstCheckIn > WellnessCheck.getMidnight(calendar)) time += getString(R.string.tomorrow);
        return time;
    }
}
