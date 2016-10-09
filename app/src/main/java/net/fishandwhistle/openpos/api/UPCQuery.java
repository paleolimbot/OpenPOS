package net.fishandwhistle.openpos.api;

import android.content.Context;
import android.util.Log;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    protected JSONObject parseJSON(String json, ScannedItem item) {
        try {
            Log.i(TAG, "Parsing JSON data");
            JSONObject o = new JSONObject(json);
            if(!o.getBoolean("valid")) {
                Log.e(TAG, "Error from database: " + o.getString("reason"));
                return null;
            } else {
                if(item != null) {
                    String description = o.getString("description");
                    if((description != null) && (description.trim().length() > 0)) {
                        item.description = description.trim();
                    }
                }

                return o;
            }
        } catch(JSONException e) {
            Log.e(TAG, "Error parsing JSON", e);
            return null;
        }
    }
}
