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

    public StringFormatAction(JSONObject jsonObject) {
        super(jsonObject);
        if(getOptionObject(OPTION_KEYMAP) == null) throw new IllegalArgumentException("key_map is required");
    }

    @Override
    public boolean doAction(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        Pattern TAG = Pattern.compile("\\{\\{(.*?)\\}\\}");
        JSONObject map = getOptionObject(OPTION_KEYMAP);
        Iterator<String> keys = map.keys();
        try {
            while(keys.hasNext()) {
                String key = keys.next();
                String value = map.getString(key);
                String formatted = formatWithObject(value, item, false);
                if(formatted != null) {
                    item.putValue(key, formatted);
                } else if(!isQuiet()) {
                    throw new ActionException("Unmapped key in " + value);
                }
            }
            return true;
        } catch(JSONException e) {
            Log.e("RegexLookup", "doAction: json exception", e);
            throw new ActionException("JSON Error: " + e.getMessage());
        }
    }

    public static String formatWithObject(String format, ScannedItem item) {
        return formatWithObject(format, item, true);
    }

    public static String formatWithObject(String format, ScannedItem item, boolean quiet) {
        Pattern TAG = Pattern.compile("\\{\\{(.*?)\\}\\}");
        StringBuffer sb = new StringBuffer();
        Matcher m = TAG.matcher(format);
        while(m.find()) {
            String attr = m.group(1);
            String attrVal = item.getValue(attr);
            if(attrVal == null) {
                if(quiet) {
                    attrVal = "{{" + attr + "}}";
                } else {
                    return null;
                }
            }
            m.appendReplacement(sb, attrVal);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
