package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.MainActivity.settings;
import static com.derrick.wellnesscheck.MainActivity.updateSettings;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MonitoringService extends Service {

    final String CHANNEL_ID = "MonitoringService";
    final String RESPONSE_ACTION = "com.derrick.wellnesscheck.CANCEL_TIMER";
    public static final String START_ACTION = "com.derrick.wellnesscheck.START_TIMER";
    public static final String INTERVAL1_EXTRA = "mainInterval";
    public static final String INTERVAL2_EXTRA = "responseInterval";
    PendingIntent pendingIntent;
    CountDownTimer countDownTimer;
    long mainInterval, responseInterval, nextCheckIn;
    boolean inResponseTimer;
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManagerCompat;
    final Timer timer = new Timer();
    final String TAG = "MonitorService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        createNotificationChannel();
        Intent intent = new Intent(this, MonitoringService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(RESPONSE_ACTION);
        pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        builder = new NotificationCompat.Builder(MonitoringService.this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.alert_light_frame)
                .setContentTitle("Time To Check In")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);

        notificationManagerCompat = NotificationManagerCompat.from(MonitoringService.this);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "service onStartCommand called, action = " + intent.getAction());
        if(intent.getAction().equalsIgnoreCase(START_ACTION)){
            mainInterval = intent.getLongExtra(INTERVAL1_EXTRA, 60 * 60 * 1000);
            responseInterval = intent.getLongExtra(INTERVAL2_EXTRA, 60 * 1000);
            nextCheckIn = new Date().getTime() + mainInterval;
            startTimer(mainInterval);
        }else if(intent.getAction().equalsIgnoreCase(RESPONSE_ACTION)){
            Log.d(TAG, "response timer canceled");
            countDownTimer.cancel();
        }else {
            long now = new Date().getTime();
            long millis = nextCheckIn - now;
            if (nextCheckIn == 0) nextCheckIn = now + mainInterval;
            if (millis <= 0) {
                while (nextCheckIn <= now)
                    nextCheckIn += mainInterval;
                updateSettings();
                millis = nextCheckIn - now;
            }

            inResponseTimer = nextCheckIn - mainInterval + responseInterval > now;
            if (inResponseTimer) {
                millis = nextCheckIn - mainInterval + responseInterval - now;
            }

            startTimer(millis);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "service destroyed");
        timer.cancel();
        if(countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }

    void startTimer(long millis){
        Log.d(TAG, "timer started");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                startCheckIn();
                startTimer(millis);
            }
        }, millis);
    }

    void startCheckIn() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "check in started");

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
