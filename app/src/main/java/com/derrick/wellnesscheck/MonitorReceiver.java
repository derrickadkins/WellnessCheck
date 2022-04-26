package com.derrick.wellnesscheck;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_DELETE;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.derrick.wellnesscheck.utils.Utils.getTime;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.derrick.wellnesscheck.model.DB;
import com.derrick.wellnesscheck.controller.SmsController;
import com.derrick.wellnesscheck.model.data.Contact;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Prefs;
import com.derrick.wellnesscheck.utils.Utils;
import com.derrick.wellnesscheck.view.activities.CheckInActivity;

public class MonitorReceiver extends BroadcastReceiver {

    final String TAG = "MonitorReceiver";

    public static final String ACTION_NOTIFY = "com.derrick.wellnesscheck.NOTIFY";
    public static final String ACTION_CHECK_IN = "com.derrick.wellnesscheck.CHECK_IN";
    public static final String ACTION_SEND_SMS = "com.derrick.wellnesscheck.SEND_SMS";

    final int NOTIFICATION_ID = 0;
    final String CHANNEL_ID = "WellnessCheck.CheckInService";
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManagerCompat;

    public static EventListener eventListener;
    public interface EventListener {
        void onCheckIn();
        void onCheckInStart();
        void onMissedCheckIn();
    }

    Intent intent;
    Context context;
    long nextCheckIn;

    public MonitorReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {
        this.intent = intent;
        this.context = context.getApplicationContext();

        Log.d(TAG, "onReceive, intent = " + intent.toString());

        createNotificationChannel();
        notificationManagerCompat = NotificationManagerCompat.from(context);

        builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.wellness_check_icon_64_transparent_background)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.wellness_check_icon_64_transparent_background))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLights(context.getColor(R.color.colorPrimary), 1000, 1000)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .setAutoCancel(true);

        Notification notification;

        switch (intent.getAction()){
            case ACTION_NOTIFY:
                nextCheckIn = WellnessCheck.getNextCheckIn();

                //next check-in
                WellnessCheck.setAlarm(context, nextCheckIn, ACTION_NOTIFY);
                //send sms
                long smsAlarmTime = System.currentTimeMillis() + Prefs.respondMinutes() * MINUTE_IN_MILLIS;
                WellnessCheck.setAlarm(context, smsAlarmTime, ACTION_SEND_SMS);

                Intent responseIntent = new Intent(context, MonitorReceiver.class).setAction(ACTION_CHECK_IN).setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                PendingIntent responsePendingIntent = PendingIntent.getBroadcast(context, 2, responseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                Intent fullScreenIntent = new Intent(context, CheckInActivity.class)
                        .addCategory("android.intent.category.LAUNCHER")
                        .setAction(ACTION_CHECK_IN)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                                Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 2, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                if(Prefs.alarm()) {
                    Log.d(TAG, "alarm:true; setting full screen intent");
                    builder.setFullScreenIntent(fullScreenPendingIntent, true);
                    builder.setCategory(NotificationCompat.CATEGORY_ALARM);
                    context.startService(new Intent(context, RingAlarmService.class));
                }

                builder.setContentTitle(context.getString(R.string.time_to_check_in))
                    .setContentIntent(responsePendingIntent)
                    .setOngoing(true)
                    .setContentText(context.getString(R.string.click_to_check_in_by) + getTime(smsAlarmTime));

                notification = builder.build();
                notification.flags |= Notification.FLAG_SHOW_LIGHTS;

                notificationManagerCompat.notify(NOTIFICATION_ID, notification);

                Prefs.checkedIn(false);
                Prefs.updateCheckIn(nextCheckIn);
                if(eventListener != null) eventListener.onCheckInStart();
                break;
            case ACTION_BOOT_COMPLETED:
                //todo: notify immediately if check-in was missed?
                Prefs.checkedIn(false);
                WellnessCheck.applyPrefs(context);
                break;
            case ACTION_CHECK_IN:
                context.stopService(new Intent(context, RingAlarmService.class));
                notificationManagerCompat.cancel(NOTIFICATION_ID);
                WellnessCheck.cancelAlarm(context, ACTION_SEND_SMS);

                Prefs.checkedIn(true);
                if(eventListener != null) eventListener.onCheckIn();
                break;
            case ACTION_SEND_SMS:
                context.stopService(new Intent(context, RingAlarmService.class));
                if(eventListener != null) eventListener.onMissedCheckIn();
                Intent lateResponseIntent = new Intent(context, MonitorReceiver.class).setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                PendingIntent lateResponsePendingIntent = PendingIntent.getBroadcast(context, 3, lateResponseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setOngoing(false)
                        .setContentIntent(lateResponsePendingIntent)
                        .setContentTitle(context.getString(R.string.you_missed_check_in))
                        .setContentText(context.getString(R.string.message_will_be_sent));
                notification = builder.build();
                notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                notificationManagerCompat.notify(NOTIFICATION_ID, notification);
                sendMissedCheckInSMS();
                break;
            case ACTION_DELETE:
                context.stopService(new Intent(context, RingAlarmService.class));
                context.stopService(new Intent(context, FallDetectionService.class));
                notificationManagerCompat.deleteNotificationChannel(CHANNEL_ID);
                WellnessCheck.cancelAlarm(context, ACTION_NOTIFY);
                WellnessCheck.cancelAlarm(context, ACTION_SEND_SMS);
                break;
        }
    }

    void sendMissedCheckInSMS(){
        SmsReceiver smsReceiver = new SmsReceiver();
        SmsController smsController = new SmsController() {
            @Override
            public void onSmsReceived(String number, String message) {}
            @Override
            public void onSmsFailedToSend() {}
            @Override
            public void onSmsSent() {}
        };

        SmsReceiver.smsController = smsController;
        DB.DbListener dbListener = (DB db) -> {
            if(Prefs.reportLocation())
                Utils.getLocation(location -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(context.getString(R.string.missed_check_in));
                    if(location != null)
                        sb.append("\n\nhttps://www.google.com/maps/search/?api=1&query=" + location.getLatitude() + "%2C" + location.getLongitude());
                    for(Contact contact : db.contacts.values())
                        smsController.sendSMS(context.getApplicationContext(), smsReceiver, smsController,
                                contact.number, sb.toString());
                });
            else {
                for(Contact contact : db.contacts.values())
                    smsController.sendSMS(context.getApplicationContext(), smsReceiver, smsController,
                            contact.number, context.getString(R.string.missed_check_in));
            }
        };

        DB.InitDB(context, dbListener,true,false);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelCompat channel = new NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.channel_name))
                .setDescription(context.getString(R.string.channel_description))
                //.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
                .setVibrationEnabled(true)
                .setLightsEnabled(true)
                .setVibrationPattern(new long[]{1000, 1000, 1000, 1000, 1000})
                .setLightColor(context.getColor(R.color.colorPrimary))
                .build();
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManagerCompat.from(context).createNotificationChannel(channel);
        }
    }
}
