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

    protected boolean checksum(Barcode b) {
        int[] numbers = new int[b.digits.size()];
        for(int i=0; i<numbers.length; i++) {
            numbers[i] = Integer.valueOf(b.digits.get(i).digit);
        }
        int oddsum = 0;
        for(int i=0; i<numbers.length; i+=2) {
            oddsum += numbers[i];
        }
        int evensum = 0;
        for(int i=1; i<numbers.length-1; i+=2) {
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

        int validDigits = 0;
        for(int i=0; i<b.digits.size(); i++) {
            if(b.digits.get(i) != null)
                validDigits++;
        }
        b.validDigits = validDigits;

        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        //try checksum
        if(!this.checksum(b)) throw new BarcodeException("Checksum failed for barcode", b);
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
