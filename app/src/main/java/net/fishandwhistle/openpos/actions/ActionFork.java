package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;

/**
 * Created by dewey on 2016-10-23.
 */

public class ActionFork extends ScannedItemAction {

    private ScannedItemAction forkAction;
    private ScannedItemAction ifTrue;
    private ScannedItemAction ifFalse;

    public ActionFork(ScannedItemAction forkAction, ScannedItemAction ifTrue, ScannedItemAction ifFalse) {
        super("fork", "{}");
        this.forkAction = forkAction;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
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
