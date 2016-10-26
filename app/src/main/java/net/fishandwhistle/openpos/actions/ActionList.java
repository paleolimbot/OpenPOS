package net.fishandwhistle.openpos.actions;

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

public class ActionList extends ScannedItemAction {

    private static final String OPTION_ACTIONS = "actions";
    private static final String OPTION_EXECUTE_UNTIL = "execute_until";

    protected List<ScannedItemAction> actions ;
    private String executeUntil;

    public ActionList(JSONObject jsonObject) {
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
            throw new IllegalArgumentException("Invalid JSON in constructor: " + e.getMessage());
        }
        executeUntil = getOptionString(OPTION_EXECUTE_UNTIL);
    }

    @Override
    public boolean doActionContent(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        boolean result = true;
        List<String> errors = new ArrayList<>();
        for(ScannedItemAction action: actions) {
            boolean actionResult;
            try {
                actionResult = action.doAction(context, item, executor);
                if((executor != null && executor.isCancelled()) || (!isQuiet() && !actionResult)) {
                    return false;
                } else if(executeUntil != null) {
                    if((actionResult && executeUntil.equals("true")) || (!actionResult && executeUntil.equals("false")))
                        return actionResult;
                } else {
                    result = actionResult && result;
                }
            } catch(ActionException e) {
                if(isQuiet()) {
                    Log.e("ActionChain", "doActionContent: exception in action chain (supressing)", e);
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
