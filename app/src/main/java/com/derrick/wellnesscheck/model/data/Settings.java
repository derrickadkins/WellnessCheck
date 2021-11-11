package com.derrick.wellnesscheck.model.data;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;

import static com.derrick.wellnesscheck.WellnessCheck.db;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.derrick.wellnesscheck.MonitorReceiver;

@Entity
public class Settings {
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
        fallDetection = false;
        allDay = false;
        monitoringOn = false;
        nextCheckIn = 0;
        prevCheckIn = 0;
        checkedIn = false;
    }

    public static Settings Init(){
        Settings settings = db.settingsDao().getSettings();
        if(settings == null) {
            settings = new Settings();
            db.settingsDao().insert(settings);
        }
        return settings;
    }

    public void update(){new Thread(() -> db.settingsDao().update(this));}

    public Bundle toBundle(){
        Bundle bundle = new Bundle();
        bundle.putInt(MonitorReceiver.EXTRA_INTERVAL1, checkInHours);
        bundle.putLong(MonitorReceiver.EXTRA_INTERVAL2, respondMinutes * HOUR_IN_MILLIS);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_HOUR, fromHour);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_MINUTE, fromMinute);
        bundle.putInt(MonitorReceiver.EXTRA_TO_HOUR, toHour);
        bundle.putInt(MonitorReceiver.EXTRA_TO_MINUTE, toMinute);
        bundle.putBoolean(MonitorReceiver.EXTRA_ALL_DAY, allDay);
        return bundle;
    }
}
