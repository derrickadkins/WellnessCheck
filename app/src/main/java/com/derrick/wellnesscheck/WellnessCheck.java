package com.derrick.wellnesscheck;

import android.app.Application;
import android.content.Context;

public class WellnessCheck extends Application {
    public static Context context;
    private final String TAG = "WellnessCheck";

    @Override
    public void onCreate() {
        super.onCreate();
        WellnessCheck.context = getApplicationContext();
        Log.d(TAG, "onCreate");
    }
}
