package com.derrick.wellnesscheck;

import static android.content.Intent.ACTION_DELETE;
import static com.derrick.wellnesscheck.DbController.InitDB;
import static com.derrick.wellnesscheck.DbController.contacts;
import static com.derrick.wellnesscheck.DbController.db;
import static com.derrick.wellnesscheck.DbController.settings;
import static com.derrick.wellnesscheck.DbController.updateSettings;
import static com.derrick.wellnesscheck.MonitorReceiver.ACTION_ALARM;
import static com.derrick.wellnesscheck.MonitorReceiver.EXTRA_INTERVAL2;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class CheckInService extends Service implements DbController.DbListener {
    final String TAG = "CheckInService";
    final int NOTIFICATION_ID = 1;
    public static final String ACTION_RESPONSE = "com.derrick.wellnesscheck.CANCEL_TIMER";
    public static final String ACTION_ALARM = "com.derrick.wellnesscheck.ALARM_TRIGGERED";
    public static final String EXTRA_INTERVAL1 = "mainInterval";
    public static final String EXTRA_INTERVAL2 = "responseInterval";
    public static final String EXTRA_ALL_DAY = "allDay";
    public static final String EXTRA_FROM_HOUR = "fromHour";
    public static final String EXTRA_FROM_MINUTE = "fromMinute";
    public static final String EXTRA_TO_HOUR = "toHour";
    public static final String EXTRA_TO_MINUTE = "toMinute";

    final String CHANNEL_ID = "WellnessCheck.CheckInService";
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManagerCompat;
    static CountDownTimer countDownTimer;
    long responseInterval;
    long mainInterval;
    int fromHour, fromMinute, toHour, toMinute;
    boolean allDay;
    AlarmManager alarmManager;

    Intent intent;
    public static CheckInListener checkInListener;
    public interface CheckInListener {
        void onCheckIn();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "CheckInService created");
        super.onCreate();
        createNotificationChannel();
        notificationManagerCompat = NotificationManagerCompat.from(this);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent responseIntent = PendingIntent.getService(this, 0,
                new Intent(this, CheckInService.class)
                        .setAction(ACTION_RESPONSE),
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.alert_light_frame)
                .setContentTitle("Time To Check In")
                .setContentText("Click to check in. Notifying Emergency Contact in 00")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(responseIntent)
                .addAction(android.R.drawable.alert_light_frame, ACTION_RESPONSE, responseIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setOngoing(true);

        Notification notification = builder.build();
        notificationManagerCompat.notify(NOTIFICATION_ID, notification);
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CheckInService started: " + intent.toString());
        this.intent = intent;
        switch (intent.getAction()){
            case ACTION_ALARM:
                //setup next one
                mainInterval = intent.getLongExtra(EXTRA_INTERVAL1, 60 * 60 * 1000);
                allDay = intent.getBooleanExtra(EXTRA_ALL_DAY, false);
                fromHour = intent.getIntExtra(EXTRA_FROM_HOUR, 8);
                fromMinute = intent.getIntExtra(EXTRA_FROM_MINUTE, 0);
                toHour = intent.getIntExtra(EXTRA_TO_HOUR, 20);
                toMinute = intent.getIntExtra(EXTRA_TO_MINUTE, 0);
                responseInterval = intent.getLongExtra(EXTRA_INTERVAL2, 60 * 1000);

                PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(this, 0,
                        new Intent(this, MonitorReceiver.class).setAction(ACTION_ALARM)
                                .putExtras(intent),
                        PendingIntent.FLAG_UPDATE_CURRENT);
                /*
                PendingIntent alarmPendingIntent;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    alarmPendingIntent = PendingIntent.getForegroundService(this, 0,
                            new Intent(this, CheckInService.class).setAction(ACTION_ALARM)
                                    .putExtras(intent),
                            PendingIntent.FLAG_UPDATE_CURRENT);
                }else{
                    alarmPendingIntent = PendingIntent.getService(this, 0,
                            new Intent(this, CheckInService.class).setAction(ACTION_ALARM)
                                    .putExtras(intent),
                            PendingIntent.FLAG_UPDATE_CURRENT);
                }
                 */
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(getNextCheckIn(), alarmPendingIntent), alarmPendingIntent);

                long minutes = responseInterval / (60 * 1000) % 60;
                long seconds = responseInterval / 1000 % 60;
                String timeLeft = minutes > 0 ? minutes + ":" : "";
                timeLeft += String.format("%02d", seconds);

                builder.setContentText("Click to check in. Notifying Emergency Contact in " + timeLeft);
                Notification notification = builder.build();
                notificationManagerCompat.notify(NOTIFICATION_ID, notification);
                startForeground(NOTIFICATION_ID, notification);

                startCheckIn();
                InitDB(this);
                break;
            case ACTION_RESPONSE:
                Log.d(TAG, "response timer requested");
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    if(checkInListener != null) checkInListener.onCheckIn();
                    notificationManagerCompat.cancel(NOTIFICATION_ID);
                    InitDB(this);
                }
                break;
            case ACTION_DELETE:
                notificationManagerCompat.deleteNotificationChannel(CHANNEL_ID);
                if(countDownTimer != null) countDownTimer.cancel();
                CheckInService.this.stopSelf();
                break;
        }
        return START_STICKY_COMPATIBILITY;
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
                CheckInService.this.stopSelf();
                break;
        }
    }

    void startCheckIn() {
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
                sendMissedCheckInSMS();
                builder.setContentText("Message has been sent to your emergency contacts");
                notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
                CheckInService.this.stopSelf();
            }
        }.start();
    }

    void sendMissedCheckInSMS(){
        Log.d(TAG, "Sending Missed Check-In SMS");
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
                    smsController.sendSMS(CheckInService.this, smsBroadcastManager, smsController, contact.number, getString(R.string.missed_check_in));
            }
        };

        if(db == null) InitDB(this, dbListener);
        else dbListener.onDbReady();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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
