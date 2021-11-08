package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.controller.DbController.settings;

import android.app.Application;
import android.content.Context;

import com.derrick.wellnesscheck.model.data.AppSettings;
import com.derrick.wellnesscheck.utils.Log;

import java.util.Calendar;

public class WellnessCheck extends Application {
    public static Context context;
    private final String TAG = "WellnessCheck";

    @Override
    public void onCreate() {
        super.onCreate();
        WellnessCheck.context = getApplicationContext();
        Log.d(TAG, "onCreate");
    }

    public static long getNextCheckIn(int checkInHours, int fromHour, int fromMinute, int toHour, int toMinute, boolean allDay){
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
            calendar.set(Calendar.HOUR_OF_DAY, checkInHours);
            calendar.set(Calendar.MINUTE, 0);
            nextCheckIn = calendar.getTimeInMillis();
        }else if(startOfDay < endOfDay && now > endOfDay){
            calendar.add(Calendar.DATE, 1);
            nextCheckIn = calendar.getTimeInMillis();
        }else if(startOfDay > endOfDay && now < endOfDay){
            calendar.add(Calendar.DATE, -1);
            nextCheckIn = calendar.getTimeInMillis();
        }

        //reminder: INTERVAL <= 24
        while(nextCheckIn < now) {
            calendar.add(Calendar.HOUR, checkInHours);
            nextCheckIn = calendar.getTimeInMillis();
        }

        //if all day and first check in is after midnight, set it to midnight
        if(allDay){
            long midnight = getMidnight(calendar);
            if(nextCheckIn > midnight) nextCheckIn = midnight;
        }
        //if after end of day push to next start
        else if (nextCheckIn > endOfDay) {
            calendar.setTimeInMillis(startOfDay);
            calendar.add(Calendar.DATE, 1);
            nextCheckIn = calendar.getTimeInMillis();
        }

        return nextCheckIn;
    }

    public static long getNextCheckIn(AppSettings settings){
        return getNextCheckIn(settings.checkInHours, settings.fromHour, settings.fromMinute, settings.toHour, settings.toMinute, settings.allDay);
    }

    public static long getNextCheckIn() throws NullPointerException{
        if(settings == null) throw new NullPointerException("Settings are null, init db first");
        return getNextCheckIn(settings);
    }

    public static long getMidnight(Calendar calendar){
        /*
        add one second from midnight because day of month or
        year might be the last one, so adding a second is easier
         */
        calendar.add(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
