package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.range;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;

/**
 * Created by dewey on 2016-10-05.
 */

public class CodabarSpec extends BarcodeSpec {


    public CodabarSpec() {
        super("Codabar", digcodabar);
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

        for(int i=0; i<(bars.length-7); i+= 8) {
            b.digits.add(this.getDigit(subset(bars, i, 7), vals[i]));
        }

        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        //check for valid start/stop characters (abcd)
        String dig1 = b.digits.get(0).digit;
        String lastdig = b.digits.get(b.digits.size()-1).digit;
        String validChars = "abcdtne";
        if(!validChars.contains(dig1) || !validChars.contains(lastdig)) throw new BarcodeException("Invalid codabar start/stop digits", b);
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
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 2, 2, 1, 2, 1}, true), new BarcodeDigit("a"));
        digcodabar.put(new BarcodePattern(new int[]{1, 2, 1, 2, 1, 1, 2}, true), new BarcodeDigit("b"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 1, 2, 2}, true), new BarcodeDigit("c"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 2, 2, 1}, true), new BarcodeDigit("d"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 2, 2, 1, 2, 1}, true), new BarcodeDigit("t"));
        digcodabar.put(new BarcodePattern(new int[]{1, 2, 1, 2, 1, 1, 2}, true), new BarcodeDigit("n"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 1, 2, 2}, true), new BarcodeDigit("*"));
        digcodabar.put(new BarcodePattern(new int[]{1, 1, 1, 2, 2, 2, 1}, true), new BarcodeDigit("e"));
    }
}
