package com.derrick.wellnesscheck.utils;

import static com.derrick.wellnesscheck.App.context;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;

import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

    public static Bitmap getRoundedCroppedBitmap(Bitmap bitmap){
        int widthLight = bitmap.getWidth();
        int heightLight = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paintColor = new Paint();
        paintColor.setFlags(Paint.ANTI_ALIAS_FLAG);

        RectF rectF = new RectF(new Rect(0, 0, widthLight, heightLight));

        canvas.drawRoundRect(rectF, widthLight / 2, heightLight / 2, paintColor);

        Paint paintImage = new Paint();
        paintImage.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(bitmap, 0, 0, paintImage);

        return output;
    }
}
