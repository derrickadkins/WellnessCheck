package com.derrick.wellnesscheck.controller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.model.data.Resource;
import com.derrick.wellnesscheck.utils.PermissionsListener;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;

public class ResourcesRecyclerAdapter extends RecyclerView.Adapter<ResourcesRecyclerAdapter.ViewHolder> {
    private Resource[] resources;
    private LayoutInflater inflater;
    private Context context;

    public ResourcesRecyclerAdapter(Context context, Resource[] resources){
        this.context = context;
        this.resources = resources;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.resource_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourcesRecyclerAdapter.ViewHolder holder, int position) {
        String action = "Swipe to call";
        if(resources[position].number.equals("911")) action = "Swipe to dial";
        else if (resources[position].isSMS) action = "Swipe to SMS";
        holder.action.setText(action);
        holder.number.setText(resources[position].number);
        holder.description.setText(resources[position].description);
        if(TextUtils.isEmpty(resources[position].descriptionAlt)) holder.descriptionAlt.setVisibility(View.GONE);
        else holder.descriptionAlt.setText(resources[position].descriptionAlt);
        holder.sms = resources[position].isSMS;
    }

    @Override
    public int getItemCount() {
        return resources.length;
    }

    public void contact(int pos){
        if(resources[pos].isSMS){
            Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("sms:" + resources[pos].number));
            if(!TextUtils.isEmpty(resources[pos].message)) intent.putExtra("sms_body", resources[pos].message);
            context.startActivity(intent);
        }else {
            ((PermissionsRequestingActivity) context).checkPermissions(new String[]{Manifest.permission.CALL_PHONE}, new PermissionsListener() {
                @Override
                public void permissionsGranted() {
                    String uri = "tel:" + resources[pos].number;
                    context.startActivity(new Intent(Intent.ACTION_CALL).setData(Uri.parse(uri)));
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

    public Context getContext() {
        return context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView action, number, description, descriptionAlt;
        boolean sms = false;
        ViewHolder(View itemView){
            super(itemView);
            description = itemView.findViewById(R.id.resource_description);
            descriptionAlt = itemView.findViewById(R.id.resource_description_alt);
            number = itemView.findViewById(R.id.resource_number);
            action = itemView.findViewById(R.id.resource_action);
        }
    }
}
