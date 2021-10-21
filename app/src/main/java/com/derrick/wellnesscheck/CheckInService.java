package com.derrick.wellnesscheck;

import static android.content.Intent.ACTION_DELETE;
import static com.derrick.wellnesscheck.DbController.InitDB;
import static com.derrick.wellnesscheck.DbController.contacts;
import static com.derrick.wellnesscheck.DbController.db;
import static com.derrick.wellnesscheck.DbController.settings;
import static com.derrick.wellnesscheck.DbController.updateSettings;
import static com.derrick.wellnesscheck.MonitorReceiver.ACTION_ALARM;
import static com.derrick.wellnesscheck.MonitorReceiver.EXTRA_INTERVAL2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class CheckInService extends Service implements DbController.DbListener {
    final String TAG = "CheckInService";
    final int NOTIFICATION_ID = 0;
    public static final String ACTION_RESPONSE = "com.derrick.wellnesscheck.CANCEL_TIMER";
    final String CHANNEL_ID = "WellnessCheck.CheckInService";
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManagerCompat;
    static CountDownTimer countDownTimer;
    long responseInterval;
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
        super.onCreate();
        createNotificationChannel();
        notificationManagerCompat = NotificationManagerCompat.from(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CheckInService started: " + intent.toString());
        this.intent = intent;
        switch (intent.getAction()){
            case ACTION_ALARM:
                responseInterval = intent.getLongExtra(EXTRA_INTERVAL2, 60 * 1000);

                long minutes = responseInterval / (60 * 1000) % 60;
                long seconds = responseInterval / 1000 % 60;
                String timeLeft = minutes > 0 ? minutes + ":" : "";
                timeLeft += String.format("%02d", seconds);

                builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.alert_light_frame)
                        .setContentTitle("Time To Check In")
                        .setContentText("Click to check in. Notifying Emergency Contact in " + timeLeft)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentIntent(PendingIntent.getService(this, 0,
                                new Intent(this, CheckInService.class)
                                        .setAction(ACTION_RESPONSE),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .setOngoing(true);

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
}
