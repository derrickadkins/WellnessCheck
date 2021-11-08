package com.derrick.wellnesscheck.view.activities;

import static com.derrick.wellnesscheck.WellnessCheck.getNextCheckIn;
import static com.derrick.wellnesscheck.controller.DbController.settings;
import static com.derrick.wellnesscheck.controller.DbController.updateSettings;
import static com.derrick.wellnesscheck.MonitorReceiver.getReadableTime;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.Nullable;

import com.derrick.wellnesscheck.WellnessCheck;
import com.derrick.wellnesscheck.utils.*;
import com.derrick.wellnesscheck.MonitorReceiver;
import com.derrick.wellnesscheck.R;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SetupSettingsActivity extends PermissionsRequestingActivity {
    static final String TAG = "SetupSettingsActivity";
    NumberPicker checkInHours, respondMinutes;
    TextView fromTime, toTime, tvFrom, tvTo, tvFirstCheck;
    Switch allDay, fallDetection;
    Button finishSetup;
    Timer timer = new Timer();
    static final long MINUTE_IN_MILLIS = 60 * 1000;
    static final long HOUR_IN_MILLIS = 60 * MINUTE_IN_MILLIS;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_activity);

        finishSetup = findViewById(R.id.btnFinishSetup);
        finishSetup.setVisibility(View.VISIBLE);
        finishSetup.setOnClickListener(v -> {
            checkPermissions(new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM,
                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS}, new PermissionsListener() {
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
            updateSettings();
            setFirstCheckText();
        });

        respondMinutes = findViewById(R.id.numberPickerMinutes);
        respondMinutes.setMinValue(1);
        respondMinutes.setMaxValue(60);
        respondMinutes.setValue(settings.respondMinutes);
        respondMinutes.setOnValueChangedListener((picker, oldVal, newVal) -> {
            settings.respondMinutes = newVal;
            updateSettings();
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
                        ((TextView) v).setText(String.format("%02d:%02d", hourOfDay1, minute1));
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
                        updateSettings();
                    }, hourOfDay, minute, true).show();
        };

        CompoundButton.OnCheckedChangeListener checkedChangeListener = (buttonView, isChecked) -> {
            switch (buttonView.getId()) {
                case R.id.switchFallDetection:
                    settings.fallDetection = isChecked;
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

            updateSettings();
        };

        allDay = findViewById(R.id.switchAllDay);
        allDay.setChecked(settings.allDay);
        allDay.setOnCheckedChangeListener(checkedChangeListener);

        tvFrom = findViewById(R.id.tvFrom);
        tvFrom.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);
        tvTo = findViewById(R.id.tvTo);
        tvTo.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        fromTime = findViewById(R.id.tvFromTime);
        fromTime.setOnClickListener(onTimeClickListener);
        fromTime.setText(String.format("%02d:%02d", settings.fromHour, settings.fromMinute));
        fromTime.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        toTime = findViewById(R.id.tvToTime);
        toTime.setOnClickListener(onTimeClickListener);
        toTime.setText(String.format("%02d:%02d", settings.toHour, settings.toMinute));
        toTime.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        tvFirstCheck = findViewById(R.id.tvFirstCheck);
        setFirstCheckText();

        fallDetection = findViewById(R.id.switchFallDetection);
        fallDetection.setChecked(settings.fallDetection);
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
        long firstCheckIn = getNextCheckIn();
        tvFirstCheck.setText("First wellness check will be at " + getFirstCheckInString(firstCheckIn));
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timer = new Timer();
                timer.schedule(this, new Date(getNextCheckIn()));
            }
        }, new Date(firstCheckIn));
    }

    void startMonitoring(){
        timer.cancel();

        settings.monitoringOn = true;
        settings.checkedIn = true;
        settings.nextCheckIn = getNextCheckIn();
        settings.prevCheckIn = System.currentTimeMillis();
        updateSettings();
        Log.d(TAG, "triggering first alarm at " + getReadableTime(settings.nextCheckIn));

        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Service.ALARM_SERVICE);

        Bundle bundle = new Bundle();
        bundle.putInt(MonitorReceiver.EXTRA_INTERVAL1, settings.checkInHours);
        bundle.putLong(MonitorReceiver.EXTRA_INTERVAL2, settings.respondMinutes * MINUTE_IN_MILLIS);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_HOUR, settings.fromHour);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_MINUTE, settings.fromMinute);
        bundle.putInt(MonitorReceiver.EXTRA_TO_HOUR, settings.toHour);
        bundle.putInt(MonitorReceiver.EXTRA_TO_MINUTE, settings.toMinute);
        bundle.putBoolean(MonitorReceiver.EXTRA_ALL_DAY, settings.allDay);

        Intent intent = new Intent(getApplicationContext(), MonitorReceiver.class).setAction(MonitorReceiver.ACTION_ALARM)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND).putExtras(bundle);

        PendingIntent pendingNotifyIntent = PendingIntent.getBroadcast(getApplicationContext(), 1,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(settings.nextCheckIn, pendingNotifyIntent);

        alarmManager.setAlarmClock(alarmClockInfo, pendingNotifyIntent);

        startActivity(new Intent(SetupSettingsActivity.this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

        SetupSettingsActivity.this.finish();
    }

    private String getFirstCheckInString(long firstCheckIn){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(firstCheckIn);
        String time = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        if(firstCheckIn > WellnessCheck.getMidnight(calendar)) time += " tomorrow";
        return time;
    }
}
