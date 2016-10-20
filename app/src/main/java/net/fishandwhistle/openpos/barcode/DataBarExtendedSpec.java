package net.fishandwhistle.openpos.barcode;

/**
 * Created by dewey on 2016-10-20.
 */

public class DataBarExtendedSpec extends BarcodeSpec {

    public DataBarExtendedSpec() {
        super("DataBarExt", null);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        throw new BarcodeException("parse not implemented", new Barcode(this.getType()));
    }

    @Override
    protected BarcodePattern getBarcodePattern(int[] bars, boolean start) throws BarWidthException {
        return null;
    }

    @Override
    public void initialize() {

    }

}
