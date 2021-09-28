package com.derrick.wellnesscheck;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MonitoringService extends Service {

    final String CHANNEL_ID = "MonitoringService";
    public static final String ACTION_FIRST = "com.derrick.wellnesscheck.START_TIMER";
    public static final String RESPONSE_ACTION = "com.derrick.wellnesscheck.CANCEL_TIMER";
    public static final String INTERVAL1_EXTRA = "mainInterval";
    public static final String INTERVAL2_EXTRA = "responseInterval";
    public static final String BROADCAST_CHECK_IN = "check in";
    CountDownTimer countDownTimer;
    long mainInterval, responseInterval;
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManagerCompat;
    final String TAG = "MonitorService";
    AlarmManager alarmManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "service onCreate called");
        createNotificationChannel();

        notificationManagerCompat = NotificationManagerCompat.from(MonitoringService.this);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        Log.d(TAG, "service onStartCommand called, action = " + action);

        mainInterval = intent.getLongExtra(INTERVAL1_EXTRA, 60 * 60 * 1000);
        responseInterval = intent.getLongExtra(INTERVAL2_EXTRA, 60 * 1000);

        Log.d("MonitorService", "interval 1 = " + mainInterval);
        Log.d("MonitorService", "interval 2 = " + responseInterval);

        PendingIntent alarmPendingIntent = PendingIntent.getService(this, 0, intent.setAction(Intent.ACTION_DEFAULT), PendingIntent.FLAG_UPDATE_CURRENT);

        if(action.equalsIgnoreCase(RESPONSE_ACTION)) {
            if (countDownTimer != null) {
                Log.d(TAG, "response timer canceled");
                countDownTimer.cancel();
                sendBroadcast(new Intent(BROADCAST_CHECK_IN));
                notificationManagerCompat.deleteNotificationChannel(CHANNEL_ID);
                stopSelf();
            }
        }else if(action.equalsIgnoreCase(Intent.ACTION_DELETE)){
            notificationManagerCompat.deleteNotificationChannel(CHANNEL_ID);
            if(countDownTimer != null) countDownTimer.cancel();
            alarmManager.cancel(alarmPendingIntent);
            stopSelf();
        }else {


            //setup next one
            long triggerMillis = mainInterval + System.currentTimeMillis();
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, alarmPendingIntent);

            if(!action.equalsIgnoreCase(ACTION_FIRST)) {
                builder = new NotificationCompat.Builder(MonitoringService.this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.alert_light_frame)
                        .setContentTitle("Time To Check In")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(PendingIntent.getService(this, 0, intent.setAction(RESPONSE_ACTION), PendingIntent.FLAG_UPDATE_CURRENT))
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true);

                startCheckIn();
            }else{
                stopSelf();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "service destroyed");
        if(countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
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

                        stopSelf();
                    }
                }.start();
            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
