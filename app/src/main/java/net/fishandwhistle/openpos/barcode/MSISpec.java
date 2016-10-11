package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dewey on 2016-10-11.
 */

public class MSISpec extends DualWidthSpec {

    public MSISpec() {
        this(5, false);
    }

    public MSISpec(int minLength, boolean fixedLength) {
        super("MSI", digMSI, minLength, fixedLength);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        return null;
    }

    private static Map<BarcodePattern, BarcodeDigit> digMSI = new HashMap<>();
    static {
        for(int i=0; i<=9; i++) {
            String bin = Integer.toBinaryString(i);
            int[] widths = new int[4];
            int off = 4 - bin.length();
            for(int j=0; j<widths.length; j++) {
                if(j < (widths.length - off)) {
                    widths[j] = 1;
                } else {
                    widths[j] = Integer.valueOf(bin.substring(j+off, j+off+1)) + 1;
                }
            }
            digMSI.put(new BarcodePattern(widths, true), new BarcodeDigit(String.valueOf(i)));
        }
    }
}
