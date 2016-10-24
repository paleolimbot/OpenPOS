package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-23.
 */

public class ActionFork extends ScannedItemAction {

    private static final String OPTION_FORK_ACTION = "fork";
    private static final String OPTION_TRUE_ACTION = "if_true";
    private static final String OPTION_FALSE_ACTION = "if_false";

    private ScannedItemAction forkAction;
    private ScannedItemAction ifTrue;
    private ScannedItemAction ifFalse;

    public ActionFork(JSONObject jsonObject) {
        super(jsonObject);
        try {
            JSONObject fork = getOptionObject(OPTION_FORK_ACTION);
            if (fork == null) throw new IllegalArgumentException("Option 'fork' is required");
            forkAction = ActionFactory.inflate(fork);
            JSONObject trueObj = getOptionObject(OPTION_TRUE_ACTION);
            if (trueObj != null) {
                ifTrue = ActionFactory.inflate(trueObj);
            } else {
                ifTrue = null;
            }
            JSONObject falseObj = getOptionObject(OPTION_FALSE_ACTION);
            if (falseObj != null) {
                ifFalse = ActionFactory.inflate(falseObj);
            }
        } catch(JSONException e) {
            throw new IllegalArgumentException("Invalid JSON in constructor: " + e.getMessage());
        }
    }

    @Override
    public boolean doAction(Context context, ScannedItem item) throws ActionException {
        if(forkAction.doAction(context, item)) {
            if(ifTrue != null) {
                return ifTrue.doAction(context, item);
            } else {
                return true;
            }
        } else {
            if(ifFalse != null) {
                return ifFalse.doAction(context, item);
            } else {
                return true;
            }
        }
    }
}
