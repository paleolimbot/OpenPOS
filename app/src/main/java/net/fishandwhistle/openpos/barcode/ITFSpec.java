package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.any;
import static net.fishandwhistle.openpos.barcode.ArrayMath.div;
import static net.fishandwhistle.openpos.barcode.ArrayMath.gt;
import static net.fishandwhistle.openpos.barcode.ArrayMath.mean;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;

/**
 * Created by dewey on 2016-10-11.
 */

public class ITFSpec extends DualWidthSpec {

    public ITFSpec() {
        this(5, false);
    }

    public ITFSpec(int minLength, boolean fixedLength) {
        super("ITF", digITF, minLength, fixedLength);
    }

    protected ITFSpec(String type, int minLength, boolean fixedLength) {
        super(type, digITF, minLength, fixedLength);
    }


    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = new Barcode(this.getType());
        //check if there are enough bars for minLength
        if(bars.length < (4 + minLength/2*10 + 3)) throw new BarcodeException("Not enough bars to cover minimum length", b);

        int startIndex = -1;
        int endIndex = -1;
        int starti = 0;

        while(true) {
            if(startIndex == -1) {
                //look for start pattern (BWB)
                if((starti+3) > bars.length) break;
                int[] start = subset(bars, starti, 3);
                double barsize = mean(start);
                if(!any(gt(div(start, barsize), 3.0))) {
                    startIndex = starti;
                }
                starti += 4;
            } else {
                //try to decode digit
                if((starti+3) > bars.length) break;
                boolean end = false;
                try {
                    BarcodePattern p = this.getBarcodePattern(subset(bars, starti, 3), true);
                    if (STOP.equals(p)) {
                        end = true;
                    }
                } catch(BarWidthException e) {
                    //nothing to do here
                }
                BarcodeDigit d1;
                BarcodeDigit d2;
                if((starti+9) < bars.length) {
                    int[] local = subset(bars, starti, 10);
                    d1 = this.getDigit(new int[] {local[0], local[2], local[4], local[6], local[8]}, true);
                    d2 = this.getDigit(new int[] {local[1], local[3], local[5], local[7], local[9]}, true);
                } else {
                    d1 = null;
                    d2 = null;
                }
                if(!partial && !end && (d1 == null) && (d2 == null)) {
                    throw new BarcodeException("Undecodable digit found", b);
                } else if(end && (d1 == null) && (d2 == null)) {
                    endIndex = starti;
                    break;
                } else {
                    b.digits.add(d1);
                    b.digits.add(d2);
                }
                starti += 10;
            }
        }
        // try to decode end digit
        if(startIndex == -1) throw new BarcodeException("No start character encountered", b);
        if(endIndex == -1) throw new BarcodeException("No end character encountered", b);
        this.checkLength(b);
        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        if(b.digits.size() == 14 && Checksums.checksum(b, 1, 3)) {
            b.type = "ITF-14";
        }

        b.isValid = true;
        return b;
    }

    @Override
    public void initialize() {
        digITF.containsValue(new BarcodeDigit("0"));
    }

    private static Map<BarcodePattern, BarcodeDigit> digITF = new HashMap<>();
    static {
        digITF.put(new BarcodePattern(new int[] {1, 1, 2, 2, 1}, true), new BarcodeDigit("0"));
        digITF.put(new BarcodePattern(new int[] {2, 1, 1, 1, 2}, true), new BarcodeDigit("1"));
        //2 is encoded differently in ITF than in Code25
        digITF.put(new BarcodePattern(new int[] {1, 2, 1, 1, 2}, true), new BarcodeDigit("2"));
        digITF.put(new BarcodePattern(new int[] {2, 2, 1, 1, 1}, true), new BarcodeDigit("3"));
        digITF.put(new BarcodePattern(new int[] {1, 1, 2, 1, 2}, true), new BarcodeDigit("4"));
        digITF.put(new BarcodePattern(new int[] {2, 1, 2, 1, 1}, true), new BarcodeDigit("5"));
        digITF.put(new BarcodePattern(new int[] {1, 2, 2, 1, 1}, true), new BarcodeDigit("6"));
        digITF.put(new BarcodePattern(new int[] {1, 1, 1, 2, 2}, true), new BarcodeDigit("7"));
        digITF.put(new BarcodePattern(new int[] {2, 1, 1, 2, 1}, true), new BarcodeDigit("8"));
        digITF.put(new BarcodePattern(new int[] {1, 2, 1, 2, 1}, true), new BarcodeDigit("9"));
    }

    private static final BarcodePattern STOP = new BarcodePattern(new int[] {2, 1, 1}, true);

}
