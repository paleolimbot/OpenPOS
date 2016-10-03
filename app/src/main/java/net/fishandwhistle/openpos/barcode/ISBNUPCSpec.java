package net.fishandwhistle.openpos.barcode;

import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.any;
import static net.fishandwhistle.openpos.barcode.ArrayMath.concatenate;
import static net.fishandwhistle.openpos.barcode.ArrayMath.div;
import static net.fishandwhistle.openpos.barcode.ArrayMath.gt;
import static net.fishandwhistle.openpos.barcode.ArrayMath.mean;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;

/**
 * Created by dewey on 2016-10-02.
 */

public abstract class ISBNUPCSpec extends BarcodeSpec {

    private int nbars;
    private int nbarsSide;

    public ISBNUPCSpec(String type, Map<BarcodePattern, BarcodeDigit> digits) {
        super(type, digits, 7);
        this.nbars = 59;
        this.nbarsSide = (nbars - 3 - 3 - 5) / 2;
    }


    protected Barcode parse_common(int[] bars) throws BarcodeException {
        // must start on a 1(true) bar 0, 2, 4, 6, etc., so i%2==0 is the value at i
        boolean[] vals = new boolean[bars.length];
        for(int i=0; i<vals.length; i++) {
            vals[i] = (i%2) == 0 ;
        }
        Barcode b = new Barcode(this.getType());
        b.timeread = System.currentTimeMillis();
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
        for(int i=0; i<decodable.length; i+= 4) {
            b.digits.add(this.getDigit(subset(decodable, i, 4), vdecodable[i]));
        }

        return b;
    }



}
