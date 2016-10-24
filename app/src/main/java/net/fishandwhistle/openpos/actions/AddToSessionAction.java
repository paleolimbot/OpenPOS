package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;
import net.fishandwhistle.openpos.items.ScannedItemManager;

import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-22.
 */

public class AddToSessionAction extends ScannedItemAction {

    public static final String OPTION_SESSION_NAME = "session_name";

    private String sessionName;

    public AddToSessionAction(JSONObject jsonObject) {
        super(jsonObject);
        sessionName = getOptionString(OPTION_SESSION_NAME); //null session means current active session
    }

    @Override
    public boolean doAction(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        ScannedItemManager manager = new ScannedItemManager(context);
        boolean result = manager.putItem(sessionName, item);
        if(result) {
            return true;
        } else if(isQuiet()) {
            return false;
        } else {
            throw new ActionException("Failed to add item to session");
        }
    }
}
