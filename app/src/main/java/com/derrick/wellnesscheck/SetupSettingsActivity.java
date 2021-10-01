package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.MainActivity.settings;
import static com.derrick.wellnesscheck.MainActivity.updateSettings;

import android.app.TimePickerDialog;
import android.content.Intent;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_activity);

        finishSetup = findViewById(R.id.btnFinishSetup);
        finishSetup.setVisibility(View.VISIBLE);
        finishSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settings.monitoringOn = true;
                settings.nextCheckIn = 0;
                updateSettings();
                startActivity(new Intent(SetupSettingsActivity.this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                SetupSettingsActivity.this.finish();
            }
        });

        tvFrom = (TextView) findViewById(R.id.textView17);
        tvTo = (TextView) findViewById(R.id.textView18);

        checkInHours = (NumberPicker) findViewById(R.id.numberPicker1);
        checkInHours.setMinValue(1);
        checkInHours.setMaxValue(24);
        checkInHours.setValue(settings.checkInHours);
        checkInHours.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                settings.checkInHours = newVal;
                updateSettings();
            }
        });

        respondMinutes = (NumberPicker) findViewById(R.id.numberPicker2);
        respondMinutes.setMinValue(1);
        respondMinutes.setMaxValue(60);
        respondMinutes.setValue(settings.respondMinutes);
        respondMinutes.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                settings.respondMinutes = newVal;
                updateSettings();
            }
        });

        View.OnClickListener onTimeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                ((TextView)v).setText(hourOfDay + ":" + minute);
                                switch (view.getId()){
                                    case R.id.textView19:
                                        settings.fromHour = hourOfDay;
                                        settings.fromMinute = minute;
                                        break;
                                    case R.id.textView20:
                                        settings.toHour = hourOfDay;
                                        settings.toMinute = minute;
                                }
                                updateSettings();
                            }
                        }, hourOfDay, minute, true).show();
            }
        };

        fromTime = (TextView) findViewById(R.id.textView19);
        fromTime.setOnClickListener(onTimeClickListener);
        fromTime.setText(settings.fromHour + ":" + settings.fromMinute);

        toTime = (TextView) findViewById(R.id.textView20);
        toTime.setOnClickListener(onTimeClickListener);
        toTime.setText(settings.toHour + ":" + settings.toMinute);

        CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
            }
        };

        allDay = (Switch) findViewById(R.id.switch2);
        allDay.setChecked(settings.allDay);
        allDay.setOnCheckedChangeListener(checkedChangeListener);

        fallDetection = (Switch) findViewById(R.id.switch1);
        fallDetection.setChecked(settings.fallDetection);
    }
}
