package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

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

    private static final String TAG = "ScannedItemAction";

    public static final String OPTION_QUIET = "quiet";
    public static final String OPTION_TYPE = "type";
    public static final String OPTION_ENABLED = "enabled";
    public static final String OPTION_ACTION_NAME = "name";

    private String actionName;
    private JSONObject options;
    private boolean quiet;
    private boolean enabled;

    public ScannedItemAction(JSONObject jsonOptions) {
        options = jsonOptions;
        actionName = getOptionString(OPTION_ACTION_NAME, this.getClass().getName());
        quiet = getOptionBoolean(OPTION_QUIET, getIsQuietDefault());
        enabled = getOptionBoolean(OPTION_ENABLED, true);
    }

    public String getOptionString(String key, String defValue) {
        try {
            return options.getString(key);
        } catch(JSONException e) {
            return defValue;
        }
    }

    public String getOptionString(String key) {
        try {
            return options.getString(key);
        } catch(JSONException e) {
            throw new IllegalArgumentException(String.format("Option '%s' is required", key));
        }
    }

    public int getOptionInt(String key, int defValue) {
        try {
            return Integer.valueOf(options.getString(key));
        } catch(JSONException e) {
            return defValue;
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Option '%s' must be a valid integer", key));
        }
    }

    public int getOptionInt(String key) {
        try {
            return Integer.valueOf(options.getString(key));
        } catch(JSONException e) {
            throw new IllegalArgumentException(String.format("Option '%s' is required", key));
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Option '%s' must be a valid integer", key));
        }
    }

    public boolean getOptionBoolean(String key, boolean defValue) {
        try {
            return Boolean.valueOf(options.getString(key));
        } catch(JSONException e) {
            return defValue;
        }
    }

    public boolean getOptionBoolean(String key) {
        try {
            return Boolean.valueOf(options.getString(key));
        } catch(JSONException e) {
            throw new IllegalArgumentException(String.format("Option '%s' is required", key));
        }
    }

    public String getOptionEnum(String key, String defValue, String[] choices) {
        String val = getOptionString(key, defValue);
        for(String s: choices) {
            if(val.equals(s)) {
                return val;
            }
        }
        throw new IllegalArgumentException(String.format("Option '%s' must be in [%s]", key, TextUtils.join(", ", choices)));
    }

    public String getOptionEnum(String key, String[] choices) {
        String val = getOptionString(key);
        for(String s: choices) {
            if(val.equals(s)) {
                return val;
            }
        }
        throw new IllegalArgumentException(String.format("Option '%s' must be in [%s]", key, TextUtils.join(", ", choices)));
    }

    public JSONArray getOptionArray(String key, JSONArray defValue) {
        try {
            return options.getJSONArray(key);
        } catch(JSONException e) {
            return null;
        }
    }

    public JSONArray getOptionArray(String key) {
        try {
            return options.getJSONArray(key);
        } catch(JSONException e) {
            throw new IllegalArgumentException(String.format("Option '%s' is required", key));
        }
    }

    public JSONObject getOptionObject(String key, JSONObject defObj) {
        try {
            return options.getJSONObject(key);
        } catch(JSONException e) {
            return null;
        }
    }

    public JSONObject getOptionObject(String key) {
        try {
            return options.getJSONObject(key);
        } catch(JSONException e) {
            throw new IllegalArgumentException(String.format("Option '%s' is required", key));
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

    public final boolean doAction(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        Log.i(TAG, "doAction: " + this.getActionName());
        return enabled && isApplicable(context, item, executor) && this.doActionContent(context, item, executor);
    }

    public boolean isApplicable(Context context, ScannedItem item, ActionExecutor executor) {
        return true;
    }

    public abstract boolean doActionContent(Context context, ScannedItem item, ActionExecutor executor) throws ActionException;

    public final ActionExecutor doActionAsync(final Context context, ScannedItem item, ScannerItemActionCallback callback) {
        ActionExecutor e = new ActionExecutor(context, item, callback);
        e.execute();
        return e;
    }

    public interface ScannerItemActionCallback {
        void onScannerItemAction(String actionName, ScannedItem item);
        void onActionException(String actionName, ScannedItem item, String message);
    }

    public static class ActionException extends Exception {

        public ActionException(String message) {
            super(message);
        }
    }

    public class ActionExecutor extends AsyncTask<Void, Runnable, Boolean> {

        private Context context;
        private String response;
        private ScannerItemActionCallback callback;
        private ScannedItem item;
        private String error;


        public ActionExecutor(Context context, ScannedItem item, ScannerItemActionCallback callback) {
            this.context = context;
            this.callback = callback;
            this.response = null;
            this.item = item;
            this.error = null;
        }

        @Override
        protected void onProgressUpdate(Runnable... values) {
            values[0].run();
        }

        public String runOnUiThread(Runnable action) {
            this.publishProgress(action);
            String result = error;
            error = null;
            return result;
        }

        public synchronized void setResponse(String response) {
            this.response = response;
        }

        private synchronized String getResponseValue() {
            return this.response;
        }

        public String getResponse() {
            while(this.getResponseValue() == null) {
                try {
                    Thread.sleep(250);
                } catch(InterruptedException e) {
                    break;
                }
                if(this.isCancelled()) {
                    break;
                }
                Log.i(TAG, "getResponse: checking for response value...");
            }
            String result = this.getResponseValue();
            Log.i(TAG, "getResponse: found response " + result);
            response = null;
            return result;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                item.isLoading = true;
                boolean result = doAction(context, item, this);
                item.isLoading = false;
                return result;
            } catch(ActionException e) {
                item.isLoading = false;
                error = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(callback != null) {
                if (result != null) {
                    callback.onScannerItemAction(getActionName(), item);
                } else if (error != null) {
                    callback.onActionException(getActionName(), item, error);
                }
            }
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
