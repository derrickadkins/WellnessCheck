package com.derrick.wellnesscheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MonitorReceiver extends BroadcastReceiver {
    CheckInListener checkInListener;
    MonitorReceiver(CheckInListener checkInListener){
        this.checkInListener = checkInListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        checkInListener.onCheckIn();
    }

    public interface CheckInListener {
        void onCheckIn();
    }
}
