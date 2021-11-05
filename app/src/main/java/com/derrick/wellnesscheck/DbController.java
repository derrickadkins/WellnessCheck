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

    static void InitDbSync(Context context, DbListener dbListener, boolean settings, boolean contacts, boolean log){
        if(db == null)
            db = Room.databaseBuilder(context,
                    DB.class, "database-name")
                    .fallbackToDestructiveMigration()
                    .build();

        if(settings) initSettings();
        if(contacts) initContacts();
        if(log) initLog();

        if(dbListener != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    dbListener.onDbReady();
                }
            });
        }
    }

    static void InitDB(Context context){
        DbListener dbListener = null;
        if(context instanceof DbListener)
            dbListener = (DbListener) context;
        InitDB(context, dbListener);
    }

    static void InitDB(Context context, DbListener dbListener){
        new Thread(() -> InitDbSync(context, dbListener, true, true, true)).start();
    }

    static void InitDB(Context context, boolean settings, boolean contacts, boolean log){
        DbListener dbListener = null;
        if(context instanceof DbListener)
            dbListener = (DbListener) context;
        InitDB(context, dbListener, settings, contacts, log);
    }

    static void InitDB(Context context, DbListener dbListener, boolean settings, boolean contacts, boolean log){
        new Thread(() -> InitDbSync(context, dbListener, settings, contacts, log)).start();
    }

    static void initSettings(){
        AppSettings tmpSettings = db.settingsDao().getSettings();
        if(tmpSettings != null) settings = tmpSettings;
        else{
            settings = new AppSettings();
            db.settingsDao().insert(settings);
        }
    }

    static void initContacts(){
        //use to clear db
        //db.contactDao().nukeTable();
        List<Contact> tmpContacts = db.contactDao().getAll();
        if(tmpContacts != null) contacts = tmpContacts;
        else contacts = new ArrayList<>();
    }

    static void initLog(){
        List<LogEntry> tmpLog = db.logDao().getAll();
        if(tmpLog != null) log = tmpLog;
        else log = new ArrayList<>();
    }

    static void updateSettings(){
        new Thread(() -> db.settingsDao().update(settings)).start();
    }

    static Log.Listener logListener;
    static void insertLogEntry(LogEntry entry) {
        new Thread(() -> {
            if (db == null && WellnessCheck.context != null)
                InitDbSync(WellnessCheck.context, null, false, false, true);
            if (db == null) return;
            db.logDao().insert(entry);
            log.add(0, entry);
            if (logListener != null)
                new Handler(Looper.getMainLooper()).post(() -> logListener.onLog(entry));
        }).start();
    }
}
