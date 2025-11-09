package com.example.focusflow_beta;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.focusflow_beta.oldactivities.HomeActivity;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private SignInButton btnGoogleSignIn;
    private TextView tvMessage, tvRegister;
    private CheckBox cbRememberMe;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("FocusFlowPrefs", MODE_PRIVATE);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvMessage = findViewById(R.id.tvMessage);
        tvRegister = findViewById(R.id.tvRegister);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        // בדיקה אם המשתמש כבר מחובר
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setupGoogleSignIn();

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("אנא מלא את כל השדות!", false);
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (cbRememberMe.isChecked()) {
                            prefs.edit().putBoolean("rememberMe", true).apply();
                        }
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        showMessage("שגיאה: " + (task.getException() != null ? task.getException().getMessage() : "לא ידוע"), false);
                    }
                });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // ודא שיש לך את המזהה הנכון ב-res/values/strings.xml
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                showMessage("Google sign-in failed: " + e.getMessage(), false);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        handleGoogleUser(mAuth.getCurrentUser(), account);
                    } else {
                        showMessage("Authentication Failed.", false);
                    }
                });
    }

    private void handleGoogleUser(FirebaseUser firebaseUser, GoogleSignInAccount account) {
        if (firebaseUser == null) return;
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        String displayName = account.getDisplayName();

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                // משתמש חדש → צור שם משתמש אוטומטי
                String username = generateUsername(displayName, email);

                Map<String, Object> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("username", username);
                userData.put("setupCompleted", false);

                db.collection("users").document(uid).set(userData);
                db.collection("usernames").document(uid).set(new HashMap<String, Object>() {{
                    put("username", username);
                }});

                startActivity(new Intent(LoginActivity.this, SetupActivity.class));
                finish();
            } else {
                Boolean setupCompleted = documentSnapshot.getBoolean("setupCompleted");
                if (setupCompleted != null && setupCompleted) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, SetupActivity.class));
                }
                finish();
            }
        });
    }

    private String generateUsername(String displayName, String email) {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName.replaceAll("\\s+", "").toLowerCase() + (int)(Math.random()*1000);
        } else if (email != null && !email.isEmpty()) {
            return email.split("@")[0] + (int)(Math.random()*1000);
        } else {
            return "user" + (int)(Math.random()*10000);
        }
    }

    private void showMessage(String msg, boolean success) {
        tvMessage.setText(msg);
        tvMessage.setTextColor(getResources().getColor(success ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
    }
}
