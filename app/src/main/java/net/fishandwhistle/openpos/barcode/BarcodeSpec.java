package net.fishandwhistle.openpos.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.all;
import static net.fishandwhistle.openpos.barcode.ArrayMath.any;
import static net.fishandwhistle.openpos.barcode.ArrayMath.concatenate;
import static net.fishandwhistle.openpos.barcode.ArrayMath.div;
import static net.fishandwhistle.openpos.barcode.ArrayMath.eq;
import static net.fishandwhistle.openpos.barcode.ArrayMath.gt;
import static net.fishandwhistle.openpos.barcode.ArrayMath.mean;
import static net.fishandwhistle.openpos.barcode.ArrayMath.round;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;
import static net.fishandwhistle.openpos.barcode.ArrayMath.sum;

/**
 * Created by dewey on 2016-09-29.
 */

public class BarcodeSpec {

    public static final boolean[] GUARD_101 = new boolean[] {true, false, true};
    public static final boolean[] GUARD_01010 = new boolean[] {false, true, false, true, false};
    public static final int NBARS_ISBN = 59;
    public static final int NBARS_DIGIT_ISBN = 4;
    public static final String TYPE_ISBN = "ISBN";

    private Map<BarcodePattern, BarcodeDigit> digits ;
    private int nbars ;
    private int nbarsDigit ;
    private boolean[] leftGuard ;
    private boolean[] rightGuard ;
    private boolean[] middleGuard ;
    private String type;

    // calculated fields
    private int nbarsSide;

    public BarcodeSpec(String type, Map<BarcodePattern, BarcodeDigit> digits, int nbars, int nbarsDigit,
                       boolean[] leftGuard, boolean[] rightGuard, boolean[] middleGuard) {
        this.type = type;
        this.digits = digits;
        this.nbars = nbars;
        this.nbarsDigit = nbarsDigit;
        this.leftGuard = leftGuard;
        this.rightGuard = rightGuard;
        this.middleGuard = middleGuard;

        //calculated
        this.nbarsSide = (nbars - leftGuard.length - rightGuard.length - middleGuard.length) / 2;
    }

    public BarcodeDigit getDigit(int[] bars, double barsize, boolean start) {
        BarcodePattern pattern = new BarcodePattern(round(div(bars, sum(bars)/7.0)), start);

        for(Map.Entry<BarcodePattern, BarcodeDigit> entry : digits.entrySet()) {
            BarcodePattern p = entry.getKey();
            if(entry.getKey().equals(pattern)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public boolean checksum(Barcode b) {
        int[] numbers = new int[b.digits.size()];
        for(int i=0; i<numbers.length; i++) {
            numbers[i] = Integer.valueOf(b.digits.get(i).digit);
        }
        int oddsum = 0;
        for(int i=0; i<numbers.length; i+=2) {
            oddsum += numbers[i];
        }
        int evensum = 0;
        for(int i=1; i<numbers.length-1; i+=2) {
            evensum += numbers[i];
        }
        int s1 = evensum * 3 + oddsum;
        int checksum = (int)Math.round(Math.pow(10, s1/10+1) - s1);
        return checksum == numbers[numbers.length-1];
    }

    public Barcode parse(int[] bars) throws BarcodeException {
        // must start on a 1(true) bar 0, 2, 4, 6, etc., so i%2==0 is the value at i
        boolean[] vals = new boolean[bars.length];
        for(int i=0; i<vals.length; i++) {
            vals[i] = (i%2) == 0 ;
        }
        Barcode b = new Barcode(type);
        //test number of bars
        if(bars.length < nbars) throw new BarcodeException("Not enough bars to create code", b);
        // test start code
        int[] leftguard = subset(bars, 0, 3);
        double barsize = mean(leftguard);
        if(any(gt(div(leftguard, barsize), 3))) throw new BarcodeException("Left guard has irregular barsize", b);
        // assign left side
        int[] leftside = subset(bars, 3, nbarsSide);
        boolean[] vleftside = subset(vals, 3, nbarsSide);
        // test middle guard
        int[] middleguard = subset(bars, nbarsSide+3, 5);
        barsize = (barsize * 3 + mean(middleguard) * 5) / 8.0;
        if(any(gt(div(middleguard, barsize), 3))) throw new BarcodeException("Middle guard has irregular barsize", b);
        int[] rightside = subset(bars, nbarsSide+3+5, nbarsSide);
        boolean[] vrightside = subset(vals, nbarsSide+3+5, nbarsSide);
        // test end guard
        int[] endguard = subset(bars, nbarsSide+3+5+nbarsSide, 3);
        barsize = (barsize * 8 + mean(endguard) * 3) / 11.0;
        if(any(gt(div(endguard, barsize), 3))) throw new BarcodeException("End guard has irregular barsize", b);

        int[] decodable = concatenate(leftside, rightside);
        boolean[] vdecodable = concatenate(vleftside, vrightside);
        for(int i=0; i<decodable.length; i+= nbarsDigit) {
            b.digits.add(this.getDigit(subset(decodable, i, nbarsDigit), barsize, vdecodable[i]));
        }

        boolean[] validDigits = new boolean[b.digits.size()];
        for(int i=0; i<validDigits.length; i++) {
            validDigits[i] = b.digits.get(i) != null;
        }
        if(!all(validDigits)) throw new BarcodeException("Not all digits could be decoded", b);

        //should scheme check the ISBN code here (not yet)
        b.digits.add(0, new BarcodeDigit("9", "MANUAL"));

        //implement checksum here

        return b;
    }

    public static class Barcode {
        public String type;
        public List<BarcodeDigit> digits;

        public Barcode(String type) {
            this.type = type;
            digits = new ArrayList<>();
        }

        public String toString() {
            String out = "";
            for(BarcodeDigit d: digits) {
                if(d == null) {
                    out += "^";
                } else {
                    out += String.valueOf(d.digit);
                }
            }
            return out;
        }
    }

    public static class BarcodePattern {
        public int[] widths;
        public boolean startsWith;

        public BarcodePattern(int[] widths, boolean startsWith) {
            this.widths = widths;
            this.startsWith = startsWith;
        }

        public boolean equals(Object o) {
            if(o instanceof BarcodePattern) {
                BarcodePattern b = (BarcodePattern) o;
                return (b.startsWith == this.startsWith) && all(eq(this.widths, b.widths));
            } else {
                return false;
            }
        }
    }

    public static class BarcodeDigit {
        public String digit;
        public String tag;

        public BarcodeDigit(String digit, String tag) {
            this.digit = digit;
            this.tag = tag;
        }

        public BarcodeDigit(String digit) {
            this.digit = digit;
            this.tag = null;
        }

        public String toString() {
            return this.digit;
        }
    }

    public static class BarcodeException extends Exception {
        public Barcode partial;
        public BarcodeException(String message, Barcode partial) {
            super(message);
            this.partial = partial;
        }
    }

    private static Map<BarcodePattern, BarcodeDigit> digisbn = new HashMap<>();

    static {
        digisbn.put(new BarcodePattern(new int[] {3, 2, 1, 1}, false), new BarcodeDigit("0", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 2, 2, 1}, false), new BarcodeDigit("1", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 1, 2, 2}, false), new BarcodeDigit("2", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 4, 1, 1}, false), new BarcodeDigit("3", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 3, 2}, false), new BarcodeDigit("4", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 3, 1}, false), new BarcodeDigit("5", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 1, 4}, false), new BarcodeDigit("6", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 3, 1, 2}, false), new BarcodeDigit("7", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 1, 3}, false), new BarcodeDigit("8", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {3, 1, 1, 2}, false), new BarcodeDigit("9", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 2, 3}, false), new BarcodeDigit("0", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 2, 2}, false), new BarcodeDigit("1", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 2, 1, 2}, false), new BarcodeDigit("2", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 4, 1}, false), new BarcodeDigit("3", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 3, 1, 1}, false), new BarcodeDigit("4", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 3, 2, 1}, false), new BarcodeDigit("5", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {4, 1, 1, 1}, false), new BarcodeDigit("6", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 1, 3, 1}, false), new BarcodeDigit("7", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {3, 1, 2, 1}, false), new BarcodeDigit("8", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 1, 1, 3}, false), new BarcodeDigit("9", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {3, 2, 1, 1}, true), new BarcodeDigit("0", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 2, 2, 1}, true), new BarcodeDigit("1", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 1, 2, 2}, true), new BarcodeDigit("2", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 4, 1, 1}, true), new BarcodeDigit("3", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 3, 2}, true), new BarcodeDigit("4", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 3, 1}, true), new BarcodeDigit("5", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 1, 4}, true), new BarcodeDigit("6", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 3, 1, 2}, true), new BarcodeDigit("7", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 1, 3}, true), new BarcodeDigit("8", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {3, 1, 1, 2}, true), new BarcodeDigit("9", "RIGHT")) ;

    }

    public static final BarcodeSpec ISBN = new BarcodeSpec(TYPE_ISBN, digisbn, NBARS_ISBN,
            NBARS_DIGIT_ISBN, GUARD_101, GUARD_101, GUARD_01010);
}
