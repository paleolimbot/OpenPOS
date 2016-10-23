package net.fishandwhistle.openpos.actions;

import android.app.Notification;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import net.fishandwhistle.openpos.items.ScannedItem;

import java.util.ArrayList;
import java.util.List;

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
    public boolean doAction(Context context, ScannedItem item) throws ActionException {
        boolean result = true;
        List<String> errors = new ArrayList<>();
        for(ScannedItemAction action: actions) {
            boolean actionResult;
            try {
                actionResult = action.doAction(context, item);
                if(!isQuiet() && !actionResult) {
                    return false;
                } else {
                    result = actionResult && result;
                }
            } catch(ActionException e) {
                if(isQuiet()) {
                    Log.e("ActionChain", "doAction: exception in action chain (supressing)", e);
                    errors.add(e.getMessage());
                } else {
                    throw new ActionException("Error occurred in ActionChain: " + e.getMessage());
                }
            }
        }
        if(errors.size() > 0) {
            item.putValue(getActionName() + "_error", TextUtils.join("; ", errors));
        }
        return result;
    }


}
