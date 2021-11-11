package com.derrick.wellnesscheck.utils;

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
}
