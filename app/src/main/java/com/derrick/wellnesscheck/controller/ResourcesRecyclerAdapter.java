package com.derrick.wellnesscheck.controller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.utils.PermissionsListener;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;

public class ResourcesRecyclerAdapter extends RecyclerView.Adapter<ResourcesRecyclerAdapter.ViewHolder> {
    private String[] resources;
    private LayoutInflater inflater;
    private Context context;

    public ResourcesRecyclerAdapter(Context context, String[] resources){
        this.context = context;
        this.resources = resources;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.contact_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourcesRecyclerAdapter.ViewHolder holder, int position) {
        holder.action.setText("Swipe to call");
        holder.number.setText(resources[position]);
    }

    @Override
    public int getItemCount() {
        return resources.length;
    }

    public void contact(int pos){
        ((PermissionsRequestingActivity)context).checkPermissions(new String[]{Manifest.permission.CALL_PHONE}, new PermissionsListener() {
            @Override
            public void permissionsGranted() {
                String uri = "tel:" + resources[pos];
                context.startActivity(new Intent(Intent.ACTION_CALL).setData(Uri.parse(uri)));
            }

            @Override
            public void permissionsDenied() { }

            @Override
            public void showRationale(String[] permissions) { }
        });
        notifyItemChanged(pos);
    }

    public Context getContext() {
        return context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView action, number;
        boolean sms = false;
        ViewHolder(View itemView){
            super(itemView);
            action = itemView.findViewById(R.id.contact_number);
            number = itemView.findViewById(R.id.contact_name);
        }
    }
}
