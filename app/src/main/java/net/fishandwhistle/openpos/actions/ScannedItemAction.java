package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.os.AsyncTask;

import net.fishandwhistle.openpos.items.ScannedItem;

/**
 * Created by dewey on 2016-10-21.
 */

public abstract class ScannedItemAction {

    private String actionName;

    public ScannedItemAction(String actionName) {
        this.actionName = actionName;
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
