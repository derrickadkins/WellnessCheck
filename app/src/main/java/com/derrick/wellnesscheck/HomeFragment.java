package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.MainActivity.settings;
import static com.derrick.wellnesscheck.MainActivity.updateSettings;

import android.os.Bundle;
import android.os.CountDownTimer;
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
    TextView tvProgressBar;
    Button btnTurnOff;
    CountDownTimer timer;
    long checkInInterval, responseInterval;
    boolean inResponseTimer = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View homeFragmentView = inflater.inflate(R.layout.home, container, false);

        checkInInterval = (long) settings.checkInHours * 60 * 60 * 1000;
        responseInterval = (long) settings.respondMinutes * 60 * 1000;

        btnTurnOff = (Button) homeFragmentView.findViewById(R.id.btnTurnOff);
        btnTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settings.monitoringOn = false;
                if(timer != null) timer.cancel();
                updateSettings();
                refreshLayout();
            }
        });

        tvProgressBar = (TextView) homeFragmentView.findViewById(R.id.progressBarText);
        tvProgressBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settings.monitoringOn = true;
                inResponseTimer = false;
                settings.nextCheckIn = new Date().getTime() + checkInInterval;
                startTimer(checkInInterval);
                updateSettings();
                refreshLayout();
            }
        });

        progressBar = (CircularProgressIndicator) homeFragmentView.findViewById(R.id.progressBar);

        refreshLayout();

        long now = new Date().getTime();
        if(settings.nextCheckIn == 0) settings.nextCheckIn = now + checkInInterval;
        long millis = settings.nextCheckIn - now;
        if(millis <= 0){
            while(settings.nextCheckIn <= now)
                settings.nextCheckIn += checkInInterval;
            updateSettings();
            millis = settings.nextCheckIn;
        }
        startTimer(millis);

        return homeFragmentView;
    }

    void startTimer(long ms){
        progressBar.setMax(inResponseTimer ? (int) responseInterval : (int) checkInInterval);
        timer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long hours = millisUntilFinished / (60 * 60 * 1000) % 24;
                long minutes = millisUntilFinished / (60 * 1000) % 60;
                long seconds = millisUntilFinished / 1000 % 60;
                String progressText = hours > 0 ? hours + ":" : "";
                progressText += String.format("%02d:%02d", minutes, seconds);
                tvProgressBar.setText(progressText);
                progressBar.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
                inResponseTimer = !inResponseTimer;
                if(inResponseTimer) {
                    settings.nextCheckIn += checkInInterval;
                    updateSettings();
                    startTimer(responseInterval);
                }else{
                    startTimer(settings.nextCheckIn - new Date().getTime());
                }
            }
        }.start();
    }

    void refreshLayout(){
        int visibility = settings.monitoringOn ? View.VISIBLE : View.GONE;
        progressBar.setVisibility(visibility);
        btnTurnOff.setVisibility(visibility);

        if(!settings.monitoringOn)
            tvProgressBar.setText("Setup Monitoring");
    }
}
