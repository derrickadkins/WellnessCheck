package com.derrick.wellnesscheck;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Contact {
    @NonNull
    @PrimaryKey
    public String id;
    public String name, number;

    public Contact(String id, String name, String number){
        this.id = id;
        this.name = name;
        this.number = number;
    }
}
