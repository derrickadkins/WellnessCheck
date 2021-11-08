package com.derrick.wellnesscheck.view.fragments;

import static com.derrick.wellnesscheck.controller.DbController.log;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.derrick.wellnesscheck.R;
import com.derrick.wellnesscheck.model.data.LogEntry;
import com.derrick.wellnesscheck.controller.DbController;
import com.derrick.wellnesscheck.utils.Log;
import com.derrick.wellnesscheck.MonitorReceiver;

public class LogFragment extends Fragment implements Log.Listener {
    LogAdapter logAdapter = new LogAdapter();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View logView = inflater.inflate(R.layout.log_fragment, container, false);

        RecyclerView rvLog = logView.findViewById(R.id.rvLog);
        rvLog.setLayoutManager(new LinearLayoutManager(getContext()));
        rvLog.setAdapter(logAdapter);

        return logView;
    }

    @Override
    public void onResume() {
        super.onResume();
        DbController.logListener = this;
    }

    @Override
    public void onPause() {
        super.onPause();
        DbController.logListener = null;
    }

    @Override
    public void onLog(LogEntry entry) {
        logAdapter.notifyItemInserted(0);
    }

    class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder>{
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, null));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ((TextView)holder.itemView.findViewById(android.R.id.text1))
                    .setText(MonitorReceiver.getReadableTime(log.get(position).time) + " : " + log.get(position).entry);
        }

        @Override
        public int getItemCount() { return log.size(); }

        class ViewHolder extends RecyclerView.ViewHolder{
            ViewHolder(View v){super(v);}
        }
    }
}