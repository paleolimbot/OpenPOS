package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

import static net.fishandwhistle.openpos.actions.Formatting.formatWithObject;

/**
 * Created by dewey on 2016-10-24.
 */

public class IntentAction extends ScannedItemAction {

    public static final String OPTION_URI_FORMAT = "uri_format";
    public static final String OPTION_ACTION = "action";
    public static final String OPTION_EXTRAS = "extras";
    public static final String OPTION_INTENT_TYPE = "intent_type"; //activity or broadcast
    public static final String OPTION_FORMAT_EXTRAS = "format_extras"; //boolean

    private String uriFormat;
    private Map<String, String> extras;
    private String intentType;
    private String action;
    private boolean formatExtras;

    public IntentAction(JSONObject jsonOptions) {
        super(jsonOptions);
        uriFormat = getOptionString(OPTION_URI_FORMAT);
        if(uriFormat == null) throw new IllegalArgumentException("Option 'uri_format' is required");
        action = getOptionString(OPTION_ACTION);
        if(action == null) {
            action = Intent.ACTION_VIEW;
        }
        intentType = getOptionString(OPTION_INTENT_TYPE);
        if(intentType == null) {
            intentType = "activity";
        } else if(!intentType.equals("activity") && !intentType.equals("broadcast")) {
            throw new IllegalArgumentException("Option 'intent_type' must be one of 'activity' or 'broadcast'");
        }
        extras = extractKeyMap(getOptionObject(OPTION_EXTRAS));
        String fExtras = getOptionString(OPTION_FORMAT_EXTRAS);
        formatExtras = fExtras != null && Boolean.valueOf(fExtras);
    }

    @Override
    public boolean doAction(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        String uriString = formatWithObject(uriFormat, item, false);
        if(uriString != null) {
            Uri uri = Uri.parse(uriString);
            if(uri != null) {
                Intent intent = new Intent(action);
                intent.setData(uri);
                if(extras != null) {
                    Bundle ext = new Bundle();
                    for(Map.Entry<String, String> entry: extras.entrySet()) {
                        if(formatExtras) {
                            ext.putString(entry.getKey(), formatWithObject(entry.getValue(), item));
                        } else {
                            ext.putString(entry.getKey(), entry.getValue());
                        }
                    }
                    intent.putExtras(ext);
                }

                if(intentType.equals("broadcast")) {
                    context.sendBroadcast(intent);
                } else {
                    context.startActivity(intent);
                }
            } else {
                throw new ActionException("Invalid URI: " + uriString);
            }
        } else {
            if(isQuiet()) {
                return false;
            } else {
                throw new ActionException("URI contained unmapped keys: " + uriFormat);
            }
        }
        return false;
    }
}
