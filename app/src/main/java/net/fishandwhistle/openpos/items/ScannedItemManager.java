package net.fishandwhistle.openpos.items;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

/**
 * Created by dewey on 2016-10-05.
 */

public class ScannedItemManager {

    public static final String USER_DEFAULT = "default_user";
    public static final String REASON_ORDER_IN = "order_in";
    public static final String REASON_CHECKOUT = "checkout";
    public static final String REASON_NON = "no_reason";
    public static final int ORDER_NUMBER_NONE = 0;


    private static final String DB_NAME = "net.fishandwhistle.ScannedItemDatabase";
    private static final int DB_VERSION = 1;

    private ScannedItemDB db ;

    public ScannedItemManager(Context context) {
        db = new ScannedItemDB(context);
    }


    public boolean putItem(String sessionName, ScannedItem item) {
        //TODO stub method
        return false;
    }

    public boolean removeItem(String sessionName, ScannedItem item) {
        //TODO stub method
        return false;
    }

    public boolean syncItem(String sessionName, ScannedItem item) {
        //TODO stub method
        return false;
    }


    private static class ScannedItemDB extends SQLiteOpenHelper {

        public ScannedItemDB(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public boolean putScans(String url, String response) {
            SQLiteDatabase db = this.getWritableDatabase();
            boolean success;
            if (response != null) {
                //insert/update
                ContentValues cv = new ContentValues();
                cv.put("theurl", url);
                cv.put("response", response);
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
                int rows = db.delete("cached_responses", "theurl=?", new String[]{url});
                success = rows > 0;
            }
            db.close();
            return success;
        }

        public String getEntry(String url) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.query("cached_responses", null, "theurl=?", new String[]{url}, null, null, null, "1");
            String out = null;
            if (c.getCount() > 0) {
                c.moveToFirst();
                out = c.getString(1);
            }
            c.close();
            return out;
        }

        public boolean[] recordScans(List<ScannedItem> items, boolean updateInventory) {
            return new boolean[] {false, false};
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE scans (_id INTEGER PRIMARY KEY, "
                    + "productcode TEXT, "
                    + "scantime INTEGER, "
                    + "scanquantity INTEGER, "
                    + "byuser TEXT, "
                    + "ordernumber INTEGER, "
                    + "price REAL, "
                    + "scanreason TEXT)");

            db.execSQL("CREATE TABLE inventory (productcode TEXT PRIMARY KEY, "
                    + "quantity INTEGER, "
                    + "lastupdate INTEGER)");

            db.execSQL("CREATE TABLE productinfo (productcode TEXT PRIMARY KEY, "
                    + "description TEXT, "
                    + "price REAL, "
                    + "byuser TEXT, "
                    + "jsonextra TEXT, "
                    + "version INTEGER, "
                    + "lastupdate INTEGER)");

            db.execSQL("CREATE TABLE orders (_id INTEGER PRIMARY KEY, "
                    + "ordertime TEXT, "
                    + "agent TEXT, "
                    + "parentorder INTEGER, "
                    + "payment TEXT, "
                    + "comment TEXT, "
                    + "lastupdate INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //only one db version
        }
    }

}
