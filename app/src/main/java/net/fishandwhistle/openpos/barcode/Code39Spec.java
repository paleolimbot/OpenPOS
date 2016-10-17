package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dewey on 2016-10-11.
 */

public class Code39Spec extends CodabarSpec {

    public Code39Spec() {
        this(5, false);
    }

    public Code39Spec(int minLength, boolean fixedLength) {
        super("Code39", digc39, 9, minLength, fixedLength, "*", "*");
    }

    @Override
    public void initialize() {
        digc39.containsValue(new BarcodeDigit("0"));
    }

    private static Map<BarcodePattern, BarcodeDigit> digc39 = new HashMap<>();
    static {
        digc39.put(new BarcodePattern(new int[] {1, 2, 1, 1, 2, 1, 2, 1, 1}, true), new BarcodeDigit("*", "0"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 2, 2, 1, 2, 1, 1}, true), new BarcodeDigit("0", "0"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 1, 2, 1, 1, 1, 1, 2}, true), new BarcodeDigit("1", "1"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 2, 2, 1, 1, 1, 1, 2}, true), new BarcodeDigit("2", "2"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 2, 2, 1, 1, 1, 1, 1}, true), new BarcodeDigit("3", "3"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 2, 2, 1, 1, 1, 2}, true), new BarcodeDigit("4", "4"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 1, 2, 2, 1, 1, 1, 1}, true), new BarcodeDigit("5", "5"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 2, 2, 2, 1, 1, 1, 1}, true), new BarcodeDigit("6", "6"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 2, 1, 1, 2, 1, 2}, true), new BarcodeDigit("7", "7"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 1, 2, 1, 1, 2, 1, 1}, true), new BarcodeDigit("8", "8"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 2, 2, 1, 1, 2, 1, 1}, true), new BarcodeDigit("9", "9"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 1, 1, 1, 2, 1, 1, 2}, true), new BarcodeDigit("A", "10"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 2, 1, 1, 2, 1, 1, 2}, true), new BarcodeDigit("B", "11"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 2, 1, 1, 2, 1, 1, 1}, true), new BarcodeDigit("C", "12"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 1, 2, 2, 1, 1, 2}, true), new BarcodeDigit("D", "13"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 1, 1, 2, 2, 1, 1, 1}, true), new BarcodeDigit("E", "14"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 2, 1, 2, 2, 1, 1, 1}, true), new BarcodeDigit("F", "15"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 1, 1, 2, 2, 1, 2}, true), new BarcodeDigit("G", "16"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 1, 1, 1, 2, 2, 1, 1}, true), new BarcodeDigit("H", "17"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 2, 1, 1, 2, 2, 1, 1}, true), new BarcodeDigit("I", "18"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 1, 2, 2, 2, 1, 1}, true), new BarcodeDigit("J", "19"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 1, 1, 1, 1, 1, 2, 2}, true), new BarcodeDigit("K", "20"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 2, 1, 1, 1, 1, 2, 2}, true), new BarcodeDigit("L", "21"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 2, 1, 1, 1, 1, 2, 1}, true), new BarcodeDigit("M", "22"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 1, 2, 1, 1, 2, 2}, true), new BarcodeDigit("N", "23"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 1, 1, 2, 1, 1, 2, 1}, true), new BarcodeDigit("O", "24"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 2, 1, 2, 1, 1, 2, 1}, true), new BarcodeDigit("P", "25"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 1, 1, 1, 2, 2, 2}, true), new BarcodeDigit("Q", "26"));
        digc39.put(new BarcodePattern(new int[] {2, 1, 1, 1, 1, 1, 2, 2, 1}, true), new BarcodeDigit("R", "27"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 2, 1, 1, 1, 2, 2, 1}, true), new BarcodeDigit("S", "28"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 1, 2, 1, 2, 2, 1}, true), new BarcodeDigit("T", "29"));
        digc39.put(new BarcodePattern(new int[] {2, 2, 1, 1, 1, 1, 1, 1, 2}, true), new BarcodeDigit("U", "30"));
        digc39.put(new BarcodePattern(new int[] {1, 2, 2, 1, 1, 1, 1, 1, 2}, true), new BarcodeDigit("V", "31"));
        digc39.put(new BarcodePattern(new int[] {2, 2, 2, 1, 1, 1, 1, 1, 1}, true), new BarcodeDigit("W", "32"));
        digc39.put(new BarcodePattern(new int[] {1, 2, 1, 1, 2, 1, 1, 1, 2}, true), new BarcodeDigit("X", "33"));
        digc39.put(new BarcodePattern(new int[] {2, 2, 1, 1, 2, 1, 1, 1, 1}, true), new BarcodeDigit("Y", "34"));
        digc39.put(new BarcodePattern(new int[] {1, 2, 2, 1, 2, 1, 1, 1, 1}, true), new BarcodeDigit("Z", "35"));
        digc39.put(new BarcodePattern(new int[] {1, 2, 1, 1, 1, 1, 2, 1, 2}, true), new BarcodeDigit("–", "36"));
        digc39.put(new BarcodePattern(new int[] {2, 2, 1, 1, 1, 1, 2, 1, 1}, true), new BarcodeDigit(".", "37"));
        digc39.put(new BarcodePattern(new int[] {1, 2, 2, 1, 1, 1, 2, 1, 1}, true), new BarcodeDigit(" ", "38"));
        digc39.put(new BarcodePattern(new int[] {1, 2, 1, 2, 1, 2, 1, 1, 1}, true), new BarcodeDigit("$", "39"));
        digc39.put(new BarcodePattern(new int[] {1, 2, 1, 2, 1, 1, 1, 2, 1}, true), new BarcodeDigit("/", "40"));
        digc39.put(new BarcodePattern(new int[] {1, 2, 1, 1, 1, 2, 1, 2, 1}, true), new BarcodeDigit("+", "41"));
        digc39.put(new BarcodePattern(new int[] {1, 1, 1, 2, 1, 2, 1, 2, 1}, true), new BarcodeDigit("%", "42"));
    }

}
