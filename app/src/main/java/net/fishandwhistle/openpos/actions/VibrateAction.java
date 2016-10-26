package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.os.Vibrator;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONObject;

/**
 * Created by dewey on 2016-10-25.
 */

public class VibrateAction extends ScannedItemAction {

    public VibrateAction(JSONObject jsonOptions) {
        super(jsonOptions);
    }

    @Override
    public boolean doActionContent(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(150);
        return true;
    }
}
