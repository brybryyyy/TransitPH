package com.transitph.app.map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import com.transitph.app.database.TerminalDao;
import com.transitph.app.models.Route;
import com.transitph.app.models.RouteStop;
import com.transitph.app.models.Terminal;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One place that knows how to draw a TransitPH Route on a map.
 * (MapActivity and RouteResultsActivity used to each have their own copy of this logic.)
 *
 * What it does, in order:
 *  1. Start point  = the route's terminal.
 *  2. Middle stops = the route's stops. Stops saved with 0,0 coordinates are looked up by name
 *                    with TomTom Search (and cached on the phone).
 *  3. End point    = a terminal in the destination town, or a TomTom Search lookup of the destination.
 *  4. Sends all points to the TomTom Routing API and draws the real road path (like Google Maps).
 *  5. If anything fails (no key, no internet, daily limit) it draws a dashed straight line instead
 *     and tells the caller why.
 */
public final class RouteMapHelper {

    public interface Listener {
        /**
         * Called on the main thread when drawing is finished.
         * @param meters       road distance, or 0 if only the straight-line fallback was drawn
         * @param seconds      driving time estimate, or 0 for the fallback
         * @param roadAccurate true if the line follows real roads
         * @param note         explanation when something went wrong, otherwise null
         */
        void onRouteReady(int meters, int seconds, boolean roadAccurate, String note);
    }

    private static final int KIND_START = 0;
    private static final int KIND_STOP = 1;
    private static final int KIND_END = 2;

    // A geocoded stop further than this from the previous point is probably the wrong place.
    private static final double MAX_STOP_JUMP_METERS = 40_000;
    // Points closer than this to the previous one are merged (avoids zero-length route legs).
    private static final double MIN_POINT_GAP_METERS = 200;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicInteger GENERATION = new AtomicInteger();
    private static final LruCache<Long, Resolved> CACHE = new LruCache<>(30);

    private RouteMapHelper() {}

    private static class Waypoint {
        final GeoPoint point;
        final String name;
        final int kind;

        Waypoint(GeoPoint point, String name, int kind) {
            this.point = point;
            this.name = name;
            this.kind = kind;
        }
    }

    private static class Resolved {
        final List<Waypoint> waypoints = new ArrayList<>();
        TomTomApi.RoadRoute road;
        String note;
    }

    // ------------------------------------------------------------------ public API

    public static void drawRoute(Context context, MapView map, Route route, Listener listener) {
        if (map == null || route == null) return;

        final Context app = context.getApplicationContext();
        final int generation = GENERATION.incrementAndGet();

        map.getOverlays().clear();
        map.getOverlays().add(new CopyrightOverlay(map.getContext()));
        map.invalidate();

        EXECUTOR.execute(() -> {
            Resolved cached = CACHE.get(route.getId());
            final Resolved result;
            if (cached != null) {
                result = cached;
            } else {
                result = resolve(app, route);
                if (result.road != null) CACHE.put(route.getId(), result);
            }

            MAIN.post(() -> {
                // A newer drawRoute() call (or leaving the screen) makes this result stale.
                if (generation != GENERATION.get() || !map.isAttachedToWindow()) return;
                render(map, result);
                if (listener != null) {
                    boolean accurate = result.road != null;
                    listener.onRouteReady(
                            accurate ? result.road.meters : 0,
                            accurate ? result.road.seconds : 0,
                            accurate,
                            result.note);
                }
            });
        });
    }

    /** "12.4 km • about 28 min by road" */
    public static String formatSummary(int meters, int seconds) {
        double km = meters / 1000.0;
        int mins = Math.max(1, Math.round(seconds / 60f));
        return String.format(Locale.US, "%.1f km • about %d min by road", km, mins);
    }

    // ------------------------------------------------------------------ background work

    private static Resolved resolve(Context ctx, Route route) {
        Resolved out = new Resolved();
        TerminalDao terminalDao = new TerminalDao(ctx);
        Terminal term = terminalDao.getTerminalById(route.getTerminalId());

        boolean apiAvailable = TomTomApi.hasKey();
        if (!apiAvailable) {
            out.note = "TomTom API key is missing, so a straight line is shown. Add TOMTOM_API_KEY to local.properties.";
        }

        // 1. Start = terminal
        if (term != null && term.getLatitude() != 0 && term.getLongitude() != 0) {
            out.waypoints.add(new Waypoint(
                    new GeoPoint(term.getLatitude(), term.getLongitude()),
                    term.getTerminalName(), KIND_START));
        }

        // 2. Middle stops
        List<RouteStop> stops = route.getStops();
        if (stops != null) {
            for (RouteStop stop : stops) {
                GeoPoint p = null;
                boolean geocoded = false;

                if (stop.getLatitude() != 0 && stop.getLongitude() != 0) {
                    p = new GeoPoint(stop.getLatitude(), stop.getLongitude());
                } else if (apiAvailable) {
                    try {
                        p = TomTomApi.geocode(ctx, stop.getStopName(), lastPoint(out));
                        geocoded = true;
                    } catch (IOException e) {
                        out.note = e.getMessage();
                        apiAvailable = false; // stop hammering the API after a failure
                    }
                }
                if (p == null) continue;

                GeoPoint prev = lastPoint(out);
                if (prev != null) {
                    double d = prev.distanceToAsDouble(p);
                    if (d < MIN_POINT_GAP_METERS) continue;
                    if (geocoded && d > MAX_STOP_JUMP_METERS) continue;
                }
                out.waypoints.add(new Waypoint(p, stop.getStopName(), KIND_STOP));
            }
        }

        // 3. End = destination
        GeoPoint dest = findDestinationInDatabase(terminalDao, route.getDestination());
        if (dest == null && apiAvailable && route.getDestination() != null) {
            try {
                dest = TomTomApi.geocode(ctx, route.getDestination() + ", Philippines", lastPoint(out));
            } catch (IOException e) {
                out.note = e.getMessage();
                apiAvailable = false;
            }
        }
        if (dest != null) {
            GeoPoint prev = lastPoint(out);
            if (prev == null || prev.distanceToAsDouble(dest) >= MIN_POINT_GAP_METERS) {
                out.waypoints.add(new Waypoint(dest, route.getDestination(), KIND_END));
            } else if (!out.waypoints.isEmpty()) {
                // The last stop already is the destination: relabel it as the finish.
                Waypoint last = out.waypoints.remove(out.waypoints.size() - 1);
                out.waypoints.add(new Waypoint(last.point, last.name, last.kind == KIND_START ? KIND_START : KIND_END));
            }
        }

        if (out.waypoints.size() < 2) {
            if (out.note == null) out.note = "Not enough location data to draw this route.";
            return out;
        }

        // 4. Ask TomTom for the real road path
        if (apiAvailable) {
            List<GeoPoint> points = new ArrayList<>();
            for (Waypoint w : out.waypoints) points.add(w.point);
            boolean walking = "Walking".equalsIgnoreCase(route.getTransportType());
            try {
                out.road = TomTomApi.route(points, walking);
            } catch (IOException e) {
                out.note = e.getMessage();
            }
        }
        return out;
    }

    private static GeoPoint lastPoint(Resolved r) {
        return r.waypoints.isEmpty() ? null : r.waypoints.get(r.waypoints.size() - 1).point;
    }

    /** Prefers a real terminal in the destination town so the line ends where commuters actually get off. */
    private static GeoPoint findDestinationInDatabase(TerminalDao dao, String destination) {
        if (destination == null || destination.trim().isEmpty()) return null;

        Terminal best = null;
        int bestScore = 0;
        for (Terminal t : dao.searchTerminals(destination)) {
            if (t.getLatitude() == 0 || t.getLongitude() == 0) continue;
            int score = 0;
            if (t.getCity() != null && t.getCity().equalsIgnoreCase(destination.trim())) score += 10;
            String name = t.getTerminalName() == null ? "" : t.getTerminalName().toLowerCase(Locale.ROOT);
            if (name.contains("terminal")) score += 3;
            if (name.contains("market")) score += 1;
            if (name.contains("dispatch") || name.contains("roadside") || name.contains("loading")) score -= 5;
            score += Math.min(t.getRouteCount(), 5);
            if (score > bestScore) {
                bestScore = score;
                best = t;
            }
        }
        // Only trust the database when the town name really matched (score >= 10).
        return (best != null && bestScore >= 10)
                ? new GeoPoint(best.getLatitude(), best.getLongitude())
                : null;
    }

    // ------------------------------------------------------------------ drawing (main thread)

    private static void render(MapView map, Resolved r) {
        Context ctx = map.getContext();
        float d = ctx.getResources().getDisplayMetrics().density;
        List<Overlay> overlays = map.getOverlays();

        // Line: road path if we have it, otherwise a dashed straight line through the points.
        List<GeoPoint> line = new ArrayList<>();
        if (r.road != null) {
            line.addAll(r.road.points);
        } else {
            for (Waypoint w : r.waypoints) line.add(w.point);
        }

        if (line.size() >= 2) {
            Polyline outline = new Polyline();
            outline.setPoints(line);
            outline.getOutlinePaint().setColor(Color.WHITE);
            outline.getOutlinePaint().setStrokeWidth(9f * d);
            outline.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
            outline.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);

            Polyline main = new Polyline();
            main.setPoints(line);
            main.getOutlinePaint().setColor(Color.parseColor("#1A73E8"));
            main.getOutlinePaint().setStrokeWidth(5f * d);
            main.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
            main.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
            if (r.road == null) {
                main.getOutlinePaint().setPathEffect(new DashPathEffect(new float[]{20f, 14f}, 0f));
            }

            overlays.add(0, outline);
            overlays.add(1, main);
        }

        // Markers
        for (Waypoint w : r.waypoints) {
            Marker m = new Marker(map);
            m.setPosition(w.point);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            if (w.kind == KIND_START) {
                m.setTitle("START: " + w.name);
                m.setIcon(circle(Color.parseColor("#2E7D32"), 18, d));
            } else if (w.kind == KIND_END) {
                m.setTitle("FINISH: " + w.name);
                m.setIcon(circle(Color.parseColor("#D32F2F"), 18, d));
            } else {
                m.setTitle(w.name);
                m.setIcon(circle(Color.parseColor("#1A73E8"), 12, d));
            }
            overlays.add(m);
        }

        // Camera
        if (line.size() >= 2) {
            BoundingBox box = BoundingBox.fromGeoPoints(line);
            if (box.getLatitudeSpan() < 0.0005 && box.getLongitudeSpan() < 0.0005) {
                map.getController().setZoom(16.0);
                map.getController().setCenter(line.get(0));
            } else {
                map.zoomToBoundingBox(box, true, (int) (72 * d));
            }
        } else if (!r.waypoints.isEmpty()) {
            map.getController().setZoom(15.0);
            map.getController().setCenter(r.waypoints.get(0).point);
        }
        map.invalidate();
    }

    private static GradientDrawable circle(int fill, int sizeDp, float density) {
        int px = (int) (sizeDp * density);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        g.setStroke((int) (2 * density), Color.WHITE);
        g.setSize(px, px);
        g.setBounds(0, 0, px, px);
        return g;
    }
}
