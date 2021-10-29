package com.derrick.wellnesscheck;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_DELETE;
import static android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED;
import static android.content.Intent.ACTION_REBOOT;
import static com.derrick.wellnesscheck.DbController.InitDB;
import static com.derrick.wellnesscheck.DbController.contacts;
import static com.derrick.wellnesscheck.DbController.db;
import static com.derrick.wellnesscheck.DbController.settings;
import static com.derrick.wellnesscheck.DbController.updateSettings;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class MonitorReceiver extends BroadcastReceiver implements DbController.DbListener {

    final String TAG = "MonitorReceiver";

    public static final String ACTION_ALARM = "com.derrick.wellnesscheck.ALARM_TRIGGERED";
    public static final String ACTION_RESPONSE = "com.derrick.wellnesscheck.CANCEL_TIMER";
    public static final String ACTION_SMS = "com.derrick.wellnesscheck.SEND_SMS";
    public static final String EXTRA_INTERVAL1 = "mainInterval";
    public static final String EXTRA_INTERVAL2 = "responseInterval";
    public static final String EXTRA_ALL_DAY = "allDay";
    public static final String EXTRA_FROM_HOUR = "fromHour";
    public static final String EXTRA_FROM_MINUTE = "fromMinute";
    public static final String EXTRA_TO_HOUR = "toHour";
    public static final String EXTRA_TO_MINUTE = "toMinute";

    final int NOTIFICATION_ID = 0;
    final String CHANNEL_ID = "WellnessCheck.CheckInService";
    AlarmManager alarmManager;
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManagerCompat;
    Intent smsIntent, alarmIntent, responseIntent;
    PendingIntent smsPendingIntent, alarmPendingIntent, responsePendingIntent;

    public static CheckInListener checkInListener;
    public interface CheckInListener {
        void onCheckIn();
    }

    Intent intent;
    Context context;

    public MonitorReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {
        this.intent = intent;
        this.context = context;

        Log.d(TAG, "onReceive, intent = " + intent.toString());

        createNotificationChannel();
        notificationManagerCompat = NotificationManagerCompat.from(context);

        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        smsIntent = new Intent(context, MonitorReceiver.class).setAction(ACTION_SMS);
        smsPendingIntent = PendingIntent.getBroadcast(context, 0, smsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmIntent = new Intent(context, MonitorReceiver.class).setAction(ACTION_ALARM).putExtras(intent);
        alarmPendingIntent = PendingIntent.getBroadcast(context, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        responseIntent = new Intent(context, MonitorReceiver.class).setAction(ACTION_RESPONSE);
        responsePendingIntent = PendingIntent.getBroadcast(context, 2, responseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.alert_light_frame)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setOngoing(true);

        switch (intent.getAction()){
            case ACTION_ALARM:
                long mainInterval = intent.getLongExtra(EXTRA_INTERVAL1, 60 * 60 * 1000);
                boolean allDay = intent.getBooleanExtra(EXTRA_ALL_DAY, false);
                int fromHour = intent.getIntExtra(EXTRA_FROM_HOUR, 8);
                int fromMinute = intent.getIntExtra(EXTRA_FROM_MINUTE, 0);
                int toHour = intent.getIntExtra(EXTRA_TO_HOUR, 20);
                int toMinute = intent.getIntExtra(EXTRA_TO_MINUTE, 0);
                long responseInterval = intent.getLongExtra(EXTRA_INTERVAL2, 60 * 1000);

                long nextCheckIn = getNextCheckIn(mainInterval, fromHour, fromMinute, toHour, toMinute, allDay);
                Log.d(TAG, "Next check-in scheduled for " + getNextCheckInReadable(nextCheckIn));

                //next check-in
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(nextCheckIn,
                        alarmPendingIntent), alarmPendingIntent);
                //send sms
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(
                        System.currentTimeMillis() + responseInterval,
                        smsPendingIntent), smsPendingIntent);
                builder.setContentTitle("Time To Check In")
                    .setContentText("Click to check in.") //todo: add time
                    .setContentIntent(responsePendingIntent);
                notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
                InitDB(context, this);
                break;
            case ACTION_BOOT_COMPLETED:
                InitDB(context, this);
                break;
            case ACTION_RESPONSE:
                if(checkInListener != null) checkInListener.onCheckIn();
                notificationManagerCompat.cancel(NOTIFICATION_ID);
                alarmManager.cancel(smsPendingIntent);
                InitDB(context, this);
                break;
            case ACTION_SMS:
                builder.setContentTitle("You've missed your check-in")
                        .setContentText("Message will been sent to your emergency contacts.")
                        .setContentIntent(responsePendingIntent);
                notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
                sendMissedCheckInSMS();
                break;
            case ACTION_DELETE:
                notificationManagerCompat.deleteNotificationChannel(CHANNEL_ID);
                alarmManager.cancel(alarmPendingIntent);
                break;
        }
    }

    @Override
    public void onDbReady() {
        switch (intent.getAction()){
            case ACTION_ALARM:
                settings.checkedIn = false;
                updateSettings();
                break;
            case ACTION_RESPONSE:
                settings.checkedIn = true;
                updateSettings();
                break;
            case ACTION_BOOT_COMPLETED:
                //todo: notify immediately if check-in was missed?
                long mainInterval = settings.checkInHours * 60 * 60 * 1000;

                long nextCheckIn = getNextCheckIn(mainInterval, settings.fromHour, settings.fromMinute,
                        settings.toHour, settings.toMinute, settings.allDay);

                Log.d(TAG, "Next check-in scheduled for " + getNextCheckInReadable(nextCheckIn));

                //next check-in
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(nextCheckIn,
                        alarmPendingIntent), alarmPendingIntent);
                break;
        }
    }

    String getNextCheckInReadable(long nextCheckIn){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nextCheckIn);
        return calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.DAY_OF_MONTH)
                + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":"
                + calendar.get(Calendar.SECOND) + "." + calendar.get(Calendar.MILLISECOND);
    }

    void sendMissedCheckInSMS(){
        SmsBroadcastManager smsBroadcastManager = new SmsBroadcastManager();
        SmsController smsController = new SmsController() {
            @Override
            void onSmsReceived(String number, String message) {}
            @Override
            void onSmsFailedToSend() {}
            @Override
            void onSmsSent() {}
        };

        SmsBroadcastManager.smsController = smsController;
        DbController.DbListener dbListener = new DbController.DbListener() {
            @Override
            public void onDbReady() {
                for(Contact contact : contacts)
                    smsController.sendSMS(context.getApplicationContext(), smsBroadcastManager, smsController, contact.number, context.getString(R.string.missed_check_in));
            }
        };

        if(db == null) InitDB(context, dbListener);
        else dbListener.onDbReady();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private long getNextCheckIn(long mainInterval, int fromHour, int fromMinute, int toHour, int toMinute, boolean allDay){
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
