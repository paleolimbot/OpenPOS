package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;
import net.fishandwhistle.openpos.items.ScannedItemManager;

/**
 * Created by dewey on 2016-10-22.
 */

public class AddToSessionAction extends ScannedItemAction {

    public static final String OPTION_SESSION_NAME = "session_name";

    private String sessionName;

    public AddToSessionAction(String jsonOptions) {
        super("addToSession", jsonOptions);
        sessionName = getOptionString(OPTION_SESSION_NAME); //null session means current active session
    }

    @Override
    public boolean doAction(Context context, ScannedItem item) {
        ScannedItemManager manager = new ScannedItemManager(context);
        return manager.putItem(sessionName, item);
    }
}
