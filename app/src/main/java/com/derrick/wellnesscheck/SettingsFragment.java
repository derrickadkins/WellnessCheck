package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.App.db;
import static com.derrick.wellnesscheck.App.settings;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment{
    NumberPicker checkInHours, respondMinutes;
    TextView fromTime, toTime, tvFrom, tvTo;
    Switch allDay, fallDetection;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View settingsFragmentView = inflater.inflate(R.layout.settings_fragment, container, false);

        tvFrom = (TextView) settingsFragmentView.findViewById(R.id.textView17);
        tvTo = (TextView) settingsFragmentView.findViewById(R.id.textView18);

        checkInHours = (NumberPicker) settingsFragmentView.findViewById(R.id.numberPicker1);
        checkInHours.setMinValue(1);
        checkInHours.setMaxValue(24);
        checkInHours.setValue(settings.checkInHours);
        checkInHours.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                settings.checkInHours = newVal;
                db.settingsDao().update(settings);
            }
        });

        respondMinutes = (NumberPicker) settingsFragmentView.findViewById(R.id.numberPicker2);
        respondMinutes.setMinValue(1);
        respondMinutes.setMaxValue(60);
        respondMinutes.setValue(settings.respondMinutes);
        respondMinutes.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                settings.respondMinutes = newVal;
                db.settingsDao().update(settings);
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
                new TimePickerDialog(getContext(),
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
                                db.settingsDao().update(settings);
                            }
                        }, hourOfDay, minute, true).show();
            }
        };

        fromTime = (TextView) settingsFragmentView.findViewById(R.id.textView19);
        fromTime.setOnClickListener(onTimeClickListener);
        fromTime.setText(settings.fromHour + ":" + settings.fromMinute);

        toTime = (TextView) settingsFragmentView.findViewById(R.id.textView20);
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

                db.settingsDao().update(settings);
            }
        };

        allDay = (Switch) settingsFragmentView.findViewById(R.id.switch2);
        allDay.setChecked(settings.allDay);
        allDay.setOnCheckedChangeListener(checkedChangeListener);

        fallDetection = (Switch) settingsFragmentView.findViewById(R.id.switch1);
        fallDetection.setChecked(settings.fallDetection);

        return settingsFragmentView;
    }
}
