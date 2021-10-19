package com.derrick.wellnesscheck;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.room.Room;

import java.util.ArrayList;

public class DbController {
    public interface DbListener {
        void onDbReady();
    }

    public static DB db;
    public static AppSettings settings;
    public static ArrayList<Contact> contacts;

    static void InitDB(Context context){
        if(context instanceof DbListener) InitDB(context, (DbListener) context);
    }

    static void InitDB(Context context, DbListener dbListener){
        new Thread(() -> {
            db = Room.databaseBuilder(context,
                    DB.class, "database-name")
                    .fallbackToDestructiveMigration()
                    .build();

            AppSettings tmpSettings = db.settingsDao().getSettings();
            if(tmpSettings != null) settings = tmpSettings;
            else{
                settings = new AppSettings();
                db.settingsDao().insert(settings);
            }

            //use to clear db
            //db.contactDao().nukeTable();
            ArrayList<Contact> tmpContacts = new ArrayList<>(db.contactDao().getAll());
            if(tmpContacts != null) contacts = tmpContacts;
            else contacts = new ArrayList<>();

            if(dbListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        dbListener.onDbReady();
                    }
                });
            }
        }).start();
    }

    static void updateSettings(){
        new Thread(() -> db.settingsDao().update(settings)).start();
    }
}
