package com.derrick.wellnesscheck;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class LogEntry {
    @NonNull
    @PrimaryKey
    public long time;
    public String entry;

    public LogEntry(String entry){
        this.time = System.currentTimeMillis();
        this.entry = entry;
    }
}
