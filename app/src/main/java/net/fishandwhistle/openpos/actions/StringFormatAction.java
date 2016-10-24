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
                StringBuffer sb = new StringBuffer();
                Matcher m = TAG.matcher(value);
                boolean hasResult = false;
                while(m.find()) {
                    hasResult = true;
                    String attr = m.group(1);
                    String attrVal = item.getValue(attr);
                    if(attrVal == null) {
                        if(isQuiet()) { // unmapped key
                            hasResult = false;
                            break;
                        } else {
                            throw new ActionException("Unmapped key in StringFormat: " + attr);
                        }
                    }
                    m.appendReplacement(sb, attrVal);
                }
                m.appendTail(sb);
                if(hasResult) item.putValue(key, sb.toString());
            }
            return true;
        } catch(JSONException e) {
            Log.e("RegexLookup", "doAction: json exception", e);
            throw new ActionException("JSON Error: " + e.getMessage());
        }
    }
}
