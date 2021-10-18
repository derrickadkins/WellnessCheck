package com.derrick.wellnesscheck;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class SmsController {
    int unsentParts, unreceivedSMS;
    abstract void onSmsReceived(String number, String message);
    abstract void onSmsFailedToSend();
    abstract void onSmsSent();
    final String TAG = "SmsController";
    PermissionsListener permissionsListener;

    public boolean checkPermissions(Context context){
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
        }

        if (permissions.size() > 0) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.SEND_SMS)
                    || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.RECEIVE_SMS)) {
            } else {
                if(context instanceof PermissionsRequestingActivity) {
                    PermissionsRequestingActivity activity = (PermissionsRequestingActivity) context;
                    activity.permissionsListener = permissionsListener;
                    activity.smsPermissionsResult.launch(permissions.toArray(new String[permissions.size()]));
                }
            }
            return false;
        } else return true;
    }

    public void sendSMS(Context context, final SmsBroadcastManager smsBroadcastManager, SmsController smsController, String number, String message) {
        permissionsListener = granted -> { if(granted) sendSMS(context, smsBroadcastManager, smsController, number, message); };

        if(!checkPermissions(context)) return;

        //Register SmsBroadcastManager for sms feedback
        SmsBroadcastManager.smsController = smsController;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        intentFilter.addAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
        intentFilter.addAction(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION);
        context.registerReceiver(smsBroadcastManager, intentFilter);

        //Set SmsBroadcastManager as the pending intent used by smsManager
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, SmsBroadcastManager.class)
                        .setAction(SmsBroadcastManager.ACTION_SEND_SMS_RESULT),
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

    public static String normalizeNumber(String number){
        //get rid of all non digits
        String normalizedNumber = number.replaceAll("\\D+", "");
        //get only the last 10 digits
        if(normalizedNumber.length() > 10){
            normalizedNumber = normalizedNumber.substring(normalizedNumber.length()-10);
        }
        return normalizedNumber;
    }
}
