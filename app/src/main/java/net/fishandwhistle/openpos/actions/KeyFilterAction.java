package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by dewey on 2016-10-22.
 * example: new FilterAction("{\"keys\"=[\".*?_isbndb\"], \"action\":\"keep\", \"is_regex\": \"true\", \"match_option\": \"matches\"}")
 */

public class KeyFilterAction extends ScannedItemAction {

    public static final String OPTION_KEYS = "keys";
    public static final String OPTION_ISREGEX = "is_regex";
    public static final String OPTION_MATCH_OPTION = "match_option"; //one of 'matches' or 'contains'
    public static final String OPTION_ACTION = "action"; //one of 'keep' or 'remove'

    private String matchOption;
    private boolean isRegex;
    private String action;
    private String[] keys;

    public KeyFilterAction(JSONObject jsonObject) {
        super(jsonObject);
        action = getOptionString(OPTION_ACTION);
        if(action == null) throw new IllegalArgumentException("Option 'action' is required");
        if(!action.equals("keep") && !action.equals("remove"))
            throw new IllegalArgumentException("Option 'action' must be one of 'keep' or 'remove'");
        String sRegex = getOptionString(OPTION_ISREGEX);
        isRegex = sRegex != null && Boolean.valueOf(sRegex);

        String sMatchOption = getOptionString(OPTION_MATCH_OPTION);
        if(sMatchOption == null) {
            matchOption = "matches";
        } else if(sMatchOption.equals("matches") || sMatchOption.equals("contains")) {
            matchOption = sMatchOption;
        } else {
            throw new IllegalArgumentException("Option 'match_option' must be one of 'matches' or 'contains'");
        }

        JSONArray a = getOptionArray(OPTION_KEYS);
        if(a == null) throw new IllegalArgumentException("Option 'keys' must be an array");
        try {
            keys = new String[a.length()];
            for (int i = 0; i < a.length(); i++) {
                keys[i] = a.getString(i);
                if (isRegex) {
                    //test regex
                    Pattern.compile(keys[i]);
                }
            }
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JSON in keys: " + e.getMessage());
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex in keys: " + a.toString());
        }

    }

    @Override
    public boolean doActionContent(Context context, ScannedItem item, ActionExecutor executor) {
        List<String> itemKeys = item.getKeys();
        boolean result = false;
            for(String itemKey: itemKeys) {
                boolean match = false;
                for(String needle: keys) {
                    if(!isRegex) {
                        if(matchOption.equals("matches")) {
                            match = itemKey.equals(needle);
                        } else {
                            match = itemKey.contains(needle);
                        }
                    } else if(matchOption.equals("matches")) {
                        match = itemKey.matches(needle);
                    } else {
                        Matcher m = Pattern.compile(needle).matcher(itemKey);
                        match = m.find();
                    }
                    if(match) {
                        break;
                    }
                }
                if ((action.equals("keep") && !match) || (action.equals("remove") && match)) {
                    item.putValue(itemKey, null);
                    result = true;
                }
        }
        return result;
    }

}
