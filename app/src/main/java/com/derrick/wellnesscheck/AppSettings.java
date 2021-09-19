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
    public boolean fallDetection, allDay;

    public AppSettings(){
        checkInHours = 1;
        respondMinutes = 1;
        fromHour = 8;
        fromMinute = 0;
        toHour = 20;
        toMinute = 0;
        fallDetection = false;
        allDay = false;
    }

    @Ignore
    public AppSettings(int checkInHours, int respondMinutes, int fromHour, int fromMinute, int toHour, int toMinute, boolean fallDetection, boolean allDay){
        this.checkInHours = checkInHours;
        this.respondMinutes = respondMinutes;
        this.fromHour = fromHour;
        this.fromMinute = fromMinute;
        this.toHour = toHour;
        this.toMinute = toMinute;
        this.fallDetection = fallDetection;
        this.allDay = allDay;
    }
}
