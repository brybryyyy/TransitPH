package com.transitph.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.transitph.app.models.User;
import com.transitph.app.utils.PasswordUtils;

public class UserDao {
    private final DatabaseHelper dbHelper;

    public UserDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public User authenticate(String email, String password) {
        if (email == null || password == null) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COL_USER_EMAIL + " = ? COLLATE NOCASE",
                new String[]{email.trim()},
                null, null, null
        );

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD_HASH));
            if (PasswordUtils.verifyPassword(password, storedHash)) {
                user = new User(
                        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_FULL_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_EMAIL)),
                        storedHash,
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ROLE))
                );
            }
            cursor.close();
        }
        return user;
    }

    public boolean isEmailTaken(String email) {
        if (email == null) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_EMAIL + " = ? COLLATE NOCASE",
                new String[]{email.trim()},
                null, null, null
        );
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }

    public long registerUser(String fullName, String email, String password, String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_FULL_NAME, fullName.trim());
        cv.put(DatabaseHelper.COL_USER_EMAIL, email.trim().toLowerCase());
        cv.put(DatabaseHelper.COL_USER_PASSWORD_HASH, PasswordUtils.hashPassword(password));
        cv.put(DatabaseHelper.COL_USER_ROLE, (role == null || role.isEmpty()) ? "USER" : role);
        return db.insert(DatabaseHelper.TABLE_USERS, null, cv);
    }

    public User getUserById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COL_USER_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = new User(
                    cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_FULL_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_EMAIL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD_HASH)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ROLE))
            );
            cursor.close();
        }
        return user;
    }

    /**
     * Creates or updates the local mirror of a Supabase user and returns its local id.
     * Passwords are managed by Supabase, so no real password hash is stored here.
     */
    public long upsertRemoteUser(String remoteId, String fullName, String email, String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_FULL_NAME, fullName);
        cv.put(DatabaseHelper.COL_USER_EMAIL, email.trim().toLowerCase());
        cv.put(DatabaseHelper.COL_USER_ROLE, (role == null || role.isEmpty()) ? "USER" : role.toUpperCase());
        cv.put(DatabaseHelper.COL_REMOTE_ID, remoteId);

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_REMOTE_ID + " = ? OR " + DatabaseHelper.COL_USER_EMAIL + " = ? COLLATE NOCASE",
                new String[]{remoteId, email.trim()},
                null, null, null
        );
        long id = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) id = cursor.getLong(0);
            cursor.close();
        }
        if (id > 0) {
            db.update(DatabaseHelper.TABLE_USERS, cv, DatabaseHelper.COL_USER_ID + " = ?", new String[]{String.valueOf(id)});
            return id;
        }
        cv.put(DatabaseHelper.COL_USER_PASSWORD_HASH, "managed-by-supabase");
        return db.insert(DatabaseHelper.TABLE_USERS, null, cv);
    }
}
