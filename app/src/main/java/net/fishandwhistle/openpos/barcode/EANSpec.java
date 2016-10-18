package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.any;
import static net.fishandwhistle.openpos.barcode.ArrayMath.concatenate;
import static net.fishandwhistle.openpos.barcode.ArrayMath.div;
import static net.fishandwhistle.openpos.barcode.ArrayMath.gt;
import static net.fishandwhistle.openpos.barcode.ArrayMath.mean;
import static net.fishandwhistle.openpos.barcode.ArrayMath.round;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;
import static net.fishandwhistle.openpos.barcode.ArrayMath.sum;

/**
 * Created by dewey on 2016-10-02.
 */

public abstract class EANSpec extends BarcodeSpec {

    private int nbars;
    private int nbarsSide;
    private int begGuardLength ;
    private int endGuardLength ;
    private int middleGuardLength ;
    private boolean partial;

    public EANSpec(String type, Map<BarcodePattern, BarcodeDigit> digits,
                   int begGuardLength, int middleGuardLength, int endGuardLength,
                   int nbars) {
        super(type, digits);
        this.begGuardLength = begGuardLength;
        this.endGuardLength = endGuardLength;
        this.middleGuardLength = middleGuardLength;
        this.nbars = nbars;
        this.nbarsSide = (nbars - begGuardLength - endGuardLength - middleGuardLength) / 2;
        partial = false; //used for decoding
    }

    protected BarcodePattern getBarcodePattern(int[] bars, boolean start) {
        return getBarcodePattern(bars, start, 7.0);
    }

    protected BarcodePattern getBarcodePattern(int[] bars, boolean start, double totalLength) {
        return new BarcodePattern(round(div(bars, sum(bars)/totalLength)), start);
    }

    protected Barcode parse_common(int[] bars) throws BarcodeException {
        // must start on a 1(true) bar 0, 2, 4, 6, etc., so i%2==0 is the value at i
        boolean[] vals = new boolean[bars.length];
        for(int i=0; i<vals.length; i++) {
            vals[i] = (i%2) == 0 ;
        }
        Barcode b = new Barcode(this.getType());
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
            BarcodeDigit d = this.getDigit(subset(decodable, i, 4), vdecodable[i]);
            if(d == null && !partial) throw new BarcodeException("Undecodable digit found", b);
            b.digits.add(d);
        }

        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);
        addSupplement(bars, b);

        return b;
    }

    protected void addSupplement(int[] bars, Barcode b) {
        //try extra barcode addition
        if(bars.length >= (this.nbars + 1 + 13)) {
            EANExtraSpec extraSpec = new EANExtraSpec();
            //allow for 1 interruption
            for(int i=0; i<5; i+=2) {
                try {
                    Barcode extra = extraSpec.parse(subset(bars, this.nbars + 1 + i, (bars.length - nbars - 1 - i)));
                    b.extra = extra.toString();
                    return;
                } catch (BarcodeException e) {
                    //do nothing
                }
            }
        }
    }

    @Override
    public void initialize() {
        digean.containsValue(new BarcodeDigit("0"));
        dig1ean.containsValue(new BarcodeDigit("1"));
    }

    protected static Map<BarcodePattern, BarcodeDigit> digean = new HashMap<>();
    protected static Map<String, BarcodeDigit> dig1ean = new HashMap<>();
    static {
        digean.put(new BarcodePattern(new int[] {3, 2, 1, 1}, false), new BarcodeDigit("0", "A")) ;
        digean.put(new BarcodePattern(new int[] {2, 2, 2, 1}, false), new BarcodeDigit("1", "A")) ;
        digean.put(new BarcodePattern(new int[] {2, 1, 2, 2}, false), new BarcodeDigit("2", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 4, 1, 1}, false), new BarcodeDigit("3", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 3, 2}, false), new BarcodeDigit("4", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 3, 1}, false), new BarcodeDigit("5", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 1, 4}, false), new BarcodeDigit("6", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 3, 1, 2}, false), new BarcodeDigit("7", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 1, 3}, false), new BarcodeDigit("8", "A")) ;
        digean.put(new BarcodePattern(new int[] {3, 1, 1, 2}, false), new BarcodeDigit("9", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 2, 3}, false), new BarcodeDigit("0", "B")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 2, 2}, false), new BarcodeDigit("1", "B")) ;
        digean.put(new BarcodePattern(new int[] {2, 2, 1, 2}, false), new BarcodeDigit("2", "B")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 4, 1}, false), new BarcodeDigit("3", "B")) ;
        digean.put(new BarcodePattern(new int[] {2, 3, 1, 1}, false), new BarcodeDigit("4", "B")) ;
        digean.put(new BarcodePattern(new int[] {1, 3, 2, 1}, false), new BarcodeDigit("5", "B")) ;
        digean.put(new BarcodePattern(new int[] {4, 1, 1, 1}, false), new BarcodeDigit("6", "B")) ;
        digean.put(new BarcodePattern(new int[] {2, 1, 3, 1}, false), new BarcodeDigit("7", "B")) ;
        digean.put(new BarcodePattern(new int[] {3, 1, 2, 1}, false), new BarcodeDigit("8", "B")) ;
        digean.put(new BarcodePattern(new int[] {2, 1, 1, 3}, false), new BarcodeDigit("9", "B")) ;
        digean.put(new BarcodePattern(new int[] {3, 2, 1, 1}, true), new BarcodeDigit("0", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {2, 2, 2, 1}, true), new BarcodeDigit("1", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {2, 1, 2, 2}, true), new BarcodeDigit("2", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 4, 1, 1}, true), new BarcodeDigit("3", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 3, 2}, true), new BarcodeDigit("4", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 3, 1}, true), new BarcodeDigit("5", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 1, 4}, true), new BarcodeDigit("6", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 3, 1, 2}, true), new BarcodeDigit("7", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 1, 3}, true), new BarcodeDigit("8", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {3, 1, 1, 2}, true), new BarcodeDigit("9", "RIGHT")) ;

        dig1ean.put("AAAAAA", new BarcodeDigit("0", "FIRSTDIGIT")) ;
        dig1ean.put("AABABB", new BarcodeDigit("1", "FIRSTDIGIT")) ;
        dig1ean.put("AABBAB", new BarcodeDigit("2", "FIRSTDIGIT")) ;
        dig1ean.put("AABBBA", new BarcodeDigit("3", "FIRSTDIGIT")) ;
        dig1ean.put("ABAABB", new BarcodeDigit("4", "FIRSTDIGIT")) ;
        dig1ean.put("ABBAAB", new BarcodeDigit("5", "FIRSTDIGIT")) ;
        dig1ean.put("ABBBAA", new BarcodeDigit("6", "FIRSTDIGIT")) ;
        dig1ean.put("ABABAB", new BarcodeDigit("7", "FIRSTDIGIT")) ;
        dig1ean.put("ABABBA", new BarcodeDigit("8", "FIRSTDIGIT")) ;
        dig1ean.put("ABBABA", new BarcodeDigit("9", "FIRSTDIGIT")) ;
    }

}
