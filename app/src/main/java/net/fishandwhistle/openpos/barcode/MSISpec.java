package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.any;
import static net.fishandwhistle.openpos.barcode.ArrayMath.div;
import static net.fishandwhistle.openpos.barcode.ArrayMath.gt;
import static net.fishandwhistle.openpos.barcode.ArrayMath.mean;
import static net.fishandwhistle.openpos.barcode.ArrayMath.range;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;

/**
 * Created by dewey on 2016-10-11.
 */

public class MSISpec extends DualWidthSpec {

    public MSISpec() {
        this(5, false);
    }

    public MSISpec(int minLength, boolean fixedLength) {
        super("MSI", digMSI, minLength, fixedLength);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = new Barcode(this.getType());
        if(bars.length < (2 + minLength*8 + 3)) throw new BarcodeException("Not enough bars to decode", b);


        int startIndex = -1;
        int endIndex = -1;
        int starti = 0;

        while(true) {
            if(startIndex == -1) {
                //look for start pattern (BWB)
                if((starti+2) > bars.length) break;
                int[] start = subset(bars, starti, 2);
                if(start[0] >= (start[1]*2)) { // ideally is 3
                    startIndex = starti;
                }
                starti += 2;
            } else {
                //try to decode end pattern (10001)
                if((starti+3) > bars.length) break;
                int[] endp = subset(bars, starti, 3);
                double r1 = endp[1] / (double)endp[0];
                double r2 = endp[1] / (double)endp[2];
                boolean end = (r1 > 2) && (r2 > 2);

                BarcodeDigit d1;
                if((starti+7) < bars.length) {
                    d1 = this.getDigit(subset(bars, starti, 8), true);
                } else {
                    d1 = null;
                }
                if(end && (d1 == null)) {
                    endIndex = starti;
                    break;
                } else {
                    b.digits.add(d1);
                }
                starti += 8;
            }
        }
        // try to decode end digit
        if(startIndex == -1) throw new BarcodeException("No start character encountered", b);
        if(endIndex == -1) throw new BarcodeException("No end character encountered", b);
        this.checkLength(b);
        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);
        b.isValid = true;
        return b;
    }

    @Override
    protected BarcodePattern getBarcodePattern(int[] bars, boolean start) throws BarWidthException {
        int[] widthRange = range(bars);
        if((widthRange[1]/widthRange[0]) > 10) throw new BarWidthException("Too great a range in bar widths", bars);
        int[] widths = new int[4];
        for(int i=0; i<4; i++) {
            int ratio = (bars[i*2]/bars[i*2+1]);
            if(ratio > 5) throw new BarWidthException("Invalid bar widths", widths);
            if(ratio >= 2) {
                widths[i] = 2;
            } else {
                widths[i] = 1;
            }
        }
        return new BarcodePattern(widths, start);
    }

    private static Map<BarcodePattern, BarcodeDigit> digMSI = new HashMap<>();
    static {
        digMSI.put(new BarcodePattern(new int[] {1, 1, 1, 1}, true), new BarcodeDigit("0"));
        digMSI.put(new BarcodePattern(new int[] {1, 1, 1, 2}, true), new BarcodeDigit("1"));
        digMSI.put(new BarcodePattern(new int[] {1, 1, 2, 1}, true), new BarcodeDigit("2"));
        digMSI.put(new BarcodePattern(new int[] {1, 1, 2, 2}, true), new BarcodeDigit("3"));
        digMSI.put(new BarcodePattern(new int[] {1, 2, 1, 1}, true), new BarcodeDigit("4"));
        digMSI.put(new BarcodePattern(new int[] {1, 2, 1, 2}, true), new BarcodeDigit("5"));
        digMSI.put(new BarcodePattern(new int[] {1, 2, 2, 1}, true), new BarcodeDigit("6"));
        digMSI.put(new BarcodePattern(new int[] {1, 2, 2, 2}, true), new BarcodeDigit("7"));
        digMSI.put(new BarcodePattern(new int[] {2, 1, 1, 1}, true), new BarcodeDigit("8"));
        digMSI.put(new BarcodePattern(new int[] {2, 1, 1, 2}, true), new BarcodeDigit("9"));
    }
}
