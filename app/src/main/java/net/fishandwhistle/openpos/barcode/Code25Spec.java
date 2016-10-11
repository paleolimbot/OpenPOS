package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.range;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;

/**
 * Created by dewey on 2016-10-10.
 */

public class Code25Spec extends BarcodeSpec {

    protected int minLength;
    protected boolean fixedLength;

    public Code25Spec() {
        this(5, false);
    }

    public Code25Spec(int minLength, boolean fixedLength) {
        this("Code25", digc25, minLength, fixedLength);
    }

    protected Code25Spec(String type, Map<BarcodePattern, BarcodeDigit> digits, int minLength, boolean fixedLength) {
        super(type, digits);
        this.minLength = minLength;
        this.fixedLength = fixedLength;
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = new Barcode(this.getType());
        //check if there are enough bars for minLength
        if(bars.length < (6 + minLength*10 + 5)) throw new BarcodeException("Not enough bars to cover minimum length", b);

        int[] blackbars ;
        if((bars.length % 2) == 0) {
            blackbars = new int[bars.length/2];
        } else {
            blackbars = new int[bars.length/2+1];
        }
        for(int i=0; i<blackbars.length; i++) {
            blackbars[i] = bars[i*2] ;
        }

        int startIndex = -1;
        int endIndex = -1;
        int starti = 0;

        while(true) {
            if(startIndex == -1) {
                //look for start pattern
                if((starti+3) > blackbars.length) break;
                try {
                    BarcodePattern p = this.getBarcodePattern(subset(blackbars, starti, 3), true);
                    if (START.equals(p)) {
                        startIndex = starti;
                    }
                } catch(BarWidthException e) {
                    //nothing to do here
                }
                starti += 3;
            } else {
                //try to decode digit
                if((starti+3) > blackbars.length) break;
                boolean end = false;
                try {
                    BarcodePattern p = this.getBarcodePattern(subset(blackbars, starti, 3), true);
                    if (STOP.equals(p)) {
                        end = true;
                    }
                } catch(BarWidthException e) {
                    //nothing to do here
                }
                BarcodeDigit d2;
                if((starti+4) < blackbars.length) {
                    d2 = this.getDigit(subset(blackbars, starti, 5), true);
                } else {
                    d2 = null;
                }
                if(end && ((d2 == null) || (!d2.digit.equals("5")))) {
                    endIndex = starti;
                    break;
                } else {
                    b.digits.add(d2);
                }
                starti += 5;
            }
        }
        // try to decode end digit
        if(startIndex == -1) throw new BarcodeException("No start character encountered", b);
        if(endIndex == -1) throw new BarcodeException("No end character encountered", b);
        if(fixedLength && b.digits.size() != minLength) throw new BarcodeException("Wrong number of decoded digits", b);
        if(b.digits.size() < minLength) throw new BarcodeException("Too few decoded digits", b);
        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);
        b.isValid = true;

        return b;
    }

    @Override
    protected BarcodePattern getBarcodePattern(int[] bars, boolean start) throws BarWidthException {
        int[] widthrange = range(bars);
        if(widthrange[1] / widthrange[0] > 10) throw new BarWidthException("Width range too great", bars);
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


    private static Map<BarcodePattern, BarcodeDigit> digc25 = new HashMap<>();
    static {
        digc25.put(new BarcodePattern(new int[] {1, 1, 2, 2, 1}, true), new BarcodeDigit("0"));
        digc25.put(new BarcodePattern(new int[] {2, 1, 1, 1, 2}, true), new BarcodeDigit("1"));
        //2 is encoded differently in ITF than in Code25
        digc25.put(new BarcodePattern(new int[] {1, 2, 1, 1, 1}, true), new BarcodeDigit("2"));
        digc25.put(new BarcodePattern(new int[] {2, 2, 1, 1, 1}, true), new BarcodeDigit("3"));
        digc25.put(new BarcodePattern(new int[] {1, 1, 2, 1, 2}, true), new BarcodeDigit("4"));
        digc25.put(new BarcodePattern(new int[] {2, 1, 2, 1, 1}, true), new BarcodeDigit("5"));
        digc25.put(new BarcodePattern(new int[] {1, 2, 2, 1, 1}, true), new BarcodeDigit("6"));
        digc25.put(new BarcodePattern(new int[] {1, 1, 1, 2, 2}, true), new BarcodeDigit("7"));
        digc25.put(new BarcodePattern(new int[] {2, 1, 1, 2, 1}, true), new BarcodeDigit("8"));
        digc25.put(new BarcodePattern(new int[] {1, 2, 1, 2, 1}, true), new BarcodeDigit("9"));
    }
    private static final BarcodePattern START = new BarcodePattern(new int[] {2, 2, 1}, true);
    private static final BarcodePattern STOP = new BarcodePattern(new int[] {2, 1, 2}, true);

}
