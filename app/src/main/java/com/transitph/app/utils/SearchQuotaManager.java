package com.transitph.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class SearchQuotaManager {
    public static final int DAILY_FREE_SEARCH_LIMIT = 5;
    private static final String PREFS = "TransitPH_SearchQuota";
    private static final Object LOCK = new Object();
    private SearchQuotaManager() {}
    private static String accountKey(SessionManager session) {
        String remote = session.getRemoteUserId();
        return (remote == null || remote.isEmpty()) ? "local_" + session.getUserId() : "remote_" + remote;
    }
    private static String today() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        return format.format(new Date());
    }
    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    private static void rollDay(SharedPreferences p, String key) {
        if (!today().equals(p.getString(key + "_date", ""))) {
            p.edit().putString(key + "_date", today()).putInt(key + "_used", 0).putInt(key + "_ad_credits", 0).apply();
        }
    }
    public static int remaining(Context context, SessionManager session) {
        if (session.isPremiumActive()) return Integer.MAX_VALUE;
        synchronized (LOCK) {
            SharedPreferences p = prefs(context);
            String key = accountKey(session);
            rollDay(p, key);
            return Math.max(0, DAILY_FREE_SEARCH_LIMIT - p.getInt(key + "_used", 0)) + Math.max(0, p.getInt(key + "_ad_credits", 0));
        }
    }
    public static boolean tryConsume(Context context, SessionManager session) {
        if (session.isPremiumActive()) return true;
        synchronized (LOCK) {
            SharedPreferences p = prefs(context);
            String key = accountKey(session);
            rollDay(p, key);
            int used = p.getInt(key + "_used", 0);
            int bonus = p.getInt(key + "_ad_credits", 0);
            if (used < DAILY_FREE_SEARCH_LIMIT) {
                p.edit().putInt(key + "_used", used + 1).apply();
                return true;
            }
            if (bonus > 0) {
                p.edit().putInt(key + "_ad_credits", bonus - 1).apply();
                return true;
            }
            return false;
        }
    }
    public static void addRewardedSearch(Context context, SessionManager session) {
        synchronized (LOCK) {
            SharedPreferences p = prefs(context);
            String key = accountKey(session);
            rollDay(p, key);
            p.edit().putInt(key + "_ad_credits", p.getInt(key + "_ad_credits", 0) + 1).apply();
        }
    }
}