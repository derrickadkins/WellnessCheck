package com.derrick.wellnesscheck.model.data;

import static com.derrick.wellnesscheck.WellnessCheck.db;

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

    public boolean matches(Contact contact){
        return contact.id.equals(id) && contact.name.equals(name) && contact.number.equals(number);
    }

    public void insert() {new Thread(() -> db.contactDao().insertAll(this)).start();}
    public void update() {new Thread(() -> db.contactDao().update(this)).start();}
    public void delete() {new Thread(() -> db.contactDao().delete(this)).start();}
}
