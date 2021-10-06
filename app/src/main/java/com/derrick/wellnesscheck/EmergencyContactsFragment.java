package com.derrick.wellnesscheck;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_OK;
import static com.derrick.wellnesscheck.MainActivity.db;
import static com.derrick.wellnesscheck.MainActivity.contacts;
import static com.derrick.wellnesscheck.MainActivity.settings;

public class EmergencyContactsFragment extends Fragment implements OnContactDeleteListener, SmsBroadcastManager.SmsListener {
    FloatingActionButton fab;
    EmergencyContactsRecyclerAdapter emergencyContactsRecyclerAdapter;
    RecyclerView contactsList;
    Button setupNext;
    SmsBroadcastManager smsBroadcastManager;
    AlertDialog alertDialog;
    Contact contact;
    int smsPartsUnsent;

    ActivityResultLauncher<String> contactPermissionsResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result) {
                        loadContacts();
                        setupAdapter();
                    } else {
                        //Log.e(TAG, "onActivityResult: PERMISSION DENIED");
                    }
                }
            });

    ActivityResultLauncher<String[]> smsPermissionsResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    for(String permission : result.keySet())
                        if(!result.get(permission))return;
                    onTryAddContact(contact);
                }
            });

    ActivityResultLauncher<Object> contactChooserResult = registerForActivityResult(new ActivityResultContract<Object, Object>() {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, Object input) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            return intent;
        }

        @Override
        public Object parseResult(int resultCode, @Nullable Intent intent) {
            if (resultCode == RESULT_OK) {
                Uri contactData = intent.getData();
                Cursor cursor = new CursorLoader(getActivity(), contactData, null, null, null, null).loadInBackground();
                if (cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    //make sure id doesn't already exist in DB
                    for (Contact contact : contacts)
                        if (contact.id.equalsIgnoreCase(id))
                            return null;
                    String hasPhone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    if (hasPhone.equalsIgnoreCase("1")) {
                        Cursor phones = getActivity().getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id},
                                null);
                        phones.moveToFirst();
                        String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                        Contact contact = new Contact(id, name, number, 0);
                        if (!emergencyContactsRecyclerAdapter.contains(contact.id))
                            onTryAddContact(contact);
                    }
                }
            }
            return null;
        }
    }, new ActivityResultCallback<Object>() {
        @Override
        public void onActivityResult(Object result) {

        }
    });

    EmergencyContactsFragment(){super();}

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
                    contactChooserResult.launch(null);
                }else{
                    // todo: Add contact manually
                }
            }
        });

        contactsList = emergencyContactsFragmentView.findViewById(R.id.emergency_contacts_recyclerview);
        contactsList.setLayoutManager(new LinearLayoutManager(getContext()));

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
                            setupAdapter();
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
                contactPermissionsResult.launch(Manifest.permission.READ_CONTACTS);
            }
        }

        setupNext = emergencyContactsFragmentView.findViewById(R.id.btnSetupNext);
        setupNext.setVisibility(getActivity().getLocalClassName().equalsIgnoreCase("SetupContactsActivity") ? View.VISIBLE : View.GONE);
        setupNext.setEnabled(false);
        setupNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SetupSettingsActivity.class));
            }
        });
        return emergencyContactsFragmentView;
    }

    public void setupAdapter() {
        onContactListSizeChange(contacts.size());
        emergencyContactsRecyclerAdapter = new EmergencyContactsRecyclerAdapter(getContext(), contacts, this);
        contactsList.setAdapter(emergencyContactsRecyclerAdapter);
        if(!settings.monitoringOn || getActivity().getLocalClassName().equalsIgnoreCase("SetupContactsActivity")) {
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(emergencyContactsRecyclerAdapter));
            itemTouchHelper.attachToRecyclerView(contactsList);
        }
    }

    public void loadContacts(){
        //create a copy to compare to for updates later
        ArrayList<Contact> dbData = new ArrayList<>(contacts);
        //used for filtering out deleted contacts
        Hashtable<String, Contact> tempContacts = new Hashtable<>();
        for (Contact contact:dbData) {
            tempContacts.put(contact.id, contact);
        }
        //get all contacts
        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor.getCount() > 0) {
            contacts = new ArrayList<>();
            while (cursor.moveToNext()) {
                Contact android_contact = new Contact("", "", "", 0);
                String contact_id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                android_contact.id = contact_id;
                //if it's not already added, move on to the next one
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
                //id recognized, add for comparison
                contacts.add(android_contact);
            }
            //check for updates in contact info
            for (int i = 0; i < dbData.size(); i++) {
                Contact contact = dbData.get(i);
                Contact mContact = contacts.get(i);
                if (contact.number != mContact.number || contact.name != mContact.name)
                    //todo: maybe re-verify contact here?
                    db.contactDao().update(mContact);
                tempContacts.remove(contact.id);
            }
            //delete contact if removed from phone
            //todo: maybe delete this loop?
            if (tempContacts.size() > 0) {
                List<Contact> tempContactsList = new ArrayList<>(tempContacts.values());
                for (Contact contact : tempContactsList) {
                    db.contactDao().delete(contact);
                }
            }
        }
    }

    public void addContact(Contact contact){
        emergencyContactsRecyclerAdapter.add(contact);
        onContactListSizeChange(emergencyContactsRecyclerAdapter.getItemCount());
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.contactDao().insertAll(contact);
            }
        }).start();
    }

    @Override
    public void onDeleteContact(Contact contact) {
        onContactListSizeChange(emergencyContactsRecyclerAdapter.getItemCount());
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.contactDao().delete(contact);
            }
        }).start();
    }

    @Override
    public void onUndoDeleteContact(Contact contact) {
        onContactListSizeChange(emergencyContactsRecyclerAdapter.getItemCount());
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.contactDao().insertAll(contact);
            }
        }).start();
    }

    public void onContactListSizeChange(int size) {
        setupNext.setEnabled(size > 0);
    }

    public void onTryAddContact(Contact contact) {
        List<String> permissions = new ArrayList<>();
        this.contact = contact;
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
        }
        if(permissions.size() > 0){
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.SEND_SMS)
                    || ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.RECEIVE_SMS)) {
            } else {
                smsPermissionsResult.launch(permissions.toArray(new String[permissions.size()]));
            }
        }else {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0,
                    new Intent(getActivity(), SmsBroadcastManager.class)
                            .setAction(SmsBroadcastManager.ACTION_SEND_SMS_RESULT),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            SmsManager smsManager = getActivity().getSystemService(SmsManager.class);
            if(smsManager == null) smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(getString(R.string.contact_request));
            ArrayList<PendingIntent> pendingIntents = new ArrayList<>();
            for(int i = 0; i < parts.size(); i++) pendingIntents.add(pendingIntent);
            smsPartsUnsent = parts.size();

            smsBroadcastManager = new SmsBroadcastManager(this);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            intentFilter.addAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
            intentFilter.addAction(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION);
            getActivity().registerReceiver(smsBroadcastManager, intentFilter);

            smsManager.sendMultipartTextMessage(contact.number, null, parts, pendingIntents, null);
            alertDialog = new AlertDialog.Builder(getActivity())
                    .setMessage("Sending request via SMS ...")
                    .setView(new ProgressBar(getActivity()))
                    .setCancelable(false)
                    .create();
            alertDialog.show();
        }
    }

    @Override
    public void onSmsReceived(String number, String message) {
        if(normalizeNumber(number).equalsIgnoreCase(normalizeNumber(contact.number))) {
            if(message.equalsIgnoreCase("Y1")
                    || message.equalsIgnoreCase("Y2")
                    || message.equalsIgnoreCase("Y3")){
                if(message.equalsIgnoreCase("Y1"))
                    contact.riskLvl = 1;
                else if(message.equalsIgnoreCase("Y2"))
                    contact.riskLvl = 2;
                else contact.riskLvl = 3;
                addContact(contact);
            }
            alertDialog.cancel();
            getActivity().unregisterReceiver(smsBroadcastManager);
        }
    }

    @Override
    public void onSmsFailedToSend() {

    }

    @Override
    public void onSmsSent() {
        if(--smsPartsUnsent == 0) {
            alertDialog.setMessage("Message Sent");
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    alertDialog.setMessage("Waiting for response ...");
                }
            }, 1000);
        }
    }

    String normalizeNumber(String number){
        return number.replaceAll("\\D+", "");
    }
}
