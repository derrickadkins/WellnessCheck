package com.derrick.wellnesscheck.model.data;

import static com.derrick.wellnesscheck.WellnessCheck.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Entry {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long time;
    public String entry;

    public Entry(){}

    @Ignore
    public Entry(String entry){
        this.time = System.currentTimeMillis();
        this.entry = entry;
    }

    public synchronized void insert() {new Thread(() -> {try{db.logDao().insert(this);}catch (Exception ex){}}).start();}
}