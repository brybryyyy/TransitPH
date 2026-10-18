package com.transitph.app.supabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.transitph.app.database.DatabaseHelper;
import com.transitph.app.supabase.SupabaseClient.SupabaseException;
import com.transitph.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the local SQLite database (used by the whole UI) in step with the classmate's Supabase tables:
 * terminals, routes (with stops + timeline) and saved_routes.
 *
 * Everything here is BLOCKING - call from a background thread.
 */
public final class SupabaseSync {

    private static final String TAG = "TransitPH-Sync";
    private static final Object LOCK = new Object();

    private SupabaseSync() {}

    // ------------------------------------------------------------------ pull

    /**
     * Downloads terminals + routes (and the signed-in user's saved routes) and merges them into the local copy.
     * Existing local-only routes and terminals are retained. Existing terminal coordinates are kept
     * so a Supabase refresh does not move established map markers.
     * Returns true when usable Supabase rows were merged. The local data is left untouched
     * if anything fails (no internet, empty tables, ...), so the app keeps working offline.
     */
    public static boolean pullAll(Context context) {
        if (!SupabaseClient.isConfigured()) return false;
        synchronized (LOCK) {
            Context app = context.getApplicationContext();
            SessionManager session = new SessionManager(app);
            String token = session.getAccessToken();

            JsonArray terminals = new JsonArray();
            JsonArray routes = new JsonArray();
            try {
                try {
                    terminals = SupabaseClient.select("terminals", "select=*", null);
                    routes = SupabaseClient.select("routes", "select=*", null);
                } catch (SupabaseException anonFailed) {
                    Log.w(TAG, "Anonymous read failed: " + anonFailed.getMessage());
                }
                // Row-level security often returns an EMPTY list (not an error) for anonymous users.
                if ((terminals.size() == 0 || routes.size() == 0) && !token.isEmpty()) {
                    terminals = SupabaseClient.select("terminals", "select=*", token);
                    routes = SupabaseClient.select("routes", "select=*", token);
                }
            } catch (SupabaseException e) {
                Log.w(TAG, "Could not read terminals/routes: " + e.getMessage());
                return false;
            }
            Log.i(TAG, "Supabase returned " + terminals.size() + " terminals and " + routes.size() + " routes");
            if (terminals.size() == 0 || routes.size() == 0) {
                return false; // never wipe the working local data with an empty result
            }

            JsonArray saved = new JsonArray();
            String remoteUserId = session.getRemoteUserId();
            if (!remoteUserId.isEmpty() && !token.isEmpty()) {
                try {
                    saved = SupabaseClient.select("saved_routes", "select=*&userId=eq." + remoteUserId, token);
                } catch (SupabaseException ignored) {
                    // keep going without saved routes (e.g. token expired)
                }
            }

            SQLiteDatabase db = DatabaseHelper.getInstance(app).getWritableDatabase();
            db.beginTransaction();
            try {
                int importedTerminals = 0;
                int importedRoutes = 0;
                Map<String, Long> terminalIds = new HashMap<>();
                for (JsonElement el : terminals) {
                    if (!el.isJsonObject()) continue;
                    JsonObject t = el.getAsJsonObject();
                    String remoteId = SupabaseClient.str(t, "", "id");
                    String name = SupabaseClient.str(t, "Unnamed terminal", "name", "terminalName");
                    String city = SupabaseClient.str(t, "", "city");
                    String province = SupabaseClient.str(t, "", "province");
                    long localId = findTerminalByRemoteId(db, remoteId);
                    if (localId <= 0) localId = findTerminalByIdentity(db, name, city, province);
                    if (localId > 0) {
                        // Attach the remote ID, but deliberately keep the existing terminal fields
                        // (especially latitude/longitude) so the user's current map pins stay put.
                        if (!remoteId.isEmpty()) {
                            ContentValues idValues = new ContentValues();
                            idValues.put(DatabaseHelper.COL_REMOTE_ID, remoteId);
                            db.update(DatabaseHelper.TABLE_TERMINALS, idValues,
                                    DatabaseHelper.COL_TERM_ID + " = ? AND (" + DatabaseHelper.COL_REMOTE_ID + " IS NULL OR " + DatabaseHelper.COL_REMOTE_ID + " = '')",
                                    new String[]{String.valueOf(localId)});
                        }
                    } else {
                        ContentValues cv = new ContentValues();
                        cv.put(DatabaseHelper.COL_TERM_NAME, name);
                        cv.put(DatabaseHelper.COL_TERM_CITY, city);
                        cv.put(DatabaseHelper.COL_TERM_PROVINCE, province);
                        cv.put(DatabaseHelper.COL_TERM_LAT, SupabaseClient.num(t, 0, "latitude", "lat"));
                        cv.put(DatabaseHelper.COL_TERM_LNG, SupabaseClient.num(t, 0, "longitude", "lng"));
                        cv.put(DatabaseHelper.COL_TERM_DESC, SupabaseClient.str(t, "", "description"));
                        cv.put(DatabaseHelper.COL_REMOTE_ID, remoteId);
                        localId = db.insert(DatabaseHelper.TABLE_TERMINALS, null, cv);
                    }
                    if (localId > 0) {
                        terminalIds.put(remoteId, localId);
                        importedTerminals++;
                    }
                }

                Map<String, Long> routeIds = new HashMap<>();
                for (JsonElement el : routes) {
                    if (!el.isJsonObject()) continue;
                    JsonObject r = el.getAsJsonObject();
                    Long localTerminal = terminalIds.get(SupabaseClient.str(r, "", "terminalId"));
                    if (localTerminal == null) continue; // route without a known terminal
                    String remoteId = SupabaseClient.str(r, "", "id");

                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseHelper.COL_ROUTE_TERM_ID, localTerminal);
                    cv.put(DatabaseHelper.COL_ROUTE_NAME, SupabaseClient.str(r, "Route", "name", "routeName"));
                    cv.put(DatabaseHelper.COL_ROUTE_ORIGIN, SupabaseClient.str(r, "", "origin"));
                    cv.put(DatabaseHelper.COL_ROUTE_DESTINATION, SupabaseClient.str(r, "", "destination"));
                    cv.put(DatabaseHelper.COL_ROUTE_TRANSPORT_TYPE, SupabaseClient.str(r, "Jeepney", "transportType"));
                    cv.put(DatabaseHelper.COL_ROUTE_FARE, SupabaseClient.num(r, 0, "fare"));
                    cv.put(DatabaseHelper.COL_ROUTE_TIME, (int) SupabaseClient.num(r, 0, "estimatedTravelTime"));
                    cv.put(DatabaseHelper.COL_ROUTE_DESC, SupabaseClient.str(r, "", "description"));
                    cv.put(DatabaseHelper.COL_ROUTE_TRANSFERS, (int) SupabaseClient.num(r, 0, "transfers"));
                    cv.put(DatabaseHelper.COL_ROUTE_WALK, (int) SupabaseClient.num(r, 0, "walkingDistanceMeters"));
                    if (r.has("timeline") && !r.get("timeline").isJsonNull()) {
                        cv.put(DatabaseHelper.COL_ROUTE_TIMELINE, r.get("timeline").toString());
                    }
                    cv.put(DatabaseHelper.COL_REMOTE_ID, remoteId);
                    long localRoute = findRouteByRemoteId(db, remoteId);
                    if (localRoute > 0) {
                        // Refresh the remote-backed row in place; local-only routes are not touched.
                        db.update(DatabaseHelper.TABLE_ROUTES, cv, DatabaseHelper.COL_ROUTE_ID + " = ?",
                                new String[]{String.valueOf(localRoute)});
                    } else {
                        localRoute = findRouteByIdentity(db, localTerminal,
                                SupabaseClient.str(r, "Route", "name", "routeName"),
                                SupabaseClient.str(r, "", "origin"),
                                SupabaseClient.str(r, "", "destination"),
                                SupabaseClient.str(r, "Jeepney", "transportType"));
                        if (localRoute > 0) {
                            cv.put(DatabaseHelper.COL_REMOTE_ID, remoteId);
                            db.update(DatabaseHelper.TABLE_ROUTES, cv, DatabaseHelper.COL_ROUTE_ID + " = ?",
                                    new String[]{String.valueOf(localRoute)});
                        } else {
                            localRoute = db.insert(DatabaseHelper.TABLE_ROUTES, null, cv);
                        }
                    }
                    if (localRoute <= 0) continue;
                    routeIds.put(remoteId, localRoute);
                    importedRoutes++;

                    // Merge stop names without clearing existing route stops.
                    if (r.has("stops") && r.get("stops").isJsonArray()) {
                        int seq = nextStopSequence(db, localRoute);
                        for (JsonElement stop : r.getAsJsonArray("stops")) {
                            if (!stop.isJsonPrimitive()) continue;
                            String stopName = stop.getAsString();
                            if (hasStop(db, localRoute, stopName)) continue;
                            ContentValues sv = new ContentValues();
                            sv.put(DatabaseHelper.COL_STOP_ROUTE_ID, localRoute);
                            sv.put(DatabaseHelper.COL_STOP_NAME, stopName);
                            sv.put(DatabaseHelper.COL_STOP_SEQUENCE, seq++);
                            sv.put(DatabaseHelper.COL_STOP_LAT, 0.0);
                            sv.put(DatabaseHelper.COL_STOP_LNG, 0.0);
                            db.insert(DatabaseHelper.TABLE_ROUTE_STOPS, null, sv);
                        }
                    }
                }

                Log.i(TAG, "Imported " + importedTerminals + " terminals and " + importedRoutes + " routes");
                if (importedTerminals == 0 || importedRoutes == 0) {
                    // Rows did not match what the app expects (ids / columns). Roll back this merge.
                    Log.w(TAG, "Nothing usable imported - keeping local data (rollback)");
                    return false;
                }

                long localUserId = session.getUserId();
                if (localUserId > 0) {
                    for (JsonElement el : saved) {
                        if (!el.isJsonObject()) continue;
                        JsonObject s = el.getAsJsonObject();
                        Long localRoute = routeIds.get(SupabaseClient.str(s, "", "routeId"));
                        if (localRoute == null) continue;
                        ContentValues cv = new ContentValues();
                        cv.put(DatabaseHelper.COL_SAVED_USER_ID, localUserId);
                        cv.put(DatabaseHelper.COL_SAVED_ROUTE_ID, localRoute);
                        cv.put(DatabaseHelper.COL_REMOTE_ID, SupabaseClient.str(s, "", "id"));
                        db.insertWithOnConflict(DatabaseHelper.TABLE_SAVED_ROUTES, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            // Supabase may contain terminals added after the bundled CSV was created.
            // Add an estimated connector only for any such terminal that still has no routes.
            DatabaseHelper.getInstance(app).ensureEstimatedRouteCoverage();
            return true;
        }
    }

    private static long findTerminalByRemoteId(SQLiteDatabase db, String remoteId) {
        if (remoteId == null || remoteId.isEmpty()) return -1;
        Cursor c = db.query(DatabaseHelper.TABLE_TERMINALS, new String[]{DatabaseHelper.COL_TERM_ID},
                DatabaseHelper.COL_REMOTE_ID + " = ?", new String[]{remoteId}, null, null, null, "1");
        try { return c.moveToFirst() ? c.getLong(0) : -1; } finally { c.close(); }
    }

    private static long findTerminalByIdentity(SQLiteDatabase db, String name, String city, String province) {
        Cursor c = db.query(DatabaseHelper.TABLE_TERMINALS, new String[]{DatabaseHelper.COL_TERM_ID},
                "lower(trim(" + DatabaseHelper.COL_TERM_NAME + ")) = lower(trim(?)) AND lower(trim(" + DatabaseHelper.COL_TERM_CITY + ")) = lower(trim(?)) AND lower(trim(" + DatabaseHelper.COL_TERM_PROVINCE + ")) = lower(trim(?))",
                new String[]{name, city, province}, null, null, null, "1");
        try { return c.moveToFirst() ? c.getLong(0) : -1; } finally { c.close(); }
    }

    private static long findRouteByRemoteId(SQLiteDatabase db, String remoteId) {
        if (remoteId == null || remoteId.isEmpty()) return -1;
        Cursor c = db.query(DatabaseHelper.TABLE_ROUTES, new String[]{DatabaseHelper.COL_ROUTE_ID},
                DatabaseHelper.COL_REMOTE_ID + " = ?", new String[]{remoteId}, null, null, null, "1");
        try { return c.moveToFirst() ? c.getLong(0) : -1; } finally { c.close(); }
    }

    private static long findRouteByIdentity(SQLiteDatabase db, long terminalId, String name, String origin, String destination, String type) {
        Cursor c = db.query(DatabaseHelper.TABLE_ROUTES, new String[]{DatabaseHelper.COL_ROUTE_ID},
                DatabaseHelper.COL_ROUTE_TERM_ID + " = ? AND lower(trim(" + DatabaseHelper.COL_ROUTE_NAME + ")) = lower(trim(?)) AND lower(trim(" + DatabaseHelper.COL_ROUTE_ORIGIN + ")) = lower(trim(?)) AND lower(trim(" + DatabaseHelper.COL_ROUTE_DESTINATION + ")) = lower(trim(?)) AND lower(trim(" + DatabaseHelper.COL_ROUTE_TRANSPORT_TYPE + ")) = lower(trim(?))",
                new String[]{String.valueOf(terminalId), name, origin, destination, type}, null, null, null, "1");
        try { return c.moveToFirst() ? c.getLong(0) : -1; } finally { c.close(); }
    }

    private static boolean hasStop(SQLiteDatabase db, long routeId, String stopName) {
        Cursor c = db.query(DatabaseHelper.TABLE_ROUTE_STOPS, new String[]{DatabaseHelper.COL_STOP_ID},
                DatabaseHelper.COL_STOP_ROUTE_ID + " = ? AND lower(trim(" + DatabaseHelper.COL_STOP_NAME + ")) = lower(trim(?))",
                new String[]{String.valueOf(routeId), stopName}, null, null, null, "1");
        try { return c.moveToFirst(); } finally { c.close(); }
    }

    private static int nextStopSequence(SQLiteDatabase db, long routeId) {
        Cursor c = db.rawQuery("SELECT COALESCE(MAX(" + DatabaseHelper.COL_STOP_SEQUENCE + "), 0) + 1 FROM " +
                DatabaseHelper.TABLE_ROUTE_STOPS + " WHERE " + DatabaseHelper.COL_STOP_ROUTE_ID + " = ?",
                new String[]{String.valueOf(routeId)});
        try { return c.moveToFirst() ? c.getInt(0) : 1; } finally { c.close(); }
    }

    // ------------------------------------------------------------------ push (saved routes)

    /** Mirrors a freshly saved local route to Supabase. Fire-and-forget. */
    public static void pushSavedRoute(Context context, long localSavedId, long localRouteId) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            if (!SupabaseClient.isConfigured()) return;
            SessionManager session = new SessionManager(app);
            String token = session.getAccessToken();
            String remoteUser = session.getRemoteUserId();
            if (token.isEmpty() || remoteUser.isEmpty()) return;

            String remoteRoute = lookupRemoteId(app, DatabaseHelper.TABLE_ROUTES, localRouteId);
            if (remoteRoute.isEmpty()) return;

            try {
                JsonObject row = new JsonObject();
                row.addProperty("userId", remoteUser);
                row.addProperty("routeId", remoteRoute);
                JsonArray inserted = SupabaseClient.insert("saved_routes", row, token);
                if (inserted.size() > 0 && inserted.get(0).isJsonObject()) {
                    String remoteId = SupabaseClient.str(inserted.get(0).getAsJsonObject(), "", "id");
                    if (!remoteId.isEmpty()) {
                        ContentValues cv = new ContentValues();
                        cv.put(DatabaseHelper.COL_REMOTE_ID, remoteId);
                        DatabaseHelper.getInstance(app).getWritableDatabase().update(
                                DatabaseHelper.TABLE_SAVED_ROUTES, cv,
                                DatabaseHelper.COL_SAVED_ID + " = ?", new String[]{String.valueOf(localSavedId)});
                    }
                }
            } catch (SupabaseException ignored) {
                // Saved locally; it will be re-uploaded the next time the user saves it.
            }
        }).start();
    }

    /** Removes a saved route from Supabase. Fire-and-forget; pass the remote ids read BEFORE the local delete. */
    public static void pushDeleteSavedRoute(Context context, String remoteSavedId, String remoteRouteId) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            if (!SupabaseClient.isConfigured()) return;
            SessionManager session = new SessionManager(app);
            String token = session.getAccessToken();
            String remoteUser = session.getRemoteUserId();
            if (token.isEmpty() || remoteUser.isEmpty()) return;
            try {
                if (remoteSavedId != null && !remoteSavedId.isEmpty()) {
                    SupabaseClient.delete("saved_routes", "id=eq." + remoteSavedId, token);
                } else if (remoteRouteId != null && !remoteRouteId.isEmpty()) {
                    SupabaseClient.delete("saved_routes", "userId=eq." + remoteUser + "&routeId=eq." + remoteRouteId, token);
                }
            } catch (SupabaseException ignored) {
                // local copy is already gone
            }
        }).start();
    }

    /** Returns the Supabase id stored for a local row ("" when unknown). */
    public static String lookupRemoteId(Context context, String table, long localId) {
        SQLiteDatabase db = DatabaseHelper.getInstance(context).getReadableDatabase();
        Cursor c = db.query(table, new String[]{DatabaseHelper.COL_REMOTE_ID}, "id = ?",
                new String[]{String.valueOf(localId)}, null, null, null);
        String result = "";
        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) result = c.getString(0);
            c.close();
        }
        return result;
    }
}
