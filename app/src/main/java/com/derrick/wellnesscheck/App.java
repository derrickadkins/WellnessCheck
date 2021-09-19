package com.derrick.wellnesscheck;

import android.content.Context;

import androidx.room.Room;

public class App {
    public static DB db;
    public static AppSettings settings;

    public static void InitDB(Context context){
        db = Room.databaseBuilder(context,
                DB.class, "database-name")
                .fallbackToDestructiveMigration()
                .build();
    }

    public static void InitSettings(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                AppSettings tmpSettings = db.settingsDao().getSettings();
                if(tmpSettings != null) settings = tmpSettings;
                else{
                    settings = new AppSettings();
                    db.settingsDao().insert(settings);
                }
            }
        }).start();
    }
}
