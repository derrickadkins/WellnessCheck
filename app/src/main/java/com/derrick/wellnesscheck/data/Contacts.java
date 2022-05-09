package com.derrick.wellnesscheck.data;

import static com.derrick.wellnesscheck.App.db;

import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Hashtable;

public class Contacts extends Hashtable<String, Contact> {
    public Contacts(){super();}
    public Contacts(Collection<Contact> contactsList){
        super();
        for(Contact contact : contactsList) put(contact.id, contact);
    }

    public Contact add(Contact contact) {
        contact.insert();
        return super.put(contact.id, contact);
    }

    public Contact update(Contact contact){
        contact.update();
        return super.put(contact.id, contact);
    }

    @Override
    public Contact remove(@Nullable Object o) {
        if(o != null && o instanceof Contact) {
            ((Contact)o).delete();
            return super.remove(((Contact)o).id);
        }
        else return null;
    }

    public Hashtable<String, Contact> getDetachedCopy(){
        return (Hashtable<String, Contact>) super.clone();
    }

    public static Contacts Init(){return new Contacts(db.contactDao().getAll());}
}
