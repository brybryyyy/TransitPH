package com.transitph.app.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.transitph.app.models.Route;
import com.transitph.app.models.SavedRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared transport filters for every route list in the app. */
public final class RouteTransportFilter {
    public static final String ALL = "All";
    private static final String[] FILTERS = {ALL, "Jeepney", "UV Express", "Bus"};

    private RouteTransportFilter() { }

    public interface OnFilterChangedListener {
        void onFilterChanged(String filter);
    }

    public static void setup(Context context, ChipGroup group, OnFilterChangedListener listener) {
        group.removeAllViews();
        int checkedId = View.generateViewId();
        for (String label : FILTERS) {
            Chip chip = new Chip(context);
            chip.setId(label.equals(ALL) ? checkedId : View.generateViewId());
            chip.setText(label);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setCheckedIconVisible(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setMinHeight(dp(context, 36));
            chip.setChipCornerRadius(dp(context, 18));
            chip.setChipStrokeWidth(dp(context, 1));
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.rgb(220, 231, 227)));
            chip.setChipBackgroundColor(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.rgb(0, 150, 110), Color.rgb(242, 247, 245)}));
            chip.setTextColor(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.WHITE, Color.rgb(55, 75, 68)}));
            chip.setRippleColor(ColorStateList.valueOf(Color.rgb(207, 240, 229)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dp(context, 6), 0);
            group.addView(chip, params);
        }
        group.check(checkedId);
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (listener == null || checkedIds.isEmpty()) return;
            Chip checked = chipGroup.findViewById(checkedIds.get(0));
            if (checked != null) listener.onFilterChanged(checked.getText().toString());
        });
    }

    public static List<Route> filterRoutes(List<Route> routes, String filter) {
        List<Route> filtered = new ArrayList<>();
        if (routes == null) return filtered;
        for (Route route : routes) {
            if (route != null && matches(filter, route.getTransportType(), route.getRouteName())) filtered.add(route);
        }
        return filtered;
    }

    public static List<SavedRoute> filterSavedRoutes(List<SavedRoute> routes, String filter) {
        List<SavedRoute> filtered = new ArrayList<>();
        if (routes == null) return filtered;
        for (SavedRoute route : routes) {
            if (route != null && matches(filter, route.getTransportType(), route.getRouteName())) filtered.add(route);
        }
        return filtered;
    }

    public static boolean matches(String filter, String transportType, String routeName) {
        if (filter == null || ALL.equalsIgnoreCase(filter)) return true;
        String type = transportType == null ? "" : transportType.toLowerCase(Locale.ROOT);
        String name = routeName == null ? "" : routeName.toLowerCase(Locale.ROOT);
        switch (filter.toLowerCase(Locale.ROOT)) {
            case "jeepney":
                return type.contains("jeep");
            case "uv express":
                // In TransitPH, "Van" and "UV Express" describe the same filter category.
                return type.contains("uv") || type.contains("van") ||
                        name.matches(".*\\b(uv express|van)\\b.*");
            case "bus":
                return type.contains("bus");
            default:
                return true;
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
