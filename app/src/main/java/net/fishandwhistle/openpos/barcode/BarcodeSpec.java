package net.fishandwhistle.openpos.barcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.all;
import static net.fishandwhistle.openpos.barcode.ArrayMath.div;
import static net.fishandwhistle.openpos.barcode.ArrayMath.eq;
import static net.fishandwhistle.openpos.barcode.ArrayMath.round;
import static net.fishandwhistle.openpos.barcode.ArrayMath.sum;

/**
 * Created by dewey on 2016-09-29.
 */

public abstract class BarcodeSpec {

    private Map<BarcodePattern, BarcodeDigit> digits ;
    private int nstripesDigit ;
    private String type;

    public BarcodeSpec(String type, Map<BarcodePattern, BarcodeDigit> digits, int nstripesDigit) {
        this.type = type;
        this.digits = digits;
        this.nstripesDigit = nstripesDigit;
    }

    public int getNStripesPerDigit() {
        return this.nstripesDigit;
    }

    public String getType() {
        return this.type;
    }

    public abstract Barcode parse(int[] bars) throws BarcodeException;

    protected BarcodeDigit getDigit(int[] bars, boolean start) {
        BarcodePattern pattern = new BarcodePattern(round(div(bars, sum(bars)/(double)this.getNStripesPerDigit())), start);

        for(Map.Entry<BarcodePattern, BarcodeDigit> entry : digits.entrySet()) {
            BarcodePattern p = entry.getKey();
            if(entry.getKey().equals(pattern)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static class Barcode {
        public String type;
        public List<BarcodeDigit> digits;
        public int validDigits;
        public String tag;
        public boolean isValid;
        public long timeread ;

        public Barcode(String type) {
            this.type = type;
            digits = new ArrayList<>();
            validDigits = 0;
            tag = null;
            isValid = false;
            timeread = 0;
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

        public boolean equals(Object o) {
            if(o instanceof Barcode) {
                Barcode b = (Barcode)o;
                return b.toString().equals(this.toString()) && this.type.equals(b.type) ;
            } else {
                return false;
            }
        }

        public boolean isComplete() {
            return (validDigits == digits.size()) && validDigits != 0;
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

}