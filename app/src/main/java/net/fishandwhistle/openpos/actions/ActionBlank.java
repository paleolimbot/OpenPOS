package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-24.
 */

public class ActionBlank extends ScannedItemAction {

    public ActionBlank(JSONObject jsonOptions) {
        super(jsonOptions);
    }

    @Override
    public boolean doAction(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        return true;
    }
}
