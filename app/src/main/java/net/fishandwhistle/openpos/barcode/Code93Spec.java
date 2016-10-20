package net.fishandwhistle.openpos.barcode;

import java.util.Map;

/**
 * Created by dewey on 2016-10-20.
 */

public class Code93Spec extends DualWidthSpec {
    public Code93Spec() {
        super("Code93", null, 5, false);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        throw new BarcodeException("parse not implemented", new Barcode(this.getType()));
    }

    @Override
    public void initialize() {

    }
}
