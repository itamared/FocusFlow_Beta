package com.example.focusflow_beta;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
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

        setupGoogleSignIn();

        // Auto-login רק אם rememberMe=true
        boolean remember = prefs.getBoolean("rememberMe", false);
        if (mAuth.getCurrentUser() != null) {
            if (remember) {
                routeUserAfterLogin(mAuth.getCurrentUser());
                return;
            } else {
                // לא בחר "זכור אותי" → לא מאפשרים Auto-login
                hardSignOut();
            }
        }

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

                        // ✅ תמיד שומרים את הבחירה (true/false)
                        prefs.edit().putBoolean("rememberMe", cbRememberMe.isChecked()).apply();

                        routeUserAfterLogin(mAuth.getCurrentUser());
                    } else {
                        showMessage("שגיאה: " +
                                        (task.getException() != null ? task.getException().getMessage() : "לא ידוע"),
                                false);
                    }
                });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
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

                        // ✅ תמיד שומרים את הבחירה (true/false)
                        prefs.edit().putBoolean("rememberMe", cbRememberMe.isChecked()).apply();

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
        String displayName = (account != null) ? account.getDisplayName() : null;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {

            // משתמש חדש לגמרי
            if (!doc.exists()) {
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
                return;
            }

            // משתמש קיים -> ננתב לפי נתונים
            routeUserAfterLogin(firebaseUser);

        }).addOnFailureListener(e -> showMessage("שגיאה בקריאת משתמש: " + e.getMessage(), false));
    }

    private void routeUserAfterLogin(FirebaseUser user) {
        if (user == null) return;

        String uid = user.getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {

            if (!doc.exists()) {
                startActivity(new Intent(LoginActivity.this, SetupActivity.class));
                finish();
                return;
            }

            String occupation = doc.getString("occupation");
            String startTime = doc.getString("startTime");
            String endTime = doc.getString("endTime");
            Object breakTimesObj = doc.get("breakTimes");

            boolean hasBreakTimes =
                    breakTimesObj instanceof java.util.List &&
                            !((java.util.List<?>) breakTimesObj).isEmpty();

            boolean hasSetupData =
                    occupation != null && !occupation.trim().isEmpty() &&
                            startTime != null && !startTime.trim().isEmpty() &&
                            endTime != null && !endTime.trim().isEmpty() &&
                            hasBreakTimes;

            if (hasSetupData) {
                db.collection("users").document(uid).update("setupCompleted", true);
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(LoginActivity.this, SetupActivity.class));
            }

            finish();
        }).addOnFailureListener(e -> showMessage("שגיאה בטעינת נתוני משתמש: " + e.getMessage(), false));
    }

    private void hardSignOut() {
        try { mAuth.signOut(); } catch (Exception ignored) {}
        try {
            GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut();
        } catch (Exception ignored) {}
    }

    private String generateUsername(String displayName, String email) {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName.replaceAll("\\s+", "").toLowerCase() + (int) (Math.random() * 1000);
        } else if (email != null && !email.isEmpty()) {
            return email.split("@")[0] + (int) (Math.random() * 1000);
        } else {
            return "user" + (int) (Math.random() * 10000);
        }
    }

    private void showMessage(String msg, boolean success) {
        tvMessage.setText(msg);
        tvMessage.setTextColor(getResources().getColor(
                success ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
        ));
    }
}
