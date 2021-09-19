package com.derrick.wellnesscheck;

import static com.derrick.wellnesscheck.MainActivity.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;

public class HomeFragment extends Fragment {
    CircularProgressIndicator progressBar;
    TextView tvProgressBar;
    Button btnTurnOff;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View homeFragmentView = inflater.inflate(R.layout.home, container, false);

        btnTurnOff = (Button) homeFragmentView.findViewById(R.id.btnTurnOff);

        tvProgressBar = (TextView) homeFragmentView.findViewById(R.id.progressBarText);

        progressBar = (CircularProgressIndicator) homeFragmentView.findViewById(R.id.progressBar);
        progressBar.setMax(100);
        progressBar.setProgress(80);

        int visibility = settings.monitoringOn ? View.VISIBLE : View.GONE;
        progressBar.setVisibility(visibility);
        btnTurnOff.setVisibility(visibility);

        if(settings.monitoringOn){
            //todo: countdown
            tvProgressBar.setText("1:35:46");
        }else{
            tvProgressBar.setText("Setup Monitoring");
        }

        return homeFragmentView;
    }
}
