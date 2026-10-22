package com.transitph.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.transitph.app.models.Route;
import com.transitph.app.models.RouteStop;
import java.util.ArrayList;
import java.util.List;

public class RouteDao {
    private final DatabaseHelper dbHelper;

    public RouteDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public List<Route> getAllRoutes() {
        // Catalog cards need route fields only. Loading stop rows one by one here was
        // blocking the Routes tab while every route (including all imported routes) loaded.
        return searchRoutes("", "", false);
    }

    public List<Route> searchRoutes(String from, String to) {
        return searchRoutes(from, to, true);
    }

    private List<Route> searchRoutes(String from, String to, boolean includeStops) {
        List<Route> routes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT r.*, t." + DatabaseHelper.COL_TERM_NAME + " AS term_name " +
                     "FROM " + DatabaseHelper.TABLE_ROUTES + " r " +
                     "JOIN " + DatabaseHelper.TABLE_TERMINALS + " t ON r." + DatabaseHelper.COL_ROUTE_TERM_ID + " = t." + DatabaseHelper.COL_TERM_ID;

        List<String> argsList = new ArrayList<>();
        StringBuilder where = new StringBuilder();

        boolean hasFrom = (from != null && !from.trim().isEmpty());
        boolean hasTo = (to != null && !to.trim().isEmpty());

        if (hasFrom && hasTo) {
            String fromWildcard = "%" + from.trim() + "%";
            String toWildcard = "%" + to.trim() + "%";
            where.append(" WHERE (r.").append(DatabaseHelper.COL_ROUTE_ORIGIN).append(" LIKE ? OR ")
                 .append("r.").append(DatabaseHelper.COL_ROUTE_NAME).append(" LIKE ? OR ")
                 .append("t.").append(DatabaseHelper.COL_TERM_NAME).append(" LIKE ? OR ")
                 .append("t.").append(DatabaseHelper.COL_TERM_CITY).append(" LIKE ?)")
                 .append(" AND (r.").append(DatabaseHelper.COL_ROUTE_DESTINATION).append(" LIKE ? OR ")
                 .append("r.").append(DatabaseHelper.COL_ROUTE_NAME).append(" LIKE ?)");
            argsList.add(fromWildcard);
            argsList.add(fromWildcard);
            argsList.add(fromWildcard);
            argsList.add(fromWildcard);
            argsList.add(toWildcard);
            argsList.add(toWildcard);
        } else if (hasFrom) {
            String fromWildcard = "%" + from.trim() + "%";
            where.append(" WHERE (r.").append(DatabaseHelper.COL_ROUTE_ORIGIN).append(" LIKE ? OR ")
                 .append("r.").append(DatabaseHelper.COL_ROUTE_NAME).append(" LIKE ? OR ")
                 .append("t.").append(DatabaseHelper.COL_TERM_NAME).append(" LIKE ? OR ")
                 .append("t.").append(DatabaseHelper.COL_TERM_CITY).append(" LIKE ?)");
            argsList.add(fromWildcard);
            argsList.add(fromWildcard);
            argsList.add(fromWildcard);
            argsList.add(fromWildcard);
        } else if (hasTo) {
            String toWildcard = "%" + to.trim() + "%";
            where.append(" WHERE (r.").append(DatabaseHelper.COL_ROUTE_DESTINATION).append(" LIKE ? OR ")
                 .append("r.").append(DatabaseHelper.COL_ROUTE_NAME).append(" LIKE ?)");
            argsList.add(toWildcard);
            argsList.add(toWildcard);
        }

        sql += where.toString() + " ORDER BY r." + DatabaseHelper.COL_ROUTE_FARE + " ASC";

        Cursor cursor = db.rawQuery(sql, argsList.toArray(new String[0]));
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Route r = cursorToRoute(cursor);
                if (includeStops) r.setStops(getStopsForRoute(r.getId()));
                routes.add(r);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return routes;
    }

    public List<Route> getRoutesByTerminalId(long terminalId) {
        List<Route> routes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT r.*, t." + DatabaseHelper.COL_TERM_NAME + " AS term_name " +
                     "FROM " + DatabaseHelper.TABLE_ROUTES + " r " +
                     "JOIN " + DatabaseHelper.TABLE_TERMINALS + " t ON r." + DatabaseHelper.COL_ROUTE_TERM_ID + " = t." + DatabaseHelper.COL_TERM_ID + " " +
                     "WHERE r." + DatabaseHelper.COL_ROUTE_TERM_ID + " = ?";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(terminalId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Route r = cursorToRoute(cursor);
                r.setStops(getStopsForRoute(r.getId()));
                routes.add(r);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return routes;
    }

    public Route getRouteById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT r.*, t." + DatabaseHelper.COL_TERM_NAME + " AS term_name " +
                     "FROM " + DatabaseHelper.TABLE_ROUTES + " r " +
                     "JOIN " + DatabaseHelper.TABLE_TERMINALS + " t ON r." + DatabaseHelper.COL_ROUTE_TERM_ID + " = t." + DatabaseHelper.COL_TERM_ID + " " +
                     "WHERE r." + DatabaseHelper.COL_ROUTE_ID + " = ?";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(id)});
        Route route = null;
        if (cursor != null && cursor.moveToFirst()) {
            route = cursorToRoute(cursor);
            route.setStops(getStopsForRoute(route.getId()));
            cursor.close();
        }
        return route;
    }

    public List<RouteStop> getStopsForRoute(long routeId) {
        List<RouteStop> stops = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_ROUTE_STOPS,
                null,
                DatabaseHelper.COL_STOP_ROUTE_ID + " = ?",
                new String[]{String.valueOf(routeId)},
                null, null,
                DatabaseHelper.COL_STOP_SEQUENCE + " ASC"
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                RouteStop stop = new RouteStop(
                        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STOP_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STOP_ROUTE_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STOP_NAME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STOP_SEQUENCE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STOP_LAT)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STOP_LNG))
                );
                stops.add(stop);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return stops;
    }

    public long insertRoute(Route route) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_ROUTE_TERM_ID, route.getTerminalId());
        cv.put(DatabaseHelper.COL_ROUTE_NAME, route.getRouteName());
        cv.put(DatabaseHelper.COL_ROUTE_ORIGIN, route.getOrigin());
        cv.put(DatabaseHelper.COL_ROUTE_DESTINATION, route.getDestination());
        cv.put(DatabaseHelper.COL_ROUTE_TRANSPORT_TYPE, route.getTransportType());
        cv.put(DatabaseHelper.COL_ROUTE_FARE, route.getFare());
        cv.put(DatabaseHelper.COL_ROUTE_TIME, route.getEstimatedTravelTime());
        cv.put(DatabaseHelper.COL_ROUTE_DESC, route.getDescription());
        return db.insert(DatabaseHelper.TABLE_ROUTES, null, cv);
    }

    public int updateRoute(Route route) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_ROUTE_TERM_ID, route.getTerminalId());
        cv.put(DatabaseHelper.COL_ROUTE_NAME, route.getRouteName());
        cv.put(DatabaseHelper.COL_ROUTE_ORIGIN, route.getOrigin());
        cv.put(DatabaseHelper.COL_ROUTE_DESTINATION, route.getDestination());
        cv.put(DatabaseHelper.COL_ROUTE_TRANSPORT_TYPE, route.getTransportType());
        cv.put(DatabaseHelper.COL_ROUTE_FARE, route.getFare());
        cv.put(DatabaseHelper.COL_ROUTE_TIME, route.getEstimatedTravelTime());
        cv.put(DatabaseHelper.COL_ROUTE_DESC, route.getDescription());
        return db.update(DatabaseHelper.TABLE_ROUTES, cv, DatabaseHelper.COL_ROUTE_ID + " = ?", new String[]{String.valueOf(route.getId())});
    }

    public int deleteRoute(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_ROUTES, DatabaseHelper.COL_ROUTE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    private Route cursorToRoute(Cursor cursor) {
        Route r = new Route(
                cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTE_ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTE_TERM_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTE_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTE_ORIGIN)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTE_DESTINATION)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTE_TRANSPORT_TYPE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTE_FARE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTE_TIME)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTE_DESC))
        );
        int transfersIdx = cursor.getColumnIndex(DatabaseHelper.COL_ROUTE_TRANSFERS);
        if (transfersIdx >= 0 && !cursor.isNull(transfersIdx)) r.setTransferCount(cursor.getInt(transfersIdx));
        int walkIdx = cursor.getColumnIndex(DatabaseHelper.COL_ROUTE_WALK);
        if (walkIdx >= 0 && !cursor.isNull(walkIdx)) r.setWalkingDistanceMeters(cursor.getInt(walkIdx));
        int timelineIdx = cursor.getColumnIndex(DatabaseHelper.COL_ROUTE_TIMELINE);
        if (timelineIdx >= 0 && !cursor.isNull(timelineIdx)) r.setTimelineJson(cursor.getString(timelineIdx));
        int termNameIdx = cursor.getColumnIndex("term_name");
        if (termNameIdx >= 0) {
            r.setTerminalName(cursor.getString(termNameIdx));
        }
        return r;
    }
}
