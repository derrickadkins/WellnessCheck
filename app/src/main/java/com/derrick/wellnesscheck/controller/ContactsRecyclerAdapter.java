package com.derrick.wellnesscheck.controller;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.model.data.Contact;
import com.derrick.wellnesscheck.model.data.Contacts;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class ContactsRecyclerAdapter extends RecyclerView.Adapter<ContactsRecyclerAdapter.ViewHolder> {
    private ArrayList<Contact> mData;
    private LayoutInflater mInflater;
    private Contact mRecentlyDeletedItem;
    private int mRecentlyDeletedItemPosition;
    private Context context;
    private OnContactDeleteListener contactDeleteListener;

    public ContactsRecyclerAdapter(Context context, Contacts dataSet, OnContactDeleteListener contactDeleteListener){
        this.context = context;
        mInflater = LayoutInflater.from(context);
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
        return context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, number;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contact_name);
            number = itemView.findViewById(R.id.contact_number);
        }
    }

    public interface OnContactDeleteListener {
        void onDeleteContact(Contact contact);
        void onUndoDeleteContact(Contact contact);
    }
}
