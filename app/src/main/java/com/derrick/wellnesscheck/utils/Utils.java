package com.derrick.wellnesscheck.utils;

import static com.derrick.wellnesscheck.WellnessCheck.context;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {

    public static boolean sameNumbers(String num1, String num2) {
        return normalizeNumber(num1).equals(normalizeNumber(num2));
    }

    public static String normalizeNumber(String number) {
        //get rid of all non digits
        String normalizedNumber = number.replaceAll("\\D+", "");
        //get only the last 10 digits
        if (normalizedNumber.length() > 10) {
            normalizedNumber = normalizedNumber.substring(normalizedNumber.length() - 10);
        }
        return normalizedNumber;
    }

    public static String getReadableTime(long time, boolean showDate, boolean showSeconds, boolean showMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String readableTime = "";
        if (showDate)
            readableTime += (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.DAY_OF_MONTH) + " ";
        readableTime += calendar.get(Calendar.HOUR_OF_DAY) + ":" + String.format("%02d", calendar.get(Calendar.MINUTE));
        if (showSeconds) readableTime += ":" + String.format("%02d", calendar.get(Calendar.SECOND));
        if (showMillis)
            readableTime += "." + String.format("%03d", calendar.get(Calendar.MILLISECOND));
        return readableTime;
    }

    public static String getReadableTime(long time) {
        return getReadableTime(time, true, true, true);
    }

    public static String getTime(Date date) {
        return new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(context) ? "HH:mm" : "h:mm a").format(date);
    }

    public static String getTime(Calendar calendar) {
        return getTime(calendar.getTime());
    }

    public static String getTime(int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return getTime(calendar);
    }

    public static String getTime(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return getTime(calendar);
    }

    public interface LocationCallback {
        void onLocationReceived(Location location);
    }

    public static void getLocation(LocationCallback locationCallback) {
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationCallback.onLocationReceived(null);
            return;
        }
        fusedLocationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(currentLocation -> {
            if (currentLocation != null) {
                locationCallback.onLocationReceived(currentLocation);
            } else {
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(lastLocation -> {
                    locationCallback.onLocationReceived(lastLocation);
                });
            }
        });
    }

    /**
     * Get ISO 3166-1 alpha-2 country code for this device (or null if not available)
     * @param context Context reference to get the TelephonyManager instance from
     * @return country code or null
     */
    public static String getUserCountry(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String simCountry = tm.getSimCountryIso();
            if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
                return simCountry.toLowerCase(Locale.US);
            } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                    return networkCountry.toLowerCase(Locale.US);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static String getCountryCode() {
        return context.getResources().getConfiguration().locale.getCountry();
    }

    public static String getCountryName() {
        return context.getResources().getConfiguration().locale.getDisplayCountry();
    }

    public static final String[] potentialEcclistProperties = {"ro.ril.ecclist", "ril.ecclist", "ril.ecclist0", "ril.ecclist00", "ril.ecclist_net0", "ril.ecclist1"};

    public static String[] getEmergencyNumbers(){
        Map<String, String> eccLists = new HashMap<>();
        for (String key : potentialEcclistProperties){
            String numbers = SystemPropertiesProxy.get(context, key);
            if(!TextUtils.isEmpty(numbers)) eccLists.put(key, numbers);
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            SubscriptionManager sb = context.getSystemService(SubscriptionManager.class);
            List<SubscriptionInfo> subInfos = sb.getActiveSubscriptionInfoList();
            for (SubscriptionInfo subInfo : subInfos) {
                String key = "ril.ecclist" + subInfo.getSimSlotIndex();
                String numbers = SystemPropertiesProxy.get(context, key);
                if (!TextUtils.isEmpty(numbers)) eccLists.put(key, numbers);
            }
        }

        ArrayList<String> ecclist = new ArrayList<>();
        for(String values : eccLists.values()) {
            String[] valArr = values.split(",");
            for(String value : valArr) if (!ecclist.contains(value)) ecclist.add(value);
        }
        return ecclist.toArray(new String[ecclist.size()]);
    }

    private static final Map<String, String> EMERGENCY_NUMBERS_FOR_COUNTRIES =
            new HashMap<String, String>() {{
                put("au", "000");
                put("ca", "911");
                put("de", "112");
                put("gb", "999");
                put("in", "112");
                put("jp", "110");
                put("sg", "999");
                put("tw", "110");
                put("us", "911");
            }};
    public static String getEmergencyNumber() {
        String cc = getCountryCode().toLowerCase();
        if(EMERGENCY_NUMBERS_FOR_COUNTRIES.containsKey(cc))
            return EMERGENCY_NUMBERS_FOR_COUNTRIES.get(cc);
        else return "911";
    }
}
