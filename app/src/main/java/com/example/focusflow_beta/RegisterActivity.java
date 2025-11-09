package com.example.focusflow_beta;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirm, etUsername;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirmPassword);
        etUsername = findViewById(R.id.etUsername);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty() || confirm.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "אנא מלא את כל השדות!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirm)) {
            Toast.makeText(this, "הסיסמאות אינן תואמות!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();

                        // שמירה ב-Collection "users"
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email);
                        userData.put("username", username);

                        db.collection("users").document(uid)
                                .set(userData)
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "שגיאה בשמירת הנתונים ב-users: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );

                        // שמירה ב-Collection חדש "usernames"
                        Map<String, Object> usernameData = new HashMap<>();
                        usernameData.put("username", username);

                        db.collection("usernames").document(uid)
                                .set(usernameData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "הרשמה הושלמה בהצלחה!", Toast.LENGTH_LONG).show();
                                    // מעבר לשאלון הגדרת המשתמש
                                    Intent intent = new Intent(RegisterActivity.this, SetupActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "שגיאה בשמירת שם המשתמש ב-usernames: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );

                    } else {
                        Toast.makeText(this, "שגיאה בהרשמה: " + (task.getException() != null ? task.getException().getMessage() : "לא ידוע"), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
