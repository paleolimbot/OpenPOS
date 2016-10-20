package net.fishandwhistle.openpos.barcode;

/**
 * Created by dewey on 2016-10-20.
 */

public class DataBarSpec extends BarcodeSpec {

    public DataBarSpec() {
        super("DataBar", null);
    }

    @Override
    public BarcodeSpec.Barcode parse(int[] bars) throws BarcodeSpec.BarcodeException {
        throw new BarcodeSpec.BarcodeException("parse not implemented", new BarcodeSpec.Barcode(this.getType()));
    }

    @Override
    protected BarcodePattern getBarcodePattern(int[] bars, boolean start) throws BarWidthException {
        return null;
    }

    @Override
    public void initialize() {

    }

}
