package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.DbController.settings;
import static com.derrick.wellnesscheck.DbController.updateSettings;
import static com.derrick.wellnesscheck.MonitorReceiver.getReadableTime;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.Nullable;
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
    static final long DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;

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

        checkInHours = (NumberPicker) findViewById(R.id.numberPickerHours);
        checkInHours.setMinValue(1);
        checkInHours.setMaxValue(24);
        checkInHours.setValue(settings.checkInHours);
        checkInHours.setOnValueChangedListener((picker, oldVal, newVal) -> {
            settings.checkInHours = newVal;
            updateSettings();
            setFirstCheckText();
        });

        respondMinutes = (NumberPicker) findViewById(R.id.numberPickerMinutes);
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
        long firstCheckIn = getNextCheckIn(settings.checkInHours, settings.fromHour, settings.fromMinute, settings.toHour, settings.toMinute, settings.allDay);
        tvFirstCheck.setText("First wellness check will be at " + getFirstCheckInString(firstCheckIn));
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timer = new Timer();
                timer.schedule(this, new Date(getNextCheckIn(settings.checkInHours, settings.fromHour, settings.fromMinute, settings.toHour, settings.toMinute, settings.allDay)));
            }
        }, new Date(firstCheckIn));
    }

    void startMonitoring(){
        timer.cancel();

        settings.monitoringOn = true;
        settings.checkedIn = true;
        settings.nextCheckIn = getNextCheckIn();
        updateSettings();
        Log.d(TAG, "triggering first alarm at " + getReadableTime(settings.nextCheckIn));

        AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);

        Bundle bundle = new Bundle();
        bundle.putLong(MonitorReceiver.EXTRA_INTERVAL1, settings.checkInHours * HOUR_IN_MILLIS);
        bundle.putLong(MonitorReceiver.EXTRA_INTERVAL2, settings.respondMinutes * MINUTE_IN_MILLIS);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_HOUR, settings.fromHour);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_MINUTE, settings.fromMinute);
        bundle.putInt(MonitorReceiver.EXTRA_TO_HOUR, settings.toHour);
        bundle.putInt(MonitorReceiver.EXTRA_TO_MINUTE, settings.toMinute);
        bundle.putBoolean(MonitorReceiver.EXTRA_ALL_DAY, settings.allDay);

        Intent intent = new Intent(this, MonitorReceiver.class).setAction(MonitorReceiver.ACTION_ALARM)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND).putExtras(bundle);

        PendingIntent pendingNotifyIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(settings.nextCheckIn, pendingNotifyIntent);

        alarmManager.setAlarmClock(alarmClockInfo, pendingNotifyIntent);

        startActivity(new Intent(SetupSettingsActivity.this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

        SetupSettingsActivity.this.finish();
    }

    public static long getMidnight(Calendar calendar){
        /*
        add one second from midnight because day of month or
        year might be the last one, so adding a second is easier
         */
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 1);
        return calendar.getTimeInMillis();
    }

    private String getFirstCheckInString(long firstCheckIn){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(firstCheckIn);
        String time = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        if(firstCheckIn > getMidnight(calendar)) time += " tomorrow";
        return time;
    }

    public static long getNextCheckIn(long interval, int fromHour, int fromMinute, int toHour, int toMinute, boolean allDay){
        //used to get excluded time boundaries
        Calendar calendar = Calendar.getInstance();
        final long now = calendar.getTimeInMillis();
        //clear for precision
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //get to time
        calendar.set(Calendar.HOUR_OF_DAY, toHour);
        calendar.set(Calendar.MINUTE, toMinute);
        long endOfDay = calendar.getTimeInMillis();

        //get first check as from time
        calendar.set(Calendar.HOUR_OF_DAY, fromHour);
        calendar.set(Calendar.MINUTE, fromMinute);
        long startOfDay = calendar.getTimeInMillis();

        /*
        set first to one interval after midnight if all day
        because midnight will always be behind now. if not
        all day, set it to the start
         */
        long nextCheckIn = startOfDay;
        if(allDay){
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            nextCheckIn = calendar.getTimeInMillis() + interval;
        }else if(nextCheckIn < endOfDay && now > endOfDay){
            nextCheckIn += DAY_IN_MILLIS;
        }else if(nextCheckIn > endOfDay && now < endOfDay){
            nextCheckIn -= DAY_IN_MILLIS;
        }

        //reminder: INTERVAL <= 24
        while(nextCheckIn < now)
            nextCheckIn += interval;

        //if all day and first check in is after midnight, set it to midnight
        if(allDay){
            long midnight = getMidnight(calendar);
            if(nextCheckIn > midnight) nextCheckIn = midnight;
        }
        //if after end of day push to next start
        else if (nextCheckIn > endOfDay) {
            nextCheckIn = startOfDay + DAY_IN_MILLIS;
        }

        return nextCheckIn;
    }

    public static long getNextCheckIn(int checkInHours, int fromHour, int fromMinute, int toHour, int toMinute, boolean allDay){
        return getNextCheckIn(checkInHours * HOUR_IN_MILLIS, fromHour, fromMinute, toHour, toMinute, allDay);
    }

    public static long getNextCheckIn(AppSettings settings){
        return getNextCheckIn(settings.checkInHours, settings.fromHour, settings.fromMinute, settings.toHour, settings.toMinute, settings.allDay);
    }

    public static long getNextCheckIn() throws NullPointerException{
        if(settings == null) throw new NullPointerException("Settings are null, init db first");
        return getNextCheckIn(settings);
    }
}
