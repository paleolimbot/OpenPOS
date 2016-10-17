package net.fishandwhistle.openpos.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.range;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;

/**
 * Created by dewey on 2016-10-05.
 */

public class CodabarSpec extends DualWidthSpec {

    private String endChars;
    private String startChars;
    private int nbarsPerDigit ;

    public CodabarSpec() {
        this(5, false, "ABCD", "ABCD");
    }

    public CodabarSpec(int minLength, boolean fixedLength, String startChars, String endChars) {
        this("Codabar", digcodabar, 7, minLength, fixedLength, startChars, endChars);
    }

    protected CodabarSpec(String type, Map<BarcodePattern, BarcodeDigit> digits, int nbarsPerDigit, int minLength,
                          boolean fixedLength, String startChars, String endChars) {
        super(type, digits, minLength, fixedLength);
        this.nbarsPerDigit = nbarsPerDigit;
        this.startChars = startChars;
        this.endChars = endChars;
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        // must start on a 1(true) bar 0, 2, 4, 6, etc., so i%2==0 is the value at i
        boolean[] vals = new boolean[bars.length];
        for(int i=0; i<vals.length; i++) {
            vals[i] = (i%2) == 0 ;
        }
        Barcode b = new Barcode(this.getType());

        if(bars.length < (nbarsPerDigit + 1 + (nbarsPerDigit+1)*minLength + nbarsPerDigit))
            throw new BarcodeException("Too few bars to decode barcode", b);

        for(int i=0; i<(bars.length-nbarsPerDigit); i+= (nbarsPerDigit+1)) {
            b.digits.add(this.getDigit(subset(bars, i, nbarsPerDigit), vals[i]));
        }

        //check for length requirement but disregard exact length
        if(b.digits.size() < minLength) throw new BarcodeException("Not enough digits", b);

        // look for start/end character
        int startIndex = -1;
        int endIndex = -1;
        List<BarcodeDigit> newdigs = new ArrayList<>();
        for(int i=0; i<b.digits.size(); i++) {
            BarcodeDigit d = b.digits.get(i);
            if(startIndex == -1) {
                if((d != null) && startChars.contains(d.digit)) {
                    startIndex = i;
                }
            } else {
                if(!partial && (d == null)) {
                    throw new BarcodeException("Undecodable digit", b);
                } else if((d != null) && endChars.contains(d.digit)) {
                    endIndex = i;
                    newdigs.add(d);
                    break;
                }
            }

            if(startIndex != -1) {
                newdigs.add(d);
            }
        }
        if(endIndex == -1) throw new BarcodeException("Invalid end character", b);
        b.digits = newdigs;
        this.checkLength(b);
        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);
        b.isValid = true;

        return b;
    }

    @Override
    public void initialize() {
        digcodabar.containsValue(new BarcodeDigit("0"));
    }

    private static Map<BarcodePattern, BarcodeDigit> digcodabar = new HashMap<>();
    static {
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 1, 1, 2, 2}, true), new BarcodeDigit("0"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 1, 2, 2, 1}, true), new BarcodeDigit("1"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 1, 1, 2}, true), new BarcodeDigit("2"));
        digcodabar.put(new BarcodePattern(new int[]{2, 2, 1, 1, 1, 1, 1}, true), new BarcodeDigit("3"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 2, 1, 1, 2, 1}, true), new BarcodeDigit("4"));
        digcodabar.put(new BarcodePattern(new int[]{2, 1, 1, 1, 1, 2, 1}, true), new BarcodeDigit("5"));
        digcodabar.put(new BarcodePattern(new int[]{1, 2, 1, 1, 1, 1, 2}, true), new BarcodeDigit("6"));
        digcodabar.put(new BarcodePattern(new int[]{1, 2, 1, 1, 2, 1, 1}, true), new BarcodeDigit("7"));
        digcodabar.put(new BarcodePattern(new int[]{1, 2, 2, 1, 1, 1, 1}, true), new BarcodeDigit("8"));
        digcodabar.put(new BarcodePattern(new int[]{2, 1, 1, 2, 1, 1, 1}, true), new BarcodeDigit("9"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 2, 1, 1}, true), new BarcodeDigit("-"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 2, 2, 1, 1, 1}, true), new BarcodeDigit("$"));
        digcodabar.put(new BarcodePattern(new int[]{2, 1, 1, 1, 2, 1, 2}, true), new BarcodeDigit(":"));
        digcodabar.put(new BarcodePattern(new int[]{2, 1, 2, 1, 1, 1, 2}, true), new BarcodeDigit("/"));
        digcodabar.put(new BarcodePattern(new int[]{2, 1, 2, 1, 2, 1, 1}, true), new BarcodeDigit("."));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 2, 1, 2, 1, 2}, true), new BarcodeDigit("+"));
        //digcodabar.put(new BarcodePattern(new int[]{1, 1, 2, 2, 1, 2, 1}, true), new BarcodeDigit("t"));
        //digcodabar.put(new BarcodePattern(new int[]{1, 2, 1, 2, 1, 1, 2}, true), new BarcodeDigit("n"));
        //digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 1, 2, 2}, true), new BarcodeDigit("*"));
        //digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 2, 2, 1}, true), new BarcodeDigit("e"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 2, 2, 1, 2, 1}, true), new BarcodeDigit("A")); // was t
        digcodabar.put(new BarcodePattern(new int[]{1, 2, 1, 2, 1, 1, 2}, true), new BarcodeDigit("B")); // was n
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 1, 2, 2}, true), new BarcodeDigit("C")); // was *
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 2, 2, 1}, true), new BarcodeDigit("D")); // was e
    }
}
