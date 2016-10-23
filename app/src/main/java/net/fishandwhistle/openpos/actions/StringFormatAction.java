package net.fishandwhistle.openpos.actions;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dewey on 2016-10-22.
 */

public class StringFormatAction extends ScannedItemAction {

    public static final String OPTION_KEYMAP = "key_map";

    public StringFormatAction(String actionName, String jsonOptions) {
        super(actionName, jsonOptions);
        if(getOptionObject(OPTION_KEYMAP) == null) throw new IllegalArgumentException("key_map is required");
    }

    @Override
    public boolean doAction(Context context, ScannedItem item) throws ActionException {
        Pattern TAG = Pattern.compile("\\{\\{(.*?)\\}\\}");
        JSONObject map = getOptionObject(OPTION_KEYMAP);
        Iterator<String> keys = map.keys();
        StringBuffer sb = new StringBuffer();

        try {
            while(keys.hasNext()) {
                String key = keys.next();
                String value = map.getString(key);
                Matcher m = TAG.matcher(value);
                while(m.find()) {
                    String attr = m.group(1);
                    String attrVal = item.getValue(attr);
                    if(attrVal == null) {
                        if(isQuiet()) { // unmapped key
                            return true;
                        } else {
                            throw new ActionException("Unmapped key in StringFormat: " + attr);
                        }
                    }
                    m.appendReplacement(sb, attrVal);
                }
                m.appendTail(sb);
                item.putValue(key, sb.toString());
            }
            return true;
        } catch(JSONException e) {
            Log.e("RegexLookup", "doAction: json exception", e);
            throw new ActionException("JSON Error: " + e.getMessage());
        }
    }
}
