package net.fishandwhistle.openpos.api;

import android.content.Context;
import android.util.Log;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-02.
 */

public class ISBNQuery extends APIQuery {
    private static final String TAG = "ISBNApi";

    public ISBNQuery(Context context, String id, ScannedItem item, APICallback callback) {
        super(context, id, item, callback);
    }

    @Override
    protected String getUrl(String input) {
        String apikey = "T89SFTZN";
        return "http://isbndb.com/api/v2/json/" + apikey + "/book/" + input;
    }

    @Override
    protected JSONObject parseJSON(String json, ScannedItem item) {
        try {
            Log.i(TAG, "Parsing JSON data");
            JSONObject o = new JSONObject(json);
            if(o.has("error")) {
                Log.e(TAG, "Error from database: " + o.getString("error"));

                return null;
            } else {
                JSONArray a = o.getJSONArray("data");
                JSONObject book = a.getJSONObject(0);
                String authorlist = "";
                if(book.has("author_data")) {
                    JSONArray authors = book.getJSONArray("author_data");
                    for (int i = 0; i < authors.length(); i++) {
                        if (authorlist.equals("")) {
                            authorlist += authors.getJSONObject(i).getString("name");
                        } else {
                            authorlist += "; " + authors.get(i);
                        }
                    }
                }
                if(authorlist.equals("")) {
                    authorlist = "No author";
                }
                if(item != null)
                    item.description = String.format("%s (%s)", book.getString("title"), authorlist);
                return book;
            }
        } catch(JSONException e) {
            Log.e(TAG, "Error parsing JSON", e);
            return null;
        }
    }
}
