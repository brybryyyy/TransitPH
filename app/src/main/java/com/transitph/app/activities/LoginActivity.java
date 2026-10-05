package com.transitph.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.transitph.app.R;
import com.transitph.app.services.AuthenticationService;
import com.transitph.app.supabase.SupabaseClient;
import com.transitph.app.supabase.SupabaseSync;
import com.transitph.app.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegisterPrompt;
    private Button btnDemoUser;
    private Button btnDemoAdmin;

    private AuthenticationService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        authService = new AuthenticationService(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterPrompt = findViewById(R.id.tv_register_prompt);
        btnDemoUser = findViewById(R.id.btn_demo_user);
        btnDemoAdmin = findViewById(R.id.btn_demo_admin);
        // Real Supabase accounts are used now (same as the web app), so hide the offline demo fillers.
        if (SupabaseClient.isConfigured()) {
            View label = findViewById(R.id.tv_demo_accounts_label);
            if (label != null) label.setVisibility(View.GONE);
            if (btnDemoUser != null) ((View) btnDemoUser.getParent()).setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());

        tvRegisterPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        if (btnDemoUser != null) {
            btnDemoUser.setOnClickListener(v -> {
                etEmail.setText("user@transitph.test");
                etPassword.setText("User123!");
            });
        }

        if (btnDemoAdmin != null) {
            btnDemoAdmin.setOnClickListener(v -> {
                etEmail.setText("admin@transitph.test");
                etPassword.setText("Admin123!");
            });
        }
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in...");
        final Context appContext = getApplicationContext();
        new Thread(() -> {
            AuthenticationService.AuthResult result = authService.login(email, password);
            if (result.isSuccess()) {
                SupabaseSync.pullAll(appContext); // load the shared terminals/routes/saved routes
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.login_button);
                if (result.isSuccess()) {
                    Toast.makeText(this, "Welcome back, " + result.getUser().getFullName() + "!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                } else {
                    Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
