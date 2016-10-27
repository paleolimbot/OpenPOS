package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-23.
 */

public class ActionIfElse extends ScannedItemAction {

    private static final String OPTION_TEST = "test";
    private static final String OPTION_TRUE_ACTION = "if_true";
    private static final String OPTION_FALSE_ACTION = "if_false";

    private ScannedItemAction forkAction;
    private ScannedItemAction ifTrue;
    private ScannedItemAction ifFalse;

    public ActionIfElse(JSONObject jsonObject) {
        super(jsonObject);
        try {
            JSONObject fork = getOptionObject(OPTION_TEST);
            forkAction = ActionFactory.inflate(fork);
            JSONObject trueObj = getOptionObject(OPTION_TRUE_ACTION, null);
            if (trueObj != null) {
                ifTrue = ActionFactory.inflate(trueObj);
            } else {
                ifTrue = null;
            }
            JSONObject falseObj = getOptionObject(OPTION_FALSE_ACTION, null);
            if (falseObj != null) {
                ifFalse = ActionFactory.inflate(falseObj);
            }
        } catch(JSONException e) {
            throw new IllegalArgumentException("Invalid JSON in constructor: " + e.getMessage());
        }
    }

    @Override
    public boolean doActionContent(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        if(forkAction.doActionContent(context, item, executor)) {
            if(ifTrue != null) {
                return ifTrue.doAction(context, item, executor);
            } else {
                return true;
            }
        } else {
            if(ifFalse != null) {
                return ifFalse.doAction(context, item, executor);
            } else {
                return true;
            }
        }
    }
}
