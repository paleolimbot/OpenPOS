package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dewey on 2016-10-02.
 */

public class UPCSpec extends ISBNUPCSpec {

    public UPCSpec() {
        super("UPC", digupc);
    }

    private boolean checksum(Barcode b) {
        int[] numbers = new int[b.digits.size()];
        for(int i=0; i<numbers.length; i++) {
            numbers[i] = Integer.valueOf(b.digits.get(i).digit);
        }
        int oddsum = 0;
        for(int i=0; i<numbers.length-1; i+=2) {
            oddsum += numbers[i];
        }
        int evensum = 0;
        for(int i=1; i<numbers.length; i+=2) {
            evensum += numbers[i];
        }
        int s1 = evensum + oddsum * 3;
        int checksum = 10*(s1/10+1) - s1;
        if(checksum == 10) checksum = 0;
        return checksum == numbers[numbers.length-1];
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = parse_common(bars) ;


        return b;
    }


    private static Map<BarcodePattern, BarcodeDigit> digupc = new HashMap<>();
    static {
        digupc.put(new BarcodePattern(new int[]{3, 2, 1, 1}, false), new BarcodeDigit("0", "op"));
        digupc.put(new BarcodePattern(new int[]{2, 2, 2, 1}, false), new BarcodeDigit("1", "op"));
        digupc.put(new BarcodePattern(new int[]{2, 1, 2, 2}, false), new BarcodeDigit("2", "op"));
        digupc.put(new BarcodePattern(new int[]{1, 4, 1, 1}, false), new BarcodeDigit("3", "op"));
        digupc.put(new BarcodePattern(new int[]{1, 1, 3, 2}, false), new BarcodeDigit("4", "op"));
        digupc.put(new BarcodePattern(new int[]{1, 2, 3, 1}, false), new BarcodeDigit("5", "op"));
        digupc.put(new BarcodePattern(new int[]{1, 1, 1, 4}, false), new BarcodeDigit("6", "op"));
        digupc.put(new BarcodePattern(new int[]{1, 3, 1, 2}, false), new BarcodeDigit("7", "op"));
        digupc.put(new BarcodePattern(new int[]{1, 2, 1, 3}, false), new BarcodeDigit("8", "op"));
        digupc.put(new BarcodePattern(new int[]{3, 1, 1, 2}, false), new BarcodeDigit("9", "op"));
        digupc.put(new BarcodePattern(new int[]{3, 2, 1, 1}, true), new BarcodeDigit("0", "ep"));
        digupc.put(new BarcodePattern(new int[]{2, 2, 2, 1}, true), new BarcodeDigit("1", "ep"));
        digupc.put(new BarcodePattern(new int[]{2, 1, 2, 2}, true), new BarcodeDigit("2", "ep"));
        digupc.put(new BarcodePattern(new int[]{1, 4, 1, 1}, true), new BarcodeDigit("3", "ep"));
        digupc.put(new BarcodePattern(new int[]{1, 1, 3, 2}, true), new BarcodeDigit("4", "ep"));
        digupc.put(new BarcodePattern(new int[]{1, 2, 3, 1}, true), new BarcodeDigit("5", "ep"));
        digupc.put(new BarcodePattern(new int[]{1, 1, 1, 4}, true), new BarcodeDigit("6", "ep"));
        digupc.put(new BarcodePattern(new int[]{1, 3, 1, 2}, true), new BarcodeDigit("7", "ep"));
        digupc.put(new BarcodePattern(new int[]{1, 2, 1, 3}, true), new BarcodeDigit("8", "ep"));
        digupc.put(new BarcodePattern(new int[]{3, 1, 1, 2}, true), new BarcodeDigit("9", "ep"));
    }
}
