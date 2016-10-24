package net.fishandwhistle.openpos.actions;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by dewey on 2016-10-23.
 */

public class ActionFactory {

    public static ScannedItemAction inflate(JSONObject o) throws JSONException {
        switch(o.getString(ScannedItemAction.OPTION_TYPE)) {
            case "chain":
                return new ActionChain(o);
            case "fork":
                return new ActionFork(o);
            case "addtosession":
                return new AddToSessionAction(o);
            case "keyfilter":
                return new KeyFilterAction(o);
            case "logic":
                return new LogicAction(o);
            case "lookup":
                return new LookupAction(o);
            case "stringformat":
                return new StringFormatAction(o);
            default: throw new IllegalArgumentException("Invalid class supplied: " + o.getString(ScannedItemAction.OPTION_TYPE));
        }
    }

}
