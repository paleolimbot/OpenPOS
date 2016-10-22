package net.fishandwhistle.openpos.actions;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by dewey on 2016-10-02.
 */

public class TextApiCache {

    private static final String TAG = "TextApiCache";
    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "net.fishandwhistle.TextApiCache";

    private CacheDB db;

    public TextApiCache(Context context) {
        this.db = new CacheDB(context);
    }

    public boolean contains(String url) {
        return this.get(url) != null;
    }

    public CachedItem get(String url) {
        return db.getEntry(url);
    }

    public boolean put(String url, String data) {
        return db.putEntry(url, data);
    }


    private static class CacheDB extends SQLiteOpenHelper {

        public CacheDB(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public boolean putEntry(String url, String response) {
            SQLiteDatabase db = this.getWritableDatabase();
            boolean success ;
            if(response != null) {
                //insert/update
                ContentValues cv = new ContentValues();
                cv.put("theurl", url);
                cv.put("response", response);
                cv.put("querytime", System.currentTimeMillis());
                Cursor c = db.query("cached_responses", null, "theurl=?", new String[]{url}, null, null, null, "1");
                if (c.getCount() == 0) {
                    long id = db.insert("cached_responses", null, cv);
                    success = id != -1;
                } else {
                    int nrows = db.update("cached_responses", cv, "theurl=?", new String[]{url});
                    success = nrows > 0;
                }
                c.close();
            } else {
                //remove
                int rows = db.delete("cached_responses", "theurl=?", new String[] {url});
                success = rows > 0;
            }
            db.close();
            return success;
        }

        public CachedItem getEntry(String url) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.query("cached_responses", null, "theurl=?", new String[] {url}, null, null, null, "1");
            CachedItem out = null;
            if(c.getCount() > 0) {
                out = new CachedItem();
                c.moveToFirst();
                out.data = c.getString(1);
                out.queryTime = c.getLong(2);
            }
            c.close();
            db.close();
            return out;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE cached_responses (theurl TEXT PRIMARY KEY, "
                    + "response TEXT, "
                    + "querytime INTEGER, "
                    + "extra TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE cached_responses");
            this.onCreate(db);
        }
    }

    public static class CachedItem {
        public String data = null;
        public long queryTime = 0;
    }

}
