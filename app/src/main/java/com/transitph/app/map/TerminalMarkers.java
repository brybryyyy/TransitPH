package com.transitph.app.map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import com.transitph.app.models.Terminal;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;

/**
 * Draws terminals as small orange dots.
 *
 * Only terminals that have at least one route are drawn. The ~315 rows imported from
 * terminals.csv have placeholder coordinates laid out on a regular grid (some land in
 * Laguna de Bay), so they must not be plotted until their coordinates are corrected.
 */
public final class TerminalMarkers {

    private TerminalMarkers() {}

    public static void addTo(MapView map, List<Terminal> terminals) {
        Context ctx = map.getContext();
        float d = ctx.getResources().getDisplayMetrics().density;
        int px = (int) (14 * d);

        for (Terminal t : terminals) {
            if (!hasUsableMapCoordinate(t)) continue;
            if (t.getRouteCount() <= 0) continue;

            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(Color.parseColor("#F57C00"));
            dot.setStroke((int) (2 * d), Color.WHITE);
            dot.setSize(px, px);
            dot.setBounds(0, 0, px, px);

            Marker m = new Marker(map);
            m.setPosition(new GeoPoint(t.getLatitude(), t.getLongitude()));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            m.setTitle(t.getTerminalName());
            m.setSnippet(t.getCity() + ", " + t.getProvince() + "\n" + t.getRouteCount() + " Routes");
            m.setIcon(dot);
            map.getOverlays().add(m);
        }
        map.invalidate();
    }

    private static boolean hasUsableMapCoordinate(Terminal terminal) {
        double latitude = terminal.getLatitude();
        double longitude = terminal.getLongitude();
        if (Double.isNaN(latitude) || Double.isInfinite(latitude) ||
                Double.isNaN(longitude) || Double.isInfinite(longitude)) return false;
        // Broad CALABARZON bounds reject corrupt coordinates while allowing its outer islands.
        if (latitude < 12.0 || latitude > 15.6 || longitude < 119.5 || longitude > 123.2) return false;

        // The imported master CSV stores route-description text in the description column.
        // Its coordinates are generated grid points, not surveyed terminal locations, and some
        // fall offshore. Keep those records and their coordinates in the database, but don't map
        // them as real terminal pins.
        String description = terminal.getDescription();
        String city = terminal.getCity();
        if (description != null && city != null && !city.trim().isEmpty()) {
            String lowerDescription = description.toLowerCase(java.util.Locale.ROOT);
            int routeLabelEnd = lowerDescription.indexOf(" - ");
            if (description.contains(";") && routeLabelEnd > 0) {
                String csvRouteCity = lowerDescription.substring(0, routeLabelEnd).trim();
                if (normalizePlace(csvRouteCity).equals(normalizePlace(city))) return false;
            }
        }
        return latitude != 0.0 && longitude != 0.0;
    }

    private static String normalizePlace(String place) {
        String normalized = place.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.endsWith(" city")) normalized = normalized.substring(0, normalized.length() - 5).trim();
        return normalized;
    }
}
