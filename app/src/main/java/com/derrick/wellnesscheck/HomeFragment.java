package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.MainActivity.settings;
import static com.derrick.wellnesscheck.MainActivity.updateSettings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;

public class HomeFragment extends Fragment implements MonitorReceiver.CheckInListener {
    CircularProgressIndicator progressBar;
    TextView tvProgressBar, tvTimerLabel;
    Button btnTurnOff;
    CountDownTimer timer;
    long checkInInterval, responseInterval;
    boolean inResponseTimer = false;
    /*
    JobScheduler jobScheduler;
    AlarmManager alarmManager;
    PendingIntent checkInIntent;
     */
    MonitorReceiver monitorReceiver;
    final String TAG = "HomeFragment";
    Intent serviceIntent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkInInterval = (long) settings.checkInHours * 60 * 60 * 1000;
        responseInterval = (long) settings.respondMinutes * 60 * 1000;

        //for testing only
        checkInInterval = 10 * 1000;
        responseInterval = 5 * 1000;

        serviceIntent = new Intent(getActivity(), MonitoringService.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View homeFragmentView = inflater.inflate(R.layout.home, container, false);

        /*
        Intent intent = new Intent(getActivity(), MonitoringService.class)
                .setAction(Intent.ACTION_DEFAULT)
                .putExtra(MonitoringService.INTERVAL1_EXTRA, checkInInterval)
                .putExtra(MonitoringService.INTERVAL2_EXTRA, responseInterval);
        checkInIntent = PendingIntent.getService(getActivity(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
         */
/*
        Log.d("HomeFragment", "is timer null ? " + (timer == null));

        if(progressBar != null)
            Log.d("HomeFragment", "progressMax = " + progressBar.getMax());
*/
        homeFragmentView.findViewById(R.id.btnStartService).setOnClickListener(v -> startService());

        homeFragmentView.findViewById(R.id.btnStopService).setOnClickListener(v -> stopService());

        btnTurnOff = homeFragmentView.findViewById(R.id.btnTurnOff);
        btnTurnOff.setOnClickListener(v -> {
            settings.monitoringOn = false;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            updateSettings();
            setTimerVisibility();
            stopService();
            //if(jobScheduler != null) jobScheduler.cancel(1);
            //if(alarmManager != null) alarmManager.cancel(checkInIntent);
        });

        tvProgressBar = homeFragmentView.findViewById(R.id.progressBarText);
        tvProgressBar.setOnClickListener(v -> {
            if (settings.monitoringOn) {
                if (inResponseTimer) {
                    onCheckIn();
                    getActivity().startService(serviceIntent.setAction(MonitoringService.RESPONSE_ACTION));
                }
                return;
            }

            //Start Monitoring
            settings.monitoringOn = true;
            inResponseTimer = false;
            settings.nextCheckIn = System.currentTimeMillis() + checkInInterval;
//                Log.d("Start Timer", "called from tvProgressBar onClickListener");
            startTimer(checkInInterval);
            updateSettings();
            setTimerVisibility();
            startService();
        });

        progressBar = homeFragmentView.findViewById(R.id.progressBar);
        tvTimerLabel = homeFragmentView.findViewById(R.id.tvTimerType);

        setTimerVisibility();

        long now = System.currentTimeMillis();
        long millis = settings.nextCheckIn - now;
        if (settings.nextCheckIn == 0) settings.nextCheckIn = now + checkInInterval;
        if (millis <= 0) {
            while (settings.nextCheckIn <= now)
                settings.nextCheckIn += checkInInterval;
            updateSettings();
            millis = settings.nextCheckIn - now;
        }

        inResponseTimer = settings.nextCheckIn - checkInInterval + responseInterval > now;
        if (inResponseTimer) {
            millis = settings.nextCheckIn - checkInInterval + responseInterval - now;
        }

        progressBar.setMax(inResponseTimer ? (int) responseInterval : (int) checkInInterval);
        tvTimerLabel.setText(inResponseTimer ? R.string.progress_label_response : R.string.progress_label_check);

        if(settings.monitoringOn && timer == null) {
            /*Log.d("Start Timer", "called from onCreateView"
                    + ", inResponseTimer = " + inResponseTimer
                    + ", millis = " + millis
                    + ", settings.nextCheckIn = " + settings.nextCheckIn
                    + ", now = " + now);*/
            startTimer(millis);
        }

        return homeFragmentView;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        if(monitorReceiver == null) monitorReceiver = new MonitorReceiver(this);
        getActivity().registerReceiver(monitorReceiver, new IntentFilter(MonitoringService.BROADCAST_CHECK_IN));
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        if(monitorReceiver != null) getActivity().unregisterReceiver(monitorReceiver);
    }

    void startTimer(long ms){
        /*Log.d("Start Timer", "inResponseTimer = " + inResponseTimer
                + ", responseInterval = " + responseInterval
                + ", checkInInterval = " + checkInInterval
                + ", ms = " + ms);*/
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

        if(!settings.monitoringOn)
            tvProgressBar.setText("Setup Monitoring");
    }

    void startService(){
        /*
        long triggerMillis = checkInInterval + System.currentTimeMillis();
        alarmManager = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, checkInIntent);
        */
        getActivity().startService(serviceIntent
                .setAction(MonitoringService.ACTION_FIRST)
                .putExtra(MonitoringService.INTERVAL1_EXTRA, checkInInterval)
                .putExtra(MonitoringService.INTERVAL2_EXTRA, responseInterval));
    }

    void stopService(){
        getActivity().startService(serviceIntent.setAction(Intent.ACTION_DELETE));
        //if(alarmManager != null) alarmManager.cancel(checkInIntent);
    }

    @Override
    public void onCheckIn() {
        Log.d(TAG, "onCheckIn");
        if(timer != null) timer.cancel();
        inResponseTimer = false;
        startTimer(settings.nextCheckIn - System.currentTimeMillis());
    }
}
