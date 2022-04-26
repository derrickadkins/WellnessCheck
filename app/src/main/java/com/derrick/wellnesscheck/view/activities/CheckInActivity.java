package com.derrick.wellnesscheck.view.activities;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.derrick.wellnesscheck.utils.Utils.getTime;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.derrick.wellnesscheck.MonitorReceiver;
import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.model.data.Prefs;

import java.util.Calendar;

public class CheckInActivity extends AppCompatActivity implements MonitorReceiver.EventListener {
    private static final String TAG = "CheckInActivity";
    ProgressBar progressBar;
    TextView tvProgressBar, tvTimerLabel, tvNextCheckIn;
    long responseInterval;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        MonitorReceiver.eventListener = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            ((KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE)).requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        setContentView(R.layout.home);

        findViewById(R.id.btnTurnOff).setVisibility(View.GONE);
        findViewById(R.id.imgSettingsIcon).setVisibility(View.GONE);
        findViewById(R.id.donate_image_view).setVisibility(View.GONE);
        findViewById(R.id.donate_text_view).setVisibility(View.GONE);

        tvNextCheckIn = findViewById(R.id.tvNextCheckIn);
        tvProgressBar = findViewById(R.id.progressBarText);
        tvTimerLabel = findViewById(R.id.tvTimerType);

        progressBar = findViewById(R.id.progressBar);

        responseInterval = Prefs.respondMinutes() * MINUTE_IN_MILLIS;
        progressBar.setOnClickListener(v -> {
            new MonitorReceiver()
                    .onReceive(CheckInActivity.this, new Intent(CheckInActivity.this, MonitorReceiver.class)
                            .setAction(MonitorReceiver.ACTION_CHECK_IN));
        });
        startTimer(Prefs.prevCheckIn() + responseInterval - System.currentTimeMillis());
    }

    void startTimer(long ms){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + ms);
        tvNextCheckIn.setText(getString(R.string.at_) + getTime(calendar));
        tvTimerLabel.setText(R.string.progress_label_response);
        progressBar.setMax((int)responseInterval);
        progressBar.setSecondaryProgress((int)responseInterval);
        Log.d(TAG, "timer started; millis:"+ms+", progressBarMax:"+ progressBar.getMax());
        new CountDownTimer(ms, 10) {
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
                Log.d(TAG, "CountDownTimer:onFinish");
                MonitorReceiver.eventListener = null;
                finish();
            }
        }.start();
    }

    @Override
    public void onCheckIn() {
        Log.d(TAG, "onCheckIn");
        MonitorReceiver.eventListener = null;
        finish();
    }

    @Override
    public void onCheckInStart() {

    }

    @Override
    public void onMissedCheckIn() {

    }
}
