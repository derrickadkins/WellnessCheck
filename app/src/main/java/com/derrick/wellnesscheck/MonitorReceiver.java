package com.derrick.wellnesscheck;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_DELETE;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.derrick.wellnesscheck.model.DB;
import com.derrick.wellnesscheck.controller.SmsController;
import com.derrick.wellnesscheck.model.data.Contact;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Settings;

import java.util.Calendar;

public class MonitorReceiver extends BroadcastReceiver implements DB.DbListener {

    final String TAG = "MonitorReceiver";

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
                .setSmallIcon(android.R.drawable.alert_light_frame)
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
                Log.d(TAG, "Next check-in scheduled for " + getReadableTime(nextCheckIn));

                //next check-in
                WellnessCheck.setAlarm(context, nextCheckIn, ACTION_ALARM, intent.getExtras());
                //send sms
                long smsAlarmTime = System.currentTimeMillis() + responseInterval;
                WellnessCheck.setAlarm(context, smsAlarmTime, ACTION_SMS, intent.getExtras());

                Intent responseIntent = new Intent(context, MonitorReceiver.class).setAction(ACTION_RESPONSE).setFlags(Intent.FLAG_RECEIVER_FOREGROUND).putExtras(intent);
                PendingIntent responsePendingIntent = PendingIntent.getBroadcast(context, 2, responseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentTitle("Time To Check In")
                    .setOngoing(true)
                    .setContentText("Click to check in by " + getReadableTime(smsAlarmTime, false, false, false))
                    .setContentIntent(responsePendingIntent);
                notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
                doDBStuff();
                break;
            case ACTION_BOOT_COMPLETED:
                doDBStuff();
                break;
            case ACTION_RESPONSE:
                if(checkInListener != null) checkInListener.onCheckIn();
                notificationManagerCompat.cancel(NOTIFICATION_ID);
                WellnessCheck.cancelAlarm(context, ACTION_SMS, intent.getExtras());
                doDBStuff();
                break;
            case ACTION_SMS:
                Intent lateResponseIntent = new Intent(context, MonitorReceiver.class).setAction(ACTION_LATE_RESPONSE).setFlags(Intent.FLAG_RECEIVER_FOREGROUND).putExtras(intent);
                PendingIntent lateResponsePendingIntent = PendingIntent.getBroadcast(context, 3, lateResponseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setOngoing(false)
                        .setContentIntent(lateResponsePendingIntent)
                        .setContentTitle("You've missed your check-in")
                        .setContentText("Message will been sent to your emergency contacts.");
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
                settings.prevCheckIn = settings.nextCheckIn;
                settings.nextCheckIn = nextCheckIn;
                settings.update();
                break;
            case ACTION_RESPONSE:
                settings.checkedIn = true;
                settings.update();
                break;
            case ACTION_BOOT_COMPLETED:
                if(!settings.monitoringOn) return;
                //todo: notify immediately if check-in was missed?
                nextCheckIn = WellnessCheck.getNextCheckIn();
                settings.prevCheckIn = db.settings.nextCheckIn;
                settings.nextCheckIn = nextCheckIn;
                settings.update();
                Log.d(TAG, "Next check-in scheduled for " + getReadableTime(nextCheckIn));
                //next check-in
                WellnessCheck.setAlarm(context, nextCheckIn, ACTION_ALARM, intent.getExtras());
                break;
        }
    }

    public static String getReadableTime(long time, boolean showDate, boolean showSeconds, boolean showMillis){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String readableTime = "";
        if(showDate) readableTime += (calendar.get(Calendar.MONTH)+1) + "/" + calendar.get(Calendar.DAY_OF_MONTH) + " ";
        readableTime += calendar.get(Calendar.HOUR_OF_DAY) + ":" + String.format("%02d", calendar.get(Calendar.MINUTE));
        if(showSeconds) readableTime += ":" + String.format("%02d", calendar.get(Calendar.SECOND));
        if(showMillis) readableTime += "." + String.format("%03d", calendar.get(Calendar.MILLISECOND));
        return readableTime;
    }

    public static String getReadableTime(long time){
        return getReadableTime(time, true, true, true);
    }

    void sendMissedCheckInSMS(){
        SmsBroadcastManager smsBroadcastManager = new SmsBroadcastManager();
        SmsController smsController = new SmsController() {
            @Override
            public void onSmsReceived(String number, String message) {}
            @Override
            public void onSmsFailedToSend() {}
            @Override
            public void onSmsSent() {}
        };

        SmsBroadcastManager.smsController = smsController;
        DB.DbListener dbListener = (DB db) -> {
            for(Contact contact : db.contacts.values())
                smsController.sendSMS(context.getApplicationContext(), smsBroadcastManager, smsController, contact.number, context.getString(R.string.missed_check_in));
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
