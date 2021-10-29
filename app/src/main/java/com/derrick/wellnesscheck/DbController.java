package com.derrick.wellnesscheck;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

public class DbController {
    public interface DbListener {
        void onDbReady();
    }

    public static DB db;
    public static AppSettings settings;
    public static List<Contact> contacts;
    public static List<LogEntry> log;

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
            List<Contact> tmpContacts = db.contactDao().getAll();
            if(tmpContacts != null) contacts = tmpContacts;
            else contacts = new ArrayList<>();

            List<LogEntry> tmpLog = db.logDao().getAll();
            if(tmpLog != null) log = tmpLog;
            else log = new ArrayList<>();

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

    static Log.Listener logListener;
    static void insertLogEntry(LogEntry entry){ new Thread(() -> {
        db.logDao().insert(entry);
        log.add(entry);
        if(logListener != null)
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    logListener.onLog(entry);
                }
            });
    }).start(); }
}
