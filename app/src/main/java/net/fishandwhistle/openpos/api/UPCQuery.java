package net.fishandwhistle.openpos.api;

import net.fishandwhistle.openpos.actions.LookupAction;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-03.
 */

public class UPCQuery extends LookupAction {

    public UPCQuery() {
        super("upcdb", jsonOptions);
    }


    private static String jsonOptions ;
    static {
        try {
            JSONObject keyMap = new JSONObject();
            keyMap.put("reason", KEY_ERROR);
            keyMap.put("itemname", "itemname");
            keyMap.put("alias", "alias");
            keyMap.put("description", "description");
            JSONObject baseOptions = new JSONObject();
            baseOptions.put(OPTION_URI_FORMAT, "http://api.upcdatabase.org/json/2bf423e9a6b6800ff83ab88d74856501/{{gtin13}}");
            baseOptions.put(OPTION_KEYMAP, keyMap);
            baseOptions.put(OPTION_QUIET, true);
            baseOptions.put(OPTION_MIMETYPE, "application/json");
            jsonOptions = baseOptions.toString();
        } catch(JSONException e) {
            throw new RuntimeException("invalid JSON generated somehow... " + e.getMessage());
        }
    }
}
