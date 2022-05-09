package com.derrick.wellnesscheck.data;

import static com.derrick.wellnesscheck.App.context;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

public class Prefs {
    static final String TAG = "Prefs";

    public static int checkInHours(){ return getInt(CHECK_IN_HOURS, 1); }
    public static void checkInHours(int i){ putInt(CHECK_IN_HOURS, i); }

    public static int respondMinutes(){ return getInt(RESPOND_MINUTES, 1); }
    public static void respondMinutes(int i){ putInt(RESPOND_MINUTES, i); }

    public static int fromHour(){ return getInt(FROM_HOUR, 8); }
    public static void fromHour(int i){ putInt(FROM_HOUR, i); }

    public static int fromMinute(){ return getInt(FROM_MINUTE, 0); }
    public static void fromMinute(int i){ putInt(FROM_MINUTE, i); }

    public static int toHour(){ return getInt(TO_HOUR, 20); }
    public static void toHour(int i){ putInt(TO_HOUR, i); }

    public static int toMinute(){ return getInt(TO_MINUTE, 0); }
    public static void toMinute(int i){ putInt(TO_MINUTE, i); }

    public static int fallSensitivity(){ return getInt(FALL_SENSITIVITY, 0); }
    public static void fallSensitivity(int i){ putInt(FALL_SENSITIVITY, i); }

    public static long nextCheckIn(){ return getLong(NEXT_CHECK_IN, 0); }
    public static void nextCheckIn(long l){ putLong(NEXT_CHECK_IN, l); }

    public static long prevCheckIn(){ return getLong(PREV_CHECK_IN, 0); }
    public static void prevCheckIn(long l){ putLong(PREV_CHECK_IN, l); }

    public static boolean onboardingComplete(){ return getBoolean(ONBOARDING_COMPLETE, false); }
    public static void onboardingComplete(boolean b){ putBoolean(ONBOARDING_COMPLETE, b); }

    public static boolean confirmAddContact(){ return getBoolean(CONFIRM_ADD_CONTACT, true); }
    public static void confirmAddContact(boolean b){ putBoolean(CONFIRM_ADD_CONTACT, b); }

    public static boolean allDay(){ return getBoolean(ALL_DAY, true); }
    public static void allDay(boolean b){ putBoolean(ALL_DAY, b); }

    public static boolean monitoringOn(){ return getBoolean(MONITORING_ON, false); }
    public static void monitoringOn(boolean b){ putBoolean(MONITORING_ON, b); }

    public static boolean checkedIn(){ return getBoolean(CHECKED_IN, false); }
    public static void checkedIn(boolean b){ putBoolean(CHECKED_IN, b); }

    public static boolean reportLocation(){ return getBoolean(REPORT_LOCATION, false); }
    public static void reportLocation(boolean b){ putBoolean(REPORT_LOCATION, b); }

    public static boolean fallDetection(){ return getBoolean(FALL_DETECTION, false); }
    public static void fallDetection(boolean b){ putBoolean(FALL_DETECTION, b); }

    public static boolean alarm(){ return getBoolean(ALARM, false); }
    public static void alarm(boolean b){ putBoolean(ALARM, b); }

    public static boolean walkthroughComplete(){ return getBoolean(WALKTHROUGH_COMPLETE, false); }
    public static void walkthroughComplete(boolean b){ putBoolean(WALKTHROUGH_COMPLETE, b); }

    public static boolean confirmAskRiskLvl() { return getBoolean(CONFIRM_ASK_RISK_LVL, true);}
    public static void confirmAskRiskLvl(boolean b) { putBoolean(CONFIRM_ASK_RISK_LVL, b);}

    private static final String CHECK_IN_HOURS = "checkInHours",
    RESPOND_MINUTES = "respondMinutes",
    FROM_HOUR = "fromHour",
    FROM_MINUTE = "fromMinute",
    TO_HOUR = "toHour",
    TO_MINUTE = "toMinute",
    FALL_SENSITIVITY = "fallSensitivity",
    NEXT_CHECK_IN = "nextCheckIn",
    PREV_CHECK_IN = "prevCheckIn",
    ONBOARDING_COMPLETE = "onboardingComplete",
    CONFIRM_ADD_CONTACT = "confirmAddContact",
    CONFIRM_ASK_RISK_LVL = "confirmAskRiskLvl",
    ALL_DAY = "allDay",
    MONITORING_ON = "monitoringOn",
    CHECKED_IN = "checkedIn",
    REPORT_LOCATION = "reportLocation",
    FALL_DETECTION = "fallDetection",
    ALARM = "alarm",
    WALKTHROUGH_COMPLETE = "walkthroughComplete";

    private static int getInt(String key, int def){ return prefs().getInt(key, def); }

    private static long getLong(String key, long def){ return prefs().getLong(key, def); }

    private static boolean getBoolean(String key, boolean def){ return prefs().getBoolean(key, def); }

    private static void putInt(String key, int i){ prefs().edit().putInt(key, i).apply(); }

    private static void putLong(String key, long l){ prefs().edit().putLong(key, l).apply(); }

    private static void putBoolean(String key, boolean b){ prefs().edit().putBoolean(key, b).apply(); }

    private static SharedPreferences prefs(){
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static void updateCheckIn(long nextCheckIn){
        if(nextCheckIn() == nextCheckIn) return;
        prevCheckIn(nextCheckIn());
        nextCheckIn(nextCheckIn);
        Log.d(TAG, "updated check-in");
    }

    @NonNull
    @Override
    public String toString() {
        return prefs().getAll().toString();
    }
}
