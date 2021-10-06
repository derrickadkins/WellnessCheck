package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.MainActivity.settings;
import static com.derrick.wellnesscheck.MainActivity.updateSettings;
import static com.derrick.wellnesscheck.MainActivity.contacts;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment implements MonitorReceiver.CheckInListener {
    CircularProgressIndicator progressBar;
    TextView tvProgressBar, tvTimerLabel, tvNextCheckIn;
    Button btnTurnOff;
    CountDownTimer timer;
    long checkInInterval, responseInterval;
    boolean inResponseTimer = false;
    SmsBroadcastManager smsBroadcastManager;
    int unsentSMS, unreceivedSMS;
    final String TAG = "HomeFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkInInterval = (long) settings.checkInHours * 60 * 60 * 1000;
        responseInterval = (long) settings.respondMinutes * 60 * 1000;

        //for testing only
        //checkInInterval = 10 * 1000;
        //responseInterval = 5 * 1000;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View homeFragmentView = inflater.inflate(R.layout.home, container, false);

/*
        Log.d("HomeFragment", "is timer null ? " + (timer == null));

        if(progressBar != null)
            Log.d("HomeFragment", "progressMax = " + progressBar.getMax());
*/

        btnTurnOff = homeFragmentView.findViewById(R.id.btnTurnOff);
        btnTurnOff.setOnClickListener(v -> {
            int riskLvl = 1;
            for(int i = 0; i < contacts.size(); i++){
                if(contacts.get(i).riskLvl > riskLvl)
                    riskLvl = contacts.get(i).riskLvl;
            }
            String message = "Are you sure you want to turn off monitoring?";
            if(riskLvl > 1) message = "Request permission from Emergency Contacts to turn off monitoring?";

            int finalRiskLvl = riskLvl;
            new AlertDialog.Builder(getActivity(), android.R.style.Theme_DeviceDefault_Dialog)
            .setMessage(message)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(finalRiskLvl == 1) {
                        stopMonitoring();
                        return;
                    }
                    requestTurnOff(finalRiskLvl);
                }
            })
            .show();
        });

        tvProgressBar = homeFragmentView.findViewById(R.id.progressBarText);
        tvProgressBar.setOnClickListener(v -> {
            if (settings.monitoringOn) {
                if (inResponseTimer) {
                    onCheckIn();
                    getActivity().sendBroadcast(new Intent(getActivity(), MonitorReceiver.class).setAction(MonitorReceiver.ACTION_RESPONSE));
                }
                return;
            }

            startActivity(new Intent(getActivity(), SetupContactsActivity.class));
        });

        progressBar = homeFragmentView.findViewById(R.id.progressBar);
        tvTimerLabel = homeFragmentView.findViewById(R.id.tvTimerType);
        tvNextCheckIn = homeFragmentView.findViewById(R.id.tvNextCheckIn);

        setTimerVisibility();

        if(settings.monitoringOn) {
            long now = System.currentTimeMillis();
            if (settings.nextCheckIn == 0)
                settings.nextCheckIn = settings.firstCheckIn;
            long millis = settings.nextCheckIn - now;
            if (millis <= 0) {
                while (settings.nextCheckIn <= now)
                    settings.nextCheckIn += checkInInterval;
                updateSettings();
                millis = settings.nextCheckIn - now;
            }

            if(!settings.checkedIn) {
                inResponseTimer = settings.nextCheckIn - checkInInterval + responseInterval > now;
                if (inResponseTimer) {
                    millis = settings.nextCheckIn - checkInInterval + responseInterval - now;
                }
            }

            progressBar.setMax(inResponseTimer ? (int) responseInterval : (int) checkInInterval);
            tvTimerLabel.setText(inResponseTimer ? R.string.progress_label_response : R.string.progress_label_check);

            if (timer == null) {
            /*Log.d("Start Timer", "called from onCreateView"
                    + ", inResponseTimer = " + inResponseTimer
                    + ", millis = " + millis
                    + ", settings.nextCheckIn = " + settings.nextCheckIn
                    + ", now = " + now);*/
                startTimer(millis);
            }
        }

        return homeFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        MonitorReceiver.checkInListener = this;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    void startTimer(long ms){
        /*Log.d("Start Timer", "inResponseTimer = " + inResponseTimer
                + ", responseInterval = " + responseInterval
                + ", checkInInterval = " + checkInInterval
                + ", ms = " + ms);*/
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + ms);
        tvNextCheckIn.setText("at " + String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));
        tvTimerLabel.setText(inResponseTimer ? R.string.progress_label_response : R.string.progress_label_check);
        progressBar.setMax(inResponseTimer ? (int) responseInterval : (int) checkInInterval);
        timer = new CountDownTimer(ms, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                long hours = millisUntilFinished / (60 * 60 * 1000) % 24;
                long minutes = millisUntilFinished / (60 * 1000) % 60;
                long seconds = millisUntilFinished / 1000 % 60;
                String progressText = hours > 0 ? hours + ":" : "";
                progressText += String.format("%02d:%02d", minutes, seconds);
                tvProgressBar.setText(progressText);
                progressBar.setProgress((int) millisUntilFinished);
//                Log.d("Timer Tick", progressText + ", progressMax = " + progressBar.getMax());
            }

            @Override
            public void onFinish() {
                inResponseTimer = !inResponseTimer;
                if(inResponseTimer) {
                    settings.nextCheckIn += checkInInterval;
                    updateSettings();
//                    Log.d("Start Timer", "called from timer onFinish : response");
                    startTimer(responseInterval);
                }else{
//                    Log.d("Start Timer", "called from timer onFinish : checkIn");
                    startTimer(settings.nextCheckIn - System.currentTimeMillis());
                }
            }
        }.start();
    }

    void setTimerVisibility(){
        int visibility = settings.monitoringOn ? View.VISIBLE : View.GONE;
        progressBar.setVisibility(visibility);
        btnTurnOff.setVisibility(visibility);
        tvTimerLabel.setVisibility(visibility);
        tvNextCheckIn.setVisibility(visibility);

        if(!settings.monitoringOn)
            tvProgressBar.setText("Tap Here to Start Monitoring");
    }

    void stopMonitoring(){
        settings.monitoringOn = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        updateSettings();
        setTimerVisibility();
        getActivity().sendBroadcast(new Intent(getActivity(), MonitorReceiver.class).setAction(Intent.ACTION_DELETE));
    }

    @Override
    public void onCheckIn() {
        Log.d(TAG, "onCheckIn");
        if(timer != null) timer.cancel();
        inResponseTimer = false;
        startTimer(settings.nextCheckIn - System.currentTimeMillis());
    }

    void requestTurnOff(int riskLvl){
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), android.R.style.Theme_DeviceDefault_Dialog)
                .setMessage("Sending request(s) via SMS ...")
                .setView(new ProgressBar(getActivity()))
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        getActivity().unregisterReceiver(smsBroadcastManager);
                    }
                })
                .setCancelable(false)
                .create();
        alertDialog.show();

        smsBroadcastManager = new SmsBroadcastManager(new SmsBroadcastManager.SmsListener() {
            @Override
            public void onSmsReceived(String number, String message) {
                //todo: different action for high risk
                //if(riskLvl == 3)

                boolean allowed = false;
                for(Contact contact : contacts){
                    if(normalizeNumber(number).equalsIgnoreCase(normalizeNumber(contact.number)) && message.equalsIgnoreCase("yes")) {
                        allowed = true;
                    }
                }
                if(allowed) {
                    alertDialog.cancel();
                    stopMonitoring();
                    getActivity().unregisterReceiver(smsBroadcastManager);
                }else if(--unreceivedSMS == 0){
                    alertDialog.cancel();
                    new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog)
                    .setMessage("Sorry, you are not allowed to turn off monitoring at this time")
                    .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create().show();
                    getActivity().unregisterReceiver(smsBroadcastManager);
                }
            }

            @Override
            public void onSmsFailedToSend() {
                alertDialog.cancel();
                new AlertDialog.Builder(getActivity(), android.R.style.Theme_DeviceDefault)
                        .setMessage("SMS Failed to Send")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                requestTurnOff(riskLvl);
                            }
                        }).create().show();
            }

            @Override
            public void onSmsSent() {
                unreceivedSMS++;
                if(--unsentSMS == 0) {
                    alertDialog.setMessage("Messages Sent");
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            alertDialog.setMessage("Waiting for response ...");
                        }
                    }, 1000);
                }
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        intentFilter.addAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
        intentFilter.addAction(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION);
        getActivity().registerReceiver(smsBroadcastManager, intentFilter);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0,
                new Intent(getActivity(), SmsBroadcastManager.class)
                        .setAction(SmsBroadcastManager.ACTION_SEND_SMS_RESULT),
                PendingIntent.FLAG_UPDATE_CURRENT);
        SmsManager smsManager = getActivity().getSystemService(SmsManager.class);
        if(smsManager == null) smsManager = SmsManager.getDefault();

        /*ArrayList<String> parts = smsManager.divideMessage(getString(R.string.contact_request));
        ArrayList<PendingIntent> pendingIntents = new ArrayList<>();
        for(int i = 0; i < parts.size(); i++) pendingIntents.add(pendingIntent);
        int smsPartsUnsent = parts.size();*/

        unsentSMS = 0;
        for(Contact contact : contacts) {
            unsentSMS++;
            smsManager.sendTextMessage(contact.number, null, getString(R.string.turn_off_request), pendingIntent, null);
        }
    }

    String normalizeNumber(String number){
        return number.replaceAll("\\D+", "");
    }
}
