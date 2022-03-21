package com.derrick.wellnesscheck.controller;

import static com.derrick.wellnesscheck.WellnessCheck.db;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.model.data.Contact;
import com.derrick.wellnesscheck.model.data.Contacts;
import com.derrick.wellnesscheck.utils.PermissionsListener;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class ContactsRecyclerAdapter extends RecyclerView.Adapter<ContactsRecyclerAdapter.ViewHolder> {
    private ArrayList<Contact> mData;
    private LayoutInflater mInflater;
    private Contact mRecentlyDeletedItem;
    private int mRecentlyDeletedItemPosition;
    private AppCompatActivity activity;
    private ActionMode actionMode;
    private OnContactDeleteListener contactDeleteListener;
    private int selectedPos=-1;

    public ContactsRecyclerAdapter(AppCompatActivity activity, Contacts dataSet, OnContactDeleteListener contactDeleteListener){
        this.activity = activity;
        mInflater = LayoutInflater.from(activity);
        mData = new ArrayList<>(dataSet.values());
        this.contactDeleteListener = contactDeleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.contact_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = mData.get(position);
        holder.name.setText(contact.name);
        holder.number.setText(contact.number);
        holder.itemView.setBackgroundColor(activity.getColor(position == selectedPos ? android.R.color.darker_gray : android.R.color.transparent));
        holder.itemView.setOnLongClickListener(v -> {
            if (actionMode != null || db.settings.monitoringOn) {
                return false;
            }

            // Start the CAB using the ActionMode.Callback defined above
            actionMode = activity.startSupportActionMode(actionModeCallback);
            v.setSelected(true);
            selectedPos = holder.getBindingAdapterPosition();
            notifyItemChanged(selectedPos);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public boolean contains(String id){
        for(Contact contact : mData) if(contact.id.equals(id)) return true;
        return false;
    }

    public void add(final Contact contact){
        mData.add(contact);
        notifyItemInserted(mData.size() - 1);
    }

    public void delete(int pos) {
        mRecentlyDeletedItem = mData.get(pos);
        mRecentlyDeletedItemPosition = pos;
        mData.remove(pos);
        notifyItemRemoved(pos);
        contactDeleteListener.onDeleteContact(mRecentlyDeletedItem);
        showUndoSnackbar();
    }

    private void showUndoSnackbar() {
        View view = ((Activity)getContext()).findViewById(R.id.emergency_contacts_fragment);
        Snackbar snackbar = Snackbar.make(view, mRecentlyDeletedItem.name + " removed",
                Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo Delete", v -> undoDelete());
        snackbar.show();
    }

    private void undoDelete() {
        mData.add(mRecentlyDeletedItemPosition, mRecentlyDeletedItem);
        contactDeleteListener.onUndoDeleteContact(mRecentlyDeletedItem);
        notifyItemInserted(mRecentlyDeletedItemPosition);
    }

    public Context getContext() {
        return activity;
    }

    public void contact(int pos, boolean sms){
        if(sms){
            activity.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("sms:" + mData.get(pos).number)));
        }else {
            ((PermissionsRequestingActivity) activity).checkPermissions(new String[]{Manifest.permission.CALL_PHONE}, new PermissionsListener() {
                @Override
                public void permissionsGranted() {
                    String uri = "tel:" + mData.get(pos).number;
                    activity.startActivity(new Intent(Intent.ACTION_CALL).setData(Uri.parse(uri)));
                }

                @Override
                public void permissionsDenied() {
                }

                @Override
                public void showRationale(String[] permissions) {
                }
            });
        }
        notifyItemChanged(pos);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, number;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.resource_description);
            number = itemView.findViewById(R.id.resource_number);
        }
    }

    public interface OnContactDeleteListener {
        void onDeleteContact(Contact contact);
        void onUndoDeleteContact(Contact contact);
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_context_contacts, menu);
            mode.setTitle("Delete Contact");
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.context_menu_item_delete:
                    delete(selectedPos);
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedPos = -1;
            notifyDataSetChanged();
        }
    };
}
