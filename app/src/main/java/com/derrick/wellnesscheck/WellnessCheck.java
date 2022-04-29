package com.derrick.wellnesscheck;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.derrick.wellnesscheck.utils.Utils.getReadableTime;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.derrick.wellnesscheck.model.DB;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Prefs;

import java.util.Calendar;

public class WellnessCheck extends Application {
    private static final String TAG = "WellnessCheck";
    public static Context context;
    public static DB db;

    @Override
    public void onCreate() {
        super.onCreate();
        WellnessCheck.context = getApplicationContext();
        Log.d(TAG, "onCreate");
    }

    public static boolean applyPrefs(Context context){
        if(!Prefs.monitoringOn()) return false;
        Prefs.updateCheckIn(WellnessCheck.getNextCheckIn());
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Service.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
        if(alarmClockInfo != null && alarmClockInfo.getTriggerTime() == Prefs.nextCheckIn()) return false;
        else{
            if(alarmClockInfo != null) {
                Log.d(TAG, "alarm already set for " + getReadableTime(alarmClockInfo.getTriggerTime()));
                cancelAlarm(context, MonitorReceiver.ACTION_NOTIFY);
            }
            long now = System.currentTimeMillis();
            long time = Prefs.nextCheckIn();
            long responseInterval = Prefs.respondMinutes() * MINUTE_IN_MILLIS;
            if(!Prefs.checkedIn() && Prefs.prevCheckIn() + responseInterval > now)
                time = Prefs.prevCheckIn() + responseInterval - now;

            //if(Prefs.fallDetection()) context.startService(new Intent(context, FallDetectionService.class));
            WellnessCheck.setAlarm(context, time, MonitorReceiver.ACTION_NOTIFY);
            return true;
        }
    }

    public static void setAlarm(Context context, long time, String action){
        context = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
        PendingIntent pendingIntent = getAlarmIntent(context, action);
        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(time, pendingIntent);
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
        Log.d(TAG, action + " alarm set for " + getReadableTime(time));
    }

    public static void cancelAlarm(Context context, String action){
        context = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
        alarmManager.cancel(getAlarmIntent(context, action));
        Log.d(TAG, action + " alarm canceled");
    }

    static PendingIntent getAlarmIntent(Context context, String action){
        Intent intent = new Intent(context, MonitorReceiver.class).setAction(action)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        int requestCode = action.equals(MonitorReceiver.ACTION_NOTIFY) ? 1 : 0;
        return PendingIntent.getBroadcast(context, requestCode,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static long getNextCheckIn() {
        return getNextCheckIn(Prefs.checkInHours(), Prefs.fromHour(), Prefs.fromMinute(), Prefs.toHour(), Prefs.toMinute(), Prefs.allDay());
    }

    private static long getNextCheckIn(int checkInHours, int fromHour, int fromMinute, int toHour, int toMinute, boolean allDay){
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
