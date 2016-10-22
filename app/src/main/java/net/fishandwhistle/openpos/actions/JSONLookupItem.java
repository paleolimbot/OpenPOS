package net.fishandwhistle.openpos.actions;

import android.text.TextUtils;
import android.util.Log;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dewey on 2016-10-21.
 */

public class JSONLookupItem extends URILookupAction {


    public JSONLookupItem(String actionName, String uriFormat, Map<String, String> keyMap) {
        super(actionName, uriFormat, keyMap);
    }

    @Override
    protected boolean parse(String data, ScannedItem item) {
        //TODO need to flatten object based on keyMap somehow...
        try {
            JSONObject o = new JSONObject(data);
            if(keyMap == null) {
                Iterator<String> keys = o.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    o.put(key, o.getString(key));
                }
            } else {
                for(Map.Entry<String, String> e: keyMap.entrySet()) {
                    String[] path = e.getKey().split("/");
                    String value = followPath(o, path, 0);
                    if(value != null) {
                        o.put(e.getValue(), value);
                    } else {
                        o.put(e.getValue(), "value not found!");
                    }
                }
            }
            return true;
        } catch(JSONException e) {
            item.putValue("lookup_error", "JSON Error: " + e.getMessage());
            return false;
        }
    }

    private static Pattern INDEX = Pattern.compile("\\[(.*?)\\]");

    private static String followPath(JSONObject o, String[] path, int index) throws JSONException {
        try {
        String key = path[index];
            Matcher m = INDEX.matcher(key);
            if(m.find()) {
                //is an index (e.g. data[0])
                String arrInd = m.group(1);
                key = key.replace("["+arrInd+"]", "");
                JSONArray a = o.getJSONArray(key);
                try {
                    if(index == (path.length-1)) {
                        return a.getString(Integer.valueOf(arrInd));
                    } else {
                        return followPath(a.getJSONObject(Integer.valueOf(arrInd)), path, index+1);
                    }
                } catch(NumberFormatException e) {
                    //we are going to join the results of everything else on arrInd
                    String[] strings = new String[a.length()];
                    if(index == (path.length-1)) {
                        for(int i=0; i<a.length(); i++) {
                            strings[i] = a.getString(i);
                        }
                    } else {
                        String[] newPath = new String[path.length-index-1];
                        for(int i=index+1; i<path.length; i++) {
                            newPath[i-index-1] = path[index];
                        }
                        for(int i=0; i<a.length(); i++) {
                            strings[i] = followPath(a.getJSONObject(i), newPath, 0);
                        }
                    }
                    //join the results
                    return TextUtils.join(arrInd, strings);
                }
            } else {
                if(index == (path.length-1)) {
                    return o.getString(key);
                } else {
                    return followPath(o.getJSONObject(key), path, index + 1);
                }
            }

        } catch(JSONException e) {
            Log.e("JSONLookupItem", "followPath: json exception", e);
            return null;
        }
    }
}
