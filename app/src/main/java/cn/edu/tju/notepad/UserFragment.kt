package cn.edu.tju.notepad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class UserFragment extends Fragment {

    private TextView textViewUsername;
    private TextView textViewEmail;
    private Button buttonSettings;
    private Button buttonAbout;
    private SharedPreferences sharedPreferences;

    public UserFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_user, container, false);

        // Initialize shared preferences
        sharedPreferences = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        // Initialize views
        textViewUsername = rootView.findViewById(R.id.textViewUsername);
        textViewEmail = rootView.findViewById(R.id.textViewEmail);
        buttonSettings = rootView.findViewById(R.id.buttonSettings);
        buttonAbout = rootView.findViewById(R.id.buttonAbout);

        // Load user data
        loadUserData();

        // Set up click listeners
        setupClickListeners();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data
        loadUserData();
    }

    private void loadUserData() {
        // Get stored user data, or use defaults if not available
        String username = sharedPreferences.getString("username", "未设置用户名");
        String email = sharedPreferences.getString("email", "未设置邮箱");

        // Update UI
        textViewUsername.setText(username);
        textViewEmail.setText(email);
    }

    private void setupClickListeners() {
        // Settings button click listener
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserSettingsDialog();
            }
        });

        // About button click listener
        buttonAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        });
    }

    private void showUserSettingsDialog() {
        // Create dialog for editing user information
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("个人信息设置");

        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_user_settings, null);
        builder.setView(dialogView);

        // Get dialog views
        EditText editTextUsername = dialogView.findViewById(R.id.editTextDialogUsername);
        EditText editTextEmail = dialogView.findViewById(R.id.editTextDialogEmail);

        // Set current values
        editTextUsername.setText(sharedPreferences.getString("username", ""));
        editTextEmail.setText(sharedPreferences.getString("email", ""));

        // Set up buttons
        builder.setPositiveButton("保存", (dialog, which) -> {
            // Get input values
            String username = editTextUsername.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();

            // Validate
            if (username.isEmpty()) {
                Toast.makeText(getActivity(), "用户名不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to shared preferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", username);
            editor.putString("email", email);
            editor.apply();

            // Update UI
            loadUserData();

            Toast.makeText(getActivity(), "保存成功", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("取消", null);

        // Show dialog
        builder.create().show();
    }

    private void showAboutDialog() {
        // Show app information
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("关于应用");
        builder.setMessage("记事本应用\n版本: 1.0.0\n开发者: TJU");
        builder.setPositiveButton("确定", null);
        builder.create().show();
    }
}