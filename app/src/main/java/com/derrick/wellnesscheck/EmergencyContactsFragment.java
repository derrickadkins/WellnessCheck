package com.derrick.wellnesscheck;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class EmergencyContactsFragment extends Fragment {
    FloatingActionButton fab;
    EmergencyContactsRecyclerAdapter emergencyContactsRecyclerAdapter;
    RecyclerView contactsList;
    DB db;
    ArrayList<Contact> contacts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View emergencyContactsFragmentView = inflater.inflate(R.layout.emergency_contacts_fragment, container, false);

        fab = emergencyContactsFragmentView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                    startActivityForResult(intent, 1);
                }else{
                    // todo: Add contact manually

                }

            }
        });

        contactsList = emergencyContactsFragmentView.findViewById(R.id.emergency_contacts_recyclerview);
        contactsList.setLayoutManager(new LinearLayoutManager(getContext()));

        db = Room.databaseBuilder(getContext(),
                DB.class, "database-name")
                .fallbackToDestructiveMigration()
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //use to clear db
                //db.contactDao().nukeTable();
                contacts = new ArrayList<>(db.contactDao().getAll());
                if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS)
                        == PackageManager.PERMISSION_GRANTED) {
                    loadContacts();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setupAdatper();
                        }
                    });
                }
            }
        }).start();

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.READ_CONTACTS},
                        0);
            }
        }

        return emergencyContactsFragmentView;
    }

    public void setupAdatper() {
        emergencyContactsRecyclerAdapter = new EmergencyContactsRecyclerAdapter(getContext(), contacts);
        contactsList.setAdapter(emergencyContactsRecyclerAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 0){
            if(resultCode == RESULT_OK){
                loadContacts();
                setupAdatper();
            }
        } else if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
//                Uri contactData = data.getData();
//                Cursor cursor = new CursorLoader(getActivity(), contactData, null, null, null, null).loadInBackground();
//                if(cursor != null && cursor.moveToFirst()) {
//                    String ID = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
//                    int id = Integer.parseInt(ID);
//                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
//                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
//
//                    Contact contact = new Contact(id, name, number);
//
//                    if (!emergencyContactsRecyclerAdapter.contains(contact.id)) {
//                        emergencyContactsRecyclerAdapter.add(contact);
//                    }
//                }

                Uri contactData = data.getData();
                Cursor cursor = new CursorLoader(getActivity(), contactData, null, null, null, null).loadInBackground();
                if (cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    String hasPhone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    if (hasPhone.equalsIgnoreCase("1")) {
                        Cursor phones = getActivity().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);
                        phones.moveToFirst();
                        String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                        final Contact contact = new Contact(id, name, number);
                        if (!emergencyContactsRecyclerAdapter.contains(contact.id)) {
                            emergencyContactsRecyclerAdapter.add(contact);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    db.contactDao().insertAll(contact);
                                }
                            }).start();
                        }
                    }
                }
            }
        }
    }

    public void loadContacts(){
        ArrayList<Contact> dbData = new ArrayList<>(contacts);
        Hashtable<String, Contact> tempContacts = new Hashtable<>();
        for (Contact contact:dbData) {
            tempContacts.put(contact.id, contact);
        }
        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor.getCount() > 0) {
            contacts = new ArrayList<>();
            while (cursor.moveToNext()) {
                Contact android_contact = new Contact("", "", "");
                String contact_id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                android_contact.id = contact_id;
                if (!tempContacts.containsKey(contact_id))
                    continue;
                String contact_display_name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                android_contact.name = contact_display_name;
                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
                if (hasPhoneNumber > 0) {
                    Cursor phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                            , null
                            , ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?"
                            , new String[]{contact_id}
                            , null);
                    while (phoneCursor.moveToNext()) {
                        String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        android_contact.number = phoneNumber;
                    }
                    phoneCursor.close();
                }
                contacts.add(android_contact);
            }
            for (int i = 0; i < dbData.size()-1; i++) {
                Contact contact = dbData.get(i);
                Contact mContact = contacts.get(i);
                if (contact.number != mContact.number || contact.name != mContact.name)
                    db.contactDao().update(mContact);
                tempContacts.remove(contact.id);
            }
            if (tempContacts.size() > 0) {
                List<Contact> tempContactsList = new ArrayList<>(tempContacts.values());
                for (Contact contact : tempContactsList) {
                    db.contactDao().delete(contact);
                }
            }
        }
    }
}
