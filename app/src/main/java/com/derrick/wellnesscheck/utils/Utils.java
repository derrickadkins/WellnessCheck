package com.derrick.wellnesscheck.utils;

import java.util.Calendar;

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
}
