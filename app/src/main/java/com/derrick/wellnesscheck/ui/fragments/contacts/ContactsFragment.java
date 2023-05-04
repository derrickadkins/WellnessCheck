package com.derrick.wellnesscheck.ui.fragments.contacts;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.data.Contact;
import com.derrick.wellnesscheck.data.Contacts;
import com.derrick.wellnesscheck.data.Prefs;
import com.derrick.wellnesscheck.utils.FragmentReadyListener;
import com.derrick.wellnesscheck.utils.PermissionsListener;
import com.derrick.wellnesscheck.ui.activities.PermissionsRequestingActivity;
import com.derrick.wellnesscheck.utils.sms.SmsReceiver;
import com.derrick.wellnesscheck.utils.sms.SmsController;
import com.derrick.wellnesscheck.ui.activities.SetupSettingsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_OK;
import static com.derrick.wellnesscheck.App.db;
import static com.derrick.wellnesscheck.utils.Utils.sameNumbers;

public class ContactsFragment extends Fragment implements ContactsRecyclerAdapter.OnContactActionListener {
    static final String TAG = "EmergencyContactsFragment";
    FloatingActionButton fab;
    TextView addAContact, riskLvl;
    ContactsRecyclerAdapter contactsRecyclerAdapter;
    RecyclerView contactsList;
    Button setupNext;
    Contacts contacts;
    ItemTouchHelper itemTouchHelper;
    FragmentReadyListener fragmentReadyListener;

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
                if(intent == null) return null;
                Uri contactData = intent.getData();
                Cursor cursor = new CursorLoader(requireActivity(), contactData, null, null, null, null).loadInBackground();
                if (cursor != null && cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    //make sure id doesn't already exist in DB
                    for (String contactId : contacts.keySet())
                        if (contactId.equalsIgnoreCase(id))
                            return null;
                    String hasPhone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    if (hasPhone.equalsIgnoreCase("1")) {
                        Cursor phones = requireActivity().getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id},
                                null);
                        phones.moveToFirst();
                        String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                        Contact contact = new Contact(id, name, number, 0);
                        if (!contactsRecyclerAdapter.contains(contact.id)) {
                            if (Prefs.confirmAddContact()) {
                                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.confirmation_dialog, null);
                                new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog_Alert)
                                        .setView(dialogView)
                                        .setMessage("Are you sure you want to ask " + contact.name + " to be your emergency contact?")
                                        .setPositiveButton("Yes", (dialog, which) -> {
                                            if (((CheckBox) dialogView.findViewById(R.id.never_ask_again)).isChecked())
                                                Prefs.confirmAddContact(false);
                                            onTryAddContact(contact);
                                        })
                                        .setNegativeButton(R.string.cancel, null)
                                        .setCancelable(false)
                                        .show();
                            } else onTryAddContact(contact);
                        }
                    }
                }
            }
            return null;
        }
    }, result -> { });

    public ContactsFragment(){super();}

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            if(intent == null) return;
            Uri contactData = intent.getData();
            Cursor cursor = new CursorLoader(requireActivity(), contactData, null, null, null, null).loadInBackground();
            if (cursor != null && cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                //make sure id doesn't already exist in DB
                for (String contactId : contacts.keySet())
                    if (contactId.equalsIgnoreCase(id))
                        return;
                String hasPhone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                if (hasPhone.equalsIgnoreCase("1")) {
                    Cursor phones = requireActivity().getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null);
                    phones.moveToFirst();
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                    Contact contact = new Contact(id, name, number, 0);
                    if (!contactsRecyclerAdapter.contains(contact.id)) {
                        if (Prefs.confirmAddContact()) {
                            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.confirmation_dialog, null);
                            new AlertDialog.Builder(requireContext(), R.style.AppTheme_Dialog_Alert)
                                    .setView(dialogView)
                                    .setMessage("Are you sure you want to ask " + contact.name + " to be your emergency contact?")
                                    .setPositiveButton("Yes", (dialog, which) -> {
                                        if (((CheckBox) dialogView.findViewById(R.id.never_ask_again)).isChecked())
                                            Prefs.confirmAddContact(false);
                                        onTryAddContact(contact);
                                    })
                                    .setNegativeButton(R.string.cancel, null)
                                    .setCancelable(false)
                                    .show();
                        } else onTryAddContact(contact);
                    }
                }
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof FragmentReadyListener)
            fragmentReadyListener = (FragmentReadyListener) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(fragmentReadyListener != null) fragmentReadyListener.onFragmentReady();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contactsView = inflater.inflate(R.layout.contacts_fragment, container, false);

        contacts = db.contacts;

        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle("Emergency Contacts");
            actionBar.show();
        }

        riskLvl = contactsView.findViewById(R.id.user_risk_lvl);
        calcRiskLvl();

        View.OnClickListener addContactClickListener = v -> ((PermissionsRequestingActivity) requireContext()).checkPermissions(
            new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS}, new PermissionsListener() {
                @Override
                public void permissionsGranted() {
                    //contactChooserResult.launch(null);
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                    startActivityForResult(intent, 0);
                }

                @Override
                public void permissionsDenied() {
                    // todo: Add contact manually
                }

                @Override
                public void showRationale(String[] permissions) {
                    Snackbar snackbar;
                    if(permissions.length > 1){
                        snackbar = Snackbar.make(contactsView, "Contacts and SMS\npermissions required", Snackbar.LENGTH_LONG);
                        ((TextView)snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text)).setMaxLines(2);
                    }else{
                        String pName = permissions[0] == Manifest.permission.READ_CONTACTS ? "Contacts" : "SMS";
                        snackbar = Snackbar.make(contactsView, pName + " permission required", Snackbar.LENGTH_LONG);
                    }

                    snackbar.setAction("Settings", v -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    });
                    snackbar.show();
                }
            });

        addAContact = contactsView.findViewById(R.id.add_a_contact_text_view);
        addAContact.setOnClickListener(addContactClickListener);

        fab = contactsView.findViewById(R.id.fab);
        fab.setOnClickListener(addContactClickListener);

        setupNext = contactsView.findViewById(R.id.btnSetupNext);
        setupNext.setVisibility(requireActivity().getLocalClassName().contains("SetupContactsActivity") ? View.VISIBLE : View.GONE);
        setupNext.setEnabled(false);
        setupNext.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SetupSettingsActivity.class)
                    .putExtra("returnToMain", false));
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        contactsList = contactsView.findViewById(R.id.emergency_contacts_recyclerview);
        contactsList.setLayoutManager(new LinearLayoutManager(getContext()));

        ((PermissionsRequestingActivity) requireContext()).checkPermissions(new String[]{Manifest.permission.READ_CONTACTS}, new PermissionsListener() {
            @Override
            public void permissionsGranted() {
                new Thread(() -> {
                    loadContacts();
                    new Handler(Looper.getMainLooper()).post(() -> setupAdapter());
                }).start();
            }

            @Override
            public void permissionsDenied() { setupAdapter(); }

            @Override
            public void showRationale(String[] permissions) { setupAdapter(); }
        });

        contactsView.findViewById(R.id.info_risk).setOnClickListener(
            view -> new AlertDialog.Builder(requireContext(), R.style.AppTheme_Dialog_Alert)
                .setTitle(R.string.risk_lvl)
                .setMessage(R.string.risk_lvl_info)
                .show());

        return contactsView;
    }

    void calcRiskLvl(){
        int r = 1;
        if(contacts != null) {
            for (Contact contact : contacts.values()) {
                if (contact.riskLvl > r)
                    r = contact.riskLvl;
            }
        }
        String risk = "Low";
        if(r == 2) risk = "Medium";
        else if(r == 3) risk = "High";

        riskLvl.setText(new StringBuilder().append(getString(R.string.your_risk_lvl)).append(risk));
    }

    public void setupAdapter() {
        onContactListSizeChange(contacts.size());
        contactsRecyclerAdapter = new ContactsRecyclerAdapter((AppCompatActivity) getActivity(), contacts, this);
        contactsList.setAdapter(contactsRecyclerAdapter);
        itemTouchHelper = new ItemTouchHelper(new SwipeOnContactCallback(contactsRecyclerAdapter));
        itemTouchHelper.attachToRecyclerView(contactsList);
    }

    public void loadContacts() {
        ContentResolver contentResolver = requireActivity().getContentResolver();
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
        contactsRecyclerAdapter.add(mContact);
        onContactListSizeChange(contactsRecyclerAdapter.getItemCount());
        calcRiskLvl();
    }

    @Override
    public void onDeleteContact(Contact mContact) {
        contacts.remove(mContact);
        onContactListSizeChange(contactsRecyclerAdapter.getItemCount());
        calcRiskLvl();
    }

    @Override
    public void onUndoDeleteContact(Contact mContact) {
        contacts.add(mContact);
        onContactListSizeChange(contactsRecyclerAdapter.getItemCount());
        calcRiskLvl();
    }

    @Override
    public void onActionModeStateChanged(boolean enabled) {
        itemTouchHelper.attachToRecyclerView(!enabled ? contactsList : null);
    }

    @Override
    public void onUpdateContact(Contact contact){
        contacts.update(contact);
        calcRiskLvl();
    }

    public void onContactListSizeChange(int size) {
        setupNext.setEnabled(size > 0);
        addAContact.setVisibility(size == 0 ? View.VISIBLE : View.GONE);
    }

    public void onTryAddContact(Contact contact) {
        final SmsReceiver smsReceiver = new SmsReceiver();
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Alert)
                .setMessage(getString(R.string.sending_request))
                .setView(new ProgressBar(getActivity()))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    getActivity().unregisterReceiver(smsReceiver);
                })
                .create();

        SmsController smsController = new SmsController() {
            @Override
            public void onSmsReceived(String number, String message) {
                message = message.toUpperCase(Locale.ROOT).trim();
                if (sameNumbers(number, contact.number)) {
                    if (message.replaceAll("\\d+", "").equals(getString(R.string.Y))) {
                        if (message.equals(getString(R.string.Y1)))
                            contact.riskLvl = 1;
                        else if (message.equals(getString(R.string.Y2)))
                            contact.riskLvl = 2;
                        else contact.riskLvl = 3;

                        addContact(contact);
                    }
                    alertDialog.cancel();
                    requireActivity().unregisterReceiver(smsReceiver);
                }
            }

            @Override
            public void onSmsFailedToSend() {

            }

            @Override
            public void onSmsSent() {
                if (--unsentParts == 0) {
                    alertDialog.setMessage(getString(R.string.message_sent));
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            new Handler(Looper.getMainLooper()).post(() -> alertDialog.setMessage(getString(R.string.waiting_for_response)));
                        }
                    }, 1000);
                }
            }
        };

        alertDialog.show();

        smsController.sendSMS((PermissionsRequestingActivity) requireActivity(), smsReceiver, smsController, contact.number, getString(R.string.contact_request));
    }
}