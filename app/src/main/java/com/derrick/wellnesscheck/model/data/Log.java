package com.derrick.wellnesscheck.model.data;

import static com.derrick.wellnesscheck.WellnessCheck.db;

import android.os.Handler;
import android.os.Looper;

import com.derrick.wellnesscheck.WellnessCheck;
import com.derrick.wellnesscheck.model.DB;

import java.util.ArrayList;
import java.util.Collection;

public class Log extends ArrayList<Entry> {
    public static Listener listener;
    public interface Listener{
        void onLog(Entry entry);
    }

    public Log(){
        super();
    }

    public Log(Collection<Entry> logEntries){
        super(logEntries);
    }

    public Entry add(String msg) {
        Entry entry = new Entry(size(), msg);
        entry.insert();
        super.add(0, entry);
        return entry;
    }

    public static int d(String tag, String msg){
        DB.InitDB(WellnessCheck.context, db -> {
            Entry entry = db.log.add(tag + ": " + msg);
            if(Log.listener != null) new Handler(Looper.getMainLooper()).post(() -> Log.listener.onLog(entry));
        }, false, false, true);
        return android.util.Log.println(android.util.Log.DEBUG, tag, msg);
    }

    public static Log Init(){return new Log(db.logDao().getAll());}
}
