package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.MainActivity.settings;
import static com.derrick.wellnesscheck.MainActivity.updateSettings;

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

import java.util.Date;

public class HomeFragment extends Fragment {
    CircularProgressIndicator progressBar;
    TextView tvProgressBar, tvTimerLabel;
    Button btnTurnOff;
    CountDownTimer timer;
    long checkInInterval, responseInterval;
    boolean inResponseTimer = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkInInterval = (long) settings.checkInHours * 60 * 60 * 1000;
        responseInterval = (long) settings.respondMinutes * 60 * 1000;

        //for testing only
        checkInInterval = 90 * 1000;
        responseInterval = 20 * 1000;
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

        btnTurnOff = (Button) homeFragmentView.findViewById(R.id.btnTurnOff);
        btnTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settings.monitoringOn = false;
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                updateSettings();
                setTimerVisibility();
            }
        });

        tvProgressBar = (TextView) homeFragmentView.findViewById(R.id.progressBarText);
        tvProgressBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (settings.monitoringOn) return;
                settings.monitoringOn = true;
                inResponseTimer = false;
                settings.nextCheckIn = new Date().getTime() + checkInInterval;
//                Log.d("Start Timer", "called from tvProgressBar onClickListener");
                startTimer(checkInInterval);
                updateSettings();
                setTimerVisibility();
            }
        });

        progressBar = (CircularProgressIndicator) homeFragmentView.findViewById(R.id.progressBar);
        tvTimerLabel = (TextView) homeFragmentView.findViewById(R.id.tvTimerType);

        setTimerVisibility();

        long now = new Date().getTime();
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
        super.onResume();
    }

    void startTimer(long ms){
        /*Log.d("Start Timer", "inResponseTimer = " + inResponseTimer
                + ", responseInterval = " + responseInterval
                + ", checkInInterval = " + checkInInterval
                + ", ms = " + ms);*/
        tvTimerLabel.setText(inResponseTimer ? "Time to Check In" : "Next Wellness Check In");
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
                    startTimer(settings.nextCheckIn - new Date().getTime());
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
}
