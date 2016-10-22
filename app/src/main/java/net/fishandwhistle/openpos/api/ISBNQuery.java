package net.fishandwhistle.openpos.api;

import net.fishandwhistle.openpos.actions.LookupAction;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-02.
 */

public class ISBNQuery extends LookupAction {

    public ISBNQuery() {
        super("isbndb", jsonOptions);
    }

    private static String jsonOptions ;
    static {
        try {
            JSONObject keyMap = new JSONObject();
            keyMap.put("error", KEY_ERROR);
            keyMap.put("data[0]/title", "title");
            keyMap.put("data[0]/author_data[; ]/name", "authors");
            keyMap.put("data[0]/dewey_normal", "dewey_normal");
            keyMap.put("data[0]/lcc_number", "lcc_number");
            keyMap.put("data[0]/publisher", "publisher_text");
            JSONObject baseOptions = new JSONObject();
            baseOptions.put(OPTION_URI_FORMAT, "http://isbndb.com/api/v2/json/T89SFTZN/book/{{isbn13}}");
            baseOptions.put(OPTION_KEYMAP, keyMap);
            baseOptions.put(OPTION_QUIET, true);
            baseOptions.put(OPTION_MIMETYPE, "application/json");
            baseOptions.put(OPTION_ENCODING, "windows-1252");
            jsonOptions = baseOptions.toString();
        } catch(JSONException e) {
            throw new RuntimeException("invalid JSON generated somehow... " + e.getMessage());
        }
    }

}
