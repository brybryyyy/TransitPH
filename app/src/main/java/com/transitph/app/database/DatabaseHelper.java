package com.transitph.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.transitph.app.utils.PasswordUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "transitph.db";
    public static final int DATABASE_VERSION = 6;

    // Table: users
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_FULL_NAME = "fullName";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_PASSWORD_HASH = "passwordHash";
    public static final String COL_USER_ROLE = "role"; // USER or ADMIN
    public static final String COL_REMOTE_ID = "remoteId"; // Supabase id (users, terminals, routes, saved_routes)

    // Table: terminals
    public static final String TABLE_TERMINALS = "terminals";
    public static final String COL_TERM_ID = "id";
    public static final String COL_TERM_NAME = "terminalName";
    public static final String COL_TERM_CITY = "city";
    public static final String COL_TERM_PROVINCE = "province";
    public static final String COL_TERM_LAT = "latitude";
    public static final String COL_TERM_LNG = "longitude";
    public static final String COL_TERM_DESC = "description";

    // Table: routes
    public static final String TABLE_ROUTES = "routes";
    public static final String COL_ROUTE_ID = "id";
    public static final String COL_ROUTE_TERM_ID = "terminalId";
    public static final String COL_ROUTE_NAME = "routeName";
    public static final String COL_ROUTE_ORIGIN = "origin";
    public static final String COL_ROUTE_DESTINATION = "destination";
    public static final String COL_ROUTE_TRANSPORT_TYPE = "transportType";
    public static final String COL_ROUTE_FARE = "fare";
    public static final String COL_ROUTE_TIME = "estimatedTravelTime";
    public static final String COL_ROUTE_DESC = "description";
    public static final String COL_ROUTE_TRANSFERS = "transfers";
    public static final String COL_ROUTE_WALK = "walkingDistanceMeters";
    public static final String COL_ROUTE_TIMELINE = "timelineJson";

    // Table: route_stops
    public static final String TABLE_ROUTE_STOPS = "route_stops";
    public static final String COL_STOP_ID = "id";
    public static final String COL_STOP_ROUTE_ID = "routeId";
    public static final String COL_STOP_NAME = "stopName";
    public static final String COL_STOP_SEQUENCE = "sequence";
    public static final String COL_STOP_LAT = "latitude";
    public static final String COL_STOP_LNG = "longitude";

    // Table: saved_routes
    public static final String TABLE_SAVED_ROUTES = "saved_routes";
    public static final String COL_SAVED_ID = "id";
    public static final String COL_SAVED_USER_ID = "userId";
    public static final String COL_SAVED_ROUTE_ID = "routeId";

    // Table: destinations
    public static final String TABLE_DESTINATIONS = "destinations";
    public static final String COL_DEST_ID = "id";
    public static final String COL_DEST_NAME = "name";
    public static final String COL_DEST_DESC = "description";
    public static final String COL_DEST_MUNICIPALITY = "municipality";
    public static final String COL_DEST_PROVINCE = "province";
    public static final String COL_DEST_CATEGORY = "category";
    public static final String COL_DEST_LAT = "latitude";
    public static final String COL_DEST_LNG = "longitude";
    public static final String COL_DEST_HOURS = "operatingHours";
    public static final String COL_DEST_TERM_ID = "nearbyTerminalId";
    public static final String COL_DEST_TERM_NAME = "nearbyTerminalName";
    public static final String COL_DEST_ROUTE = "suggestedRoute";
    public static final String COL_DEST_ACCESSIBILITY = "accessibilityInfo";
    public static final String COL_DEST_IMAGE = "imageUrl";

    // Table: safety_reports
    public static final String TABLE_SAFETY_REPORTS = "safety_reports";
    public static final String COL_REPORT_ID = "id";
    public static final String COL_REPORT_USER_ID = "userId";
    public static final String COL_REPORT_USER_NAME = "userFullName";
    public static final String COL_REPORT_CATEGORY = "category";
    public static final String COL_REPORT_LOCATION = "location";
    public static final String COL_REPORT_DESC = "description";
    public static final String COL_REPORT_STATUS = "status";
    public static final String COL_REPORT_SEVERITY = "severity";
    public static final String COL_REPORT_CREATED_AT = "createdAt";
    public static final String COL_REPORT_ADMIN_NOTES = "adminNotes";

    // Table: safety_info
    public static final String TABLE_SAFETY_INFO = "safety_info";
    public static final String COL_INFO_ID = "id";
    public static final String COL_INFO_LOCATION = "locationName";
    public static final String COL_INFO_MUNICIPALITY = "municipality";
    public static final String COL_INFO_PROVINCE = "province";
    public static final String COL_INFO_CROWD = "crowdLevel";
    public static final String COL_INFO_LIGHTING = "lightingCondition";
    public static final String COL_INFO_SCORE = "safetyScore";
    public static final String COL_INFO_NOTES = "safetyNotes";
    public static final String COL_INFO_REPORTS_COUNT = "activeReportCount";
    public static final String COL_INFO_UPDATED = "lastUpdated";

    private static DatabaseHelper instance;
    private final Context context;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users Table
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_FULL_NAME + " TEXT NOT NULL, " +
                COL_USER_EMAIL + " TEXT UNIQUE NOT NULL, " +
                COL_USER_PASSWORD_HASH + " TEXT NOT NULL, " +
                COL_USER_ROLE + " TEXT NOT NULL DEFAULT 'USER', " +
                COL_REMOTE_ID + " TEXT);");

        // Create Terminals Table
        db.execSQL("CREATE TABLE " + TABLE_TERMINALS + " (" +
                COL_TERM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TERM_NAME + " TEXT NOT NULL, " +
                COL_TERM_CITY + " TEXT NOT NULL, " +
                COL_TERM_PROVINCE + " TEXT NOT NULL, " +
                COL_TERM_LAT + " REAL NOT NULL, " +
                COL_TERM_LNG + " REAL NOT NULL, " +
                COL_TERM_DESC + " TEXT, " +
                COL_REMOTE_ID + " TEXT);");

        // Create Routes Table
        db.execSQL("CREATE TABLE " + TABLE_ROUTES + " (" +
                COL_ROUTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ROUTE_TERM_ID + " INTEGER NOT NULL, " +
                COL_ROUTE_NAME + " TEXT NOT NULL, " +
                COL_ROUTE_ORIGIN + " TEXT NOT NULL, " +
                COL_ROUTE_DESTINATION + " TEXT NOT NULL, " +
                COL_ROUTE_TRANSPORT_TYPE + " TEXT NOT NULL, " +
                COL_ROUTE_FARE + " REAL NOT NULL, " +
                COL_ROUTE_TIME + " INTEGER NOT NULL, " +
                COL_ROUTE_DESC + " TEXT, " +
                COL_ROUTE_TRANSFERS + " INTEGER, " +
                COL_ROUTE_WALK + " INTEGER, " +
                COL_ROUTE_TIMELINE + " TEXT, " +
                COL_REMOTE_ID + " TEXT, " +
                "FOREIGN KEY (" + COL_ROUTE_TERM_ID + ") REFERENCES " + TABLE_TERMINALS + "(" + COL_TERM_ID + ") ON DELETE CASCADE);");

        // Create Route Stops Table
        db.execSQL("CREATE TABLE " + TABLE_ROUTE_STOPS + " (" +
                COL_STOP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_STOP_ROUTE_ID + " INTEGER NOT NULL, " +
                COL_STOP_NAME + " TEXT NOT NULL, " +
                COL_STOP_SEQUENCE + " INTEGER NOT NULL, " +
                COL_STOP_LAT + " REAL, " +
                COL_STOP_LNG + " REAL, " +
                "FOREIGN KEY (" + COL_STOP_ROUTE_ID + ") REFERENCES " + TABLE_ROUTES + "(" + COL_ROUTE_ID + ") ON DELETE CASCADE);");

        // Create Saved Routes Table
        db.execSQL("CREATE TABLE " + TABLE_SAVED_ROUTES + " (" +
                COL_SAVED_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SAVED_USER_ID + " INTEGER NOT NULL, " +
                COL_SAVED_ROUTE_ID + " INTEGER NOT NULL, " +
                COL_REMOTE_ID + " TEXT, " +
                "UNIQUE(" + COL_SAVED_USER_ID + ", " + COL_SAVED_ROUTE_ID + "), " +
                "FOREIGN KEY (" + COL_SAVED_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ") ON DELETE CASCADE, " +
                "FOREIGN KEY (" + COL_SAVED_ROUTE_ID + ") REFERENCES " + TABLE_ROUTES + "(" + COL_ROUTE_ID + ") ON DELETE CASCADE);");

        // Create Destinations Table
        db.execSQL("CREATE TABLE " + TABLE_DESTINATIONS + " (" +
                COL_DEST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DEST_NAME + " TEXT NOT NULL, " +
                COL_DEST_DESC + " TEXT, " +
                COL_DEST_MUNICIPALITY + " TEXT NOT NULL, " +
                COL_DEST_PROVINCE + " TEXT NOT NULL, " +
                COL_DEST_CATEGORY + " TEXT NOT NULL, " +
                COL_DEST_LAT + " REAL NOT NULL, " +
                COL_DEST_LNG + " REAL NOT NULL, " +
                COL_DEST_HOURS + " TEXT, " +
                COL_DEST_TERM_ID + " INTEGER, " +
                COL_DEST_TERM_NAME + " TEXT, " +
                COL_DEST_ROUTE + " TEXT, " +
                COL_DEST_ACCESSIBILITY + " TEXT, " +
                COL_DEST_IMAGE + " TEXT);");

        // Create Safety Reports Table
        db.execSQL("CREATE TABLE " + TABLE_SAFETY_REPORTS + " (" +
                COL_REPORT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_REPORT_USER_ID + " INTEGER NOT NULL, " +
                COL_REPORT_USER_NAME + " TEXT NOT NULL, " +
                COL_REPORT_CATEGORY + " TEXT NOT NULL, " +
                COL_REPORT_LOCATION + " TEXT NOT NULL, " +
                COL_REPORT_DESC + " TEXT NOT NULL, " +
                COL_REPORT_STATUS + " TEXT NOT NULL DEFAULT 'PENDING_REVIEW', " +
                COL_REPORT_SEVERITY + " TEXT NOT NULL DEFAULT 'LOW', " +
                COL_REPORT_CREATED_AT + " TEXT NOT NULL, " +
                COL_REPORT_ADMIN_NOTES + " TEXT);");

        // Create Safety Information Table
        db.execSQL("CREATE TABLE " + TABLE_SAFETY_INFO + " (" +
                COL_INFO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_INFO_LOCATION + " TEXT NOT NULL, " +
                COL_INFO_MUNICIPALITY + " TEXT NOT NULL, " +
                COL_INFO_PROVINCE + " TEXT NOT NULL, " +
                COL_INFO_CROWD + " TEXT NOT NULL, " +
                COL_INFO_LIGHTING + " TEXT NOT NULL, " +
                COL_INFO_SCORE + " INTEGER NOT NULL, " +
                COL_INFO_NOTES + " TEXT, " +
                COL_INFO_REPORTS_COUNT + " INTEGER DEFAULT 0, " +
                COL_INFO_UPDATED + " TEXT NOT NULL);");

        seedDemoData(db);
        importTerminalsFromCsv(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAFETY_INFO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAFETY_REPORTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DESTINATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAVED_ROUTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTE_STOPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TERMINALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /** Re-adds the original bundled transit data without deleting or moving existing records. */
    public void restoreBundledTransitData() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            seedTransitData(db);
            importTerminalsFromCsv(db);
            ensureEstimatedRouteCoverage(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Backfills empty terminals with a fare computed from the supplied Region IV-A fare matrix. */
    public void ensureEstimatedRouteCoverage() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ensureEstimatedRouteCoverage(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static final class CoverageTerminal {
        final long id;
        final String name;
        final String city;
        final String province;
        final double latitude;
        final double longitude;

        CoverageTerminal(long id, String name, String city, String province, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.city = city;
            this.province = province;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private void ensureEstimatedRouteCoverage(SQLiteDatabase db) {
        List<CoverageTerminal> terminals = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT " + COL_TERM_ID + ", " + COL_TERM_NAME + ", " + COL_TERM_CITY +
                ", " + COL_TERM_PROVINCE + ", " + COL_TERM_LAT + ", " + COL_TERM_LNG + " FROM " +
                TABLE_TERMINALS + " ORDER BY " + COL_TERM_ID, null)) {
            while (cursor.moveToNext()) {
                terminals.add(new CoverageTerminal(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                        cursor.getDouble(4), cursor.getDouble(5)));
            }
        }

        for (CoverageTerminal origin : terminals) {
            updateGeneratedConnectorIfPresent(db, origin, terminals);
            long routeCount = DatabaseUtils.longForQuery(db,
                    "SELECT COUNT(*) FROM " + TABLE_ROUTES + " WHERE " + COL_ROUTE_TERM_ID + " = ?",
                    new String[]{String.valueOf(origin.id)});
            if (routeCount > 0) continue;

            CoverageTerminal destination = findNearestOtherMunicipality(origin, terminals);
            if (destination == null) continue;

            double straightLineKm = distanceKm(origin.latitude, origin.longitude,
                    destination.latitude, destination.longitude);
            double roadKm = Math.max(1.0, straightLineKm * 1.35);
            String type = inferTransportType(origin.name);
            double fare = fareFromRegionIvAMatrix(type, origin.name, roadKm);
            double averageKph = "Bus".equals(type) ? 30.0 : 18.0;
            int minutes = Math.max(10, (int) Math.ceil(roadKm / averageKph * 60.0));

            String name = origin.city + " to " + destination.city;
            String description = "Connector from " + origin.name + " to " + destination.name +
                    ". Fare is calculated using the supplied Region IV-A " + fareMatrixName(type, origin.name) +
                    " matrix; the actual fare depends on the vehicle class and route distance.";
            long routeId = insertRoute(db, origin.id, name, origin.city, destination.city, type, fare, minutes, description);
            if (routeId > 0 && destination.latitude != 0 && destination.longitude != 0) {
                insertStop(db, routeId, destination.name, 1, destination.latitude, destination.longitude);
            }
        }
    }

    /** Converts routes created by the earlier backfill in place, retaining their IDs and stops. */
    private void updateGeneratedConnectorIfPresent(SQLiteDatabase db, CoverageTerminal origin,
                                                   List<CoverageTerminal> terminals) {
        try (Cursor cursor = db.query(TABLE_ROUTES,
                new String[]{COL_ROUTE_ID},
                COL_ROUTE_TERM_ID + " = ? AND (" + COL_ROUTE_NAME + " LIKE 'ESTIMATED CONNECTOR%' OR " +
                        COL_ROUTE_DESC + " LIKE 'DEMO ESTIMATE ONLY%')",
                new String[]{String.valueOf(origin.id)}, null, null, null)) {
            while (cursor.moveToNext()) {
                CoverageTerminal destination = findNearestOtherMunicipality(origin, terminals);
                if (destination == null) continue;
                double roadKm = Math.max(1.0, distanceKm(origin.latitude, origin.longitude,
                        destination.latitude, destination.longitude) * 1.35);
                String type = inferTransportType(origin.name);
                String name = origin.city + " to " + destination.city;
                String description = "Connector from " + origin.name + " to " + destination.name +
                        ". Fare is calculated using the supplied Region IV-A " + fareMatrixName(type, origin.name) +
                        " matrix; the actual fare depends on the vehicle class and route distance.";
                ContentValues values = new ContentValues();
                values.put(COL_ROUTE_NAME, name);
                values.put(COL_ROUTE_ORIGIN, origin.city);
                values.put(COL_ROUTE_DESTINATION, destination.city);
                values.put(COL_ROUTE_TRANSPORT_TYPE, type);
                values.put(COL_ROUTE_FARE, fareFromRegionIvAMatrix(type, origin.name, roadKm));
                values.put(COL_ROUTE_TIME, Math.max(10, (int) Math.ceil(roadKm /
                        ("Bus".equals(type) ? 30.0 : 18.0) * 60.0)));
                values.put(COL_ROUTE_DESC, description);
                db.update(TABLE_ROUTES, values, COL_ROUTE_ID + " = ?",
                        new String[]{String.valueOf(cursor.getLong(0))});
            }
        }
    }

    private static double fareFromRegionIvAMatrix(String type, String terminalName, double distanceKm) {
        if ("Bus".equals(type)) {
            boolean airConditioned = terminalName != null &&
                    (terminalName.toLowerCase(Locale.ROOT).contains("aircon") ||
                            terminalName.toLowerCase(Locale.ROOT).contains("air-conditioned") ||
                            terminalName.toLowerCase(Locale.ROOT).contains("ac bus"));
            return (airConditioned ? 15.0 : 13.0) + Math.max(0.0, distanceKm - 5.0) *
                    (airConditioned ? 2.65 : 2.25);
        }
        // Traditional public utility jeepney fares from the supplied CALABARZON matrix.
        return 14.0 + Math.max(0.0, distanceKm - 4.0) * 2.0;
    }

    private static String fareMatrixName(String type, String terminalName) {
        if (!"Bus".equals(type)) return "Traditional PUJ";
        boolean airConditioned = terminalName != null &&
                (terminalName.toLowerCase(Locale.ROOT).contains("aircon") ||
                        terminalName.toLowerCase(Locale.ROOT).contains("air-conditioned") ||
                        terminalName.toLowerCase(Locale.ROOT).contains("ac bus"));
        return airConditioned ? "Air-Conditioned Provincial Bus" : "Ordinary Provincial Bus";
    }

    private static CoverageTerminal findNearestOtherMunicipality(CoverageTerminal origin, List<CoverageTerminal> terminals) {
        CoverageTerminal best = null;
        double bestScore = Double.MAX_VALUE;
        for (CoverageTerminal candidate : terminals) {
            if (candidate.id == origin.id || sameLocation(origin.city, candidate.city) ||
                    candidate.latitude == 0 || candidate.longitude == 0) continue;
            double score = distanceKm(origin.latitude, origin.longitude, candidate.latitude, candidate.longitude);
            if (!origin.province.equalsIgnoreCase(candidate.province)) score += 100.0;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean sameLocation(String first, String second) {
        String a = first == null ? "" : first.trim().toLowerCase(Locale.ROOT);
        String b = second == null ? "" : second.trim().toLowerCase(Locale.ROOT);
        return a.equals(b);
    }

    private static String inferTransportType(String terminalName) {
        String name = terminalName == null ? "" : terminalName.toLowerCase(Locale.ROOT);
        if (name.contains("bus")) return "Bus";
        return "Jeepney";
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private long seedTransitData(SQLiteDatabase db) {
        // 2. Seed CALABARZON Terminals (16 realistic terminals across Laguna, Cavite, Batangas, Rizal, Quezon)
        // LAGUNA
        long termCalamba = insertTerminal(db, "Calamba Jeepney Terminal", "Calamba", "Laguna", 14.2132, 121.1648, "Central terminal near SM City Calamba and Crossing.");
        long termCabuyao = insertTerminal(db, "Cabuyao Bayan Jeepney Terminal", "Cabuyao", "Laguna", 14.2700, 121.1300, "Cabuyao Bayan loading point on the Calamba-Laguna National Highway.");
        long termBalibago = insertTerminal(db, "Balibago Complex Terminal", "Santa Rosa", "Laguna", 14.2965, 121.1114, "Primary transit hub connecting Santa Rosa to Metro Manila and Laguna towns.");
        long termPacita = insertTerminal(db, "Pacita Central Terminal", "San Pedro", "Laguna", 14.3492, 121.0543, "Major southern gateway terminal serving San Pedro and Biñan commuters.");
        long termBinan = insertTerminal(db, "Biñan Central Jeepney Terminal", "Biñan", "Laguna", 14.3382, 121.0825, "Located near Biñan People's Center and market district.");
        long termLosBanos = insertTerminal(db, "Los Baños Junction Terminal", "Los Baños", "Laguna", 14.1706, 121.2428, "Gateway terminal for UPLB campus, IRRI, and thermal springs.");
        long termSanPablo = insertTerminal(db, "San Pablo City Public Terminal", "San Pablo", "Laguna", 14.0683, 121.3256, "Central hub servicing the City of Seven Lakes.");

        // CAVITE
        long termDasma = insertTerminal(db, "Dasmariñas Central Terminal (Pala-Pala)", "Dasmariñas", "Cavite", 14.2981, 120.9575, "Major Cavite crossroad intersection terminal near SM Dasmariñas and Robinsons.");
        long termBacoor = insertTerminal(db, "Bacoor St. Dominic Terminal", "Bacoor", "Cavite", 14.4442, 120.9702, "North Cavite terminal connecting coastal commuters to Metro Manila.");
        long termImus = insertTerminal(db, "Imus Transport Terminal (Lumang Bayan)", "Imus", "Cavite", 14.4295, 120.9367, "Central district hub along Aguinaldo Highway.");
        long termTagaytay = insertTerminal(db, "Tagaytay Olivarez Plaza Terminal", "Tagaytay", "Cavite", 14.1153, 120.9621, "Tourist and commuter hub along Tagaytay-Calamba Road.");

        // BATANGAS
        long termBatangasGrand = insertTerminal(db, "Batangas City Grand Terminal", "Batangas City", "Batangas", 13.7844, 121.0664, "Provincial multimodal terminal with direct connections to Batangas Port.");
        long termLipa = insertTerminal(db, "Lipa SM City Grand Terminal", "Lipa City", "Batangas", 13.9419, 121.1631, "Major eastern Batangas hub serving routes to Manila and Laguna.");
        long termTanauan = insertTerminal(db, "Tanauan City Transport Terminal", "Tanauan", "Batangas", 14.0858, 121.1506, "Northern Batangas junction serving industrial parks and commuters.");

        // RIZAL
        long termAntipolo = insertTerminal(db, "Antipolo Masinag Transit Terminal", "Antipolo", "Rizal", 14.6231, 121.1219, "LRT-2 connected multimodal terminal serving upper and lower Antipolo.");
        long termTaytay = insertTerminal(db, "Taytay Bagong Palengke Terminal", "Taytay", "Rizal", 14.5682, 121.1342, "Garments capital hub with routes to Ortigas, Pasig, and Cainta.");

        // QUEZON
        long termLucena = insertTerminal(db, "Lucena Grand Central Terminal", "Lucena City", "Quezon", 13.9511, 121.6169, "Quezon province's premier integrated interprovincial bus and jeepney terminal.");

        // 3. Seed Realistic Routes (34 sample routes across all 5 provinces)
        // LAGUNA ROUTES
        long r1 = insertRoute(db, termCalamba, "Calamba – Santa Rosa (Via Balibago)", "Calamba", "Santa Rosa", "Jeepney", 30.00, 45, "Regular jeepney line running via National Highway passing Cabuyao and Balibago.");
        insertStop(db, r1, "Calamba Crossing Terminal", 1, 14.2132, 121.1648);
        insertStop(db, r1, "Parian Checkpoint", 2, 14.2300, 121.1500);
        insertStop(db, r1, "Cabuyao Bayan", 3, 14.2700, 121.1300);
        insertStop(db, r1, "Balibago Commercial Complex", 4, 14.2965, 121.1114);
        insertStop(db, r1, "Santa Rosa Bayan", 5, 14.3100, 121.1100);

        long r2 = insertRoute(db, termCalamba, "Calamba – Santa Rosa (Nuvali Bus Express)", "Calamba", "Santa Rosa", "Bus", 40.00, 35, "Air-conditioned P2P bus from Calamba SM to Nuvali Santa Rosa.");
        insertStop(db, r2, "SM City Calamba Bay 2", 1, 0.0, 0.0);
        insertStop(db, r2, "Mayapa SLEX Entry", 2, 0.0, 0.0);
        insertStop(db, r2, "Nuvali Robinsons Transport Hub", 3, 0.0, 0.0);
        insertStop(db, r2, "Santa Rosa Hospital & Bayan", 4, 0.0, 0.0);

        long r3 = insertRoute(db, termCalamba, "Calamba Crossing – Los Baños Junction", "Calamba", "Los Baños", "Jeepney", 22.00, 25, "Jeepney connecting Calamba town proper to UPLB entrance and hot spring resorts.");
        insertStop(db, r3, "Calamba Terminal", 1, 0.0, 0.0);
        insertStop(db, r3, "Bucal Bypass", 2, 0.0, 0.0);
        insertStop(db, r3, "Pansol Spring Resort Strip", 3, 0.0, 0.0);
        insertStop(db, r3, "Los Baños Junction", 4, 0.0, 0.0);

        long r4 = insertRoute(db, termBalibago, "Santa Rosa (Balibago) – Biñan Bayan", "Santa Rosa", "Biñan", "Jeepney", 18.00, 20, "Short-haul connector jeepney via Old National Highway.");
        insertStop(db, r4, "Balibago Terminal", 1, 0.0, 0.0);
        insertStop(db, r4, "Macabling Junction", 2, 0.0, 0.0);
        insertStop(db, r4, "Pavilion Mall", 3, 0.0, 0.0);
        insertStop(db, r4, "Biñan Public Market", 4, 0.0, 0.0);

        long r5 = insertRoute(db, termBalibago, "Santa Rosa – Calamba (Via Cabuyao)", "Santa Rosa", "Calamba", "Jeepney", 30.00, 45, "Return journey from Santa Rosa Balibago complex southwards to Calamba Crossing.");
        insertStop(db, r5, "Balibago Terminal", 1, 0.0, 0.0);
        insertStop(db, r5, "SM City Santa Rosa", 2, 0.0, 0.0);
        insertStop(db, r5, "Cabuyao Katapatan", 3, 0.0, 0.0);
        insertStop(db, r5, "Calamba Crossing Terminal", 4, 0.0, 0.0);

        long r6 = insertRoute(db, termPacita, "Pacita – Biñan Central", "San Pedro", "Biñan", "Jeepney", 15.00, 15, "Fast local route along Manila South Road connecting San Pedro and Biñan.");
        long r7 = insertRoute(db, termPacita, "San Pedro – Alabang Starmall", "San Pedro", "Muntinlupa", "Jeepney", 25.00, 30, "Metro boundary connection from Pacita directly to Alabang terminal.");
        long r8 = insertRoute(db, termBinan, "Biñan – Carmona Cavite", "Biñan", "Carmona", "Jeepney", 17.00, 20, "Interprovincial Laguna-Cavite route through Southwoods.");
        long r9 = insertRoute(db, termLosBanos, "Los Baños – San Pablo City", "Los Baños", "San Pablo", "Jeepney", 35.00, 40, "Scenic route via Bay and Calauan pinya highway to San Pablo.");
        long r10 = insertRoute(db, termSanPablo, "San Pablo – Calamba Crossing", "San Pablo", "Calamba", "Bus", 55.00, 50, "Direct provincial bus via Maharlika Highway.");

        // CAVITE ROUTES
        long r11 = insertRoute(db, termDasma, "Dasmariñas (Pala-Pala) – Tagaytay Olivarez", "Dasmariñas", "Tagaytay", "Bus", 50.00, 40, "Air-conditioned provincial bus climbing Aguinaldo Highway into Tagaytay.");
        insertStop(db, r11, "Robinsons Dasma Terminal", 1, 0.0, 0.0);
        insertStop(db, r11, "Silang Bypass", 2, 0.0, 0.0);
        insertStop(db, r11, "Tagaytay Rotonda", 3, 0.0, 0.0);
        insertStop(db, r11, "Olivarez Plaza Hub", 4, 0.0, 0.0);

        long r12 = insertRoute(db, termDasma, "Dasmariñas – Imus Lumang Bayan", "Dasmariñas", "Imus", "Jeepney", 24.00, 30, "Main commuter spine connecting south and central Cavite.");
        long r13 = insertRoute(db, termDasma, "Dasmariñas – Bacoor St. Dominic", "Dasmariñas", "Bacoor", "Jeepney", 32.00, 45, "High-density commuter route along Aguinaldo Highway.");
        long r14 = insertRoute(db, termTagaytay, "Tagaytay Olivarez – Santa Rosa (Balibago)", "Tagaytay", "Santa Rosa", "Jeepney", 45.00, 55, "Scenic downhill descent along Santa Rosa-Tagaytay Road passing Paseo de Santa Rosa.");
        insertStop(db, r14, "Olivarez Jeepney Bay", 1, 0.0, 0.0);
        insertStop(db, r14, "Nuvali South Gate", 2, 0.0, 0.0);
        insertStop(db, r14, "Paseo Outlets", 3, 0.0, 0.0);
        insertStop(db, r14, "Balibago Terminal", 4, 0.0, 0.0);

        long r15 = insertRoute(db, termTagaytay, "Tagaytay – Calamba Crossing", "Tagaytay", "Calamba", "Jeepney", 55.00, 65, "Interprovincial Cavite-Laguna jeepney descending into Canlubang.");
        long r16 = insertRoute(db, termBacoor, "Bacoor – PITX Metro Manila", "Bacoor", "Parañaque", "Bus", 35.00, 30, "Express bus via Cavitex directly to Parañaque Integrated Terminal Exchange.");
        long r17 = insertRoute(db, termImus, "Imus – Dasmariñas Pala-Pala", "Imus", "Dasmariñas", "Jeepney", 24.00, 30, "Southbound jeepney to Dasmariñas commerce district.");

        // BATANGAS ROUTES
        long r18 = insertRoute(db, termBatangasGrand, "Batangas Grand Terminal – Lipa City SM", "Batangas City", "Lipa City", "Jeepney", 42.00, 50, "Main arterial Batangas route connecting capital city to Lipa.");
        long r19 = insertRoute(db, termBatangasGrand, "Batangas Grand Terminal – Calamba Crossing", "Batangas City", "Calamba", "Bus", 95.00, 75, "Provincial highway bus via STAR Tollway to Laguna.");
        long r20 = insertRoute(db, termLipa, "Lipa City – Tanauan Terminal", "Lipa City", "Tanauan", "Jeepney", 30.00, 35, "Jeepney route passing Malvar industrial zone.");
        long r21 = insertRoute(db, termLipa, "Lipa SM – San Pablo City", "Lipa City", "San Pablo", "Jeepney", 38.00, 45, "Route passing through Alaminos into Laguna.");
        long r22 = insertRoute(db, termTanauan, "Tanauan – Calamba Crossing", "Tanauan", "Calamba", "Jeepney", 28.00, 35, "Boundary crossing jeepney connecting Batangas to Laguna.");
        insertStop(db, r22, "Tanauan Public Market", 1, 0.0, 0.0);
        insertStop(db, r22, "Santo Tomas Junction", 2, 0.0, 0.0);
        insertStop(db, r22, "Turbina Flyover", 3, 0.0, 0.0);
        insertStop(db, r22, "Calamba Crossing", 4, 0.0, 0.0);

        // RIZAL ROUTES
        long r23 = insertRoute(db, termAntipolo, "Antipolo Masinag – Taytay Bagong Palengke", "Antipolo", "Taytay", "Jeepney", 20.00, 25, "Connector route down Cabrera Road to Taytay marketplace.");
        long r24 = insertRoute(db, termAntipolo, "Antipolo Simbahan – Cubao LRT-2", "Antipolo", "Quezon City", "Jeepney", 35.00, 45, "Iconic eastern Rizal to Metro Manila commuter jeepney.");
        long r25 = insertRoute(db, termTaytay, "Taytay Bagong Palengke – Cainta Junction", "Taytay", "Cainta", "Jeepney", 16.00, 18, "Short haul route along Ortigas Extension.");
        long r26 = insertRoute(db, termTaytay, "Taytay – Pasig Palengke", "Taytay", "Pasig", "Jeepney", 24.00, 30, "Direct boundary commuter jeepney into eastern Metro Manila.");

        // QUEZON ROUTES
        long r27 = insertRoute(db, termLucena, "Lucena Grand Terminal – Sariaya Town Proper", "Lucena City", "Sariaya", "Jeepney", 25.00, 30, "Westbound Quezon commuter line.");
        long r28 = insertRoute(db, termLucena, "Lucena Grand Terminal – San Pablo City", "Lucena City", "San Pablo", "Bus", 75.00, 60, "Provincial bus connecting Quezon province into southern Laguna.");
        long r29 = insertRoute(db, termLucena, "Lucena – Batangas Grand Terminal", "Lucena City", "Batangas City", "Bus", 120.00, 90, "Interprovincial bus connecting Southern Tagalog coastal hubs.");

        // MULTI-MODAL DEMO SPECIFIC ROUTES
        long r30 = insertRoute(db, termCalamba, "Calamba Crossing to SM City Walkway", "Calamba", "Calamba SM", "Walking", 0.00, 8, "Designated pedestrian overpass and sidewalk walkway.");
        long r31 = insertRoute(db, termBalibago, "Balibago Terminal to Target Mall Walkway", "Santa Rosa", "Santa Rosa Target Mall", "Walking", 0.00, 5, "Covered pedestrian walkway linking terminal to mall entrance.");
        long r32 = insertRoute(db, termDasma, "Dasmariñas Pala-Pala Inter-Terminal Link", "Dasmariñas", "SM Dasmariñas", "Walking", 0.00, 6, "Pedestrian footbridge connecting bus bay to jeepney queue.");
        long r33 = insertRoute(db, termCalamba, "Calamba – Santa Rosa (Via Cabuyao Express Jeep)", "Calamba", "Santa Rosa", "Jeepney", 32.00, 40, "Fast commuter jeepney with minimal passenger stops along bypass road.");
        long r34 = insertRoute(db, termBalibago, "Santa Rosa – Manila (Buendia LRT P2P)", "Santa Rosa", "Pasay", "Bus", 85.00, 60, "Point-to-point commuter bus operating via SLEX.");
        // Distance-based PUJ fare estimates from the user-supplied Region IV-A matrix:
        // ₱14 base for the first 4 km, then ₱2/km; eligible discount is 20%.
        long calambaCabuyao = insertRoute(db, termCalamba, "Calamba – Cabuyao Bayan via National Road (Jeepney)", "Calamba", "Cabuyao City", "Jeepney", 25.00, 30,
                "Estimated 9.5 km via National Road. Estimated matrix fare: ₱25 regular / ₱20 discounted (20%). Distance, fare and travel time are estimates; confirm with the driver/operator.");
        insertStop(db, calambaCabuyao, "Calamba Crossing Terminal", 1, 14.2132, 121.1648);
        insertStop(db, calambaCabuyao, "Parian Checkpoint", 2, 14.2300, 121.1500);
        insertStop(db, calambaCabuyao, "Cabuyao Bayan Jeepney Terminal", 3, 14.2700, 121.1300);
        long cabuyaoCalamba = insertRoute(db, termCabuyao, "Cabuyao – Calamba via National Road (Jeepney)", "Cabuyao City", "Calamba", "Jeepney", 25.00, 30,
                "Return via National Road. Estimated 9.5 km and matrix fare ₱25 regular / ₱20 discounted (20%). Distance, fare and travel time are estimates; confirm with the driver/operator.");
        insertStop(db, cabuyaoCalamba, "Cabuyao Bayan Jeepney Terminal", 1, 14.2700, 121.1300);
        insertStop(db, cabuyaoCalamba, "Parian Checkpoint", 2, 14.2300, 121.1500);
        insertStop(db, cabuyaoCalamba, "Calamba Crossing Terminal", 3, 14.2132, 121.1648);

        long calambaBinan = insertRoute(db, termCalamba, "Calamba – Biñan via National Road (Jeepney)", "Calamba", "Biñan", "Jeepney", 44.00, 55,
                "Estimated 19 km via National Road through Cabuyao and Santa Rosa. Estimated matrix fare: ₱44 regular / ₱35.20 discounted (20%). Distance, fare and travel time are estimates; confirm with the driver/operator.");
        insertStop(db, calambaBinan, "Calamba Crossing Terminal", 1, 14.2132, 121.1648);
        insertStop(db, calambaBinan, "Cabuyao Bayan", 2, 14.2700, 121.1300);
        insertStop(db, calambaBinan, "Santa Rosa Bayan", 3, 14.3100, 121.1100);
        insertStop(db, calambaBinan, "Biñan Central Jeepney Terminal", 4, 14.3382, 121.0825);
        long binanCalamba = insertRoute(db, termBinan, "Biñan – Calamba via National Road (Jeepney)", "Biñan", "Calamba", "Jeepney", 44.00, 55,
                "Return via National Road through Santa Rosa and Cabuyao. Estimated 19 km and matrix fare ₱44 regular / ₱35.20 discounted (20%). Distance, fare and travel time are estimates; confirm with the driver/operator.");
        insertStop(db, binanCalamba, "Biñan Central Jeepney Terminal", 1, 14.3382, 121.0825);
        insertStop(db, binanCalamba, "Santa Rosa Bayan", 2, 14.3100, 121.1100);
        insertStop(db, binanCalamba, "Cabuyao Bayan", 3, 14.2700, 121.1300);
        insertStop(db, binanCalamba, "Calamba Crossing Terminal", 4, 14.2132, 121.1648);
        // Calamba–Lipa fares supplied by the user. Regular fare is stored in fare;
        // discounted fare is shown in the description because this schema has one fare column.
        insertRoute(db, termCalamba, "Calamba – Lipa City via National Road (Jeepney)", "Calamba", "Lipa City", "Jeepney", 60.00, 90,
                "Via National Road. Regular fare: ₱60; discounted fare: ₱50 for eligible passengers. Travel time is an estimate; confirm the route and fare with the operator.");
        insertRoute(db, termLipa, "Lipa City – Calamba via National Road (Jeepney)", "Lipa City", "Calamba", "Jeepney", 60.00, 90,
                "Return direction via National Road. Regular fare: ₱60; discounted fare: ₱50 for eligible passengers. Travel time is an estimate; confirm with the operator.");
        insertRoute(db, termCalamba, "Calamba – Lipa City via SLEX Expressway (Van)", "Calamba", "Lipa City", "UV Express", 130.00, 75,
                "Via SLEX Expressway. Regular fare: ₱130; discounted fare: ₱90 for eligible passengers. Travel time is an estimate; confirm the route and fare with the operator.");
        insertRoute(db, termLipa, "Lipa City – Calamba via SLEX Expressway (Van)", "Lipa City", "Calamba", "UV Express", 130.00, 75,
                "Return direction via SLEX Expressway. Regular fare: ₱130; discounted fare: ₱90 for eligible passengers. Travel time is an estimate; confirm with the operator.");

        // User requested this connection; fare and travel time are estimates pending operator verification.
        insertRoute(db, termCalamba, "Calamba – Robinsons Dasma Pala-Pala (Estimated Multi-modal)", "Calamba", "Dasmariñas", "Multi-modal", 120.00, 120,
                "Estimated connection to Robinsons Place Dasma Pala-Pala, Dasmariñas. Estimated fare ₱120 regular / ₱96 discounted and travel time about 120 minutes; vehicle legs, transfer points, fare, and schedule are not operator-verified. Confirm locally before travel.");
        insertRoute(db, termDasma, "Robinsons Dasma Pala-Pala – Calamba (Estimated Multi-modal)", "Dasmariñas", "Calamba", "Multi-modal", 120.00, 120,
                "Return connection to Calamba. Estimated fare ₱120 regular / ₱96 discounted and travel time about 120 minutes; vehicle legs, transfer points, fare, and schedule are not operator-verified. Confirm locally before travel.");

        return r1;
    }

    private void seedDemoData(SQLiteDatabase db) {
        // 1. Seed Demo Accounts
        // Admin account: admin@transitph.test / Admin123!
        ContentValues adminUser = new ContentValues();
        adminUser.put(COL_USER_FULL_NAME, "TransitPH Administrator");
        adminUser.put(COL_USER_EMAIL, "admin@transitph.test");
        adminUser.put(COL_USER_PASSWORD_HASH, PasswordUtils.hashPassword("Admin123!"));
        adminUser.put(COL_USER_ROLE, "ADMIN");
        db.insert(TABLE_USERS, null, adminUser);

        // Commuter account: user@transitph.test / User123!
        ContentValues normalUser = new ContentValues();
        normalUser.put(COL_USER_FULL_NAME, "Maria Santos");
        normalUser.put(COL_USER_EMAIL, "user@transitph.test");
        normalUser.put(COL_USER_PASSWORD_HASH, PasswordUtils.hashPassword("User123!"));
        normalUser.put(COL_USER_ROLE, "USER");
        long normalUserId = db.insert(TABLE_USERS, null, normalUser);

        long r1 = seedTransitData(db);

        // 4. Pre-save one sample route for the demo user
        ContentValues saved = new ContentValues();
        saved.put(COL_SAVED_USER_ID, normalUserId);
        saved.put(COL_SAVED_ROUTE_ID, r1);
        db.insert(TABLE_SAVED_ROUTES, null, saved);

        // 5. Seed CALABARZON Popular Destinations & Safety Data
        seedDestinationsAndSafety(db, normalUserId);
    }

    private void seedDestinationsAndSafety(SQLiteDatabase db, long normalUserId) {
        // Seed Destinations
        insertDestinationDirect(db, "Enchanted Kingdom", "World-class theme park and premier amusement destination.", "Santa Rosa", "Laguna", "Tourist Attraction", 14.2829, 121.0975, "11:00 AM - 8:00 PM", 2, "Balibago Complex Terminal", "Take tricycle or shuttle from Balibago Complex direct to EK Gate.", "Wheelchair accessible, dedicated drop-off zone", "");
        insertDestinationDirect(db, "Nuvali Park & Solenad Malls", "Eco-city commercial complex with dining, lake, and outlet shops.", "Santa Rosa", "Laguna", "Shopping", 14.2384, 121.0583, "10:00 AM - 9:00 PM", 2, "Balibago Complex Terminal", "P2P bus or Nuvali e-jeep from Balibago or Calamba Crossing.", "Paved ramps, accessible restrooms, EV charging", "");
        insertDestinationDirect(db, "UPLB Campus & Makiling Reserve", "Premier state university botanical haven and research institution.", "Los Baños", "Laguna", "School/University", 14.1675, 121.2435, "6:00 AM - 8:00 PM", 5, "Los Baños Junction Terminal", "UPLB College jeepneys directly loop through campus from junction.", "Wide pedestrian paths, university infirmary", "");
        insertDestinationDirect(db, "Tagaytay Picnic Grove & Ridge", "Iconic ridge overlooking Taal Volcano with open-air gazebos.", "Tagaytay", "Cavite", "Tourist Attraction", 14.1337, 120.9856, "7:00 AM - 7:00 PM", 10, "Tagaytay Olivarez Plaza Terminal", "Tagaytay-Calamba jeepney drops off directly in front of entrance.", "Elevated viewing decks, assistance staff", "");
        insertDestinationDirect(db, "Aguinaldo Shrine & Museum", "Historic birthplace of the Philippine Republic and Independence.", "Kawit", "Cavite", "Tourist Attraction", 14.4447, 120.9022, "8:00 AM - 4:00 PM", 8, "Bacoor St. Dominic Terminal", "Jeepney via Kawit / Noveleta stops right in front of historical park.", "Ramps at ground floor, guided tour assistance", "");
        insertDestinationDirect(db, "De La Salle University - Dasmariñas", "Leading educational institution with lush sprawling campus.", "Dasmariñas", "Cavite", "School/University", 14.3262, 120.9599, "7:00 AM - 8:00 PM", 7, "Dasmariñas Central Terminal (Pala-Pala)", "DBB-1 or Area-C jeepney from Pala-Pala terminal directly to Gate 1 & 3.", "Tactile paving, campus shuttle service", "");
        insertDestinationDirect(db, "Batangas Port Passenger Terminal", "Major maritime seaport connecting Luzon to Mindoro, Visayas.", "Batangas City", "Batangas", "Government", 13.7578, 121.0450, "24/7 Operations", 11, "Batangas City Grand Terminal", "Port jeepney or commuter shuttle direct from Grand Terminal.", "Elevators, luggage assistance, accessible security gates", "");
        insertDestinationDirect(db, "Antipolo Cathedral (National Shrine)", "Renowned pilgrimage destination housing Our Lady of Peace and Good Voyage.", "Antipolo", "Rizal", "Tourist Attraction", 14.5878, 121.1764, "5:00 AM - 9:00 PM", 14, "Antipolo Masinag Transit Terminal", "Antipolo Simbahan jeepney from Masinag or Cubao.", "Ground level church ramps, priority pews", "");

        // Seed Safety Information
        insertSafetyInfoDirect(db, "Calamba Crossing Walkway", "Calamba", "Laguna", "High", "Well Lit", 88, "Covered overpass with security personnel active until 10:00 PM.", 0, "2026-09-15");
        insertSafetyInfoDirect(db, "Balibago Complex Terminal Waiting Bay", "Santa Rosa", "Laguna", "High", "Well Lit", 92, "High police presence, well-lit perimeter, 24/7 terminal dispatchers.", 0, "2026-09-15");
        insertSafetyInfoDirect(db, "Pala-Pala Dasmariñas Footbridge", "Dasmariñas", "Cavite", "Moderate", "Moderate", 82, "Adequate lighting, peak crowd during 5:00 PM - 8:00 PM commute.", 1, "2026-09-14");
        insertSafetyInfoDirect(db, "Antipolo Masinag LRT-2 Transfer Bay", "Antipolo", "Rizal", "High", "Well Lit", 94, "CCTV monitored, security roving, direct station connection.", 0, "2026-09-15");

        // Seed Initial Safety Report
        ContentValues rep = new ContentValues();
        rep.put(COL_REPORT_USER_ID, normalUserId);
        rep.put(COL_REPORT_USER_NAME, "Juan Dela Cruz");
        rep.put(COL_REPORT_CATEGORY, "Poor Lighting");
        rep.put(COL_REPORT_LOCATION, "Cabuyao Bayan Jeepney Stop");
        rep.put(COL_REPORT_DESC, "Streetlight near the waiting shed flickers intermittently during night commute. Commuters advised to wait near convenience store.");
        rep.put(COL_REPORT_STATUS, "VERIFIED");
        rep.put(COL_REPORT_SEVERITY, "LOW");
        rep.put(COL_REPORT_CREATED_AT, "2026-09-14 19:30:00");
        rep.put(COL_REPORT_ADMIN_NOTES, "Report forwarded to local engineering office for maintenance.");
        db.insert(TABLE_SAFETY_REPORTS, null, rep);
    }

    private void insertDestinationDirect(SQLiteDatabase db, String name, String desc, String muni, String prov,
                                         String cat, double lat, double lng, String hours, long termId,
                                         String termName, String route, String access, String img) {
        ContentValues cv = new ContentValues();
        cv.put(COL_DEST_NAME, name);
        cv.put(COL_DEST_DESC, desc);
        cv.put(COL_DEST_MUNICIPALITY, muni);
        cv.put(COL_DEST_PROVINCE, prov);
        cv.put(COL_DEST_CATEGORY, cat);
        cv.put(COL_DEST_LAT, lat);
        cv.put(COL_DEST_LNG, lng);
        cv.put(COL_DEST_HOURS, hours);
        cv.put(COL_DEST_TERM_ID, termId);
        cv.put(COL_DEST_TERM_NAME, termName);
        cv.put(COL_DEST_ROUTE, route);
        cv.put(COL_DEST_ACCESSIBILITY, access);
        cv.put(COL_DEST_IMAGE, img);
        db.insert(TABLE_DESTINATIONS, null, cv);
    }

    private void insertSafetyInfoDirect(SQLiteDatabase db, String loc, String muni, String prov,
                                        String crowd, String light, int score, String notes,
                                        int repCount, String updated) {
        ContentValues cv = new ContentValues();
        cv.put(COL_INFO_LOCATION, loc);
        cv.put(COL_INFO_MUNICIPALITY, muni);
        cv.put(COL_INFO_PROVINCE, prov);
        cv.put(COL_INFO_CROWD, crowd);
        cv.put(COL_INFO_LIGHTING, light);
        cv.put(COL_INFO_SCORE, score);
        cv.put(COL_INFO_NOTES, notes);
        cv.put(COL_INFO_REPORTS_COUNT, repCount);
        cv.put(COL_INFO_UPDATED, updated);
        db.insert(TABLE_SAFETY_INFO, null, cv);
    }

    private long insertTerminal(SQLiteDatabase db, String name, String city, String province, double lat, double lng, String desc) {
        long existingId = DatabaseUtils.longForQuery(db,
                "SELECT COALESCE((SELECT " + COL_TERM_ID + " FROM " + TABLE_TERMINALS +
                        " WHERE lower(trim(" + COL_TERM_NAME + ")) = lower(trim(?)) AND lower(trim(" + COL_TERM_CITY +
                        ")) = lower(trim(?)) AND lower(trim(" + COL_TERM_PROVINCE + ")) = lower(trim(?)) LIMIT 1), -1)",
                new String[]{name, city, province});
        if (existingId > 0) return existingId; // preserve existing terminal fields and coordinates
        ContentValues cv = new ContentValues();
        cv.put(COL_TERM_NAME, name);
        cv.put(COL_TERM_CITY, city);
        cv.put(COL_TERM_PROVINCE, province);
        cv.put(COL_TERM_LAT, lat);
        cv.put(COL_TERM_LNG, lng);
        cv.put(COL_TERM_DESC, desc);
        return db.insert(TABLE_TERMINALS, null, cv);
    }

    private long insertRoute(SQLiteDatabase db, long termId, String name, String origin, String dest, String type, double fare, int time, String desc) {
        long existingId = DatabaseUtils.longForQuery(db,
                "SELECT COALESCE((SELECT " + COL_ROUTE_ID + " FROM " + TABLE_ROUTES + " WHERE " + COL_ROUTE_TERM_ID +
                        " = ? AND lower(trim(" + COL_ROUTE_NAME + ")) = lower(trim(?)) AND lower(trim(" + COL_ROUTE_ORIGIN +
                        ")) = lower(trim(?)) AND lower(trim(" + COL_ROUTE_DESTINATION + ")) = lower(trim(?)) AND lower(trim(" +
                        COL_ROUTE_TRANSPORT_TYPE + ")) = lower(trim(?)) LIMIT 1), -1)",
                new String[]{String.valueOf(termId), name, origin, dest, type});
        if (existingId > 0) return existingId; // preserve existing route values
        ContentValues cv = new ContentValues();
        cv.put(COL_ROUTE_TERM_ID, termId);
        cv.put(COL_ROUTE_NAME, name);
        cv.put(COL_ROUTE_ORIGIN, origin);
        cv.put(COL_ROUTE_DESTINATION, dest);
        cv.put(COL_ROUTE_TRANSPORT_TYPE, type);
        cv.put(COL_ROUTE_FARE, fare);
        cv.put(COL_ROUTE_TIME, time);
        cv.put(COL_ROUTE_DESC, desc);
        return db.insert(TABLE_ROUTES, null, cv);
    }

    private void importTerminalsFromCsv(SQLiteDatabase db) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open("terminals.csv")));
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] tokens = line.split(",");
                if (tokens.length >= 8) {
                    String name = tokens[1].trim();
                    String city = tokens[2].trim();
                    String province = tokens[3].trim();
                    double lat = 0.0;
                    double lng = 0.0;
                    try {
                        lat = Double.parseDouble(tokens[5].trim());
                        lng = Double.parseDouble(tokens[6].trim());
                    } catch (NumberFormatException e) {
                        // Keep as 0.0
                    }
                    String description = tokens[7].trim();
                    insertTerminal(db, name, city, province, lat, lng, description);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void insertStop(SQLiteDatabase db, long routeId, String stopName, int seq, double lat, double lng) {
        long existingCount = DatabaseUtils.longForQuery(db,
                "SELECT COUNT(*) FROM " + TABLE_ROUTE_STOPS + " WHERE " + COL_STOP_ROUTE_ID +
                        " = ? AND lower(trim(" + COL_STOP_NAME + ")) = lower(trim(?))",
                new String[]{String.valueOf(routeId), stopName});
        if (existingCount > 0) return;
        ContentValues cv = new ContentValues();
        cv.put(COL_STOP_ROUTE_ID, routeId);
        cv.put(COL_STOP_NAME, stopName);
        cv.put(COL_STOP_SEQUENCE, seq);
        cv.put(COL_STOP_LAT, lat);
        cv.put(COL_STOP_LNG, lng);
        db.insert(TABLE_ROUTE_STOPS, null, cv);
    }
}
