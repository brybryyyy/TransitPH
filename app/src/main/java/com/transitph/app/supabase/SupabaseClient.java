package com.transitph.app.supabase;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.transitph.app.BuildConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Minimal Supabase REST + Auth client (OkHttp + Gson).
 * Every method is BLOCKING: call it from a background thread, never from the UI thread.
 *
 * URL / anon key come from local.properties (SUPABASE_URL, SUPABASE_ANON_KEY) through BuildConfig.
 */
public final class SupabaseClient {

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();

    private SupabaseClient() {}

    /** Thrown for HTTP errors (code = HTTP status) or network failures (code = -1). */
    public static class SupabaseException extends Exception {
        private final int code;

        public SupabaseException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() { return code; }

        public boolean isNetworkError() { return code == -1; }
    }

    public static boolean isConfigured() {
        String url = BuildConfig.SUPABASE_URL;
        String key = BuildConfig.SUPABASE_ANON_KEY;
        return url != null && !url.isEmpty() && key != null && !key.isEmpty() && !key.startsWith("PASTE");
    }

    private static String baseUrl() {
        String u = BuildConfig.SUPABASE_URL;
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private static Request.Builder request(String path, String bearerToken) {
        String bearer = (bearerToken != null && !bearerToken.isEmpty()) ? bearerToken : BuildConfig.SUPABASE_ANON_KEY;
        return new Request.Builder()
                .url(baseUrl() + path)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + bearer)
                .header("Accept", "application/json");
    }

    private static String execute(Request request) throws SupabaseException {
        try (Response response = HTTP.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new SupabaseException(response.code(), extractMessage(body, response.code()));
            }
            return body;
        } catch (IOException e) {
            throw new SupabaseException(-1, "No internet connection.");
        }
    }

    private static String extractMessage(String body, int code) {
        try {
            JsonElement el = JsonParser.parseString(body);
            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                for (String k : new String[]{"error_description", "msg", "message", "error"}) {
                    if (o.has(k) && !o.get(k).isJsonNull()) return o.get(k).getAsString();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "Request failed (HTTP " + code + ").";
    }

    // ------------------------------------------------------------------ REST (PostgREST)

    /** GET /rest/v1/{table}?{query}  e.g. select("terminals", "select=*", token) */
    public static JsonArray select(String table, String query, String bearerToken) throws SupabaseException {
        String path = "/rest/v1/" + table + (query == null || query.isEmpty() ? "" : "?" + query);
        String body = execute(request(path, bearerToken).get().build());
        JsonElement el = JsonParser.parseString(body.isEmpty() ? "[]" : body);
        return el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
    }

    /** POST a single row and return the inserted row(s). */
    public static JsonArray insert(String table, JsonObject row, String bearerToken) throws SupabaseException {
        Request req = request("/rest/v1/" + table, bearerToken)
                .header("Prefer", "return=representation")
                .post(RequestBody.create(row.toString(), JSON_TYPE))
                .build();
        String body = execute(req);
        JsonElement el = JsonParser.parseString(body.isEmpty() ? "[]" : body);
        return el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
    }

    /** DELETE /rest/v1/{table}?{filter}  e.g. delete("saved_routes", "id=eq.5", token) */
    public static void delete(String table, String filter, String bearerToken) throws SupabaseException {
        execute(request("/rest/v1/" + table + "?" + filter, bearerToken).delete().build());
    }

    // ------------------------------------------------------------------ Auth (GoTrue)

    /** POST /auth/v1/token?grant_type=password  ->  {access_token, user:{id,email,user_metadata}} */
    public static JsonObject signIn(String email, String password) throws SupabaseException {
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        Request req = request("/auth/v1/token?grant_type=password", null)
                .post(RequestBody.create(payload.toString(), JSON_TYPE))
                .build();
        return JsonParser.parseString(execute(req)).getAsJsonObject();
    }

    /**
     * POST /auth/v1/signup. The full name is stored as user metadata so the profile row can be
     * created later (at first login) when email confirmation is turned on in Supabase.
     */
    public static JsonObject signUp(String email, String password, String fullName) throws SupabaseException {
        JsonObject meta = new JsonObject();
        meta.addProperty("full_name", fullName);
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.add("data", meta);
        Request req = request("/auth/v1/signup", null)
                .post(RequestBody.create(payload.toString(), JSON_TYPE))
                .build();
        return JsonParser.parseString(execute(req)).getAsJsonObject();
    }

    // ------------------------------------------------------------------ JSON helpers

    /** First non-null string value among the given keys, or fallback. Numbers are converted to text. */
    public static String str(JsonObject o, String fallback, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.get(k).isJsonNull()) {
                JsonElement e = o.get(k);
                if (e.isJsonPrimitive()) return e.getAsString();
            }
        }
        return fallback;
    }

    public static double num(JsonObject o, double fallback, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.get(k).isJsonNull()) {
                try { return o.get(k).getAsDouble(); } catch (Exception ignored) { /* try next */ }
            }
        }
        return fallback;
    }
}
