package net.fishandwhistle.openpos.barcode;

import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.range;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;

/**
 * Created by dewey on 2016-10-11.
 */

public class PharmacodeSpec extends BarcodeSpec {

    public PharmacodeSpec() {
        super("Pharmacode", null);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = new Barcode(this.getType());

        //try to find maximum width that no error occurs
        for(int width=16; width>=3; width--) {
            int nbars = width*2-1;
            if(bars.length > nbars) {
                int[] sub = subset(bars, 0, nbars);
                try {
                    BarcodePattern p = this.getBarcodePattern(sub, true);
                    long value = 0;
                    for(int i=0; i<(p.widths.length/2+1); i++) {
                        if(p.widths[p.widths.length-i*2-1] == 1) {
                            value += pow2(i);
                        } else {
                            value += pow2(i+1);
                        }
                    }
                    String base10 = String.valueOf(value);
                    for(int i=0; i<base10.length(); i++) {
                        b.digits.add(new BarcodeDigit(base10.substring(i, i+1)));
                    }
                    b.isValid = true;
                    return b;
                } catch(BarWidthException e) {
                    //don't do anything
                }
            }
        }
        throw new BarcodeException("No valid bars found", b);
    }

    @Override
    protected BarcodePattern getBarcodePattern(int[] bars, boolean start) throws BarWidthException {
        int[] widthrange = range(bars);
        // more stringent width verifying here because width is really the only veryfying thing there is
        if(widthrange[1] / widthrange[0] > 5) throw new BarWidthException("Width range too great", bars);
        if(widthrange[0] < 10) throw new BarWidthException("Smallest bar is too narrow", bars);
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

    private static long pow2(int n) {
        long result = 1;
        for(int i=0; i<n; i++) {
            result *= 2;
        }
        return result;
    }
}
