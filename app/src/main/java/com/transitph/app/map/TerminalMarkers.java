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
            if (t.getLatitude() == 0 || t.getLongitude() == 0) continue;
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
}
