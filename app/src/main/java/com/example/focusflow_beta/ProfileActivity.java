package com.example.focusflow_beta;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ProfileActivity extends AppCompatActivity {

    private Button btnBackProfile;
    private TextView tvProfileInfo, tvProfileTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        btnBackProfile = findViewById(R.id.btnBackProfile);
        tvProfileInfo = findViewById(R.id.tvProfileInfo);
        tvProfileTitle = findViewById(R.id.tvProfileTitle);

        btnBackProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // חזרה לעמוד הקודם
            }
        });
    }
}
