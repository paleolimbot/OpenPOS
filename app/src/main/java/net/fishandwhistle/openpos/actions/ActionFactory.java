package net.fishandwhistle.openpos.actions;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by dewey on 2016-10-23.
 */

public class ActionFactory {

    public static ScannedItemAction inflate(JSONObject o) throws JSONException {
        switch(o.getString(ScannedItemAction.OPTION_TYPE)) {
            case "list":
                return new ActionList(o);
            case "ifelse":
                return new ActionIfElse(o);
            case "switch":
                return new ActionSwitch(o);
            case "blank":
                return new ActionBlank(o);
            case "session":
                return new SessionAction(o);
            case "keyfilter":
                return new KeyFilterAction(o);
            case "match":
                return new LogicAction(o);
            case "lookup":
                return new LookupAction(o);
            case "stringformat":
                return new StringFormatAction(o);
            case "dialog":
                return new DialogAction(o);
            case "details":
                return new DetailsDialogAction(o);
            case "intent":
                return new IntentAction(o);
            case "vibrate":
                return new VibrateAction(o);
            case "chooser":
                return new ChooserDialogAction(o);
            case "toast":
                return new ToastAction(o);
            default: throw new IllegalArgumentException("Invalid class supplied: " + o.getString(ScannedItemAction.OPTION_TYPE));
        }
    }

}
