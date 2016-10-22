package net.fishandwhistle.openpos.api;

import net.fishandwhistle.openpos.actions.RegexLookupAction;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-22.
 */

public class AmazonURLLookup extends RegexLookupAction {

    public AmazonURLLookup() {
        super("amazonurl", jsonOptions);
    }

    private static String jsonOptions ;
    static {
        try {
            JSONObject keyMap = new JSONObject();
            keyMap.put("amazon_url", "https://www.amazon.com/s/field-keywords={{isbn13}}");
            JSONObject baseOptions = new JSONObject();
            baseOptions.put(OPTION_KEYMAP, keyMap);
            baseOptions.put(OPTION_QUIET, true);
            jsonOptions = baseOptions.toString();
        } catch (JSONException e) {
            throw new RuntimeException("invalid JSON generated somehow... " + e.getMessage());
        }
    }

}
