package com.example.poc_concept;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.poc_concept.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if it came here due to failed verification
        Bundle args = getArguments();
        boolean isVerified;

        if (args != null && args.containsKey("verified_result")) {
            isVerified = args.getBoolean("verified_result", true);
        } else {
            isVerified = true;
        }

        if (!isVerified) {
            showErrorDialog(); // Show the error once fragment loads
        }

        binding.NextButton.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );



    }

    private void showErrorDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Integrity Check Failed")
                .setMessage("This app could not verify its integrity. Please reinstall from Google Play.")
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}