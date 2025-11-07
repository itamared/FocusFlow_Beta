package com.example.focusflow_beta;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvMessage, tvRegister;
    private CheckBox cbRememberMe;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvMessage = findViewById(R.id.tvMessage);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        tvRegister = findViewById(R.id.tvRegister);

        prefs = getSharedPreferences("FocusFlowPrefs", MODE_PRIVATE);

        // בדיקה אם המשתמש כבר מחובר
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                tvMessage.setText("אנא מלא את כל השדות!");
                tvMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                tvMessage.setText("התחברת בהצלחה!");
                                tvMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                                if (cbRememberMe.isChecked()) {
                                    prefs.edit().putBoolean("rememberMe", true).apply();
                                }

                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                finish();
                            } else {
                                tvMessage.setText("שגיאה: " + task.getException().getMessage());
                                tvMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            }
                        });
            }
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}
