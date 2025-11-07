package com.example.focusflow_beta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView tvStatus, tvTimer;
    private Button btnStart, btnPause, btnReset;
    private boolean isRunning = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvStatus = view.findViewById(R.id.tvStatus);
        tvTimer = view.findViewById(R.id.tvTimer);
        btnStart = view.findViewById(R.id.btnStart);
        btnPause = view.findViewById(R.id.btnPause);
        btnReset = view.findViewById(R.id.btnReset);

        btnStart.setOnClickListener(v -> startSession());
        btnPause.setOnClickListener(v -> pauseSession());
        btnReset.setOnClickListener(v -> resetSession());

        return view;
    }

    private void startSession() {
        if (!isRunning) {
            tvStatus.setText("ממוקד 🚀");
            isRunning = true;
        }
    }

    private void pauseSession() {
        if (isRunning) {
            tvStatus.setText("מושהה ⏸️");
            isRunning = false;
        }
    }

    private void resetSession() {
        tvStatus.setText("מוכן להתחלה");
        tvTimer.setText("25:00");
        isRunning = false;
    }
}
