package com.derrick.wellnesscheck.view.fragments;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.derrick.wellnesscheck.WellnessCheck.db;
import static com.derrick.wellnesscheck.utils.Utils.getTime;
import static com.derrick.wellnesscheck.utils.Utils.sameNumbers;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.derrick.wellnesscheck.FallDetectionService;
import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.MonitorReceiver;
import com.derrick.wellnesscheck.SmsReceiver;
import com.derrick.wellnesscheck.WellnessCheck;
import com.derrick.wellnesscheck.model.data.Contact;
import com.derrick.wellnesscheck.model.data.Contacts;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Settings;
import com.derrick.wellnesscheck.utils.FragmentReadyListener;
import com.derrick.wellnesscheck.utils.PermissionsListener;
import com.derrick.wellnesscheck.view.activities.MainActivity;
import com.derrick.wellnesscheck.view.activities.SetupContactsActivity;
import com.derrick.wellnesscheck.controller.SmsController;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;
import com.derrick.wellnesscheck.view.activities.SetupSettingsActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment implements MonitorReceiver.EventListener {
    final String TAG = "HomeFragment";
    ProgressBar progressBar;
    TextView tvProgressBar, tvTimerLabel, tvNextCheckIn;
    Button btnTurnOff;
    ImageView imgSettingsIcon;
    Bundle bundle;
    long responseInterval;
    String at;
    Settings settings;
    Contacts contacts;
    FragmentReadyListener fragmentReadyListener;
    CountDownTimer timer;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach");
        if(context instanceof FragmentReadyListener)
            this.fragmentReadyListener = (FragmentReadyListener) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        settings = db.settings;
        contacts = db.contacts;

        responseInterval = settings.respondMinutes * MINUTE_IN_MILLIS;

        bundle = settings.toBundle();

        at = getString(R.string.at_);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View homeFragmentView = inflater.inflate(R.layout.home, container, false);

        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();

        imgSettingsIcon = homeFragmentView.findViewById(R.id.imgSettingsIcon);
        imgSettingsIcon.setOnClickListener(v -> startActivity(new Intent(getActivity(), SetupSettingsActivity.class)
                .putExtra("returnToMain", true)));

        btnTurnOff = homeFragmentView.findViewById(R.id.btnTurnOff);
        btnTurnOff.setOnClickListener(v -> {
            int riskLvl = 1;
            for(Contact contact : contacts.values()){
                if(contact.riskLvl > riskLvl)
                    riskLvl = contact.riskLvl;
            }
            String message = getString(R.string.are_you_sure_turn_off);
            if(riskLvl > 1) message = getString(R.string.request_permission_turn_off);

            int finalRiskLvl = riskLvl;
            new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Alert)
            .setMessage(message)
            .setPositiveButton(getString(R.string.Yes), (dialog, which) -> {
                if(finalRiskLvl == 1) {
                    stopMonitoring();
                    dialog.dismiss();
                    return;
                }
                requestTurnOff(finalRiskLvl);
            })
            .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
            .show();
        });

        tvProgressBar = homeFragmentView.findViewById(R.id.progressBarText);

        progressBar = homeFragmentView.findViewById(R.id.progressBar);

        progressBar.setOnClickListener(v -> {
            if (settings.monitoringOn) {
                if (!settings.checkedIn && settings.prevCheckIn + responseInterval > System.currentTimeMillis())
                    new MonitorReceiver().onReceive(getActivity(), new Intent(getActivity(), MonitorReceiver.class)
                            .setAction(MonitorReceiver.ACTION_RESPONSE).putExtras(bundle));
                return;
            }

            startActivity(new Intent(getActivity(), SetupContactsActivity.class));
        });
        tvTimerLabel = homeFragmentView.findViewById(R.id.tvTimerType);
        tvNextCheckIn = homeFragmentView.findViewById(R.id.tvNextCheckIn);

        setTimerVisibility();

        View.OnClickListener onDonateClicked = v -> {
            //https://www.paypal.com/pools/c/8IK7AMc2BZ
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/pools/c/8IK7AMc2BZ"));
            startActivity(browserIntent);
        };

        homeFragmentView.findViewById(R.id.donate_image_view).setOnClickListener(onDonateClicked);
        homeFragmentView.findViewById(R.id.donate_text_view).setOnClickListener(onDonateClicked);

        return homeFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        MonitorReceiver.eventListener = this;
        if(fragmentReadyListener != null) fragmentReadyListener.onFragmentReady();
        updateTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        MonitorReceiver.eventListener = null;
    }

    void updateTimer(){
        if(settings.monitoringOn) {
            long now = System.currentTimeMillis();

            if(!settings.checkedIn && settings.prevCheckIn + responseInterval > now) {
                startTimer(settings.prevCheckIn + responseInterval - now, true);
            }else {
                if(settings.nextCheckIn - now < 0)
                    settings.updateCheckIn(WellnessCheck.getNextCheckIn());
                startTimer(settings.nextCheckIn - now, false);
            }
        }
    }

    String getProgressText(long ms){
        long hours = ms / (60 * 60 * 1000) % 24;
        long minutes = ms / (60 * 1000) % 60;
        long seconds = ms / 1000 % 60;
        String progressText = hours > 0 ? hours + ":" : "";
        progressText += String.format("%02d:%02d", minutes, seconds);
        return progressText;
    }

    CountDownTimer getNewTimer(long ms){
        return new CountDownTimer(ms, 10) {
            @Override
            public void onTick(long millisTilDone) {
                tvProgressBar.setText(getProgressText(millisTilDone));
                progressBar.setProgress((int) millisTilDone);
            }

            @Override
            public void onFinish() { }
        };
    }

    void startTimer(long ms, boolean tapToCheckInTimer){
        stopTimer();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + ms);
        tvNextCheckIn.setText(at + getTime(calendar));
        tvTimerLabel.setText(tapToCheckInTimer ? R.string.progress_label_response : R.string.progress_label_check);
        int max = tapToCheckInTimer ? (int) responseInterval : (int) (settings.nextCheckIn - settings.prevCheckIn);
        progressBar.setMax(max);
        progressBar.setSecondaryProgress(max);
        timer = getNewTimer(ms).start();
    }

    void stopTimer(){
        if(timer != null) timer.cancel();
    }

    void setTimerVisibility(){
        Log.d(TAG, settings.toString());
        int visibility = settings.monitoringOn ? View.VISIBLE : View.GONE;
        btnTurnOff.setVisibility(visibility);
        tvTimerLabel.setVisibility(visibility);
        tvNextCheckIn.setVisibility(visibility);

        if(settings.monitoringOn){
            tvProgressBar.setTextSize(64);
        }else{
            tvProgressBar.setText(R.string.tap_to_setup);
            progressBar.setProgress(progressBar.getMax());
            tvProgressBar.setTextSize(32);
        }
    }

    void stopMonitoring(){
        settings.monitoringOn = false;
        stopTimer();
        settings.update();
        setTimerVisibility();
        new MonitorReceiver().onReceive(getActivity(), new Intent(getActivity(), MonitorReceiver.class).setAction(Intent.ACTION_DELETE).putExtras(bundle));
        getActivity().stopService(new Intent(getActivity(), FallDetectionService.class));
    }

    @Override
    public void onCheckIn() {
        startTimer(settings.nextCheckIn - System.currentTimeMillis(), false);
    }

    @Override
    public void onCheckInStart() {
        startTimer(settings.prevCheckIn + responseInterval - System.currentTimeMillis(), true);
    }

    @Override
    public void onMissedCheckIn() {
        startTimer(settings.nextCheckIn - System.currentTimeMillis(), false);
    }

    void requestTurnOff(int riskLvl) {
        final SmsReceiver smsReceiver = new SmsReceiver();

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Alert)
                .setMessage(getString(R.string.sending_requests))
                .setView(new ProgressBar(getActivity()))
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    dialog.cancel();
                    getActivity().unregisterReceiver(smsReceiver);
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
                for (Contact contact : contacts.values()) {
                    if (sameNumbers(number, contact.number)){
                        messageReceivedFromContact = true;
                        if(message.equals(getString(R.string.yes))) {
                            allowed = true;
                        }
                    }
                }
                if(!messageReceivedFromContact) return;
                if (allowed) {
                    alertDialog.cancel();
                    stopMonitoring();
                    getActivity().unregisterReceiver(smsReceiver);
                } else if (--unreceivedSMS == 0) {
                    alertDialog.cancel();
                    new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog_Alert)
                            .setMessage(getString(R.string.sorry_not_allowed))
                            .setNeutralButton(getString(R.string.okay), (dialog, which) -> dialog.cancel()).create().show();
                    getActivity().unregisterReceiver(smsReceiver);
                }
            }

            @Override
            public void onSmsFailedToSend() {
                alertDialog.cancel();
                new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Alert)
                        .setMessage(getString(R.string.sms_failed))
                        .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel())
                        .setPositiveButton(getString(R.string.retry), (dialog, which) -> {
                            dialog.cancel();
                            requestTurnOff(riskLvl);
                        }).create().show();
            }

            @Override
            public void onSmsSent() {
                unreceivedSMS++;
                if (--unsentParts == 0) {
                    alertDialog.setMessage(getString(R.string.messages_sent));
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            alertDialog.setMessage(getString(R.string.waiting_for_response));
                        }
                    }, 1000);
                }
            }
        };

        for (Contact contact : contacts.values()) {
            smsController.sendSMS((PermissionsRequestingActivity) getContext(), smsReceiver, smsController, contact.number, getString(R.string.turn_off_request));
        }
    }
}