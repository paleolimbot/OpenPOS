package net.fishandwhistle.openpos.barcode;

import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.range;

/**
 * Created by dewey on 2016-10-11.
 */

public abstract class DualWidthSpec extends BarcodeSpec {

    protected int minLength;
    protected boolean fixedLength;
    protected boolean partial;

    public DualWidthSpec(String type, Map<BarcodePattern, BarcodeDigit> digits, int minLength, boolean fixedLength) {
        super(type, digits);
        this.minLength = minLength;
        this.fixedLength = fixedLength;
        this.partial = false;
    }

    protected void checkLength(Barcode b) throws BarcodeException {
        if(fixedLength && b.digits.size() != minLength) throw new BarcodeException("Wrong number of decoded digits", b);
        if(b.digits.size() < minLength) throw new BarcodeException("Too few decoded digits", b);
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

}
