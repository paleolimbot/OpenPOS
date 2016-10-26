package net.fishandwhistle.openpos.actions;

import net.fishandwhistle.openpos.items.ScannedItem;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dewey on 2016-10-26.
 */

public class Formatting {

    public interface Formattable {
        String getValue(String key);
    }

    public static String formatWithObject(String format, Formattable item) {
        return formatWithObject(format, item, true);
    }

    public static String formatWithObject(String format, Formattable item, boolean quiet) {
        Pattern TAG = Pattern.compile("\\{\\{(.*?)\\}\\}");
        StringBuffer sb = new StringBuffer();
        Matcher m = TAG.matcher(format);
        while(m.find()) {
            String attr = m.group(1);
            String attrVal = item.getValue(attr);
            if(attrVal == null) {
                if(quiet) {
                    attrVal = "{{" + attr + "}}";
                } else {
                    return null;
                }
            }
            m.appendReplacement(sb, attrVal);
        }
        m.appendTail(sb);
        return sb.toString();
    }



}
