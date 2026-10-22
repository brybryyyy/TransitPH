package com.transitph.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.transitph.app.models.Route;
import com.transitph.app.models.SavedRoute;

import java.util.Locale;

/** Persists the commuter's fare display choice and formats the fare consistently. */
public final class RouteFarePreference {
    private static final String PREFS = "route_fare_preferences";
    private static final String KEY_DISCOUNTED = "discounted_fare_selected";
    private static final double DISCOUNT_RATE = 0.20;

    private RouteFarePreference() { }

    public interface OnFareOptionChangedListener {
        void onFareOptionChanged(boolean discounted);
    }

    public static boolean isDiscounted(Context context) {
        return preferences(context).getBoolean(KEY_DISCOUNTED, false);
    }

    public static double displayedFare(Context context, double regularFare) {
        return isDiscounted(context) ? regularFare * (1.0 - DISCOUNT_RATE) : regularFare;
    }

    public static String formatFare(Context context, double regularFare) {
        return String.format(Locale.US, "₱%.2f", displayedFare(context, regularFare));
    }

    public static String formatFare(Context context, Route route) {
        return formatFare(context, route == null ? 0 : route.getFare());
    }

    public static String formatFare(Context context, SavedRoute route) {
        return formatFare(context, route == null ? 0 : route.getFare());
    }

    public static void setup(Context context, ChipGroup group, OnFareOptionChangedListener listener) {
        group.removeAllViews();
        int regularId = View.generateViewId();
        addChip(context, group, "Regular", regularId);
        addChip(context, group, "PWD / Student · 20% off", View.generateViewId());
        group.check(isDiscounted(context) ? group.getChildAt(1).getId() : regularId);
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            boolean discounted = checkedIds.get(0) != regularId;
            preferences(context).edit().putBoolean(KEY_DISCOUNTED, discounted).apply();
            if (listener != null) listener.onFareOptionChanged(discounted);
        });
    }

    private static void addChip(Context context, ChipGroup group, String label, int id) {
        Chip chip = new Chip(context);
        chip.setId(id);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCheckedIconVisible(false);
        chip.setEnsureMinTouchTargetSize(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(context, 6), 0);
        group.addView(chip, params);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
