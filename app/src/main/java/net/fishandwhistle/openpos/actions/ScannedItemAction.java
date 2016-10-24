package net.fishandwhistle.openpos.actions;

import android.app.Notification;
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
    public static final String OPTION_TYPE = "type";
    public static final String OPTION_ACTION_NAME = "name";

    private String actionName;
    private JSONObject options;
    private boolean quiet;
    private boolean uiThread;

    public ScannedItemAction(JSONObject jsonOptions) {
        options = jsonOptions;
        this.actionName = getOptionString(OPTION_ACTION_NAME);
        if(this.actionName == null) {
            this.actionName = this.getClass().getName();
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

    private boolean isUiThread() {
        return uiThread;
    }

    private void setUiThread(boolean uiThread) {
        this.uiThread = uiThread;
    }

    public abstract boolean doAction(Context context, ScannedItem item, ActionExecutor executor) throws ActionException;

    public ActionExecutor doActionAsync(final Context context, ScannedItem item, ScannerItemActionCallback callback) {
        if(isUiThread()) {
            try {
                doAction(context, item, null);
            } catch(ActionException e) {
                callback.onActionException(getActionName(), item, e.getMessage());
            }
            return null;
        } else {
            ActionExecutor e = new ActionExecutor(context, item, callback);
            e.execute();
            return e;
        }
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

    public class ActionExecutor extends AsyncTask<Void, ScannedItemAction, Boolean> {

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
        protected void onProgressUpdate(ScannedItemAction... values) {
            try {
                values[0].doAction(context, item, this);
            } catch(ActionException e) {
                this.cancel(true);
                error = e.getMessage();
            }
        }

        public String runOnUiThread(ScannedItemAction action) {
            this.publishProgress(action);
            String result = error;
            error = null;
            return result;
        }

        public synchronized void setResponse(String response) {
            this.response = response;
        }

        private synchronized String getResponse() {
            return this.response;
        }

        public synchronized String getResponse(long timeout) {
            long start = System.currentTimeMillis();
            while(this.getResponse() == null && (System.currentTimeMillis()-start) < timeout) {
                try {
                    Thread.sleep(250);
                } catch(InterruptedException e) {
                    break;
                }
                if(this.isCancelled()) {
                    break;
                }
            }
            String result = this.getResponse();
            response = null;
            return result;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return doAction(context, item, this);
            } catch(ActionException e) {
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
