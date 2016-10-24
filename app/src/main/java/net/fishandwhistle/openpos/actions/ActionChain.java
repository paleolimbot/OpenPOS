package net.fishandwhistle.openpos.actions;

import android.app.Notification;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dewey on 2016-10-22.
 */

public class ActionChain extends ScannedItemAction {

    private static final String OPTION_ACTIONS = "actions";

    protected List<ScannedItemAction> actions ;

    public ActionChain(JSONObject jsonObject) {
        super(jsonObject);
        this.actions = new ArrayList<>();
        JSONArray jsonActions = getOptionArray(OPTION_ACTIONS);
        if(jsonActions == null) throw new IllegalArgumentException("Option 'actions' required");
        try {
            for(int i=0; i<jsonActions.length(); i++) {
                JSONObject actionJson = jsonActions.getJSONObject(i);
                this.actions.add(ActionFactory.inflate(actionJson));
            }
        } catch(JSONException e) {
            throw new IllegalArgumentException("Invalid JSON in constructor");
        }


    }

    @Override
    public boolean doAction(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        boolean result = true;
        List<String> errors = new ArrayList<>();
        for(ScannedItemAction action: actions) {
            boolean actionResult;
            try {
                actionResult = action.doAction(context, item, executor);
                if((executor != null && executor.isCancelled()) || (!isQuiet() && !actionResult)) {
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
