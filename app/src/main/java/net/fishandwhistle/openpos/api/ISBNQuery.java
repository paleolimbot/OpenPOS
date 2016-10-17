package net.fishandwhistle.openpos.api;

import android.content.Context;
import android.util.Log;

import net.fishandwhistle.openpos.R;
import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

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
    protected boolean parseJSON(String json, ScannedItem item) {
        try {
            Log.i(TAG, "Parsing JSON data");
            JSONObject o = new JSONObject(json);
            if(o.has("error")) {
                Log.e(TAG, "Error from database: " + o.getString("error"));
                item.putValue("lookup_error", o.getString("error"));
                return false;
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
                            authorlist += "; " + authors.getJSONObject(i).getString("name");
                        }
                    }
                }
                if(authorlist.equals("")) {
                    authorlist = "No author";
                } else {
                    item.putValue("authors", authorlist);
                }

                item.description = String.format("%s (%s)", book.getString("title"), authorlist);
                Iterator<String> keyIter = book.keys();
                String[] skipKeys = new String[] {"author_data", "physical_description_text",
                                                    "subject_ids", "book_id", "dewey_decimal",
                                                    "publisher_id"};
                while(keyIter.hasNext()) {
                    String key = keyIter.next();
                    boolean good = true;
                    for(String badKey: skipKeys) {
                        if(badKey.equals(key)) {
                            good = false;
                            break;
                        }
                    }
                    if(good) {
                        String val = book.getString(key);
                        if(val != null && val.trim().length() > 1) {
                            item.putValue(key, val.trim());
                        }
                    }
                }
                return true;
            }
        } catch(JSONException e) {
            Log.e(TAG, "Error parsing JSON", e);
            item.putValue("lookup_error", String.format(context.getString(R.string.api_errorjson), e.getMessage()));
            return false;
        }
    }
}
