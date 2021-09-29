package com.derrick.wellnesscheck;

import static android.content.Intent.ACTION_DEFAULT;
import static android.content.Intent.ACTION_DELETE;

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

public class MonitorReceiver extends BroadcastReceiver {
    final String CHANNEL_ID = "WellnessCheck.MonitorReceiver";
    public static final String RESPONSE_ACTION = "com.derrick.wellnesscheck.CANCEL_TIMER";
    public static final String INTERVAL1_EXTRA = "mainInterval";
    public static final String INTERVAL2_EXTRA = "responseInterval";
    public static final String BROADCAST_ALARM = "alarm.triggered";
    static CountDownTimer countDownTimer;
    long mainInterval, responseInterval;
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManagerCompat;
    AlarmManager alarmManager;
    final String TAG = "MonitorReceiver";
    public static CheckInListener checkInListener;
    public MonitorReceiver(){}
    MonitorReceiver(CheckInListener checkInListener){
        this.checkInListener = checkInListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive, intent = " + intent.toString());
        createNotificationChannel(context);

        notificationManagerCompat = NotificationManagerCompat.from(context);

        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        mainInterval = intent.getLongExtra(INTERVAL1_EXTRA, 60 * 60 * 1000);
        responseInterval = intent.getLongExtra(INTERVAL2_EXTRA, 60 * 1000);

        Log.d(TAG, "interval 1 = " + mainInterval);
        Log.d(TAG, "interval 2 = " + responseInterval);

        switch (intent.getAction()){
            case BROADCAST_ALARM:
                long minutes = responseInterval / (60 * 1000) % 60;
                long seconds = responseInterval / 1000 % 60;
                String timeLeft = minutes > 0 ? minutes + ":" : "";
                timeLeft += String.format("%02d", seconds);

                PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, 0,
                        new Intent(context, MonitorReceiver.class).setAction(BROADCAST_ALARM)
                                .putExtras(intent),
                        PendingIntent.FLAG_UPDATE_CURRENT);

                //setup next one
                long triggerMillis = mainInterval + System.currentTimeMillis();
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerMillis, alarmPendingIntent), alarmPendingIntent);
                builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.alert_light_frame)
                        .setContentTitle("Time To Check In")
                        .setContentText("Click to check in. Notifying Emergency Contact in " + timeLeft)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(PendingIntent.getBroadcast(context, 0,
                                new Intent(context, MonitorReceiver.class)
                                        .setAction(RESPONSE_ACTION),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true);

                startCheckIn();
                break;
            case RESPONSE_ACTION:
                Log.d(TAG, "response timer requested");
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    if(checkInListener != null) checkInListener.onCheckIn();
                    notificationManagerCompat.deleteNotificationChannel(CHANNEL_ID);
                }
                break;
            case ACTION_DELETE:
                notificationManagerCompat.deleteNotificationChannel(CHANNEL_ID);
                if(countDownTimer != null) countDownTimer.cancel();
                alarmManager.cancel(PendingIntent.getBroadcast(context, 0,
                        new Intent(context, MonitorReceiver.class).setAction(BROADCAST_ALARM)
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
                        notificationManagerCompat.notify(1, builder.build());
                    }

                    @Override
                    public void onFinish() {
                        //todo: Message Emergency Contact
                        builder.setContentText("Message has been sent to your emergency contacts");
                        notificationManagerCompat.notify(1, builder.build());
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
}