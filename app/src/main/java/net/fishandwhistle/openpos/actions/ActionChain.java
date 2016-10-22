package net.fishandwhistle.openpos.actions;

import android.app.Notification;
import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;

/**
 * Created by dewey on 2016-10-22.
 */

public class ActionChain extends ScannedItemAction {

    private ScannedItemAction[] actions ;

    public ActionChain(String actionName, String jsonOptions, ScannedItemAction... actions) {
        super(actionName, jsonOptions);
        this.actions = actions;
    }

    @Override
    public boolean doAction(Context context, ScannedItem item) {
        boolean result = false;
        for(ScannedItemAction action: actions) {
            boolean actionResult = action.doAction(context, item);
            result = actionResult || result;
        }
        return result;
    }


}
