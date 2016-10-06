package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
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

public abstract class EANUPCSpec extends BarcodeSpec {

    private int nbars;
    private int nbarsSide;
    private int begGuardLength ;
    private int endGuardLength ;
    private int middleGuardLength ;

    public EANUPCSpec(String type, Map<BarcodePattern, BarcodeDigit> digits,
                      int begGuardLength, int middleGuardLength, int endGuardLength,
                      int nbars) {
        super(type, digits, 7);
        this.begGuardLength = begGuardLength;
        this.endGuardLength = endGuardLength;
        this.middleGuardLength = middleGuardLength;
        this.nbars = nbars;
        this.nbarsSide = (nbars - begGuardLength - endGuardLength - middleGuardLength) / 2;
    }


    protected boolean checksum(Barcode b, int evenweight, int oddweight) {
        int[] numbers = new int[b.digits.size()];
        for(int i=0; i<numbers.length; i++) {
            numbers[i] = Integer.valueOf(b.digits.get(i).digit);
        }
        int oddsum = 0;
        for(int i=0; i<numbers.length-1; i+=2) {
            oddsum += numbers[i];
        }
        int evensum = 0;
        for(int i=1; i<numbers.length-1; i+=2) {
            evensum += numbers[i];
        }
        int s1 = evensum * evenweight + oddsum * oddweight;
        int checksum = 10*(s1/10+1) - s1;
        if(checksum == 10) checksum = 0;
        return checksum == numbers[numbers.length-1];
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
        int[] leftguard = subset(bars, 0, begGuardLength);
        double barsize = mean(leftguard);
        if(any(gt(div(leftguard, barsize), 3.0))) throw new BarcodeException("Left guard has irregular barsize", b);
        // assign left side
        int[] leftside = subset(bars, begGuardLength, nbarsSide);
        boolean[] vleftside = subset(vals, begGuardLength, nbarsSide);
        // test middle guard
        int[] middleguard = subset(bars, nbarsSide+begGuardLength, middleGuardLength);
        barsize = (barsize * begGuardLength + mean(middleguard) * middleGuardLength) / (begGuardLength+middleGuardLength);
        if(any(gt(div(middleguard, barsize), 3.0))) throw new BarcodeException("Middle guard has irregular barsize", b);
        int[] rightside = subset(bars, nbarsSide+begGuardLength+middleGuardLength, nbarsSide);
        boolean[] vrightside = subset(vals, nbarsSide+begGuardLength+middleGuardLength, nbarsSide);
        // test end guard
        int[] endguard = subset(bars, nbarsSide+begGuardLength+middleGuardLength+nbarsSide, endGuardLength);
        barsize = (barsize * (begGuardLength+middleGuardLength) + mean(endguard) * endGuardLength) / (begGuardLength+middleGuardLength+endGuardLength);
        if(any(gt(div(endguard, barsize), 3.0))) throw new BarcodeException("End guard has irregular barsize", b);

        int[] decodable = concatenate(leftside, rightside);
        boolean[] vdecodable = concatenate(vleftside, vrightside);
        for(int i=0; i<decodable.length; i+= 4) {
            b.digits.add(this.getDigit(subset(decodable, i, 4), vdecodable[i]));
        }

        return b;
    }

}
