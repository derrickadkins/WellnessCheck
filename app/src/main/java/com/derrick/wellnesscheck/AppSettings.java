package com.derrick.wellnesscheck;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class AppSettings {
    @NonNull
    @PrimaryKey
    public int id = 1;
    public int checkInHours, respondMinutes, fromHour, fromMinute, toHour, toMinute;
    public boolean fallDetection, allDay, monitoringOn;

    public AppSettings(){
        checkInHours = 1;
        respondMinutes = 1;
        fromHour = 8;
        fromMinute = 0;
        toHour = 20;
        toMinute = 0;
        fallDetection = false;
        allDay = false;
        monitoringOn = false;
    }
}