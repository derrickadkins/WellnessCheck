package com.derrick.wellnesscheck;

import static android.content.Intent.ACTION_DELETE;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class MonitorReceiver extends BroadcastReceiver {

    public static final String ACTION_ALARM = "com.derrick.wellnesscheck.ALARM_TRIGGERED";
    public static final String EXTRA_INTERVAL1 = "mainInterval";
    public static final String EXTRA_INTERVAL2 = "responseInterval";
    public static final String EXTRA_ALL_DAY = "allDay";
    public static final String EXTRA_FROM_HOUR = "fromHour";
    public static final String EXTRA_FROM_MINUTE = "fromMinute";
    public static final String EXTRA_TO_HOUR = "toHour";
    public static final String EXTRA_TO_MINUTE = "toMinute";

    final String TAG = "MonitorReceiver";
    long mainInterval;
    int fromHour, fromMinute, toHour, toMinute;
    boolean allDay;
    AlarmManager alarmManager;

    Intent intent;
    Context context;

    public MonitorReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {
        this.intent = intent;
        this.context = context;

        Log.d(TAG, "onReceive, intent = " + intent.toString());

        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        switch (intent.getAction()){
            case ACTION_ALARM:
                //setup next one
                mainInterval = intent.getLongExtra(EXTRA_INTERVAL1, 60 * 60 * 1000);
                allDay = intent.getBooleanExtra(EXTRA_ALL_DAY, false);
                fromHour = intent.getIntExtra(EXTRA_FROM_HOUR, 8);
                fromMinute = intent.getIntExtra(EXTRA_FROM_MINUTE, 0);
                toHour = intent.getIntExtra(EXTRA_TO_HOUR, 20);
                toMinute = intent.getIntExtra(EXTRA_TO_MINUTE, 0);

                PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, 0,
                        new Intent(context, MonitorReceiver.class).setAction(ACTION_ALARM)
                                .putExtras(intent),
                        PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(getNextCheckIn(), alarmPendingIntent), alarmPendingIntent);
                break;
            case ACTION_DELETE:
                alarmManager.cancel(PendingIntent.getBroadcast(context, 0,
                        new Intent(context, MonitorReceiver.class).setAction(ACTION_ALARM)
                                .putExtras(intent),
                        PendingIntent.FLAG_UPDATE_CURRENT));
                break;
        }
        context.startService(new Intent(context, CheckInService.class).setAction(intent.getAction()).putExtras(intent));
    }

    private long getNextCheckIn(){
        final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

        //used to get excluded time boundaries
        Calendar calendar = Calendar.getInstance();
        //clear for precision
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //start with default and change if in excluded hours
        long nextCheckIn = calendar.getTimeInMillis() + mainInterval;

        //get from time
        calendar.set(Calendar.HOUR_OF_DAY, fromHour);
        calendar.set(Calendar.MINUTE, fromMinute);
        long startOfDay = calendar.getTimeInMillis();

        //get to time
        calendar.set(Calendar.HOUR_OF_DAY, toHour);
        calendar.set(Calendar.MINUTE, toMinute);
        long endOfDay = calendar.getTimeInMillis();

        /*
        add one minute from midnight because day of month or
        year might be the last one, so adding a minute is easier
         */
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 1);
        long midnight = calendar.getTimeInMillis();

        //put excluded time boundaries on either side of next check-in
        if(allDay && nextCheckIn > midnight){
            nextCheckIn = midnight;
        }else if(startOfDay < endOfDay) {
            if(nextCheckIn > endOfDay)
                startOfDay += DAY_IN_MILLIS;
            else if(nextCheckIn < startOfDay)
                endOfDay -= DAY_IN_MILLIS;
        }else if(startOfDay > endOfDay) {
            if(nextCheckIn > startOfDay)
                endOfDay += DAY_IN_MILLIS;
            else if(nextCheckIn < endOfDay)
                startOfDay -= DAY_IN_MILLIS;
        }

        //return default if all day or not in excluded time, otherwise return next start time
        if(startOfDay == endOfDay || (nextCheckIn < endOfDay && nextCheckIn > startOfDay))
            return nextCheckIn;
        else return startOfDay;
    }
}
