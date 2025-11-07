package com.example.focusflow_beta.oldactivities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.example.focusflow_beta.BaseActivity;
import com.example.focusflow_beta.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends BaseActivity {

    private Button btnBackProfile;
    private TextView tvProfileInfo, tvProfileTitle;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_old);

        // Toolbar עם חץ חזרה
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        // Views
        btnBackProfile = findViewById(R.id.btnBackProfile);
        tvProfileInfo = findViewById(R.id.tvProfileInfo);
        tvProfileTitle = findViewById(R.id.tvProfileTitle);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // כפתור חזור
        btnBackProfile.setOnClickListener(v -> finish());

        // שימוש ב-BaseActivity להגדרת NavBar אחיד
        setupBottomNavigation(bottomNavigationView, R.id.nav_profile);
    }
}
