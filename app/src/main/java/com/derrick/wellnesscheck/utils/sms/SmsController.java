package com.derrick.wellnesscheck.utils.sms;

import static com.derrick.wellnesscheck.utils.Utils.normalizeNumber;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Telephony;
import android.telephony.SmsManager;

import com.derrick.wellnesscheck.utils.PermissionsListener;
import com.derrick.wellnesscheck.ui.activities.PermissionsRequestingActivity;

import java.util.ArrayList;

public abstract class SmsController {
    public int unsentParts, unreceivedSMS;
    public abstract void onSmsReceived(String number, String message);
    public abstract void onSmsFailedToSend();
    public abstract void onSmsSent();
    final String TAG = "SmsController";

    public void sendSMS(Context context, SmsReceiver smsReceiver, SmsController smsController, String number, String message) {
        //Register SmsBroadcastManager for sms feedback
        SmsReceiver.smsController = smsController;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        intentFilter.addAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
        intentFilter.addAction(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION);
        context.registerReceiver(smsReceiver, intentFilter);

        //Set SmsBroadcastManager as the pending intent used by smsManager
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, SmsReceiver.class)
                        .setAction(SmsReceiver.ACTION_SEND_SMS_RESULT),
                PendingIntent.FLAG_UPDATE_CURRENT);

        //Get smsManager
        SmsManager smsManager = context.getSystemService(SmsManager.class);
        if (smsManager == null) smsManager = SmsManager.getDefault();

        //Determine weather to send single or multi-part sms
        ArrayList<String> parts = smsManager.divideMessage(message);
        int smsPartsUnsent = parts.size();
        smsController.unsentParts += smsPartsUnsent;

        if (smsPartsUnsent == 1) {
            smsManager.sendTextMessage(normalizeNumber(number), null, message, pendingIntent, null);
        } else {
            ArrayList<PendingIntent> pendingIntents = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++) pendingIntents.add(pendingIntent);

            String normalizedNumber = normalizeNumber(number);
            smsManager.sendMultipartTextMessage(normalizedNumber, null, parts, pendingIntents, null);
        }
    }

    public void sendSMS(PermissionsRequestingActivity context, SmsReceiver smsReceiver, SmsController smsController, String number, String message) {
        context.checkPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS}, new PermissionsListener() {
            @Override
            public void permissionsGranted() {
                sendSMS((Context) context, smsReceiver, smsController, number, message);
            }

            @Override
            public void permissionsDenied() {

            }

            @Override
            public void showRationale(String[] permissions) {

            }
        });
    }
}