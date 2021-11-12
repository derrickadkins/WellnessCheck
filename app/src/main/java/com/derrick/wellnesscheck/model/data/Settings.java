package com.derrick.wellnesscheck.model.data;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import static com.derrick.wellnesscheck.WellnessCheck.db;
import static com.derrick.wellnesscheck.utils.Utils.getReadableTime;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.derrick.wellnesscheck.MonitorReceiver;
import com.derrick.wellnesscheck.WellnessCheck;

import java.lang.reflect.Field;

@Entity
public class Settings {
    static final String TAG = "Settings";

    @NonNull
    @PrimaryKey
    public int id = 1;
    public int checkInHours, respondMinutes, fromHour, fromMinute, toHour, toMinute;
    public long nextCheckIn, prevCheckIn;
    public boolean fallDetection, allDay, monitoringOn, checkedIn;

    public Settings(){
        checkInHours = 1;
        respondMinutes = 1;
        fromHour = 8;
        fromMinute = 0;
        toHour = 20;
        toMinute = 0;
        prevCheckIn = 0;
        nextCheckIn = 0;
        allDay = false;
        monitoringOn = false;
        checkedIn = false;
        fallDetection = false;
    }

    public static Settings Init(){
        Settings settings = db.settingsDao().getSettings();
        if(settings == null) {
            Log.d(TAG, "settings null, creating new");
            settings = new Settings();
            db.settingsDao().insert(settings);
        }
        return settings;
    }

    public void updateCheckIn(long nextCheckIn){
        if(this.nextCheckIn == nextCheckIn) return;
        prevCheckIn = this.nextCheckIn;
        this.nextCheckIn = nextCheckIn;
        update();
    }

    public void update(){new Thread(() -> db.settingsDao().update(this)).start();}

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        try {
            for (Field f : getClass().getDeclaredFields()) {
                Object v = f.get(this);
                if (v instanceof Long)
                    sb.append(String.format("%s:%s(%s), ", f.getName(), v, getReadableTime((long) v)));
                else
                    sb.append(String.format("%s:%s, ", f.getName(), v));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return sb.substring(0, sb.length() - 2);
    }

    public Bundle toBundle(){
        Bundle bundle = new Bundle();
        bundle.putInt(MonitorReceiver.EXTRA_INTERVAL1, checkInHours);
        bundle.putLong(MonitorReceiver.EXTRA_INTERVAL2, respondMinutes * MINUTE_IN_MILLIS);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_HOUR, fromHour);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_MINUTE, fromMinute);
        bundle.putInt(MonitorReceiver.EXTRA_TO_HOUR, toHour);
        bundle.putInt(MonitorReceiver.EXTRA_TO_MINUTE, toMinute);
        bundle.putBoolean(MonitorReceiver.EXTRA_ALL_DAY, allDay);
        return bundle;
    }
}
