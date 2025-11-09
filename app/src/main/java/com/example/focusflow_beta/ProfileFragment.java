package com.example.focusflow_beta;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private TextView tvProfileTitle, tvProfileInfo;
    private Button btnLogout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileTitle = view.findViewById(R.id.tvProfileTitle);
        tvProfileInfo = view.findViewById(R.id.tvProfileInfo);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            // התנתקות מ-Firebase
            FirebaseAuth.getInstance().signOut();

            // מעבר ל-LoginActivity והסרת כל ה-Back Stack
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });



        return view;
    }
}
