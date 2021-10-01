package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.MainActivity.settings;
import static com.derrick.wellnesscheck.MainActivity.updateSettings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SetupSettingsActivity extends AppCompatActivity {
    NumberPicker checkInHours, respondMinutes;
    TextView fromTime, toTime, tvFrom, tvTo;
    Switch allDay, fallDetection;
    Button finishSetup;
    final int HOUR_IN_MILLIS = 60 * 60 * 1000;
    final int MINUTE_IN_MILLIS = 60 * 1000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_activity);

        finishSetup = findViewById(R.id.btnFinishSetup);
        finishSetup.setVisibility(View.VISIBLE);
        finishSetup.setOnClickListener(v -> {
            settings.monitoringOn = true;
            settings.nextCheckIn = 0;
            settings.checkedIn = true;
            updateSettings();
            startMonitoring();
            startActivity(new Intent(SetupSettingsActivity.this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            SetupSettingsActivity.this.finish();
        });

        checkInHours = (NumberPicker) findViewById(R.id.numberPicker1);
        checkInHours.setMinValue(1);
        checkInHours.setMaxValue(24);
        checkInHours.setValue(settings.checkInHours);
        checkInHours.setOnValueChangedListener((picker, oldVal, newVal) -> {
            settings.checkInHours = newVal;
            updateSettings();
        });

        respondMinutes = (NumberPicker) findViewById(R.id.numberPicker2);
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
                case R.id.textView19:
                    hourOfDay = settings.fromHour;
                    minute = settings.fromMinute;
                    break;
                case R.id.textView20:
                    hourOfDay = settings.toHour;
                    minute = settings.toMinute;
                    break;
                default:
                    hourOfDay = 0;
                    minute = 0;
            }
            new TimePickerDialog(SetupSettingsActivity.this,
                    (view, hourOfDay1, minute1) -> {
                        ((TextView)v).setText(hourOfDay1 + ":" + minute1);
                        switch (view.getId()){
                            case R.id.textView19:
                                settings.fromHour = hourOfDay1;
                                settings.fromMinute = minute1;
                                break;
                            case R.id.textView20:
                                settings.toHour = hourOfDay1;
                                settings.toMinute = minute1;
                        }
                        updateSettings();
                    }, hourOfDay, minute, true).show();
        };

        CompoundButton.OnCheckedChangeListener checkedChangeListener = (buttonView, isChecked) -> {
            switch (buttonView.getId()){
                case R.id.switch1:
                    settings.fallDetection = isChecked;
                    break;
                case R.id.switch2:
                    settings.allDay = isChecked;
                    int visibility = View.VISIBLE;
                    if(isChecked){
                        visibility = View.GONE;
                    }
                    tvFrom.setVisibility(visibility);
                    fromTime.setVisibility(visibility);
                    tvTo.setVisibility(visibility);
                    toTime.setVisibility(visibility);
                    break;
            }

            updateSettings();
        };

        allDay = findViewById(R.id.switch2);
        allDay.setChecked(settings.allDay);
        allDay.setOnCheckedChangeListener(checkedChangeListener);

        tvFrom = findViewById(R.id.textView17);
        tvFrom.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);
        tvTo = findViewById(R.id.textView18);
        tvTo.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        fromTime = findViewById(R.id.textView19);
        fromTime.setOnClickListener(onTimeClickListener);
        fromTime.setText(String.format("%02d:%02d", settings.fromHour, settings.fromMinute));
        fromTime.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        toTime = findViewById(R.id.textView20);
        toTime.setOnClickListener(onTimeClickListener);
        toTime.setText(String.format("%02d:%02d", settings.toHour, settings.toMinute));
        toTime.setVisibility(settings.allDay ? View.GONE : View.VISIBLE);

        fallDetection = findViewById(R.id.switch1);
        fallDetection.setChecked(settings.fallDetection);
    }

    void startMonitoring(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
        //Log.d(TAG, "triggering first alarm in " + (settings.nextCheckIn - System.currentTimeMillis()) + " millis");
        PendingIntent pendingNotifyIntent;
        Intent intent = new Intent(this, MonitorReceiver.class).setAction(MonitorReceiver.ACTION_ALARM)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(MonitorReceiver.EXTRA_INTERVAL1, settings.checkInHours * HOUR_IN_MILLIS)
                .putExtra(MonitorReceiver.EXTRA_INTERVAL2, settings.respondMinutes * MINUTE_IN_MILLIS);
        pendingNotifyIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //todo: set custom start time
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(
                System.currentTimeMillis() + settings.checkInHours * HOUR_IN_MILLIS,
                pendingNotifyIntent), pendingNotifyIntent);
    }
}
