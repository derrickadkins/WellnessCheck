package com.derrick.wellnesscheck;

import static android.content.Intent.ACTION_DEFAULT;
import static android.content.Intent.ACTION_DELETE;

import static com.derrick.wellnesscheck.MainActivity.InitDB;
import static com.derrick.wellnesscheck.MainActivity.db;
import static com.derrick.wellnesscheck.MainActivity.settings;
import static com.derrick.wellnesscheck.MainActivity.updateSettings;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class MonitorReceiver extends BroadcastReceiver {
    final String CHANNEL_ID = "WellnessCheck.MonitorReceiver";
    final int NOTIFICATION_ID = 0;
    public static final String ACTION_RESPONSE = "com.derrick.wellnesscheck.CANCEL_TIMER";
    public static final String ACTION_ALARM = "com.derrick.wellnesscheck.ALARM_TRIGGERED";
    public static final String EXTRA_INTERVAL1 = "mainInterval";
    public static final String EXTRA_INTERVAL2 = "responseInterval";
    public static final String EXTRA_FROM_HOUR = "fromHour";
    public static final String EXTRA_FROM_MINUTE = "fromMinute";
    public static final String EXTRA_TO_HOUR = "toHour";
    public static final String EXTRA_TO_MINUTE = "toMinute";
    static CountDownTimer countDownTimer;
    long mainInterval, responseInterval;
    int fromHour, fromMinute, toHour, toMinute;
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManagerCompat;
    AlarmManager alarmManager;
    final String TAG = "MonitorReceiver";
    public static CheckInListener checkInListener;
    public MonitorReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive, intent = " + intent.toString());
        createNotificationChannel(context);
        notificationManagerCompat = NotificationManagerCompat.from(context);
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        switch (intent.getAction()){
            case ACTION_ALARM:
                mainInterval = intent.getLongExtra(EXTRA_INTERVAL1, 60 * 60 * 1000);
                responseInterval = intent.getLongExtra(EXTRA_INTERVAL2, 60 * 1000);
                fromHour = intent.getIntExtra(EXTRA_FROM_HOUR, 8);
                fromMinute = intent.getIntExtra(EXTRA_FROM_MINUTE, 0);
                toHour = intent.getIntExtra(EXTRA_TO_HOUR, 20);
                toMinute = intent.getIntExtra(EXTRA_TO_MINUTE, 0);

                long minutes = responseInterval / (60 * 1000) % 60;
                long seconds = responseInterval / 1000 % 60;
                String timeLeft = minutes > 0 ? minutes + ":" : "";
                timeLeft += String.format("%02d", seconds);

                PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, 0,
                        new Intent(context, MonitorReceiver.class).setAction(ACTION_ALARM)
                                .putExtras(intent),
                        PendingIntent.FLAG_UPDATE_CURRENT);

                //setup next one
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(getNextCheckIn(), alarmPendingIntent), alarmPendingIntent);
                builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.alert_light_frame)
                        .setContentTitle("Time To Check In")
                        .setContentText("Click to check in. Notifying Emergency Contact in " + timeLeft)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(PendingIntent.getBroadcast(context, 0,
                                new Intent(context, MonitorReceiver.class)
                                        .setAction(ACTION_RESPONSE),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true);

                if(db == null) InitDB(context);
                settings.checkedIn = false;
                updateSettings();

                startCheckIn();
                break;
            case ACTION_RESPONSE:
                Log.d(TAG, "response timer requested");
                if (countDownTimer != null) {
                    if(db == null) InitDB(context);
                    settings.checkedIn = true;
                    updateSettings();
                    countDownTimer.cancel();
                    if(checkInListener != null) checkInListener.onCheckIn();
                    notificationManagerCompat.cancel(NOTIFICATION_ID);
                }
                break;
            case ACTION_DELETE:
                notificationManagerCompat.deleteNotificationChannel(CHANNEL_ID);
                if(countDownTimer != null) countDownTimer.cancel();
                alarmManager.cancel(PendingIntent.getBroadcast(context, 0,
                        new Intent(context, MonitorReceiver.class).setAction(ACTION_ALARM)
                                .putExtras(intent),
                        PendingIntent.FLAG_UPDATE_CURRENT));
                break;
            case ACTION_DEFAULT:
                break;
        }
    }

    void startCheckIn() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "check in started, responseInterval = " + responseInterval);

                countDownTimer = new CountDownTimer(responseInterval, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long minutes = millisUntilFinished / (60 * 1000) % 60;
                        long seconds = millisUntilFinished / 1000 % 60;
                        String timeLeft = minutes > 0 ? minutes + ":" : "";
                        timeLeft += String.format("%02d", seconds);

                        builder.setContentText("Click to check in. Notifying Emergency Contact in " + timeLeft);
                        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
                    }

                    @Override
                    public void onFinish() {
                        //todo: Message Emergency Contact
                        builder.setContentText("Message has been sent to your emergency contacts");
                        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
                    }
                }.start();
            }
        });
    }

    public interface CheckInListener {
        void onCheckIn();
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private long getNextCheckIn(){
        final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

        //used to get excluded time boundaries
        Calendar calendar = Calendar.getInstance();

        //start with default and change if in excluded hours
        calendar.add(Calendar.MILLISECOND, (int)mainInterval);
        long nextCheckIn = calendar.getTimeInMillis();

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
        calendar.add(Calendar.MINUTE, 1);
        long midnight = calendar.getTimeInMillis();

        //put excluded time boundaries on either side of next check-in
        if(startOfDay < endOfDay) {
            if(nextCheckIn > endOfDay)
                startOfDay += DAY_IN_MILLIS;
            else if(nextCheckIn < startOfDay)
                endOfDay -= DAY_IN_MILLIS;
        }else if(startOfDay > endOfDay) {
            if(nextCheckIn > startOfDay)
                endOfDay += DAY_IN_MILLIS;
            else if(nextCheckIn < endOfDay)
                startOfDay -= DAY_IN_MILLIS;
        }else if(nextCheckIn > midnight){
            nextCheckIn = midnight;
        }

        //return default if all day or not in excluded time, otherwise return next start time
        if(startOfDay == endOfDay || (nextCheckIn < endOfDay && nextCheckIn > startOfDay))
            return nextCheckIn;
        else return startOfDay;
    }
}
