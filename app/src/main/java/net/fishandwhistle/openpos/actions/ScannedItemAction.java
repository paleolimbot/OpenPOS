package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.os.AsyncTask;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by dewey on 2016-10-21.
 */

public abstract class ScannedItemAction {

    public static final String OPTION_QUIET = "quiet";

    private String actionName;
    private JSONObject options;
    private boolean quiet;

    public ScannedItemAction(String actionName, String jsonOptions) {
        this.actionName = actionName;
        try {
            options = new JSONObject(jsonOptions);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JSON passed to ScannedItemAction: " + e.getMessage());
        }
        String isQuiet = getOptionString(OPTION_QUIET);
        if(isQuiet == null) {
            quiet = getIsQuietDefault();
        } else {
            quiet = Boolean.valueOf(isQuiet);
        }
    }

    public String getOptionString(String key) {
        try {
            return options.getString(key);
        } catch(JSONException e) {
            return null;
        }
    }

    public JSONArray getOptionArray(String key) {
        try {
            return options.getJSONArray(key);
        } catch(JSONException e) {
            return null;
        }
    }

    public JSONObject getOptionObject(String key) {
        try {
            return options.getJSONObject(key);
        } catch(JSONException e) {
            return null;
        }
    }

    public boolean getIsQuietDefault() {
        return true;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public String getActionName() {
        return actionName;
    }

    public abstract boolean doAction(Context context, ScannedItem item) throws ActionException;

    public void doActionAsync(final Context context, ScannedItem item, final ScannerItemActionCallback callback) {
        new AsyncTask<ScannedItem, Void, ScannedItem>() {

            private String error = null;

            @Override
            protected ScannedItem doInBackground(ScannedItem... params) {
                ScannedItem item = params[0];
                try {
                    if (doAction(context, item)) {
                        return item;
                    } else {
                        return null;
                    }
                } catch(ActionException e) {
                    error = e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ScannedItem item) {
                if(item != null) {
                    callback.onScannerItemAction(getActionName(), item);
                } else if(error != null) {
                    callback.onActionException(getActionName(), item, error);
                }
            }
        }.execute(item);
    }

    public interface ScannerItemActionCallback {
        void onScannerItemAction(String actionName, ScannedItem item);
        void onActionException(String actionName, ScannedItem item, String message);
    }

    public class ActionException extends Exception {

        public ActionException(String message) {
            super(message);
        }
    }

    protected static Map<String, String> extractKeyMap(JSONObject keysJson) {
        if(keysJson != null) {
            Map<String, String> keyMap = new HashMap<>();
            Iterator<String> keys = keysJson.keys();
            try {
                while (keys.hasNext()) {
                    String key = keys.next();
                    keyMap.put(key, keysJson.getString(key));
                }
            } catch(JSONException e) {
                throw new IllegalArgumentException("Invalid JSON encountered in URILookupAction: " + e.getMessage());
            }
            return keyMap;
        } else {
            return null;
        }
    }

}
