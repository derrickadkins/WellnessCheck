package com.derrick.wellnesscheck.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utils {
    public static boolean sameNumbers(String num1, String num2){
        return normalizeNumber(num1).equals(normalizeNumber(num2));
    }

    public static String normalizeNumber(String number){
        //get rid of all non digits
        String normalizedNumber = number.replaceAll("\\D+", "");
        //get only the last 10 digits
        if(normalizedNumber.length() > 10){
            normalizedNumber = normalizedNumber.substring(normalizedNumber.length()-10);
        }
        return normalizedNumber;
    }

    public static String getReadableTime(long time, boolean showDate, boolean showSeconds, boolean showMillis){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String readableTime = "";
        if(showDate) readableTime += (calendar.get(Calendar.MONTH)+1) + "/" + calendar.get(Calendar.DAY_OF_MONTH) + " ";
        readableTime += calendar.get(Calendar.HOUR_OF_DAY) + ":" + String.format("%02d", calendar.get(Calendar.MINUTE));
        if(showSeconds) readableTime += ":" + String.format("%02d", calendar.get(Calendar.SECOND));
        if(showMillis) readableTime += "." + String.format("%03d", calendar.get(Calendar.MILLISECOND));
        return readableTime;
    }

    public static String getReadableTime(long time){
        return getReadableTime(time, true, true, true);
    }

    public static String getTime(Date date){
        return new SimpleDateFormat(android.provider.Settings.System.TIME_12_24 == "12" ? "hh:mm a" : "HH:MM").format(date);
    }

    public static String getTime(Calendar calendar){
        return getTime(calendar.getTime());
    }

    public static String getTime(int hourOfDay, int minute){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        return getTime(calendar);
    }

    public static String getTime(long millis){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return getTime(calendar);
    }
}
