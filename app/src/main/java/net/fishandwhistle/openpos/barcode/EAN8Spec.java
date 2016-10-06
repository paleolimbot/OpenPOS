package net.fishandwhistle.openpos.barcode;

/**
 * Created by dewey on 2016-10-05.
 */

public class EAN8Spec extends EANSpec {

    public EAN8Spec() {
        super("EAN/8", 3, 5, 3, 43);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = this.parse_common(bars);

        int validDigits = 0;
        for(int i=0; i<b.digits.size(); i++) {
            if(b.digits.get(i) != null)
                validDigits++;
        }
        b.validDigits = validDigits;

        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        //try checksum
        if(!this.checksum(b, 1, 3)) throw new BarcodeException("Checksum failed for barcode", b);
        b.isValid = true;

        return b;
    }
}
