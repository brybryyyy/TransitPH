package com.transitph.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.transitph.app.models.Terminal;
import java.util.ArrayList;
import java.util.List;

public class TerminalDao {
    private final DatabaseHelper dbHelper;

    public TerminalDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public List<Terminal> getAllTerminals() {
        return searchTerminals(null);
    }

    public List<Terminal> searchTerminals(String query) {
        List<Terminal> terminals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = "";
        String[] selectionArgs = new String[0];

        if (query != null && !query.trim().isEmpty()) {
            String wildcard = "%" + query.trim() + "%";
            selection = " WHERE t." + DatabaseHelper.COL_TERM_NAME + " LIKE ? OR " +
                        "t." + DatabaseHelper.COL_TERM_CITY + " LIKE ? OR " +
                        "t." + DatabaseHelper.COL_TERM_PROVINCE + " LIKE ?";
            selectionArgs = new String[]{wildcard, wildcard, wildcard};
        }

        String sql = "SELECT t.*, COUNT(r." + DatabaseHelper.COL_ROUTE_ID + ") AS route_count " +
                "FROM " + DatabaseHelper.TABLE_TERMINALS + " t " +
                "LEFT JOIN " + DatabaseHelper.TABLE_ROUTES + " r ON r." + DatabaseHelper.COL_ROUTE_TERM_ID +
                " = t." + DatabaseHelper.COL_TERM_ID + selection +
                " GROUP BY t." + DatabaseHelper.COL_TERM_ID +
                " ORDER BY t." + DatabaseHelper.COL_TERM_PROVINCE + " ASC, t." + DatabaseHelper.COL_TERM_CITY + " ASC";
        Cursor cursor = db.rawQuery(sql, selectionArgs);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Terminal t = cursorToTerminal(cursor);
                t.setRouteCount(cursor.getInt(cursor.getColumnIndexOrThrow("route_count")));
                terminals.add(t);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return terminals;
    }

    public Terminal getTerminalById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TERMINALS,
                null,
                DatabaseHelper.COL_TERM_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        Terminal terminal = null;
        if (cursor != null && cursor.moveToFirst()) {
            terminal = cursorToTerminal(cursor);
            terminal.setRouteCount(countRoutesForTerminal(db, terminal.getId()));
            cursor.close();
        }
        return terminal;
    }

    public long insertTerminal(Terminal terminal) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_TERM_NAME, terminal.getTerminalName());
        cv.put(DatabaseHelper.COL_TERM_CITY, terminal.getCity());
        cv.put(DatabaseHelper.COL_TERM_PROVINCE, terminal.getProvince());
        cv.put(DatabaseHelper.COL_TERM_LAT, terminal.getLatitude());
        cv.put(DatabaseHelper.COL_TERM_LNG, terminal.getLongitude());
        cv.put(DatabaseHelper.COL_TERM_DESC, terminal.getDescription());
        return db.insert(DatabaseHelper.TABLE_TERMINALS, null, cv);
    }

    public int updateTerminal(Terminal terminal) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_TERM_NAME, terminal.getTerminalName());
        cv.put(DatabaseHelper.COL_TERM_CITY, terminal.getCity());
        cv.put(DatabaseHelper.COL_TERM_PROVINCE, terminal.getProvince());
        cv.put(DatabaseHelper.COL_TERM_LAT, terminal.getLatitude());
        cv.put(DatabaseHelper.COL_TERM_LNG, terminal.getLongitude());
        cv.put(DatabaseHelper.COL_TERM_DESC, terminal.getDescription());
        return db.update(DatabaseHelper.TABLE_TERMINALS, cv, DatabaseHelper.COL_TERM_ID + " = ?", new String[]{String.valueOf(terminal.getId())});
    }

    public int deleteTerminal(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_TERMINALS, DatabaseHelper.COL_TERM_ID + " = ?", new String[]{String.valueOf(id)});
    }

    private int countRoutesForTerminal(SQLiteDatabase db, long terminalId) {
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ROUTES + " WHERE " + DatabaseHelper.COL_ROUTE_TERM_ID + " = ?",
                new String[]{String.valueOf(terminalId)}
        );
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    private Terminal cursorToTerminal(Cursor cursor) {
        return new Terminal(
                cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TERM_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TERM_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TERM_CITY)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TERM_PROVINCE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TERM_LAT)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TERM_LNG)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TERM_DESC))
        );
    }
}
