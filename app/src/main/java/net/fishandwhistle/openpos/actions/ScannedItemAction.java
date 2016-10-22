package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.os.AsyncTask;

import net.fishandwhistle.openpos.barcode.PharmacodeSpec;
import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-21.
 */

public abstract class ScannedItemAction {

    private String actionName;
    private JSONObject options;

    public ScannedItemAction(String actionName, String jsonOptions) {
        this.actionName = actionName;
        try {
            options = new JSONObject(jsonOptions);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid JSON passed to ScannedItemAction: " + e.getMessage());
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

    public String getActionName() {
        return actionName;
    }

    public abstract boolean doAction(Context context, ScannedItem item);

    public void doActionAsync(final Context context, ScannedItem item, final ScannerItemActionCallback callback) {
        new AsyncTask<ScannedItem, Void, ScannedItem>() {
            @Override
            protected ScannedItem doInBackground(ScannedItem... params) {
                ScannedItem item = params[0];
                if(doAction(context, item)) {
                    return item;
                } else {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ScannedItem item) {
                if(item != null) {
                    callback.onScannerItemAction(getActionName(), item);
                }
            }
        }.execute(item);
    }

    public interface ScannerItemActionCallback {
        void onScannerItemAction(String actionName, ScannedItem item);
    }

}
