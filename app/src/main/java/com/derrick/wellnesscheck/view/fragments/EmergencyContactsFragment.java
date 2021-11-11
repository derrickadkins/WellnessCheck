package com.derrick.wellnesscheck.view.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.controller.EmergencyContactsRecyclerAdapter;
import com.derrick.wellnesscheck.model.data.Contact;
import com.derrick.wellnesscheck.model.data.Contacts;
import com.derrick.wellnesscheck.model.data.Log;
import com.derrick.wellnesscheck.utils.PermissionsListener;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;
import com.derrick.wellnesscheck.SmsBroadcastManager;
import com.derrick.wellnesscheck.controller.SmsController;
import com.derrick.wellnesscheck.controller.SwipeToDeleteCallback;
import com.derrick.wellnesscheck.view.activities.SetupSettingsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_OK;
import static com.derrick.wellnesscheck.WellnessCheck.db;
import static com.derrick.wellnesscheck.utils.Utils.sameNumbers;

public class EmergencyContactsFragment extends Fragment implements EmergencyContactsRecyclerAdapter.OnContactDeleteListener {
    static final String TAG = "EmergencyContactsFragment";
    FloatingActionButton fab;
    EmergencyContactsRecyclerAdapter emergencyContactsRecyclerAdapter;
    RecyclerView contactsList;
    Button setupNext;
    Contacts contacts;

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
                    for (String contactId : contacts.keySet())
                        if (contactId.equalsIgnoreCase(id))
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
    }, result -> { });

    public EmergencyContactsFragment(){super();}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View emergencyContactsFragmentView = inflater.inflate(R.layout.emergency_contacts_fragment, container, false);

        contacts = db.contacts;

        fab = emergencyContactsFragmentView.findViewById(R.id.fab);
        fab.setOnClickListener(view -> ((PermissionsRequestingActivity) getContext()).checkPermissions(new String[]{Manifest.permission.READ_CONTACTS}, new PermissionsListener() {
            @Override
            public void permissionsGranted() {
                contactChooserResult.launch(null);
            }

            @Override
            public void permissionsDenied() {
                // todo: Add contact manually
            }

            @Override
            public void showRationale(String[] permissions) {

            }
        }));

        contactsList = emergencyContactsFragmentView.findViewById(R.id.emergency_contacts_recyclerview);
        contactsList.setLayoutManager(new LinearLayoutManager(getContext()));

        ((PermissionsRequestingActivity) getContext()).checkPermissions(new String[]{Manifest.permission.READ_CONTACTS}, new PermissionsListener() {
            @Override
            public void permissionsGranted() {
                new Thread(() -> {
                    loadContacts();
                    getActivity().runOnUiThread(() -> setupAdapter());
                }).start();
            }

            @Override
            public void permissionsDenied() {

            }

            @Override
            public void showRationale(String[] permissions) {

            }
        });

        setupNext = emergencyContactsFragmentView.findViewById(R.id.btnSetupNext);
        setupNext.setVisibility(getActivity().getLocalClassName().contains("SetupContactsActivity") ? View.VISIBLE : View.GONE);
        setupNext.setEnabled(false);
        setupNext.setOnClickListener(v -> startActivity(new Intent(getActivity(), SetupSettingsActivity.class)));
        return emergencyContactsFragmentView;
    }

    public void setupAdapter() {
        onContactListSizeChange(contacts.size());
        emergencyContactsRecyclerAdapter = new EmergencyContactsRecyclerAdapter(getContext(), contacts, this);
        contactsList.setAdapter(emergencyContactsRecyclerAdapter);
        if(!db.settings.monitoringOn || getActivity().getLocalClassName().equalsIgnoreCase("SetupContactsActivity")) {
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(emergencyContactsRecyclerAdapter));
            itemTouchHelper.attachToRecyclerView(contactsList);
        }
    }

    public void loadContacts() {
        ContentResolver contentResolver = getActivity().getContentResolver();
        for(Contact contact : contacts.values()) {
            Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, ContactsContract.Contacts._ID + " = ?", new String[]{contact.id}, null);
            if (!cursor.moveToFirst()) {
                contacts.remove(contact);
                continue;
            }
            String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            String number = null;
            int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
            if (hasPhoneNumber > 0) {
                Cursor phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                        , null
                        , ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?"
                        , new String[]{contact.id}
                        , null);
                while (phoneCursor.moveToNext()) {
                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    if(number == null) number = phoneNumber;
                    if(sameNumbers(contact.number, phoneNumber)) {
                        number = contact.number;
                        break;
                    }
                }
                phoneCursor.close();
            }
            cursor.close();

            if(!contact.name.equals(name) || !contact.number.equals(number)){
                if(!contact.name.equals(name)) contact.name = name;
                if(!contact.number.equals(number)) contact.number = number;
                contacts.update(contact);
            }
        }
    }

    public void addContact(Contact mContact){
        contacts.add(mContact);
        emergencyContactsRecyclerAdapter.add(mContact);
        onContactListSizeChange(emergencyContactsRecyclerAdapter.getItemCount());
    }

    @Override
    public void onDeleteContact(Contact mContact) {
        contacts.remove(mContact);
        onContactListSizeChange(emergencyContactsRecyclerAdapter.getItemCount());
    }

    @Override
    public void onUndoDeleteContact(Contact mContact) {
        contacts.add(mContact);
        onContactListSizeChange(emergencyContactsRecyclerAdapter.getItemCount());
    }

    public void onContactListSizeChange(int size) {
        setupNext.setEnabled(size > 0);
    }

    public void onTryAddContact(Contact contact) {
        final SmsBroadcastManager smsBroadcastManager = new SmsBroadcastManager();

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Alert)
                .setMessage("Sending request via SMS ...")
                .setView(new ProgressBar(getActivity()))
                .setCancelable(false)
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                    getActivity().unregisterReceiver(smsBroadcastManager);
                })
                .create();

        SmsController smsController = new SmsController() {
            @Override
            public void onSmsReceived(String number, String message) {
                message = message.toUpperCase(Locale.ROOT).trim();
                if (sameNumbers(number, contact.number)) {
                    if (message.replaceAll("\\d+", "").equals("Y")) {
                        if (message.equals("Y1"))
                            contact.riskLvl = 1;
                        else if (message.equals("Y2"))
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
                if (--unsentParts == 0) {
                    alertDialog.setMessage("Message Sent");
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getActivity().runOnUiThread(() -> alertDialog.setMessage("Waiting for response ..."));
                        }
                    }, 1000);
                }
            }
        };

        alertDialog.show();

        smsController.sendSMS((PermissionsRequestingActivity) getContext(), smsBroadcastManager, smsController, contact.number, getString(R.string.contact_request));
    }
}