package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.widget.Toast;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONObject;

import static net.fishandwhistle.openpos.actions.Formatting.formatWithObject;

/**
 * Created by dewey on 2016-10-27.
 */

public class ToastAction extends ScannedItemAction {

    public static final String OPTION_MESSAGE = "message";
    public static final String OPTION_DURATION = "duration";

    private String message;
    private int duration;

    public ToastAction(JSONObject jsonOptions) {
        super(jsonOptions);
        message = getOptionString(OPTION_MESSAGE);
        duration = getOptionInt(OPTION_DURATION, Toast.LENGTH_SHORT);
    }

    @Override
    public boolean isApplicable(Context context, ScannedItem item, ActionExecutor executor) {
        return formatWithObject(message, item, false) != null;
    }

    @Override
    public boolean doActionContent(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        Toast.makeText(context, formatWithObject(message, item), duration).show();
        return true;
    }
}
