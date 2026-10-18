package com.transitph.app.services;

import android.content.Context;
import com.transitph.app.database.UserDao;
import com.transitph.app.models.User;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.transitph.app.supabase.SupabaseClient;
import com.transitph.app.supabase.SupabaseClient.SupabaseException;
import com.transitph.app.utils.SessionManager;

public class AuthenticationService {
    private final UserDao userDao;
    private final SessionManager sessionManager;

    public AuthenticationService(Context context) {
        this.userDao = new UserDao(context);
        this.sessionManager = new SessionManager(context);
    }

    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final User user;

        public AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
    }

    /**
     * Signs in with Supabase Auth (same accounts as the web app). Falls back to the local
     * database only when there is no internet connection. BLOCKING - call from a background thread.
     */
    public AuthResult login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return new AuthResult(false, "Please enter your email and password.", null);
        }
        email = email.trim();

        if (SupabaseClient.isConfigured()) {
            try {
                JsonObject session = SupabaseClient.signIn(email, password);
                String token = SupabaseClient.str(session, "", "access_token");
                JsonObject remoteUser = session.has("user") && session.get("user").isJsonObject()
                        ? session.getAsJsonObject("user") : new JsonObject();
                String remoteId = SupabaseClient.str(remoteUser, "", "id");
                if (token.isEmpty() || remoteId.isEmpty()) {
                    return new AuthResult(false, "Login failed. Please try again.", null);
                }

                String fullName = "";
                String role = "USER";
                String premiumUntil = "";
                try {
                    JsonArray profile = SupabaseClient.select("profiles", "select=full_name,role,premium_until&id=eq." + remoteId, token);
                    if (profile.size() > 0 && profile.get(0).isJsonObject()) {
                        JsonObject p = profile.get(0).getAsJsonObject();
                        fullName = SupabaseClient.str(p, "", "full_name");
                        role = SupabaseClient.str(p, "USER", "role");
                        premiumUntil = SupabaseClient.str(p, "", "premium_until");
                    } else {
                        // Profile row missing (e.g. email confirmation was required at sign-up): create it now.
                        String metaName = "";
                        if (remoteUser.has("user_metadata") && remoteUser.get("user_metadata").isJsonObject()) {
                            metaName = SupabaseClient.str(remoteUser.getAsJsonObject("user_metadata"), "", "full_name");
                        }
                        fullName = metaName;
                        JsonObject row = new JsonObject();
                        row.addProperty("id", remoteId);
                        row.addProperty("full_name", metaName.isEmpty() ? "New User" : metaName);
                        row.addProperty("role", "USER");
                        try {
                            SupabaseClient.insert("profiles", row, token);
                        } catch (SupabaseException ignored) {
                            // not fatal
                        }
                    }
                } catch (SupabaseException ignored) {
                    // profile unavailable - continue with defaults
                }
                if (fullName.isEmpty()) fullName = "New User";

                long localId = userDao.upsertRemoteUser(remoteId, fullName, email, role);
                User user = userDao.getUserById(localId);
                if (user == null) {
                    return new AuthResult(false, "Login failed. Please try again.", null);
                }
                sessionManager.createLoginSession(user);
                sessionManager.setSupabaseSession(token, remoteId);
                sessionManager.setPremiumUntil(premiumUntil);
                return new AuthResult(true, "Login successful.", user);
            } catch (SupabaseException e) {
                if (!e.isNetworkError()) {
                    String msg = e.getCode() == 400 ? "Invalid email or password." : e.getMessage();
                    return new AuthResult(false, msg, null);
                }
                // network error -> try the offline copy below
            }
        }

        User user = userDao.authenticate(email, password);
        if (user == null) {
            return new AuthResult(false, SupabaseClient.isConfigured()
                    ? "No internet connection. Connect to sign in."
                    : "Invalid email or password.", null);
        }
        sessionManager.createLoginSession(user);
        return new AuthResult(true, "Offline mode: signed in with the saved account.", user);
    }

    public AuthResult register(String fullName, String email, String password, String confirmPassword) {
        if (fullName == null || fullName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return new AuthResult(false, "All fields are required.", null);
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return new AuthResult(false, "Please enter a valid email address.", null);
        }

        if (password.length() < 6) {
            return new AuthResult(false, "Password must be at least 6 characters long.", null);
        }

        if (!password.equals(confirmPassword)) {
            return new AuthResult(false, "Passwords do not match.", null);
        }

        if (SupabaseClient.isConfigured()) {
            try {
                JsonObject res = SupabaseClient.signUp(email.trim(), password, fullName.trim());
                JsonObject remoteUser = res.has("user") && res.get("user").isJsonObject() ? res.getAsJsonObject("user") : res;
                String remoteId = SupabaseClient.str(remoteUser, "", "id");
                String token = SupabaseClient.str(res, "", "access_token");
                if (!token.isEmpty() && !remoteId.isEmpty()) {
                    JsonObject row = new JsonObject();
                    row.addProperty("id", remoteId);
                    row.addProperty("full_name", fullName.trim());
                    row.addProperty("role", "USER");
                    try {
                        SupabaseClient.insert("profiles", row, token);
                    } catch (SupabaseException ignored) {
                        // profile is also created on first login
                    }
                    return new AuthResult(true, "Account created successfully.", null);
                }
                return new AuthResult(true, "Account created. Check your email to confirm it, then log in.", null);
            } catch (SupabaseException e) {
                if (e.isNetworkError()) {
                    return new AuthResult(false, "No internet connection. Connect to create an account.", null);
                }
                return new AuthResult(false, e.getMessage(), null);
            }
        }
        if (userDao.isEmailTaken(email.trim())) {
            return new AuthResult(false, "An account with this email already exists.", null);
        }

        long userId = userDao.registerUser(fullName.trim(), email.trim(), password, "USER");
        if (userId > 0) {
            User newUser = userDao.getUserById(userId);
            return new AuthResult(true, "Account created successfully.", newUser);
        } else {
            return new AuthResult(false, "Registration failed. Please try again.", null);
        }
    }

    public void logout() {
        sessionManager.logoutUser();
    }
}
