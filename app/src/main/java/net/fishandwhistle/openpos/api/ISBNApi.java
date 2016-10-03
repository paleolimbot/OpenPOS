package net.fishandwhistle.openpos.api;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-02.
 */

public class ISBNApi extends APIQuery {
    private static final String TAG = "ISBNApi";

    public ISBNApi(Context context, String isbn, APICallback callback) {
        super(context, isbn, callback);
    }

    @Override
    protected String getUrl(String input) {
        String apikey = "T89SFTZN";
        return "http://isbndb.com/api/v2/json/" + apikey + "/book/" + input;
    }

    @Override
    protected JSONObject parseJSON(String json) {
        try {
            Log.i(TAG, "Parsing JSON data");
            JSONObject o = new JSONObject(json);
            if(o.has("error")) {
                Log.e(TAG, "Error from database: " + o.getString("error"));

                return null;
            } else {
                JSONArray a = o.getJSONArray("data");
                JSONObject book = a.getJSONObject(0);
                return book;
            }
        } catch(JSONException e) {
            Log.e(TAG, "Error parsing JSON", e);
            return null;
        }
    }
}
