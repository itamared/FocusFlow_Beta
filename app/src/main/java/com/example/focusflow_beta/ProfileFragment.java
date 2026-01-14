package com.example.focusflow_beta;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvGreeting, tvOccupation, tvWorkHours, tvBreakCount;
    private Button btnLogout, btnSettings;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvOccupation = view.findViewById(R.id.tvOccupation);
        tvWorkHours = view.findViewById(R.id.tvWorkHours);
        tvBreakCount = view.findViewById(R.id.tvBreakCount);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSettings = view.findViewById(R.id.btnSettings);
        progressBar = view.findViewById(R.id.progressBarProfile);

        // אם אין משתמש מחובר – נשלח ל-Login
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return view;
        }

        loadUsername();
        loadUserData();

        btnSettings.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    // שליפת שם המשתמש מ-collection "usernames"
    private void loadUsername() {
        progressBar.setVisibility(View.VISIBLE);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("usernames").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String username = doc.getString("username");
                    tvGreeting.setText("שלום, " + (username != null ? username : "משתמש"));
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    tvGreeting.setText("שלום, משתמש");
                    progressBar.setVisibility(View.GONE);
                });
    }

    // שליפת יתר נתוני המשתמש מ-"users"
    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(this::populateProfile)
                .addOnFailureListener(e -> {
                    tvOccupation.setText("עיסוק: -");
                    tvWorkHours.setText("שעות פעילות: ? - ?");
                    tvBreakCount.setText("מספר הפסקות: 0");
                });
    }

    private void populateProfile(DocumentSnapshot doc) {
        if (doc.exists()) {
            String occupation = doc.getString("occupation");
            String startTime = doc.getString("startTime");
            String endTime = doc.getString("endTime");
            Long breakCount = doc.getLong("breakCount");

            tvOccupation.setText("עיסוק: " + (occupation != null ? occupation : "-"));
            tvWorkHours.setText("שעות פעילות: " +
                    (startTime != null ? startTime : "?") + " - " + (endTime != null ? endTime : "?"));
            tvBreakCount.setText("מספר הפסקות: " + (breakCount != null ? breakCount : 0));
        }
    }
}
