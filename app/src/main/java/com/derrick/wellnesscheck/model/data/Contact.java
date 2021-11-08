package com.derrick.wellnesscheck.model.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Contact {
    @NonNull
    @PrimaryKey
    public String id;
    public String name, number;
    public int riskLvl;

    public Contact(String id, String name, String number, int riskLvl){
        this.id = id;
        this.name = name;
        this.number = number;
        this.riskLvl = riskLvl;
    }
}
