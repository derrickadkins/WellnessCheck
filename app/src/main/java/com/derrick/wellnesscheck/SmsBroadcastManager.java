package com.derrick.wellnesscheck;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsBroadcastManager extends BroadcastReceiver {
    public static final String ACTION_SEND_SMS_RESULT = "send_sms_result";
    static SmsListener smsListener;
    final String TAG = "SmsReceiver";
    public SmsBroadcastManager(){super();}
    SmsBroadcastManager(SmsListener smsListener){super(); this.smsListener = smsListener;}

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");
        switch (intent.getAction()) {
            case Telephony.Sms.Intents.SMS_DELIVER_ACTION:
                Log.d(TAG, "SMS_DELIVER_ACTION");
                try{
                    Log.d(TAG, "has pdus = " + (intent.getExtras().get("pdus") != null));
                }catch (Exception ex){
                    Log.d(TAG, "has pdus = false");
                }
                break;
            case Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION:
                Log.d(TAG, "DATA_SMS_RECEIVED_ACTION");
                try{
                    Log.d(TAG, "has pdus = " + (intent.getExtras().get("pdus") != null));
                }catch (Exception ex){
                    Log.d(TAG, "has pdus = false");
                }
                break;
            case Telephony.Sms.Intents.SMS_RECEIVED_ACTION:
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
            case ACTION_SEND_SMS_RESULT:
                switch (getResultCode()){
                    case Activity.RESULT_OK:
                        Log.i(TAG, "Message sent");
                        smsListener.onSmsSent();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Log.i(TAG, "Message failed to send");
                        smsListener.onSmsFailedToSend();
                        break;
                    default:
                        Log.i(TAG, "Result Code = " + getResultCode());
                        break;
                }
                break;
            default:
                Log.i(TAG, "Action = " + intent.getAction());
                Log.i(TAG, "Result Code = " + getResultCode());
                break;
        }
    }

    public interface SmsListener{
        void onSmsReceived(String number, String message);
        void onSmsFailedToSend();
        void onSmsSent();
    }
}
