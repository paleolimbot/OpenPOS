package net.fishandwhistle.openpos.api;

import android.content.Context;
import android.util.Log;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by dewey on 2016-10-03.
 */

public class UPCQuery extends APIQuery {

    private static final String TAG = "UPCApi";

    public UPCQuery(Context context, String isbn, ScannedItem item, APICallback callback) {
        super(context, isbn, item, callback);
    }

    @Override
    protected String getUrl(String input) {
        String apikey = "2bf423e9a6b6800ff83ab88d74856501";
        return String.format("http://api.upcdatabase.org/json/%s/%s", apikey, input);
    }

    @Override
    protected boolean parseJSON(String json, ScannedItem item) {
        try {
            Log.i(TAG, "Parsing JSON data");
            JSONObject o = new JSONObject(json);
            if(!o.getBoolean("valid")) {
                Log.e(TAG, "Error from database: " + o.getString("reason"));
                return false;
            } else {
                String description = o.getString("description");
                String name = o.getString("itemname");
                if((description != null) && (description.trim().length() > 0)) {
                    item.description = description.trim();
                } else if((name != null) && name.trim().length() > 0) {
                    item.description = name.trim();
                }
                Iterator<String> keyIter = o.keys();
                while(keyIter.hasNext()) {
                    String key = keyIter.next();
                    if(key.equals("rate_up") || key.equals("rate_down") || key.equals("number")
                            || key.equals("valid")) {
                        continue;
                    }
                    String val = o.getString(key);
                    if(val != null && val.trim().length() > 0)
                        item.putValue(key, val.trim());
                }
                item.jsonSource = "api.upcdatabase.org";
                return true;
            }
        } catch(JSONException e) {
            Log.e(TAG, "Error parsing JSON", e);
            return false;
        }
    }
}
