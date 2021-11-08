package com.derrick.wellnesscheck.utils;

import static com.derrick.wellnesscheck.controller.DbController.insertLogEntry;

import com.derrick.wellnesscheck.model.data.LogEntry;

public class Log{
    public interface Listener{
        void onLog(LogEntry entry);
    }
    public static int d(String tag, String msg){
        insertLogEntry(new LogEntry(tag + ": " + msg));
        return android.util.Log.println(android.util.Log.DEBUG, tag, msg);
    }
}
