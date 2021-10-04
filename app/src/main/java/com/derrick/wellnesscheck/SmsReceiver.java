package com.derrick.wellnesscheck;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    SmsListener smsListener;
    final String TAG = "SmsReceiver";
    SmsReceiver(){super();}
    SmsReceiver(SmsListener smsListener){super(); this.smsListener = smsListener;}

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");
        switch (intent.getAction()) {
            case "android.provider.Telephony.SMS_RECEIVED":
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    for (int i = 0; i < pdus.length; i++) {
                        String format = bundle.getString("format");
                        SmsMessage currentSMS = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                        String senderNo = currentSMS.getDisplayOriginatingAddress();
                        String message = currentSMS.getDisplayMessageBody();
                        smsListener.onSmsReceived(senderNo, message);
                    }
                }
                break;
            default:

                break;
        }
    }

    public interface SmsListener{
        void onSmsReceived(String number, String message);
    }
}
