package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by dewey on 2016-10-24.
 */

public class ActionSwitch extends ScannedItemAction {

    public static final String OPTION_KEY = "key";
    public static final String OPTION_VALUES = "values";
    public static final String OPTION_ACTIONS = "actions";
    public static final String OPTION_ISREGEX = "is_regex";
    public static final String OPTION_DEFAULT = "default";

    private String key;
    private boolean isRegex;
    private List<String> values;
    private List<ScannedItemAction> actions;
    private ScannedItemAction defaultAction;

    public ActionSwitch(JSONObject jsonOptions) {
        super(jsonOptions);
        key = getOptionString(OPTION_KEY);
        isRegex = getOptionBoolean(OPTION_ISREGEX, false);
        JSONArray vals = getOptionArray(OPTION_VALUES);
        JSONArray acts = getOptionArray(OPTION_ACTIONS);
        if(vals.length() != acts.length()) throw new IllegalArgumentException("values and actions must be of identical length");
        if(vals.length() == 0) throw new IllegalArgumentException("values must be of non-zero length");

        try {
            values = new ArrayList<>();
            actions = new ArrayList<>();
            for(int i=0; i<vals.length(); i++) {
                String key = vals.getString(i);
                if(isRegex) {
                    Pattern.compile(key);
                }
                values.add(key);
                actions.add(ActionFactory.inflate(acts.getJSONObject(i)));
            }


            JSONObject defObj = getOptionObject(OPTION_DEFAULT, null);
            if(defObj != null) {
                defaultAction = ActionFactory.inflate(defObj);
            } else {
                defaultAction = null;
            }

        } catch(JSONException e) {
            throw new IllegalArgumentException("Invalid JSON found in constructor: " + e.getMessage());
        } catch(PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regular expression in keys: " + e.getMessage());
        }
    }

    @Override
    public boolean doActionContent(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        String value = item.getValue(key);
        for(int i=0; i<values.size(); i++) {
            String toMatch = values.get(i);
            boolean match;
            if(value == null) {
                match = toMatch.equals("");
            } else if(isRegex) {
                match = value.matches(toMatch);
            } else {
                match = value.equals(toMatch);
            }
            if(match) {
                return actions.get(i).doAction(context, item, executor);
            }
        }

        if(defaultAction != null) {
            return defaultAction.doAction(context, item, executor);
        }

        return false;
    }
}
