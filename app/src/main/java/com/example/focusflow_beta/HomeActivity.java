package com.example.focusflow_beta;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class HomeActivity extends AppCompatActivity {

    private TextView tvStatus, tvTimer;
    private Button btnStart, btnPause, btnReset;
    private BottomNavigationView bottomNavigationView;
    private boolean isRunning = false;
    private int seconds = 25 * 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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

        // נוויגציה תחתונה
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_stats) {
                // בעתיד יעבור לעמוד סטטיסטיקה
                startActivity(new Intent(this, BreakStatsActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_profile) {
                // בעתיד יעבור לעמוד פרופיל
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                return true;
            }
            return false;
        });
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
