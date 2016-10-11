package net.fishandwhistle.openpos.barcode;

/**
 * Created by dewey on 2016-10-11.
 */

public class ITF14Spec extends ITFSpec {

    public ITF14Spec() {
        super("ITF-14", 14, true);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = super.parse(bars);
        if(!Checksums.checksum(b, 1, 3)) throw new BarcodeException("Checksum failed", b);
        return b;
    }
}
