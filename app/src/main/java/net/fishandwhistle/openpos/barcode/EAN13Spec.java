package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dewey on 2016-10-02.
 */

public class EAN13Spec extends EANSpec {

    public EAN13Spec() {
        super("EAN-13", digean, 3, 5, 3, 59);
    }

    public EAN13Spec(String type, int begGuardLength, int middleGuardLength, int endGuardLength, int nbars) {
        super(type, digean, begGuardLength, middleGuardLength, endGuardLength, nbars);
    }

    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = this.parse_common(bars);

        //decode first digit
        String firstSchemes = "";
        for(int i=0; i<6; i++) {
            BarcodeDigit d = b.digits.get(i);
            if(d==null) {
                break;
            } else {
                firstSchemes += d.tag;
            }
        }
        if(dig1ean.containsKey(firstSchemes)) {
            b.digits.add(0, dig1ean.get(firstSchemes));
        } else {
            b.digits.add(0, null);
        }

        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        //try checksum
        if(!Checksums.checksum(b, 3, 1)) throw new BarcodeException("Checksum failed for barcode", b);

        b.isValid = true;

        return b;
    }

}
