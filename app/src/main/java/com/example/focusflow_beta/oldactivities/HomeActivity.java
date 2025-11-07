package com.example.focusflow_beta.oldactivities;

import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import com.example.focusflow_beta.BaseActivity;
import com.example.focusflow_beta.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends BaseActivity {

    private TextView tvStatus, tvTimer;
    private Button btnStart, btnPause, btnReset;
    private BottomNavigationView bottomNavigationView;
    private boolean isRunning = false;
    private int seconds = 25 * 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_old);

        tvStatus = findViewById(R.id.tvStatus);
        tvTimer = findViewById(R.id.tvTimer);
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        btnReset = findViewById(R.id.btnReset);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // אנימציה קטנה לסטטוס
        Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(800);
        tvStatus.startAnimation(fadeIn);

        btnStart.setOnClickListener(v -> startSession());
        btnPause.setOnClickListener(v -> pauseSession());
        btnReset.setOnClickListener(v -> resetSession());

        // הגדרת NavBar אחיד דרך BaseActivity
        setupBottomNavigation(bottomNavigationView, R.id.nav_home);
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
