package com.example.focusflow_beta;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvMessage, tvRegister;
    private CheckBox cbRememberMe;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvMessage = findViewById(R.id.tvMessage);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        tvRegister = findViewById(R.id.tvRegister);

        prefs = getSharedPreferences("FocusFlowPrefs", MODE_PRIVATE);

        // בדיקה אם המשתמש כבר שמור
        if (prefs.getBoolean("rememberMe", false)) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
            return;
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    tvMessage.setText("אנא מלא את כל השדות!");
                    tvMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if (email.equals("1") && password.equals("1")) {
                    tvMessage.setText("התחברת בהצלחה!");
                    tvMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                    if (cbRememberMe.isChecked()) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("rememberMe", true);
                        editor.apply();
                    }

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    tvMessage.setText("אימייל או סיסמה שגויים.");
                    tvMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }
        });

        // מעבר לעמוד הרשמה
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
