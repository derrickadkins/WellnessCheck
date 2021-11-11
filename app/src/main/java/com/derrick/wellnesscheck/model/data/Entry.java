package com.derrick.wellnesscheck.model.data;

import static com.derrick.wellnesscheck.WellnessCheck.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Entry {
    @NonNull
    @PrimaryKey
    public long time;
    public String entry;

    public Entry(String entry){
        this.time = System.currentTimeMillis();
        this.entry = entry;
    }

    public void insert() {new Thread(() -> db.logDao().insert(this));}
}