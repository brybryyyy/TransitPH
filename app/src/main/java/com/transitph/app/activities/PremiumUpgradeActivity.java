package com.transitph.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.transitph.app.R;
import com.transitph.app.supabase.SupabaseClient;
import com.transitph.app.utils.SearchQuotaManager;
import com.transitph.app.utils.SessionManager;

public class PremiumUpgradeActivity extends AppCompatActivity {
    private static final int MAX_PAYMENT_CHECKS = 30;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SessionManager sessionManager;
    private TextView tvCurrentTier;
    private Button btnSubscribe;
    private boolean checkoutOpening;
    private boolean checkingPaymentReturn;
    private boolean paymentSuccessReturn;
    private int paymentChecks;
    private boolean hasShownPaymentSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_upgrade);
        sessionManager = new SessionManager(this);

        ImageButton btnBack = findViewById(R.id.btn_back_upgrade);
        btnBack.setOnClickListener(v -> finish());
        tvCurrentTier = findViewById(R.id.tv_current_tier);
        btnSubscribe = findViewById(R.id.btn_subscribe_monthly);
        btnSubscribe.setOnClickListener(v -> startCheckout());
        handleReturnIntent(getIntent());
        updateStatusText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPremiumStatus();
        if (checkingPaymentReturn) checkPaymentAgain();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleReturnIntent(intent);
        refreshPremiumStatus();
    }

    private void handleReturnIntent(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data != null && "transitph".equals(data.getScheme()) && "payment-return".equals(data.getHost())) {
            paymentSuccessReturn = "success".equalsIgnoreCase(data.getQueryParameter("result"));
            if (paymentSuccessReturn) {
                checkingPaymentReturn = true;
                paymentChecks = 0;
                hasShownPaymentSuccess = false;
            }
        }
    }

    private void refreshPremiumStatus() {
        updateStatusText();
        String remoteId = sessionManager.getRemoteUserId();
        String token = sessionManager.getAccessToken();
        if (remoteId.isEmpty() || token.isEmpty() || !SupabaseClient.isConfigured()) return;
        new Thread(() -> {
            String premiumUntil = "";
            try {
                JsonArray rows = SupabaseClient.select("profiles",
                        "select=premium_until&id=eq." + Uri.encode(remoteId), token);
                if (rows.size() > 0 && rows.get(0).isJsonObject()) {
                    premiumUntil = SupabaseClient.str(rows.get(0).getAsJsonObject(), "", "premium_until");
                }
            } catch (Exception ignored) {
                // Keep the last known entitlement if temporarily offline.
            }
            String result = premiumUntil;
            runOnUiThread(() -> {
                sessionManager.setPremiumUntil(result);
                updateStatusText();
                if (sessionManager.isPremiumActive()) {
                    checkingPaymentReturn = false;
                    if (!hasShownPaymentSuccess && paymentSuccessReturn) {
                        hasShownPaymentSuccess = true;
                        Toast.makeText(this, "Payment confirmed. Your Premium benefits are active.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }).start();
    }

    private void checkPaymentAgain() {
        if (!checkingPaymentReturn || !paymentSuccessReturn || isFinishing()) return;
        if (sessionManager.isPremiumActive()) {
            checkingPaymentReturn = false;
            return;
        }
        if (paymentChecks++ >= MAX_PAYMENT_CHECKS) {
            checkingPaymentReturn = false;
            tvCurrentTier.setText("Payment is still being confirmed. Your Premium benefits will appear here as soon as PayMongo confirms it.");
            return;
        }
        handler.postDelayed(() -> {
            refreshPremiumStatus();
            checkPaymentAgain();
        }, 2000);
    }

    private void updateStatusText() {
        if (tvCurrentTier == null || sessionManager == null) return;
        if (sessionManager.isPremiumActive()) {
            tvCurrentTier.setText("⭐ Premium active — unlimited searches. Valid until " + sessionManager.getPremiumUntil());
            if (btnSubscribe != null) {
                btnSubscribe.setText("Premium Active");
                btnSubscribe.setEnabled(false);
            }
        } else {
            int remaining = SearchQuotaManager.remaining(this, sessionManager);
            tvCurrentTier.setText("Free plan — " + remaining + "/5 searches remaining today.");
            if (btnSubscribe != null) {
                btnSubscribe.setText("Continue to PayMongo checkout · ₱49");
                btnSubscribe.setEnabled(true);
            }
        }
    }

    private void startCheckout() {
        if (checkoutOpening) return;
        String token = sessionManager.getAccessToken();
        if (sessionManager.getRemoteUserId().isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "Please sign in to your Supabase account before subscribing.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!SupabaseClient.isConfigured()) {
            Toast.makeText(this, "Supabase is not configured for this build.", Toast.LENGTH_LONG).show();
            return;
        }

        checkoutOpening = true;
        btnSubscribe.setEnabled(false);
        btnSubscribe.setText("Connecting to secure checkout...");
        new Thread(() -> {
            String checkoutUrl = "";
            String error = "";
            try {
                JsonObject result = SupabaseClient.invokeFunction("create-paymongo-checkout", new JsonObject(), token);
                checkoutUrl = SupabaseClient.str(result, "", "checkoutUrl", "checkout_url");
                if (checkoutUrl.isEmpty()) error = "Checkout could not be started. Please try again.";
            } catch (Exception e) {
                error = e.getMessage() == null ? "Could not connect to checkout." : e.getMessage();
            }
            String finalUrl = checkoutUrl;
            String finalError = error;
            runOnUiThread(() -> {
                checkoutOpening = false;
                updateStatusText();
                if (finalUrl.isEmpty()) {
                    Toast.makeText(this, finalError, Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)));
                } catch (Exception e) {
                    Toast.makeText(this, "No browser is available to open PayMongo checkout.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}
