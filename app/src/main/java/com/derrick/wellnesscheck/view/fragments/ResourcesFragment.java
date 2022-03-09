package com.derrick.wellnesscheck.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.controller.ResourcesRecyclerAdapter;
import com.derrick.wellnesscheck.controller.SwipeToContactCallback;
import com.derrick.wellnesscheck.utils.PermissionsListener;
import com.derrick.wellnesscheck.utils.PermissionsRequestingActivity;
import com.derrick.wellnesscheck.utils.Utils;

public class ResourcesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.resources_fragment, container, false);

        ((PermissionsRequestingActivity)getActivity()).checkPermissions(new String[]{}, new PermissionsListener() {
            @Override
            public void permissionsGranted() {
                String[] eNums = Utils.getEmergencyNumbers();
                TextView tvNoResources = v.findViewById(R.id.tvNoResources);
                RecyclerView recyclerView = v.findViewById(R.id.resources_recycler_view);
                if(eNums.length == 0) {
                    tvNoResources.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }else {
                    tvNoResources.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    ResourcesRecyclerAdapter resourcesRecyclerAdapter = new ResourcesRecyclerAdapter(getContext(), eNums);
                    recyclerView.setAdapter(resourcesRecyclerAdapter);
                    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToContactCallback(resourcesRecyclerAdapter));
                    itemTouchHelper.attachToRecyclerView(recyclerView);
                }
            }

            @Override
            public void permissionsDenied() { }

            @Override
            public void showRationale(String[] permissions) { }
        });
        return v;
    }
}
