package net.fishandwhistle.openpos.barcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.div;
import static net.fishandwhistle.openpos.barcode.ArrayMath.round;
import static net.fishandwhistle.openpos.barcode.ArrayMath.sum;

/**
 * Created by dewey on 2016-09-29.
 */

public abstract class BarcodeSpec {

    private Map<BarcodePattern, BarcodeDigit> digits ;
    private String type;

    public BarcodeSpec(String type, Map<BarcodePattern, BarcodeDigit> digits) {
        this.type = type;
        this.digits = digits;
    }

    public String getType() {
        return this.type;
    }

    public abstract Barcode parse(int[] bars) throws BarcodeException;

    protected abstract BarcodePattern getBarcodePattern(int[] bars, boolean start) throws BarWidthException ;

    protected BarcodeDigit getDigit(int[] bars, boolean start) {
        try {
            BarcodePattern pattern = this.getBarcodePattern(bars, start);
            if (digits.containsKey(pattern)) {
                return digits.get(pattern);
            } else {
                return null;
            }
        } catch(BarWidthException e) {
            return null;
        }
    }

    public static class Barcode {
        public String type;
        public List<BarcodeDigit> digits;
        public String tag;
        public boolean isValid;
        public long timeread ;

        public Barcode(String type) {
            this.type = type;
            digits = new ArrayList<>();
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

        public int getValidDigits() {
            int validDigits = 0;
            for(int i=0; i<this.digits.size(); i++) {
                if(this.digits.get(i) != null)
                    validDigits++;
            }
            return validDigits;
        }

        public boolean isComplete() {
            int validDigits = this.getValidDigits();
            return (validDigits == digits.size()) && validDigits != 0;
        }
    }

    public static class BarcodePattern {
        public int[] widths;
        public boolean startsWith;
        private int intValue ;

        public BarcodePattern(int[] widths, boolean startsWith) {
            this.widths = widths;
            this.startsWith = startsWith;

            String outbin = "";
            boolean value = startsWith;
            for (int width : widths) {
                String val;
                if (value) {
                    val = "1";
                } else {
                    val = "0";
                }
                for (int j = 0; j < width; j++) {
                    outbin += val;
                }
                value = !value;
            }
            intValue = Long.valueOf(outbin, 2).intValue();
        }

        @Override
        public int hashCode() {
            return this.intValue;
        }

        public boolean equals(Object o) {
            return this.hashCode() == o.hashCode();
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

    public static class BarWidthException extends Exception {
        public int[] bars;
        public BarWidthException(String message, int[] bars) {
            super(message);
            this.bars = bars;
        }
    }

}
