package com.example.focusflow_beta;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

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

        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(this,
                                "שגיאה בהרשמה: " + (task.getException() != null ? task.getException().getMessage() : "לא ידוע"),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    AuthResult result = task.getResult();
                    FirebaseUser user = (result != null) ? result.getUser() : null;

                    if (user == null) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(this, "שגיאה: המשתמש לא נוצר (user=null)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String uid = user.getUid();

                    // 1) users/{uid}
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", email);
                    userData.put("username", username);

                    // 2) usernames/{uid}
                    Map<String, Object> usernameData = new HashMap<>();
                    usernameData.put("username", username);

                    // כתיבה אחת שמבצעת את שתי השמירות יחד
                    WriteBatch batch = db.batch();
                    batch.set(db.collection("users").document(uid), userData);
                    batch.set(db.collection("usernames").document(uid), usernameData);

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "הרשמה הושלמה בהצלחה!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(RegisterActivity.this, SetupActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                Toast.makeText(this, "שגיאה בשמירת הנתונים ב-Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                });
    }
}
