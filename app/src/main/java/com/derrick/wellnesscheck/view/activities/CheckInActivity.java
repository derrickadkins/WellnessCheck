package com.derrick.wellnesscheck.view.activities;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.derrick.wellnesscheck.MonitorReceiver;
import com.derrick.wellnesscheck.R;

public class CheckInActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        findViewById(R.id.progressBar).setOnClickListener(v -> {
            new MonitorReceiver()
                    .onReceive(CheckInActivity.this, new Intent(CheckInActivity.this, MonitorReceiver.class)
                    .setAction(MonitorReceiver.ACTION_RESPONSE));
            finish();
        });
    }
}
