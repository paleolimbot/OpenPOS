package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;
import net.fishandwhistle.openpos.items.ScannedItemManager;

import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-22.
 */

public class SessionAction extends ScannedItemAction {

    public static final String OPTION_SESSION_NAME = "session_name";
    public static final String OPTION_ACTION = "action";

    private String sessionName;
    private String action;

    public SessionAction(JSONObject jsonObject) {
        super(jsonObject);
        sessionName = getOptionString(OPTION_SESSION_NAME, null); //null session means current active session
        action = getOptionEnum(OPTION_ACTION, new String[] {"add", "remove", "sync"});
    }

    @Override
    public boolean doActionContent(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        ScannedItemManager manager = new ScannedItemManager(context);
        boolean result;
        switch (action) {
            case "add":
                result = manager.putItem(sessionName, item);
                break;
            case "remove":
                result = manager.removeItem(sessionName, item);
                break;
            case "sync":
                result = manager.syncItem(sessionName, item);
                break;
            default: throw new ActionException("Unrecognized action: " + action);

        }
        if(result) {
            return true;
        } else if(isQuiet()) {
            return false;
        } else {
            throw new ActionException(String.format("Failed to do action '%s' for item", action));
        }
    }
}
