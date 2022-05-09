package com.derrick.wellnesscheck.data;

import static com.derrick.wellnesscheck.App.db;

import android.os.Handler;
import android.os.Looper;

import com.derrick.wellnesscheck.App;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class Log extends ArrayList<Entry> {
    public static Listener listener;
    public Queue<Entry> logQ;
    public interface Listener{
        void onLog(Entry entry);
    }

    public Log(){
        super();
        logQ = new LinkedList<>();
    }

    public Log(Collection<Entry> logEntries){
        super(logEntries);
        logQ = new LinkedList<>();
    }

    public Entry add(String msg) {
        Entry entry = new Entry(msg);
        logQ.add(entry);
        processQueue();
        return entry;
    }

    public static int d(String tag, String msg){
        int result = android.util.Log.println(android.util.Log.DEBUG, tag, msg);
        DB.InitDB(App.context, db -> {
            Entry entry = db.log.add(tag + ": " + msg);
            if(Log.listener != null) new Handler(Looper.getMainLooper()).post(() -> Log.listener.onLog(entry));
        }, false, true);
        return result;
    }

    public static Log Init(){return new Log(db.logDao().getAll());}

    synchronized void processQueue(){
        while(!logQ.isEmpty()){
            Entry entry = logQ.poll();
            entry.insert();
            super.add(0, entry);
        }
    }
}