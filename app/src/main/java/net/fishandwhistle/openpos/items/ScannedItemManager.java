package net.fishandwhistle.openpos.items;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by dewey on 2016-10-05.
 */

public class ScannedItemManager {

    private static final String TAG = "ScannedItemManager";

    private static final String DB_NAME = "net.fishandwhistle.ScannedItemDatabase";
    private static final int DB_VERSION = 1;

    private ScannedItemDB db ;

    public ScannedItemManager(Context context) {
        db = new ScannedItemDB(context);
    }


    public boolean putItem(String sessionName, ScannedItem item) {
        Log.i(TAG, "putItem: " + item);
        if(sessionName == null) {
            sessionName = "default";
        }
        item.session = sessionName;
        return db.putScan(item);
    }

    public boolean removeItem(ScannedItem item) {
        return db.removeScan(item);
    }

    public boolean syncItem(ScannedItem item) {
        Log.i(TAG, "syncItem: " + item);
        return db.putScan(item);
    }

    public List<ScannedItem> getAllItems(String sessionName) {
        if(sessionName == null) {
            sessionName = "default";
        }
        return db.getScans(sessionName);
    }

    public boolean[] putAllItems(String sessionName, List<ScannedItem> items) {
        if(sessionName == null) {
            sessionName = "default";
        }
        for(ScannedItem item: items) {
            item.session = sessionName;
        }
        return db.putScans(items);
    }

    public String dump() {
        return db.dump();
    }


    private static class ScannedItemDB extends SQLiteOpenHelper {

        public ScannedItemDB(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public boolean putScan(ScannedItem item) {
            SQLiteDatabase db = this.getWritableDatabase();
            boolean result = putScan(item, db);
            db.close();
            return result;
        }

        public boolean putScan(ScannedItem item, SQLiteDatabase db) {
            boolean success;
            ContentValues cv = getCV(item);
            if (item.dbId != 0) {
                //update
                int nrows = db.update("scans", cv, "_id=?", new String[]{String.valueOf(item.dbId)});
                success = nrows == 1;
            } else {
                //insert
                long id = db.insert("scans", null, cv);
                if(id > 0) {
                    item.dbId = id;
                    success = true;
                } else {
                    success = false;
                }
            }
            return success;
        }

        public boolean removeScan(ScannedItem item) {
            if(item.dbId == 0) {
                return false;
            }
            SQLiteDatabase db = this.getWritableDatabase();
            int rows = db.delete("scans", "_id=?", new String[] {String.valueOf(item.dbId)});
            boolean success = rows == 1;
            db.close();
            if(success) {
                item.dbId = 0;
            }
            return success;
        }

        public List<ScannedItem> getScans(String session) {
            SQLiteDatabase db = this.getReadableDatabase();
            List<ScannedItem> out = new ArrayList<>();
            Cursor c = db.query("scans", null, "session=?", new String[]{session}, null, null, "updatetime");
            for(int i=0; i<c.getCount(); i++) {
                c.moveToPosition(i);
                out.add(fromCursor(c));
            }
            c.close();
            return out;
        }

        public boolean[] putScans(List<ScannedItem> items) {
            boolean[] success = new boolean[items.size()];
            if(items.size() == 0) {
                return success;
            }
            SQLiteDatabase db = this.getWritableDatabase();
            for(int i = 0; i<items.size(); i++) {
                success[i] = putScan(items.get(i), db);
            }
            db.close();
            return success;
        }

        public String dump() {
            SQLiteDatabase db = this.getReadableDatabase();
            JSONArray a = new JSONArray();
            try {
                Cursor c = db.query("scans", null, "1", new String[] {}, null, null, null);
                for(int i=0; i<c.getCount(); i++) {
                    c.moveToPosition(i);
                    ScannedItem item = fromCursor(c);
                    JSONObject o = new JSONObject(item.getJSON());
                    o.put("_description", item.getValue("_description"));
                    o.put("_subtext", item.getValue("_subtext"));
                    o.put("_barcode_text", item.getValue("_barcode_text"));
                    o.put("_barcode_type", item.getValue("_barcode_type"));
                    a.put(o);
                }
                c.close();
            } catch(JSONException e) {
                Log.e(TAG, "dump: json error", e);
            }
            db.close();
            return a.toString();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE scans (_id INTEGER PRIMARY KEY, "
                    + "session TEXT, "
                    + "barcode TEXT, "
                    + "barcode_type TEXT, "
                    + "scantime INTEGER, "
                    + "updatetime INTEGER, "
                    + "datestring TEXT, "
                    + "scanquantity INTEGER, "
                    + "title TEXT, "
                    + "subtext TEXT, "
                    + "json TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //only one db version
        }

        private ScannedItem fromCursor(Cursor c) {
            long id = c.getLong(0);
            String session = c.getString(1);
            String barcode = c.getString(2);
            String barcodeType = c.getString(3);
            long scanTime = c.getLong(4);
            long updateTime = c.getLong(5);
            int qty = c.getInt(7);
            String description = c.getString(8);
            String subtext = c.getString(9);
            String json = c.getString(10);
            ScannedItem si = new ScannedItem(barcodeType, barcode);
            si.nScans = qty;
            si.description = description;
            si.subtext = subtext;
            si.scanTime = scanTime;
            si.updateTime = updateTime;
            si.setJSON(json);
            si.session = session;
            si.dbId = id;
            return si;
        }

        private ContentValues getCV(ScannedItem si) {
            ContentValues cv = new ContentValues();
            if(si.dbId != 0) cv.put("_id", si.dbId);
            cv.put("session", si.session);
            cv.put("barcode", si.barcodeText);
            cv.put("barcode_type", si.barcodeType);
            cv.put("scantime", si.scanTime);
            cv.put("updatetime", si.updateTime);
            cv.put("datestring", new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(new Date(si.updateTime)));
            cv.put("scanquantity", si.nScans);
            cv.put("title", si.description);
            cv.put("subtext", si.subtext);
            cv.put("json", si.getJSON());
            return cv;
        }

    }

}
