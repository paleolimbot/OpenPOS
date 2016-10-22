package net.fishandwhistle.openpos.api;

import android.content.Context;
import android.util.Log;

import net.fishandwhistle.openpos.R;
import net.fishandwhistle.openpos.actions.JSONLookupItem;
import net.fishandwhistle.openpos.actions.URILookupAction;
import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by dewey on 2016-10-02.
 */

public class ISBNQuery extends JSONLookupItem {
    private static final String TAG = "ISBNApi";

    public ISBNQuery() {
        super("ISBN-Lookup", "http://isbndb.com/api/v2/json/T89SFTZN/book/{{isbn13}}", keyMap);
    }

    private static Map<String, String> keyMap = new HashMap<>();
    static {
        keyMap.put("data[0]/title", "title");
        keyMap.put("data[0]/author_data[, ]/name", "authors");
    }

}
