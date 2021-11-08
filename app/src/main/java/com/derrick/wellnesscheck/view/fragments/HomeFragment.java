package com.derrick.wellnesscheck.view.fragments;

import static com.derrick.wellnesscheck.controller.DbController.settings;
import static com.derrick.wellnesscheck.controller.DbController.updateSettings;
import static com.derrick.wellnesscheck.controller.DbController.contacts;
import static com.derrick.wellnesscheck.WellnessCheck.getNextCheckIn;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.MonitorReceiver;
import com.derrick.wellnesscheck.SmsBroadcastManager;
import com.derrick.wellnesscheck.model.data.Contact;
import com.derrick.wellnesscheck.view.activities.SetupContactsActivity;
import com.derrick.wellnesscheck.controller.SmsController;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment implements MonitorReceiver.CheckInListener {
    CircularProgressIndicator progressBar;
    TextView tvProgressBar, tvTimerLabel, tvNextCheckIn;
    Button btnTurnOff;
    CountDownTimer timer;
    Bundle bundle;
    long responseInterval;
    boolean inResponseTimer = false;
    final String TAG = "HomeFragment";
    static final long MINUTE_IN_MILLIS = 60 * 1000;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        responseInterval = settings.respondMinutes * MINUTE_IN_MILLIS;

        bundle = new Bundle();
        bundle.putInt(MonitorReceiver.EXTRA_INTERVAL1, settings.checkInHours);
        bundle.putLong(MonitorReceiver.EXTRA_INTERVAL2, responseInterval);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_HOUR, settings.fromHour);
        bundle.putInt(MonitorReceiver.EXTRA_FROM_MINUTE, settings.fromMinute);
        bundle.putInt(MonitorReceiver.EXTRA_TO_HOUR, settings.toHour);
        bundle.putInt(MonitorReceiver.EXTRA_TO_MINUTE, settings.toMinute);
        bundle.putBoolean(MonitorReceiver.EXTRA_ALL_DAY, settings.allDay);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View homeFragmentView = inflater.inflate(R.layout.home, container, false);

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
            new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Alert)
            .setMessage(message)
            .setPositiveButton("Yes", (dialog, which) -> {
                if(finalRiskLvl == 1) {
                    stopMonitoring();
                    dialog.dismiss();
                    return;
                }
                requestTurnOff(finalRiskLvl);
            })
            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
            .show();
        });

        tvProgressBar = homeFragmentView.findViewById(R.id.progressBarText);

        progressBar = homeFragmentView.findViewById(R.id.progressBar);
        progressBar.setOnClickListener(v -> {
            if (settings.monitoringOn) {
                if (inResponseTimer) {
                    onCheckIn();
                    getActivity().sendBroadcast(new Intent(getActivity(), MonitorReceiver.class).setAction(MonitorReceiver.ACTION_RESPONSE).putExtras(bundle));
                }
                return;
            }

            startActivity(new Intent(getActivity(), SetupContactsActivity.class));
        });
        tvTimerLabel = homeFragmentView.findViewById(R.id.tvTimerType);
        tvNextCheckIn = homeFragmentView.findViewById(R.id.tvNextCheckIn);

        setTimerVisibility();

        if(settings.monitoringOn) {
            long now = System.currentTimeMillis();
            long millis = settings.nextCheckIn - now;

            if(!settings.checkedIn) {
                inResponseTimer = settings.prevCheckIn + responseInterval > now;
                if (inResponseTimer) {
                    millis = settings.prevCheckIn + responseInterval - now;
                }
            }else inResponseTimer = false;

            startTimer(millis);
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
        MonitorReceiver.checkInListener = null;
    }

    void startTimer(long ms){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + ms);
        tvNextCheckIn.setText("at " + String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));
        tvTimerLabel.setText(inResponseTimer ? R.string.progress_label_response : R.string.progress_label_check);
        progressBar.setMax(inResponseTimer ? (int) responseInterval : (int) (settings.nextCheckIn - settings.prevCheckIn));
        if(timer != null) timer.cancel();
        timer = new CountDownTimer(ms, 10) {
            @Override
            public void onTick(long millisTilDone) {
                long hours = millisTilDone / (60 * 60 * 1000) % 24;
                long minutes = millisTilDone / (60 * 1000) % 60;
                long seconds = millisTilDone / 1000 % 60;
                String progressText = hours > 0 ? hours + ":" : "";
                progressText += String.format("%02d:%02d", minutes, seconds);
                tvProgressBar.setText(progressText);
                progressBar.setProgress((int) millisTilDone);
            }

            @Override
            public void onFinish() {
                inResponseTimer = !inResponseTimer;
                if(inResponseTimer) {
                    settings.nextCheckIn = getNextCheckIn();
                    updateSettings();
                    startTimer(responseInterval);
                }else{
                    startTimer(settings.nextCheckIn - System.currentTimeMillis());
                }
            }
        }.start();
    }

    void setTimerVisibility(){
        int visibility = settings.monitoringOn ? View.VISIBLE : View.GONE;
        btnTurnOff.setVisibility(visibility);
        tvTimerLabel.setVisibility(visibility);
        tvNextCheckIn.setVisibility(visibility);

        if(settings.monitoringOn){
            tvProgressBar.setTextSize(64);
        }else{
            tvProgressBar.setText("Tap to Setup\nWellness Checks");
            progressBar.setProgress(progressBar.getMax());
            tvProgressBar.setTextSize(32);
        }
    }

    void stopMonitoring(){
        settings.monitoringOn = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        updateSettings();
        setTimerVisibility();
        getActivity().sendBroadcast(new Intent(getActivity(), MonitorReceiver.class).setAction(Intent.ACTION_DELETE).putExtras(bundle));
    }

    @Override
    public void onCheckIn() {
        if(timer != null) timer.cancel();
        inResponseTimer = false;
        startTimer(settings.nextCheckIn - System.currentTimeMillis());
    }

    void requestTurnOff(int riskLvl) {
        final SmsBroadcastManager smsBroadcastManager = new SmsBroadcastManager();

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Alert)
                .setMessage("Sending request(s) via SMS ...")
                .setView(new ProgressBar(getActivity()))
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                    getActivity().unregisterReceiver(smsBroadcastManager);
                })
                .setCancelable(false)
                .create();
        alertDialog.show();

        SmsController smsController = new SmsController() {
            @Override
            public void onSmsReceived(String number, String message) {
                //todo: different action for high risk
                //if(riskLvl == 3)

                boolean allowed = false;
                boolean messageReceivedFromContact = false;
                message = message.trim().toLowerCase(Locale.ROOT);
                for (Contact contact : contacts) {
                    String normalizedContactNumber = normalizeNumber(contact.number);
                    if (number.equals(normalizedContactNumber)){
                        messageReceivedFromContact = true;
                        if(message.equals("yes")) {
                            allowed = true;
                        }
                    }
                }
                if(!messageReceivedFromContact) return;
                if (allowed) {
                    alertDialog.cancel();
                    stopMonitoring();
                    getActivity().unregisterReceiver(smsBroadcastManager);
                } else if (--unreceivedSMS == 0) {
                    alertDialog.cancel();
                    new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog_Alert)
                            .setMessage("Sorry, you are not allowed to turn off monitoring at this time")
                            .setNeutralButton("Okay", (dialog, which) -> dialog.cancel()).create().show();
                    getActivity().unregisterReceiver(smsBroadcastManager);
                }
            }

            @Override
            public void onSmsFailedToSend() {
                alertDialog.cancel();
                new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Alert)
                        .setMessage("SMS Failed to Send")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                        .setPositiveButton("Retry", (dialog, which) -> {
                            dialog.cancel();
                            requestTurnOff(riskLvl);
                        }).create().show();
            }

            @Override
            public void onSmsSent() {
                unreceivedSMS++;
                if (--unsentParts == 0) {
                    alertDialog.setMessage("Messages Sent");
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            alertDialog.setMessage("Waiting for response ...");
                        }
                    }, 1000);
                }
            }
        };

        for (Contact contact : contacts) {
            smsController.sendSMS((PermissionsRequestingActivity) getContext(), smsBroadcastManager, smsController, contact.number, getString(R.string.turn_off_request));
        }
    }
}