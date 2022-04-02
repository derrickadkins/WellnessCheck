package com.derrick.wellnesscheck.model.data;

import static com.derrick.wellnesscheck.WellnessCheck.db;
import static com.derrick.wellnesscheck.utils.Utils.getRoundedCroppedBitmap;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.derrick.wellnesscheck.R;

import java.io.IOException;
import java.io.InputStream;

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

    public void applyPhoto(Context context, ImageView contactImage) {
        Bitmap photo = retrieveContactPhoto(context);
        if(photo != null) contactImage.setImageBitmap(getRoundedCroppedBitmap(photo));
        else contactImage.setImageDrawable(context.getDrawable(R.drawable.ic_contact_default));
    }

    private Bitmap retrieveContactPhoto(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String contactId = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID};

        Cursor cursor =
                contentResolver.query(
                        uri,
                        projection,
                        null,
                        null,
                        null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
            }
            cursor.close();
        }

        Bitmap photo = null;
        try {
            if(contactId != null) {
                InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactId)));

                if (inputStream != null) photo = BitmapFactory.decodeStream(inputStream);

                //assert inputStream != null;
                if(inputStream != null) inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return photo;
    }
}
