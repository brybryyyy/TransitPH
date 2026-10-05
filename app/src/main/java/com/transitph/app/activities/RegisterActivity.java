package com.transitph.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.transitph.app.R;
import com.transitph.app.services.AuthenticationService;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private Button btnBackToLogin;

    private AuthenticationService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = new AuthenticationService(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.et_register_full_name);
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);
        btnRegister = findViewById(R.id.btn_register_create_account);
        btnBackToLogin = findViewById(R.id.btn_register_back_to_login);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());
        btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        btnRegister.setEnabled(false);
        new Thread(() -> {
            AuthenticationService.AuthResult result = authService.register(fullName, email, password, confirmPassword);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                btnRegister.setEnabled(true);
                if (result.isSuccess()) {
                    Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
