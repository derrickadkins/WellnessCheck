package com.derrick.wellnesscheck.view.activities;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static com.derrick.wellnesscheck.WellnessCheck.db;
import static com.derrick.wellnesscheck.utils.Utils.getReadableTime;
import static com.derrick.wellnesscheck.utils.Utils.getTime;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.derrick.wellnesscheck.FallDetectionService;
import com.derrick.wellnesscheck.MonitorReceiver;
import com.derrick.wellnesscheck.WellnessCheck;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Settings;
import com.derrick.wellnesscheck.utils.*;
import com.derrick.wellnesscheck.R;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SetupSettingsActivity extends PermissionsRequestingActivity {
    static final String TAG = "SetupSettingsActivity";
    NumberPicker checkInHours, respondMinutes;
    TextView fromTime, toTime, tvFrom, tvTo, tvFirstCheck, tvSensitivity;
    SeekBar sbSensitivity;
    Switch allDay, fallDetection, reportLocation;
    Button finishSetup;
    Timer timer = new Timer();
    Settings settings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_activity);

        settings = db.settings;

        finishSetup = findViewById(R.id.btnFinishSetup);
        finishSetup.setVisibility(View.VISIBLE);
        finishSetup.setOnClickListener(v -> {
            ArrayList permissions = new ArrayList();
            permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM);
            permissions.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            if(settings.fallDetection) permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
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

                }
            });
        });

        checkInHours = findViewById(R.id.numberPickerHours);
        checkInHours.setMinValue(1);
        checkInHours.setMaxValue(24);
        checkInHours.setValue(settings.checkInHours);
        checkInHours.setOnValueChangedListener((picker, oldVal, newVal) -> {
            settings.checkInHours = newVal;
            settings.update();
            setFirstCheckText();
        });

        respondMinutes = findViewById(R.id.numberPickerMinutes);
        respondMinutes.setMinValue(1);
        respondMinutes.setMaxValue(60);
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
                    }, hourOfDay, minute, android.provider.Settings.System.TIME_12_24 == "24").show();
        };

        CompoundButton.OnCheckedChangeListener checkedChangeListener = (buttonView, isChecked) -> {
            switch (buttonView.getId()) {
                case R.id.switchReportLocation:
                    settings.reportLocation = isChecked;
                    break;
                case R.id.switchFallDetection:
                    settings.fallDetection = isChecked;
                    tvSensitivity.setVisibility(isChecked?View.VISIBLE:View.GONE);
                    sbSensitivity.setVisibility(isChecked?View.VISIBLE:View.GONE);
                    break;
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
        allDay.setChecked(settings.allDay);
        allDay.setOnCheckedChangeListener(checkedChangeListener);

        tvFrom = findViewById(R.id.tvFrom);
        tvFrom.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);
        tvTo = findViewById(R.id.tvTo);
        tvTo.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, settings.fromHour);
        calendar.set(Calendar.MINUTE, settings.fromMinute);

        fromTime = findViewById(R.id.tvFromTime);
        fromTime.setOnClickListener(onTimeClickListener);
        fromTime.setText(getTime(calendar));
        fromTime.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        calendar.set(Calendar.HOUR_OF_DAY, settings.toHour);
        calendar.set(Calendar.MINUTE, settings.toMinute);

        toTime = findViewById(R.id.tvToTime);
        toTime.setOnClickListener(onTimeClickListener);
        toTime.setText(getTime(calendar));
        toTime.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        tvFirstCheck = findViewById(R.id.tvFirstCheck);
        setFirstCheckText();

        fallDetection = findViewById(R.id.switchFallDetection);
        fallDetection.setChecked(settings.fallDetection);
        fallDetection.setOnCheckedChangeListener(checkedChangeListener);

        tvSensitivity = findViewById(R.id.tvSensitivity);
        sbSensitivity = findViewById(R.id.seekBar_fallSensitivity);
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
        sbSensitivity.setVisibility(settings.fallDetection?View.VISIBLE:View.GONE);

        reportLocation = findViewById(R.id.switchReportLocation);
        reportLocation.setOnCheckedChangeListener(checkedChangeListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFirstCheckText();
    }

    void checkInterval(){
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
        tvFirstCheck.setText("First wellness check will be at " + getFirstCheckInString(firstCheckIn));
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timer = new Timer();
                timer.schedule(this, new Date(WellnessCheck.getNextCheckIn()));
            }
        }, new Date(firstCheckIn));
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

        if(settings.fallDetection) startService(new Intent(this, FallDetectionService.class));

        startActivity(new Intent(SetupSettingsActivity.this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

        SetupSettingsActivity.this.finish();
    }

    private String getFirstCheckInString(long firstCheckIn){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(firstCheckIn);
        String time = getTime(calendar);
        if(firstCheckIn > WellnessCheck.getMidnight(calendar)) time += " tomorrow";
        return time;
    }
}
