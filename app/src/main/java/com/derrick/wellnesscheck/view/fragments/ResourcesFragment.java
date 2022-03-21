package com.derrick.wellnesscheck.view.fragments;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.controller.ResourcesRecyclerAdapter;
import com.derrick.wellnesscheck.controller.SwipeToContactCallback;
import com.derrick.wellnesscheck.model.data.Resource;
import com.derrick.wellnesscheck.utils.PermissionsListener;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;
import com.derrick.wellnesscheck.utils.Utils;

public class ResourcesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.resources_fragment, container, false);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle("Resources");
        actionBar.show();

        ((PermissionsRequestingActivity)getActivity()).checkPermissions(new String[]{Manifest.permission.CALL_PHONE}, new PermissionsListener() {
            @Override
            public void permissionsGranted() {
                Resource[] resources = new Resource[]{
                        new Resource("Police/Fire/Ambulance", "911", false),
                        new Resource("National Suicide Prevention Lifeline", "800-273-8255", false),
                        new Resource("Crisis Text Line\nText HOME to 741741 to reach a volunteer Crisis Counselor", "741741", true).setMessage("HOME")
                };
                RecyclerView recyclerView = v.findViewById(R.id.resources_recycler_view);
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                ResourcesRecyclerAdapter resourcesRecyclerAdapter = new ResourcesRecyclerAdapter(getContext(), resources);
                recyclerView.setAdapter(resourcesRecyclerAdapter);
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToContactCallback(resourcesRecyclerAdapter));
                itemTouchHelper.attachToRecyclerView(recyclerView);
            }

            @Override
            public void permissionsDenied() { }

            @Override
            public void showRationale(String[] permissions) { }
        });
        return v;
    }
}