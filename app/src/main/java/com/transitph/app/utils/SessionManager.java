package com.transitph.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.transitph.app.models.User;

public class SessionManager {
    private static final String PREF_NAME = "TransitPH_Session";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_ACCESS_TOKEN = "supabaseAccessToken";
    private static final String KEY_REMOTE_USER_ID = "supabaseUserId";
    private static final String KEY_SELECTED_LANGUAGE = "selectedLanguage"; // "en", "fil", "bikol"

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, user.getId());
        editor.putString(KEY_FULL_NAME, user.getFullName());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_ROLE, user.getRole());
        editor.apply();
    }

    public void setSupabaseSession(String accessToken, String remoteUserId) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REMOTE_USER_ID, remoteUserId);
        editor.apply();
    }

    public String getAccessToken() {
        return pref.getString(KEY_ACCESS_TOKEN, "");
    }

    public String getRemoteUserId() {
        return pref.getString(KEY_REMOTE_USER_ID, "");
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public long getUserId() {
        return pref.getLong(KEY_USER_ID, -1);
    }

    public String getFullName() {
        return pref.getString(KEY_FULL_NAME, "Commuter");
    }

    public String getUserFullName() {
        return getFullName();
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getRole() {
        return pref.getString(KEY_ROLE, "USER");
    }

    public String getUserRole() {
        return getRole();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getRole());
    }

    public void setSelectedLanguage(String lang) {
        editor.putString(KEY_SELECTED_LANGUAGE, lang);
        editor.apply();
    }

    public String getSelectedLanguage() {
        return pref.getString(KEY_SELECTED_LANGUAGE, "en");
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();
    }
}
