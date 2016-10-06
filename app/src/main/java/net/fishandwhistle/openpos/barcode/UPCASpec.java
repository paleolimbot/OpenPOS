package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dewey on 2016-10-02.
 */

public class UPCASpec extends EANUPCSpec {

    public UPCASpec() {
        super("UPC/A", digupc, 3, 5, 3, 59);
    }

    protected UPCASpec(String type, Map<BarcodePattern, BarcodeDigit> digitMap, int begGuardLength, int middleGuardLength, int endGuardLength, int nbars) {
        super(type, digitMap, begGuardLength, middleGuardLength, endGuardLength, nbars);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = parse_common(bars) ;

        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        //try checksum
        if(!this.checksum(b, 1, 3)) throw new BarcodeException("Checksum failed for barcode", b);
        //checksum isn't part of the code, remove it
        b.digits.remove(b.digits.size()-1);
        b.isValid = true;

        return b;
    }

    protected static Map<BarcodePattern, BarcodeDigit> digupc = new HashMap<>();
    static {
        digupc.put(new BarcodePattern(new int[]{3, 2, 1, 1}, false), new BarcodeDigit("0", "O"));
        digupc.put(new BarcodePattern(new int[]{2, 2, 2, 1}, false), new BarcodeDigit("1", "O"));
        digupc.put(new BarcodePattern(new int[]{2, 1, 2, 2}, false), new BarcodeDigit("2", "O"));
        digupc.put(new BarcodePattern(new int[]{1, 4, 1, 1}, false), new BarcodeDigit("3", "O"));
        digupc.put(new BarcodePattern(new int[]{1, 1, 3, 2}, false), new BarcodeDigit("4", "O"));
        digupc.put(new BarcodePattern(new int[]{1, 2, 3, 1}, false), new BarcodeDigit("5", "O"));
        digupc.put(new BarcodePattern(new int[]{1, 1, 1, 4}, false), new BarcodeDigit("6", "O"));
        digupc.put(new BarcodePattern(new int[]{1, 3, 1, 2}, false), new BarcodeDigit("7", "O"));
        digupc.put(new BarcodePattern(new int[]{1, 2, 1, 3}, false), new BarcodeDigit("8", "O"));
        digupc.put(new BarcodePattern(new int[]{3, 1, 1, 2}, false), new BarcodeDigit("9", "O"));
        digupc.put(new BarcodePattern(new int[]{3, 2, 1, 1}, true), new BarcodeDigit("0", "E"));
        digupc.put(new BarcodePattern(new int[]{2, 2, 2, 1}, true), new BarcodeDigit("1", "E"));
        digupc.put(new BarcodePattern(new int[]{2, 1, 2, 2}, true), new BarcodeDigit("2", "E"));
        digupc.put(new BarcodePattern(new int[]{1, 4, 1, 1}, true), new BarcodeDigit("3", "E"));
        digupc.put(new BarcodePattern(new int[]{1, 1, 3, 2}, true), new BarcodeDigit("4", "E"));
        digupc.put(new BarcodePattern(new int[]{1, 2, 3, 1}, true), new BarcodeDigit("5", "E"));
        digupc.put(new BarcodePattern(new int[]{1, 1, 1, 4}, true), new BarcodeDigit("6", "E"));
        digupc.put(new BarcodePattern(new int[]{1, 3, 1, 2}, true), new BarcodeDigit("7", "E"));
        digupc.put(new BarcodePattern(new int[]{1, 2, 1, 3}, true), new BarcodeDigit("8", "E"));
        digupc.put(new BarcodePattern(new int[]{3, 1, 1, 2}, true), new BarcodeDigit("9", "E"));
    }
}
