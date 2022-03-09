package com.derrick.wellnesscheck.model.data;

import static com.derrick.wellnesscheck.WellnessCheck.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Entry {
    @NonNull
    @PrimaryKey
    public int id;
    public long time;
    public String entry;

    public Entry(){}

    @Ignore
    public Entry(int id, String entry){
        this.id = id;
        this.time = System.currentTimeMillis();
        this.entry = entry;
    }

    public synchronized void insert() {new Thread(() -> {try{db.logDao().insert(this);}catch (Exception ex){}}).start();}
}