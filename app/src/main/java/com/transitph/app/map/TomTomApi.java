package com.transitph.app.map;

import android.content.Context;
import android.content.SharedPreferences;

import com.transitph.app.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Small blocking client for two TomTom services:
 *  - Search API (Fuzzy Search)  -> turns a place name into coordinates
 *  - Routing API (calculateRoute) -> returns the real road path between points
 *
 * Every method here does network I/O, so call them from a background thread
 * (RouteMapHelper already does this for you).
 *
 * Free plan: 2,500 non-tile requests per day, shared by search + routing.
 * Geocoding results are cached on the phone so each place name is only looked up once.
 */
public final class TomTomApi {

    private static final String PREFS = "tomtom_geocode_cache";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    private TomTomApi() {}

    /** The road path returned by TomTom. */
    public static class RoadRoute {
        public final List<GeoPoint> points;
        public final int meters;
        public final int seconds;

        public RoadRoute(List<GeoPoint> points, int meters, int seconds) {
            this.points = points;
            this.meters = meters;
            this.seconds = seconds;
        }
    }

    public static boolean hasKey() {
        String key = BuildConfig.TOMTOM_API_KEY;
        return key != null && !key.trim().isEmpty();
    }

    /**
     * Finds the coordinates of a place in the Philippines.
     * @param query e.g. "Mayapa SLEX Entry, Calamba, Laguna"
     * @param bias  results closer to this point are preferred (may be null)
     * @return the point, or null if nothing was found
     */
    public static GeoPoint geocode(Context ctx, String query, GeoPoint bias) throws IOException {
        if (query == null || query.trim().isEmpty()) return null;

        SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String cacheKey = query.trim().toLowerCase(Locale.ROOT);
        String cached = prefs.getString(cacheKey, null);
        if (cached != null) {
            String[] p = cached.split(",");
            if (p.length == 2) {
                try {
                    return new GeoPoint(Double.parseDouble(p[0]), Double.parseDouble(p[1]));
                } catch (NumberFormatException ignored) {
                    // fall through and ask TomTom again
                }
            }
        }

        HttpUrl.Builder url = new HttpUrl.Builder()
                .scheme("https")
                .host("api.tomtom.com")
                .addPathSegments("search/2/search")
                .addPathSegment(query.trim() + ".json")
                .addQueryParameter("key", BuildConfig.TOMTOM_API_KEY)
                .addQueryParameter("countrySet", "PH")
                .addQueryParameter("limit", "1");
        if (bias != null) {
            url.addQueryParameter("lat", String.format(Locale.US, "%.6f", bias.getLatitude()));
            url.addQueryParameter("lon", String.format(Locale.US, "%.6f", bias.getLongitude()));
        }

        String body = httpGet(url.build().toString());
        try {
            JSONArray results = new JSONObject(body).getJSONArray("results");
            if (results.length() == 0) return null;
            JSONObject pos = results.getJSONObject(0).getJSONObject("position");
            GeoPoint point = new GeoPoint(pos.getDouble("lat"), pos.getDouble("lon"));
            prefs.edit()
                    .putString(cacheKey, String.format(Locale.US, "%.6f,%.6f", point.getLatitude(), point.getLongitude()))
                    .apply();
            return point;
        } catch (JSONException e) {
            throw new IOException("Could not read TomTom search response", e);
        }
    }

    /**
     * Calculates the real road route through the given points (in order).
     * @param pedestrian true for walking routes, false for jeepney/bus (uses car roads)
     */
    public static RoadRoute route(List<GeoPoint> waypoints, boolean pedestrian) throws IOException {
        if (waypoints == null || waypoints.size() < 2) {
            throw new IOException("Need at least two points to build a route");
        }

        StringBuilder loc = new StringBuilder();
        for (int i = 0; i < waypoints.size(); i++) {
            if (i > 0) loc.append(':');
            loc.append(String.format(Locale.US, "%.6f,%.6f",
                    waypoints.get(i).getLatitude(), waypoints.get(i).getLongitude()));
        }

        String url = "https://api.tomtom.com/routing/1/calculateRoute/" + loc + "/json"
                + "?key=" + BuildConfig.TOMTOM_API_KEY
                + "&travelMode=" + (pedestrian ? "pedestrian" : "car")
                + "&routeType=fastest"
                + "&traffic=false";

        String body = httpGet(url);
        try {
            JSONObject route = new JSONObject(body).getJSONArray("routes").getJSONObject(0);
            JSONObject summary = route.getJSONObject("summary");
            JSONArray legs = route.getJSONArray("legs");

            List<GeoPoint> points = new ArrayList<>();
            for (int i = 0; i < legs.length(); i++) {
                JSONArray leg = legs.getJSONObject(i).getJSONArray("points");
                for (int j = 0; j < leg.length(); j++) {
                    JSONObject o = leg.getJSONObject(j);
                    points.add(new GeoPoint(o.getDouble("latitude"), o.getDouble("longitude")));
                }
            }
            return new RoadRoute(points,
                    summary.getInt("lengthInMeters"),
                    summary.getInt("travelTimeInSeconds"));
        } catch (JSONException e) {
            throw new IOException("Could not read TomTom route response", e);
        }
    }

    private static String httpGet(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() == 403) {
                throw new IOException("TomTom rejected the API key (403). Check the key and that Search + Routing are enabled for it.");
            }
            if (response.code() == 429) {
                throw new IOException("TomTom daily request limit reached (429).");
            }
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("TomTom error: HTTP " + response.code());
            }
            return response.body().string();
        }
    }
}
