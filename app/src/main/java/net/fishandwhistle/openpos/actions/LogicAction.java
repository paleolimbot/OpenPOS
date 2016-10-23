package net.fishandwhistle.openpos.actions;

import android.content.Context;

import net.fishandwhistle.openpos.items.ScannedItem;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by dewey on 2016-10-22.
 */

public class LogicAction extends ScannedItemAction {

    public static final String OPTION_KEYMAP = "key_map";
    public static final String OPTION_ISREGEX = "is_regex";
    public static final String OPTION_LOGIC = "logic"; // any or all
    public static final String OPTION_INVERT_RESULT = "invert_result";
    public static final String OPTION_MATCH_OPTION = "match_option"; //matches or contains
    public static final String OPTION_OUTKEY = "out_key"; //can be null (value is returned instead)

    private String logic;
    private String matchOption;
    private boolean invert;
    private boolean isRegex;
    private Map<String, String> keyMap;
    private String outKey;

    public LogicAction(String actionName, String jsonOptions) {
        super(actionName, jsonOptions);

        String sMatchOption = getOptionString(OPTION_MATCH_OPTION);
        if(sMatchOption == null) {
            matchOption = "matches";
        } else if(sMatchOption.equals("matches") || sMatchOption.equals("contains")) {
            matchOption = sMatchOption;
        } else {
            throw new IllegalArgumentException("Option 'match_option' must be one of 'matches' or 'contains'");
        }

        outKey = getOptionString(OPTION_OUTKEY);

        String sInvert = getOptionString(OPTION_INVERT_RESULT);
        invert = sInvert != null && Boolean.valueOf(sInvert);
        String sRegex = getOptionString(OPTION_ISREGEX);
        isRegex = sRegex != null && Boolean.valueOf(sRegex);

        keyMap = extractKeyMap(getOptionObject(OPTION_KEYMAP));
        if(keyMap == null) throw new IllegalArgumentException("Option 'key_map' is required");
        if(isRegex) {
            try {
                for (Map.Entry<String, String> entry : keyMap.entrySet()) {
                    Pattern.compile(entry.getValue());
                }
            } catch(PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regular expression: " + e.getMessage());
            }
        }

        String sLogic = getOptionString(OPTION_LOGIC);
        if(sLogic == null) {
            logic = "all";
        } else if(sLogic.equals("any") || sLogic.equals("all")) {
            logic = sLogic;
        } else {
            throw new IllegalArgumentException("Option 'logic' must be one of 'any' or 'all'");
        }
    }

    @Override
    public boolean doAction(Context context, ScannedItem item) {

        boolean result = logic.equals("all");

        for(Map.Entry<String, String> entry: keyMap.entrySet()) {
            String mapKey = entry.getKey();
            String regex = entry.getValue();
            String value = item.getValue(mapKey);
            boolean match;
            if(value == null) {
                match = regex.equals("");
            } else if(isRegex) {
                if(matchOption.equals("matches")) {
                    match = value.matches(regex);
                } else {
                    Matcher m = Pattern.compile(regex).matcher(value);
                    match = m.find();
                }
            } else {
                if(matchOption.equals("matches")) {
                    match = value.equals(regex);
                } else {
                    match = value.contains(regex);
                }
            }

            if(logic.equals("all")) {
                result = result && match;
            } else {
                result = true;
                break;
            }
        }
        if(invert) {
            result = !result;
        }
        if(outKey == null) {
            return result;
        } else {
            item.putValue(outKey, String.valueOf(result));
            return true;
        }
    }

}
