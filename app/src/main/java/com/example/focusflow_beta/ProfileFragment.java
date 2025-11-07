package com.example.focusflow_beta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private TextView tvProfileTitle, tvProfileInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileTitle = view.findViewById(R.id.tvProfileTitle);
        tvProfileInfo = view.findViewById(R.id.tvProfileInfo);

        // כאן אפשר להוסיף קוד לטעינת פרטי המשתמש

        return view;
    }
}
