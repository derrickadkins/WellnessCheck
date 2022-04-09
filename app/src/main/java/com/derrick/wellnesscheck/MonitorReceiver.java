package com.derrick.wellnesscheck;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_DELETE;
import static com.derrick.wellnesscheck.utils.Utils.getTime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.derrick.wellnesscheck.model.DB;
import com.derrick.wellnesscheck.controller.SmsController;
import com.derrick.wellnesscheck.model.data.Contact;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Settings;
import com.derrick.wellnesscheck.utils.Utils;

public class MonitorReceiver extends BroadcastReceiver implements DB.DbListener {

    final String TAG = "MonitorReceiver";

    //todo: make enum?
    public static final String ACTION_ALARM = "com.derrick.wellnesscheck.ALARM_TRIGGERED";
    public static final String ACTION_RESPONSE = "com.derrick.wellnesscheck.CANCEL_TIMER";
    public static final String ACTION_SMS = "com.derrick.wellnesscheck.SEND_SMS";
    public static final String ACTION_LATE_RESPONSE = "com.derrick.wellnesscheck.LATE_CHECK_IN";
    public static final String EXTRA_INTERVAL1 = "checkInHours";
    public static final String EXTRA_INTERVAL2 = "responseInterval";
    public static final String EXTRA_ALL_DAY = "allDay";
    public static final String EXTRA_FROM_HOUR = "fromHour";
    public static final String EXTRA_FROM_MINUTE = "fromMinute";
    public static final String EXTRA_TO_HOUR = "toHour";
    public static final String EXTRA_TO_MINUTE = "toMinute";

    final int NOTIFICATION_ID = 0;
    final String CHANNEL_ID = "WellnessCheck.CheckInService";
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManagerCompat;

    public static CheckInListener checkInListener;
    public interface CheckInListener {
        void onCheckIn();
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
                .setAutoCancel(true);

        switch (intent.getAction()){
            case ACTION_ALARM:
                int checkInHours = intent.getIntExtra(EXTRA_INTERVAL1, 1);
                boolean allDay = intent.getBooleanExtra(EXTRA_ALL_DAY, false);
                int fromHour = intent.getIntExtra(EXTRA_FROM_HOUR, 8);
                int fromMinute = intent.getIntExtra(EXTRA_FROM_MINUTE, 0);
                int toHour = intent.getIntExtra(EXTRA_TO_HOUR, 20);
                int toMinute = intent.getIntExtra(EXTRA_TO_MINUTE, 0);
                long responseInterval = intent.getLongExtra(EXTRA_INTERVAL2, 60 * 1000);

                nextCheckIn = WellnessCheck.getNextCheckIn(checkInHours, fromHour, fromMinute, toHour, toMinute, allDay);

                //next check-in
                WellnessCheck.setAlarm(context, nextCheckIn, ACTION_ALARM, intent.getExtras());
                //send sms
                long smsAlarmTime = System.currentTimeMillis() + responseInterval;
                WellnessCheck.setAlarm(context, smsAlarmTime, ACTION_SMS, intent.getExtras());

                Intent responseIntent = new Intent(context, MonitorReceiver.class).setAction(ACTION_RESPONSE).setFlags(Intent.FLAG_RECEIVER_FOREGROUND).putExtras(intent);
                PendingIntent responsePendingIntent = PendingIntent.getBroadcast(context, 2, responseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentTitle(context.getString(R.string.time_to_check_in))
                    .setOngoing(true)
                    .setContentText(context.getString(R.string.click_to_check_in_by) + getTime(smsAlarmTime))
                    .setContentIntent(responsePendingIntent);
                notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
                doDBStuff();
                break;
            case ACTION_BOOT_COMPLETED:
                doDBStuff();
                break;
            case ACTION_RESPONSE:
                notificationManagerCompat.cancel(NOTIFICATION_ID);
                WellnessCheck.cancelAlarm(context, ACTION_SMS, intent.getExtras());
                doDBStuff();
                break;
            case ACTION_SMS:
                Intent lateResponseIntent = new Intent(context, MonitorReceiver.class).setAction(ACTION_LATE_RESPONSE).setFlags(Intent.FLAG_RECEIVER_FOREGROUND).putExtras(intent);
                PendingIntent lateResponsePendingIntent = PendingIntent.getBroadcast(context, 3, lateResponseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setOngoing(false)
                        .setContentIntent(lateResponsePendingIntent)
                        .setContentTitle(context.getString(R.string.you_missed_check_in))
                        .setContentText(context.getString(R.string.message_will_be_sent));
                notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
                sendMissedCheckInSMS();
                break;
            case ACTION_DELETE:
                notificationManagerCompat.deleteNotificationChannel(CHANNEL_ID);
                WellnessCheck.cancelAlarm(context, ACTION_ALARM, intent.getExtras());
                WellnessCheck.cancelAlarm(context, ACTION_SMS, intent.getExtras());
                break;
            case ACTION_LATE_RESPONSE:
                //do nothing
                break;
        }
    }

    public void doDBStuff(){
        DB.InitDB(context, this, true, false, false);
    }

    @Override
    public void onDbReady(DB db) {
        Settings settings = db.settings;
        switch (intent.getAction()){
            case ACTION_ALARM:
                settings.checkedIn = false;
                settings.updateCheckIn(nextCheckIn);
                break;
            case ACTION_RESPONSE:
                settings.checkedIn = true;
                settings.update();
                if(checkInListener != null) checkInListener.onCheckIn();
                break;
            case ACTION_BOOT_COMPLETED:
                //todo: notify immediately if check-in was missed?
                settings.checkedIn = false;
                settings.update();
                WellnessCheck.applySettings(context, settings);
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
            if(db.settings.reportLocation)
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

        DB.InitDB(context, dbListener, false, true, false);
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
}
