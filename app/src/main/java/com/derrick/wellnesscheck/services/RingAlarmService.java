package com.derrick.wellnesscheck.services;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.derrick.wellnesscheck.data.Log;

import java.io.IOException;

public class RingAlarmService extends Service {
    final static String TAG = "RingAlarmService";
    MediaPlayer mp;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        ringAlarm();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if(mp != null) mp.stop();
    }

    public void ringAlarm() {
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        if (alarmUri == null) {
            Log.d("ringAlarm" , "alarmUri null. Unable to get default sound URI");
            return;
        }

        mp = new MediaPlayer();
        // This is what sets the media type as alarm
        // Thus, the sound will be influenced by alarm volume
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).build());

        try {
            mp.setDataSource(getApplicationContext(), alarmUri);
            mp.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }


        mp.start();
        // To continuously loop the alarm sound
        mp.setLooping(true);
    }
}
