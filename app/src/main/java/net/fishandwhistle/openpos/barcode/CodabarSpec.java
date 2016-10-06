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

public class CodabarSpec extends BarcodeSpec {

    private int minLength;
    private boolean fixedLength;
    private String endChars;
    private String startChars;

    public CodabarSpec() {
        this(5, "abcd", "abcd", false);
    }

    public CodabarSpec(int minLength, String startChars, String endChars, boolean fixedLength) {
        super("Codabar", digcodabar);
        this.minLength = minLength;
        this.startChars = startChars;
        this.endChars = endChars;
    }

    @Override
    protected BarcodePattern getBarcodePattern(int[] bars, boolean start) {
        int[] widthrange = range(bars);
        int threshold = widthrange[0] + (widthrange[1]-widthrange[0]) / 2;
        for(int i=0; i<bars.length; i++) {
            if(bars[i] >= threshold) {
                bars[i] = 2;
            } else {
                bars[i] = 1;
            }
        }
        return new BarcodePattern(bars, start);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        // must start on a 1(true) bar 0, 2, 4, 6, etc., so i%2==0 is the value at i
        boolean[] vals = new boolean[bars.length];
        for(int i=0; i<vals.length; i++) {
            vals[i] = (i%2) == 0 ;
        }
        Barcode b = new Barcode(this.getType());
        b.timeread = System.currentTimeMillis();

        if(bars.length < 15) throw new BarcodeException("To few bars to decode barcode", b);

        for(int i=0; i<(bars.length-7); i+= 8) {
            b.digits.add(this.getDigit(subset(bars, i, 7), vals[i]));
        }
        if(b.digits.size() < minLength) throw new BarcodeException("Too few decoded digits", b);

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
                if((d != null) && endChars.contains(d.digit)) {
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
        if(fixedLength && newdigs.size() != minLength) throw new BarcodeException("Wrong number of decoded digits", b);
        if(newdigs.size() < minLength) throw new BarcodeException("Too few decoded digits", b);
        b.digits = newdigs;
        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);
        b.isValid = true;

        return b;
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
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 2, 2, 1, 2, 1}, true), new BarcodeDigit("a")); // was t
        digcodabar.put(new BarcodePattern(new int[]{1, 2, 1, 2, 1, 1, 2}, true), new BarcodeDigit("b")); // was n
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 1, 2, 2}, true), new BarcodeDigit("c")); // was *
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 2, 2, 1}, true), new BarcodeDigit("d")); // was e
    }
}
